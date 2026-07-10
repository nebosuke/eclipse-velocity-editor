package io.github.nebosuke.velocityeditor.assist;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

import io.github.nebosuke.velocityeditor.Activator;
import io.github.nebosuke.velocityeditor.Messages;
import io.github.nebosuke.velocityeditor.preferences.PreferenceConstants;

/**
 * Content assist processor for Velocity templates
 * Provides auto-completion for VTL directives, variables, and HTML tags
 */
public class VelocityContentAssistProcessor implements IContentAssistProcessor {

    private static final Pattern MACRO_DEFINITION_PATTERN =
            Pattern.compile("#macro\\s*\\(\\s*([A-Za-z_][A-Za-z0-9_]*)", Pattern.CASE_INSENSITIVE);
    private static final long PROJECT_MACRO_CACHE_TTL_MILLIS = 10_000L;
    private static final ConcurrentMap<String, MacroDescriptionCacheEntry> PROJECT_MACRO_DESCRIPTION_CACHE =
            new ConcurrentHashMap<>();

    private static final class MacroDescriptionCacheEntry {
        private volatile Map<String, String> descriptions = Collections.emptyMap();
        private volatile Map<String, String> invocations = Collections.emptyMap();
        private volatile long lastUpdatedMillis = 0L;
        private final AtomicBoolean refreshing = new AtomicBoolean(false);
    }

    private IProject project;
    private final JavaScriptFunctionResolver javaScriptFunctionResolver = new JavaScriptFunctionResolver();

    public VelocityContentAssistProcessor() {
        this(null);
    }

    public VelocityContentAssistProcessor(IProject project) {
        this.project = project;
    }

    public void setProject(IProject project) {
        this.project = project;
    }

    // VTL Directive templates
    private static final String[][] VTL_DIRECTIVES = {
        { "#if", "#if($condition)\n\t\n#end", "Conditional statement" },
        { "#elseif", "#elseif($condition)\n\t", "Else-if condition" },
        { "#else", "#else\n\t", "Else clause" },
        { "#end", "#end", "End block" },
        { "#foreach", "#foreach($item in $collection)\n\t$item\n#end", "Loop through collection" },
        { "#set", "#set($variable = value)", "Set variable" },
        { "#macro", "#macro(name $arg1 $arg2)\n\t\n#end", "Define macro" },
        { "#parse", "#parse(\"template.vm\")", "Parse and include template" },
        { "#include", "#include(\"file.txt\")", "Include file as-is" },
        { "#stop", "#stop", "Stop template processing" },
        { "#break", "#break", "Break from foreach loop" },
        { "#evaluate", "#evaluate($dynamicVTL)", "Evaluate VTL string" },
        { "#define", "#define($block)\n\t\n#end", "Define reusable block" },
        { "#literal", "#literal()\n\t\n#end", "Output literal text (no VTL processing)" }
    };

    // Built-in VTL variables
    private static final String[][] BUILTIN_VTL_VARIABLES = {
        { "$foreach.count", "$foreach.count", "Current iteration count (1-based)" },
        { "$foreach.index", "$foreach.index", "Current iteration index (0-based)" },
        { "$foreach.hasNext", "$foreach.hasNext", "True if more elements" },
        { "$foreach.first", "$foreach.first", "True if first iteration" },
        { "$foreach.last", "$foreach.last", "True if last iteration" }
    };

    // Common HTML tags
    private static final String[][] HTML_TAGS = {
        { "<div>", "<div>\n\t\n</div>", "Division container" },
        { "<span>", "<span></span>", "Inline container" },
        { "<p>", "<p></p>", "Paragraph" },
        { "<a>", "<a href=\"\"></a>", "Anchor/link" },
        { "<img>", "<img src=\"\" alt=\"\" />", "Image" },
        { "<ul>", "<ul>\n\t<li></li>\n</ul>", "Unordered list" },
        { "<ol>", "<ol>\n\t<li></li>\n</ol>", "Ordered list" },
        { "<li>", "<li></li>", "List item" },
        { "<table>", "<table>\n\t<tr>\n\t\t<td></td>\n\t</tr>\n</table>", "Table" },
        { "<tr>", "<tr>\n\t<td></td>\n</tr>", "Table row" },
        { "<td>", "<td></td>", "Table cell" },
        { "<th>", "<th></th>", "Table header cell" },
        { "<form>", "<form action=\"\" method=\"post\">\n\t\n</form>", "Form" },
        { "<input>", "<input type=\"text\" name=\"\" />", "Input field" },
        { "<button>", "<button type=\"button\"></button>", "Button" },
        { "<select>", "<select name=\"\">\n\t<option value=\"\"></option>\n</select>", "Dropdown" },
        { "<textarea>", "<textarea name=\"\" rows=\"4\" cols=\"50\"></textarea>", "Text area" },
        { "<h1>", "<h1></h1>", "Heading 1" },
        { "<h2>", "<h2></h2>", "Heading 2" },
        { "<h3>", "<h3></h3>", "Heading 3" },
        { "<header>", "<header>\n\t\n</header>", "Header section" },
        { "<footer>", "<footer>\n\t\n</footer>", "Footer section" },
        { "<nav>", "<nav>\n\t\n</nav>", "Navigation section" },
        { "<main>", "<main>\n\t\n</main>", "Main content" },
        { "<section>", "<section>\n\t\n</section>", "Section" },
        { "<article>", "<article>\n\t\n</article>", "Article" },
        { "<aside>", "<aside>\n\t\n</aside>", "Sidebar content" },
        { "<script>", "<script>\n\t\n</script>", "JavaScript" },
        { "<style>", "<style>\n\t\n</style>", "CSS styles" },
        { "<link>", "<link rel=\"stylesheet\" href=\"\" />", "External stylesheet" },
        { "<meta>", "<meta name=\"\" content=\"\" />", "Meta information" }
    };

    @Override
    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
        List<ICompletionProposal> proposals = new ArrayList<>();
        IDocument document = viewer.getDocument();

        try {
            String documentText = document.get();
            boolean inScriptBlock = javaScriptFunctionResolver.isOffsetInScriptBlock(documentText, offset);

            // Get the text before cursor to determine context
            String prefix = getPrefix(document, offset);
            int prefixLength = prefix.length();
            int replacementOffset = offset - prefixLength;

            if (inScriptBlock && !prefix.startsWith("#") && !prefix.startsWith("$") && !prefix.startsWith("<")) {
                String javaScriptPrefix = getJavaScriptPrefix(documentText, offset);
                int javaScriptReplacementOffset = offset - javaScriptPrefix.length();
                addJavaScriptFunctionProposals(proposals, documentText, javaScriptPrefix,
                        javaScriptReplacementOffset, javaScriptPrefix.length(), offset);
            }

            // VTL Directives (triggered by #)
            if (prefix.startsWith("#") || prefix.isEmpty()) {
                Set<String> existingDirectives = new LinkedHashSet<>();
                for (String[] directive : VTL_DIRECTIVES) {
                    if (directive[0].toLowerCase().startsWith(prefix.toLowerCase()) || prefix.isEmpty()) {
                        existingDirectives.add(directive[0]);
                        proposals.add(createProposal(directive[0], directive[1], directive[2],
                                replacementOffset, prefixLength, offset));
                    }
                }

                Set<String> macroNames = new LinkedHashSet<>();
                Map<String, String> macroDescriptions = buildMacroDescriptionsFast(document.get(), project);
                Map<String, String> macroInvocations = buildMacroInvocationsFast(document.get(), project);
                macroNames.addAll(macroDescriptions.keySet());
                macroNames.addAll(macroInvocations.keySet());

                for (String macroName : macroNames) {
                    String macroDirective = "#" + macroName;
                    if (existingDirectives.contains(macroDirective)) {
                        continue;
                    }
                    if (macroDirective.toLowerCase().startsWith(prefix.toLowerCase()) || prefix.isEmpty()) {
                        String replacement = macroInvocations.getOrDefault(macroName, macroDirective);
                        proposals.add(createProposal(macroDirective, replacement,
                                macroDescriptions.getOrDefault(macroName, Messages.MACRO_PROJECT_DEFINED),
                                replacementOffset, prefixLength, offset));
                    }
                }
            }

            // VTL Variables (triggered by $)
            if (prefix.startsWith("$") || prefix.isEmpty()) {
                for (String[] variable : getVtlVariables()) {
                    if (variable[0].toLowerCase().startsWith(prefix.toLowerCase()) || prefix.isEmpty()) {
                        proposals.add(createProposal(variable[0], variable[1], variable[2],
                                replacementOffset, prefixLength, offset));
                    }
                }
            }

            // HTML Tags (triggered by <)
            if (prefix.startsWith("<") || prefix.isEmpty()) {
                for (String[] tag : HTML_TAGS) {
                    if (tag[0].toLowerCase().startsWith(prefix.toLowerCase()) || prefix.isEmpty()) {
                        proposals.add(createProposal(tag[0], tag[1], tag[2],
                                replacementOffset, prefixLength, offset));
                    }
                }
            }

            // HTML closing tags (triggered by </)
            if (prefix.startsWith("</")) {
                String[] closingTags = { "</div>", "</span>", "</p>", "</a>", "</ul>", "</ol>", 
                        "</li>", "</table>", "</tr>", "</td>", "</th>", "</form>", 
                        "</select>", "</textarea>", "</h1>", "</h2>", "</h3>",
                        "</header>", "</footer>", "</nav>", "</main>", "</section>",
                        "</article>", "</aside>", "</script>", "</style>" };
                for (String tag : closingTags) {
                    if (tag.toLowerCase().startsWith(prefix.toLowerCase())) {
                        proposals.add(new CompletionProposal(tag, replacementOffset, prefixLength,
                                tag.length(), null, tag, null, "Closing tag"));
                    }
                }
            }

        } catch (BadLocationException e) {
            // Ignore and return empty proposals
        }

        return proposals.toArray(new ICompletionProposal[proposals.size()]);
    }

    /**
     * Get the prefix (partial text) before the cursor
     */
    private String getPrefix(IDocument document, int offset) throws BadLocationException {
        int start = offset;
        while (start > 0) {
            char c = document.getChar(start - 1);
            if (Character.isWhitespace(c) || c == '\n' || c == '\r') {
                break;
            }
            // Stop at certain delimiters but include them in prefix
            if (c == '#' || c == '$' || c == '<') {
                start--;
                break;
            }
            start--;
        }
        return document.get(start, offset - start);
    }

    private List<String[]> getVtlVariables() {
        List<String[]> variables = new ArrayList<>();

        for (String variableName : parseVariableNames(getConfiguredVariablesRaw())) {
            String variable = "$" + variableName;
            variables.add(new String[] { variable, variable, "Custom variable" });
        }

        for (String[] builtinVariable : BUILTIN_VTL_VARIABLES) {
            variables.add(builtinVariable);
        }

        return variables;
    }

    private String getConfiguredVariablesRaw() {
        if (Activator.getDefault() == null) {
            return PreferenceConstants.DEFAULT_CONTENT_ASSIST_VARIABLES;
        }
        return Activator.getDefault().getPreferenceStore()
                .getString(PreferenceConstants.CONTENT_ASSIST_VARIABLES);
    }

    static List<String> parseVariableNames(String rawVariables) {
        Set<String> variableNames = new LinkedHashSet<>();
        if (rawVariables == null || rawVariables.isBlank()) {
            return new ArrayList<>();
        }

        String[] tokens = rawVariables.split("[\\r\\n,]+");
        for (String token : tokens) {
            String normalized = normalizeVariableName(token);
            if (isValidVariableName(normalized)) {
                variableNames.add(normalized);
            }
        }

        return new ArrayList<>(variableNames);
    }

    static List<String> parseMacroNames(String templateText) {
        Set<String> macroNames = new LinkedHashSet<>();
        if (templateText == null || templateText.isBlank()) {
            return new ArrayList<>();
        }

        Matcher matcher = MACRO_DEFINITION_PATTERN.matcher(templateText);
        while (matcher.find()) {
            String macroName = matcher.group(1);
            if (macroName != null && !macroName.isBlank()) {
                macroNames.add(macroName);
            }
        }

        return new ArrayList<>(macroNames);
    }

    static List<String> collectProjectAndReferencedMacroNames(IProject project) {
        Set<String> macroNames = new LinkedHashSet<>();
        for (IProject macroProject : collectMacroProjects(project)) {
            macroNames.addAll(collectSingleProjectMacroNames(macroProject));
        }
        return new ArrayList<>(macroNames);
    }

    static Map<String, String> collectMacroDescriptions(String currentDocumentText, IProject project) {
        Map<String, String> descriptions = new LinkedHashMap<>();

        for (MacroDefinitionResolver.MacroDefinition definition
                : MacroDefinitionResolver.parseDefinitions(currentDocumentText, null)) {
            descriptions.putIfAbsent(definition.getName(), buildMacroDescription(definition));
        }

        for (IProject macroProject : collectMacroProjects(project)) {
            for (MacroDefinitionResolver.MacroDefinition definition : collectSingleProjectMacroDefinitions(macroProject)) {
                descriptions.putIfAbsent(definition.getName(), buildMacroDescription(definition));
            }
        }

        return descriptions;
    }

    private Map<String, String> buildMacroDescriptionsFast(String currentDocumentText, IProject project) {
        Map<String, String> descriptions = new LinkedHashMap<>();

        for (MacroDefinitionResolver.MacroDefinition definition
                : MacroDefinitionResolver.parseDefinitions(currentDocumentText, null)) {
            descriptions.putIfAbsent(definition.getName(), buildMacroDescription(definition));
        }

        for (Map.Entry<String, String> entry : getCachedProjectMacroDescriptions(project).entrySet()) {
            descriptions.putIfAbsent(entry.getKey(), entry.getValue());
        }

        return descriptions;
    }

    private Map<String, String> buildMacroInvocationsFast(String currentDocumentText, IProject project) {
        Map<String, String> invocations = new LinkedHashMap<>();

        for (MacroDefinitionResolver.MacroDefinition definition
                : MacroDefinitionResolver.parseDefinitions(currentDocumentText, null)) {
            invocations.putIfAbsent(definition.getName(), buildMacroInvocation(definition));
        }

        for (Map.Entry<String, String> entry : getCachedProjectMacroInvocations(project).entrySet()) {
            invocations.putIfAbsent(entry.getKey(), entry.getValue());
        }

        return invocations;
    }

    private Map<String, String> getCachedProjectMacroDescriptions(IProject project) {
        if (project == null || !project.isAccessible()) {
            return Collections.emptyMap();
        }

        String cacheKey = buildProjectCacheKey(project);
        MacroDescriptionCacheEntry entry = PROJECT_MACRO_DESCRIPTION_CACHE.computeIfAbsent(
                cacheKey, key -> new MacroDescriptionCacheEntry());

        if (isCacheExpired(entry.lastUpdatedMillis)) {
            scheduleProjectMacroRefresh(project, entry);
        }

        return entry.descriptions;
    }

    private Map<String, String> getCachedProjectMacroInvocations(IProject project) {
        if (project == null || !project.isAccessible()) {
            return Collections.emptyMap();
        }

        String cacheKey = buildProjectCacheKey(project);
        MacroDescriptionCacheEntry entry = PROJECT_MACRO_DESCRIPTION_CACHE.computeIfAbsent(
                cacheKey, key -> new MacroDescriptionCacheEntry());

        if (isCacheExpired(entry.lastUpdatedMillis)) {
            scheduleProjectMacroRefresh(project, entry);
        }

        return entry.invocations;
    }

    private static String buildProjectCacheKey(IProject project) {
        if (project == null) {
            return "";
        }
        return project.getName() + "::" + project.getFullPath();
    }

    private static boolean isCacheExpired(long lastUpdatedMillis) {
        if (lastUpdatedMillis <= 0L) {
            return true;
        }
        return System.currentTimeMillis() - lastUpdatedMillis > PROJECT_MACRO_CACHE_TTL_MILLIS;
    }

    private void scheduleProjectMacroRefresh(IProject project, MacroDescriptionCacheEntry entry) {
        if (project == null || !project.isAccessible()) {
            return;
        }
        if (!entry.refreshing.compareAndSet(false, true)) {
            return;
        }

        Job refreshJob = new Job(Messages.MACRO_REFRESH_JOB_NAME) {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    Map<String, String> refreshedDescriptions = collectProjectMacroDescriptionsSync(project);
                    Map<String, String> refreshedInvocations = collectProjectMacroInvocationsSync(project);
                    entry.descriptions = Collections.unmodifiableMap(refreshedDescriptions);
                    entry.invocations = Collections.unmodifiableMap(refreshedInvocations);
                    entry.lastUpdatedMillis = System.currentTimeMillis();
                    return Status.OK_STATUS;
                } finally {
                    entry.refreshing.set(false);
                }
            }
        };
        refreshJob.setSystem(true);
        refreshJob.schedule();
    }

    private Map<String, String> collectProjectMacroDescriptionsSync(IProject project) {
        Map<String, String> descriptions = new HashMap<>();
        for (IProject macroProject : collectMacroProjects(project)) {
            for (MacroDefinitionResolver.MacroDefinition definition : collectSingleProjectMacroDefinitions(macroProject)) {
                descriptions.putIfAbsent(definition.getName(), buildMacroDescription(definition));
            }
        }
        return descriptions;
    }

    private Map<String, String> collectProjectMacroInvocationsSync(IProject project) {
        Map<String, String> invocations = new HashMap<>();
        for (IProject macroProject : collectMacroProjects(project)) {
            for (MacroDefinitionResolver.MacroDefinition definition : collectSingleProjectMacroDefinitions(macroProject)) {
                invocations.putIfAbsent(definition.getName(), buildMacroInvocation(definition));
            }
        }
        return invocations;
    }

    static List<IProject> collectMacroProjects(IProject project) {
        Set<IProject> projects = new LinkedHashSet<>();
        collectMacroProjectsRecursive(project, projects);
        return new ArrayList<>(projects);
    }

    private static void collectMacroProjectsRecursive(IProject project, Set<IProject> projects) {
        if (project == null || !project.isAccessible() || projects.contains(project)) {
            return;
        }
        projects.add(project);

        for (IProject referencedProject : getReferencedProjects(project)) {
            collectMacroProjectsRecursive(referencedProject, projects);
        }
    }

    private static List<IProject> getReferencedProjects(IProject project) {
        try {
            IProject[] referencedProjects = project.getReferencedProjects();
            if (referencedProjects == null || referencedProjects.length == 0) {
                return Collections.emptyList();
            }
            List<IProject> result = new ArrayList<>();
            for (IProject referencedProject : referencedProjects) {
                if (referencedProject != null) {
                    result.add(referencedProject);
                }
            }
            return result;
        } catch (CoreException e) {
            return Collections.emptyList();
        }
    }

    private static List<String> collectSingleProjectMacroNames(IProject project) {
        Set<String> macroNames = new LinkedHashSet<>();
        if (project == null || !project.isAccessible()) {
            return new ArrayList<>();
        }

        try {
            project.accept(new IResourceVisitor() {
                @Override
                public boolean visit(IResource resource) throws CoreException {
                    if (resource.getType() != IResource.FILE) {
                        return true;
                    }

                    IFile file = (IFile) resource;
                    if (!isVelocityTemplateFileName(file.getName())) {
                        return false;
                    }

                    for (String macroName : parseMacroNames(readFileContent(file))) {
                        macroNames.add(macroName);
                    }
                    return false;
                }
            });
        } catch (CoreException e) {
            // Ignore and return already collected macro names.
        }

        return new ArrayList<>(macroNames);
    }

    private static List<MacroDefinitionResolver.MacroDefinition> collectSingleProjectMacroDefinitions(IProject project) {
        List<MacroDefinitionResolver.MacroDefinition> definitions = new ArrayList<>();
        if (project == null || !project.isAccessible()) {
            return definitions;
        }

        try {
            project.accept(new IResourceVisitor() {
                @Override
                public boolean visit(IResource resource) throws CoreException {
                    if (resource.getType() != IResource.FILE) {
                        return true;
                    }

                    IFile file = (IFile) resource;
                    if (!isVelocityTemplateFileName(file.getName())) {
                        return false;
                    }

                    definitions.addAll(MacroDefinitionResolver.parseDefinitions(readFileContent(file), file));
                    return false;
                }
            });
        } catch (CoreException e) {
            // Ignore and return already collected macro definitions.
        }

        return definitions;
    }

    private static String buildMacroDescription(MacroDefinitionResolver.MacroDefinition definition) {
        String args = definition.getArguments().isEmpty()
                ? Messages.MACRO_ARGS_NONE
                : String.join(", ", definition.getArguments());
        String comment = definition.getComment() == null || definition.getComment().isBlank()
                ? Messages.MACRO_COMMENT_NONE
                : MacroCommentFormatter.format(definition.getComment());

        return Messages.MACRO_ARGS_LABEL + " " + args + "\n" + comment;
    }

    private static String buildMacroInvocation(MacroDefinitionResolver.MacroDefinition definition) {
        String macroDirective = "#" + definition.getName();
        if (definition.getArguments().isEmpty()) {
            return macroDirective;
        }
        return macroDirective + "(" + String.join(" ", definition.getArguments()) + ")";
    }

    private void addJavaScriptFunctionProposals(List<ICompletionProposal> proposals, String documentText,
            String prefix, int replacementOffset, int replacementLength, int cursorOffset) {
        for (JavaScriptFunctionResolver.JavaScriptFunctionDefinition definition
                : javaScriptFunctionResolver.parseDefinitions(documentText)) {
            if (!(definition.getName().toLowerCase().startsWith(prefix.toLowerCase()) || prefix.isEmpty())) {
                continue;
            }
            proposals.add(createProposal(definition.getName(),
                    buildJavaScriptInvocation(definition),
                    buildJavaScriptDescription(definition),
                    replacementOffset, replacementLength, cursorOffset));
        }
    }

    private static String buildJavaScriptDescription(JavaScriptFunctionResolver.JavaScriptFunctionDefinition definition) {
        String args = definition.getArguments().isEmpty()
                ? Messages.MACRO_ARGS_NONE
                : String.join(", ", definition.getArguments());
        String comment = definition.getComment() == null || definition.getComment().isBlank()
                ? Messages.JAVASCRIPT_FUNCTION_DEFINED
                : MacroCommentFormatter.format(definition.getComment());
        return Messages.MACRO_ARGS_LABEL + " " + args + "\n" + comment;
    }

    private static String buildJavaScriptInvocation(JavaScriptFunctionResolver.JavaScriptFunctionDefinition definition) {
        if (definition.getArguments().isEmpty()) {
            return definition.getName() + "()";
        }
        return definition.getName() + "(" + String.join(", ", definition.getArguments()) + ")";
    }

    private static String getJavaScriptPrefix(String text, int offset) {
        if (text == null || text.isEmpty() || offset <= 0) {
            return "";
        }
        int safeOffset = Math.min(offset, text.length());
        int start = safeOffset;
        while (start > 0 && isJavaScriptIdentifierPart(text.charAt(start - 1))) {
            start--;
        }
        return text.substring(start, safeOffset);
    }

    private static boolean isJavaScriptIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    static boolean isVelocityTemplateFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return false;
        }
        String lowerCase = fileName.toLowerCase();
        return lowerCase.endsWith(".vm") || lowerCase.endsWith(".vtl");
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

    private static String normalizeVariableName(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.startsWith("$!{")) {
            normalized = normalized.substring(3);
        } else if (normalized.startsWith("${")) {
            normalized = normalized.substring(2);
        } else if (normalized.startsWith("$!")) {
            normalized = normalized.substring(2);
        } else if (normalized.startsWith("$")) {
            normalized = normalized.substring(1);
        }
        if (normalized.endsWith("}")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.trim();
    }

    private static boolean isValidVariableName(String variableName) {
        return variableName != null && variableName.matches("[A-Za-z_][A-Za-z0-9_]*");
    }

    /**
     * Create a completion proposal
     */
    private ICompletionProposal createProposal(String displayString, String replacementString,
            String description, int replacementOffset, int replacementLength, int cursorOffset) {
        
        // Calculate cursor position (find first placeholder or end)
        int cursorPosition = replacementString.length();
        int placeholderPos = replacementString.indexOf("$");
        if (placeholderPos == -1) {
            placeholderPos = replacementString.indexOf("\"\"");
            if (placeholderPos != -1) {
                cursorPosition = placeholderPos + 1;
            }
        }

        return new CompletionProposal(
                replacementString,
                replacementOffset,
                replacementLength,
                cursorPosition,
                null,
                displayString,
                null,
                description
        );
    }

    @Override
    public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
        return null;
    }

    @Override
    public char[] getCompletionProposalAutoActivationCharacters() {
        StringBuilder activationChars = new StringBuilder();
        activationChars.append("#$<");
        for (char c = 'a'; c <= 'z'; c++) {
            activationChars.append(c);
        }
        for (char c = 'A'; c <= 'Z'; c++) {
            activationChars.append(c);
        }
        activationChars.append("_$");
        return activationChars.toString().toCharArray();
    }

    @Override
    public char[] getContextInformationAutoActivationCharacters() {
        return null;
    }

    @Override
    public String getErrorMessage() {
        return null;
    }

    @Override
    public IContextInformationValidator getContextInformationValidator() {
        return null;
    }
}
