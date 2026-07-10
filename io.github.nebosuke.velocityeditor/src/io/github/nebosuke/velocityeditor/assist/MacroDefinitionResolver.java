package io.github.nebosuke.velocityeditor.assist;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

/**
 * Resolves Velocity {@code #macro} definitions from current/project templates.
 */
public class MacroDefinitionResolver {

    private static final Pattern MACRO_DEFINITION_PATTERN = Pattern.compile(
            "#macro\\s*\\(\\s*([A-Za-z_][A-Za-z0-9_]*)([^)]*)\\)",
            Pattern.CASE_INSENSITIVE);

    /**
     * Information of a macro definition.
     */
    public static final class MacroDefinition {
        private final String name;
        private final List<String> arguments;
        private final String comment;
        private final IFile file;
        private final int offset;
        private final int length;

        MacroDefinition(String name, List<String> arguments, String comment,
                IFile file, int offset, int length) {
            this.name = name;
            this.arguments = arguments;
            this.comment = comment;
            this.file = file;
            this.offset = offset;
            this.length = length;
        }

        public String getName() {
            return name;
        }

        public List<String> getArguments() {
            return arguments;
        }

        public String getComment() {
            return comment;
        }

        public IFile getFile() {
            return file;
        }

        public int getOffset() {
            return offset;
        }

        public int getLength() {
            return length;
        }
    }

    /**
     * Finds macro definition in current document first, then project/referenced projects.
     */
    public MacroDefinition resolve(String macroName, IDocument currentDocument,
            IFile currentFile, IProject project) {
        if (macroName == null || macroName.isBlank()) {
            return null;
        }

        MacroDefinition inCurrent = findInCurrentDocument(macroName, currentDocument, currentFile);
        if (inCurrent != null) {
            return inCurrent;
        }

        for (IProject targetProject : collectProjects(project)) {
            MacroDefinition inProject = findInProject(macroName, targetProject, currentFile);
            if (inProject != null) {
                return inProject;
            }
        }
        return null;
    }

    public String findMacroNameAt(IDocument document, int offset) {
        if (document == null || offset < 0) {
            return null;
        }
        try {
            int length = document.getLength();
            if (length == 0) {
                return null;
            }

            int cursor = Math.min(offset, length - 1);
            char current = document.getChar(cursor);
            if (!isMacroNamePart(current) && current != '#') {
                if (cursor == 0) {
                    return null;
                }
                cursor--;
                current = document.getChar(cursor);
                if (!isMacroNamePart(current) && current != '#') {
                    return null;
                }
            }

            int start = current == '#' ? cursor + 1 : cursor;
            while (start > 0 && isMacroNamePart(document.getChar(start - 1))) {
                start--;
            }

            int hashPos = start - 1;
            if (hashPos < 0 || document.getChar(hashPos) != '#') {
                return null;
            }

            int end = start;
            while (end < length && isMacroNamePart(document.getChar(end))) {
                end++;
            }

            if (end <= start) {
                return null;
            }

            String macroName = document.get(start, end - start);
            if (macroName.equalsIgnoreCase("macro")
                    || macroName.equalsIgnoreCase("if")
                    || macroName.equalsIgnoreCase("else")
                    || macroName.equalsIgnoreCase("elseif")
                    || macroName.equalsIgnoreCase("end")
                    || macroName.equalsIgnoreCase("foreach")
                    || macroName.equalsIgnoreCase("set")
                    || macroName.equalsIgnoreCase("parse")
                    || macroName.equalsIgnoreCase("include")
                    || macroName.equalsIgnoreCase("stop")
                    || macroName.equalsIgnoreCase("break")
                    || macroName.equalsIgnoreCase("evaluate")
                    || macroName.equalsIgnoreCase("define")
                    || macroName.equalsIgnoreCase("literal")) {
                return null;
            }
            return macroName;
        } catch (BadLocationException e) {
            return null;
        }
    }

    public int[] findInvocationRegion(IDocument document, int offset) {
        if (document == null || offset < 0) {
            return null;
        }

        try {
            int length = document.getLength();
            if (length == 0) {
                return null;
            }
            int cursor = Math.min(offset, length - 1);

            char current = document.getChar(cursor);
            if (!isMacroNamePart(current) && current != '#') {
                if (cursor == 0) {
                    return null;
                }
                cursor--;
                current = document.getChar(cursor);
                if (!isMacroNamePart(current) && current != '#') {
                    return null;
                }
            }

            int start = current == '#' ? cursor : cursor;
            while (start > 0 && isMacroNamePart(document.getChar(start - 1))) {
                start--;
            }
            if (start > 0 && document.getChar(start - 1) == '#') {
                start--;
            } else if (document.getChar(start) != '#') {
                return null;
            }

            int end = start + 1;
            while (end < length && isMacroNamePart(document.getChar(end))) {
                end++;
            }

            if (end <= start + 1) {
                return null;
            }

            return new int[] { start, end - start };
        } catch (BadLocationException e) {
            return null;
        }
    }

    private MacroDefinition findInCurrentDocument(String macroName, IDocument document, IFile file) {
        if (document == null || file == null) {
            return null;
        }
        try {
            for (MacroDefinition definition : parseDefinitions(document.get(), file)) {
                if (definition.getName().equals(macroName)) {
                    return definition;
                }
            }
        } catch (RuntimeException e) {
            return null;
        }
        return null;
    }

    private MacroDefinition findInProject(String macroName, IProject project, IFile currentFile) {
        if (project == null || !project.isAccessible()) {
            return null;
        }

        final MacroDefinition[] result = new MacroDefinition[1];
        try {
            project.accept(new IResourceVisitor() {
                @Override
                public boolean visit(IResource resource) throws CoreException {
                    if (result[0] != null) {
                        return false;
                    }
                    if (resource.getType() != IResource.FILE) {
                        return true;
                    }

                    IFile file = (IFile) resource;
                    if (currentFile != null && currentFile.equals(file)) {
                        return false;
                    }
                    if (!VelocityContentAssistProcessor.isVelocityTemplateFileName(file.getName())) {
                        return false;
                    }

                    for (MacroDefinition definition : parseDefinitions(readFileContent(file), file)) {
                        if (definition.getName().equals(macroName)) {
                            result[0] = definition;
                            return false;
                        }
                    }
                    return false;
                }
            });
        } catch (CoreException e) {
            return null;
        }
        return result[0];
    }

    private List<IProject> collectProjects(IProject project) {
        Set<IProject> projects = new LinkedHashSet<>();
        collectProjectsRecursive(project, projects);
        return new ArrayList<>(projects);
    }

    private void collectProjectsRecursive(IProject project, Set<IProject> projects) {
        if (project == null || !project.isAccessible() || projects.contains(project)) {
            return;
        }
        projects.add(project);

        try {
            for (IProject referencedProject : project.getReferencedProjects()) {
                collectProjectsRecursive(referencedProject, projects);
            }
        } catch (CoreException e) {
            // Ignore.
        }
    }

    static List<MacroDefinition> parseDefinitions(String text, IFile file) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        List<MacroDefinition> definitions = new ArrayList<>();
        Matcher matcher = MACRO_DEFINITION_PATTERN.matcher(text);
        while (matcher.find()) {
            String name = matcher.group(1);
            String args = matcher.group(2);
            List<String> arguments = parseArguments(args);
            String comment = extractPrecedingComment(text, matcher.start());
            definitions.add(new MacroDefinition(name, arguments, comment, file,
                    matcher.start(), matcher.end() - matcher.start()));
        }
        return definitions;
    }

    private static List<String> parseArguments(String rawArgs) {
        if (rawArgs == null || rawArgs.isBlank()) {
            return Collections.emptyList();
        }
        List<String> args = new ArrayList<>();
        Matcher argMatcher = Pattern.compile("\\$!?\\{?[A-Za-z_][A-Za-z0-9_]*\\}?").matcher(rawArgs);
        while (argMatcher.find()) {
            args.add(argMatcher.group());
        }
        return args;
    }

    static String extractPrecedingComment(String text, int macroStartOffset) {
        if (text == null || macroStartOffset <= 0) {
            return "";
        }

        int macroLineStart = text.lastIndexOf('\n', Math.max(0, macroStartOffset - 1)) + 1;
        int cursor = macroLineStart - 1;
        while (cursor >= 0 && (text.charAt(cursor) == '\n' || text.charAt(cursor) == '\r')) {
            cursor--;
        }
        if (cursor < 0) {
            return "";
        }

        int lineStart = text.lastIndexOf('\n', cursor) + 1;
        String previousLine = text.substring(lineStart, cursor + 1).trim();
        if (previousLine.startsWith("##")) {
            List<String> commentLines = new ArrayList<>();
            while (cursor >= 0) {
                lineStart = text.lastIndexOf('\n', cursor) + 1;
                String line = text.substring(lineStart, cursor + 1).trim();
                if (!line.startsWith("##")) {
                    break;
                }
                commentLines.add(0, line.substring(2).trim());
                cursor = lineStart - 1;
                while (cursor >= 0 && (text.charAt(cursor) == '\n' || text.charAt(cursor) == '\r')) {
                    cursor--;
                }
            }
            return String.join("\n", commentLines).trim();
        }

        int blockEnd = text.lastIndexOf("*#", macroStartOffset);
        if (blockEnd >= 0) {
            String betweenEndAndMacro = text.substring(blockEnd + 2, macroStartOffset);
            if (betweenEndAndMacro.trim().isEmpty()) {
                int blockStart = text.lastIndexOf("#*", blockEnd);
                if (blockStart >= 0 && blockStart < blockEnd) {
                    String block = text.substring(blockStart + 2, blockEnd);
                    return normalizeBlockComment(block);
                }
            }
        }

        return "";
    }

    private static String normalizeBlockComment(String blockComment) {
        if (blockComment == null || blockComment.isBlank()) {
            return "";
        }
        String[] lines = blockComment.split("\\r?\\n");
        List<String> normalized = new ArrayList<>();
        for (String line : lines) {
            String value = line == null ? "" : line.trim();
            if (value.startsWith("*")) {
                value = value.substring(1).trim();
            }
            normalized.add(value);
        }
        return String.join("\n", normalized).trim();
    }

    private static boolean isMacroNamePart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private static String readFileContent(IFile file) {
        try (InputStream stream = file.getContents()) {
            byte[] bytes = stream.readAllBytes();
            Charset charset = Charset.forName(file.getCharset(true));
            return new String(bytes, charset);
        } catch (CoreException | IOException | RuntimeException e) {
            return "";
        }
    }
}