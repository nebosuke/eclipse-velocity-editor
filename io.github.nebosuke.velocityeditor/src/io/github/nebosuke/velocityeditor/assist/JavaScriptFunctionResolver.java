package io.github.nebosuke.velocityeditor.assist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

/**
 * Resolves JavaScript function definitions in &lt;script&gt; blocks.
 */
public class JavaScriptFunctionResolver {

    private static final Pattern SCRIPT_BLOCK_PATTERN = Pattern.compile(
            "<script\\b[^>]*>(.*?)</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern FUNCTION_DECLARATION_PATTERN = Pattern.compile(
            "(?m)^[\\t ]*function\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\(([^)]*)\\)");
    private static final Pattern FUNCTION_EXPRESSION_PATTERN = Pattern.compile(
            "(?m)^[\\t ]*(?:const|let|var)\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*=\\s*function\\s*\\(([^)]*)\\)");
    private static final Pattern ARROW_FUNCTION_PATTERN = Pattern.compile(
            "(?m)^[\\t ]*(?:const|let|var)\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*=\\s*(?:\\(([^)]*)\\)|([A-Za-z_$][A-Za-z0-9_$]*))\\s*=>");

    /**
     * Information of a JavaScript function definition.
     */
    public static final class JavaScriptFunctionDefinition {
        private final String name;
        private final List<String> arguments;
        private final String comment;
        private final int offset;
        private final int length;

        JavaScriptFunctionDefinition(String name, List<String> arguments, String comment,
                int offset, int length) {
            this.name = name;
            this.arguments = arguments;
            this.comment = comment;
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

        public int getOffset() {
            return offset;
        }

        public int getLength() {
            return length;
        }
    }

    public List<JavaScriptFunctionDefinition> parseDefinitions(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        Map<String, JavaScriptFunctionDefinition> uniqueByName = new LinkedHashMap<>();
        Matcher scriptMatcher = SCRIPT_BLOCK_PATTERN.matcher(text);
        while (scriptMatcher.find()) {
            String scriptBody = scriptMatcher.group(1);
            int scriptBodyOffset = scriptMatcher.start(1);
            collectDefinitions(scriptBody, scriptBodyOffset, text, uniqueByName,
                    FUNCTION_DECLARATION_PATTERN, 1, 2);
            collectDefinitions(scriptBody, scriptBodyOffset, text, uniqueByName,
                    FUNCTION_EXPRESSION_PATTERN, 1, 2);
            collectArrowDefinitions(scriptBody, scriptBodyOffset, text, uniqueByName);
        }

        return new ArrayList<>(uniqueByName.values());
    }

    public boolean isOffsetInScriptBlock(String text, int offset) {
        if (text == null || text.isBlank() || offset < 0) {
            return false;
        }

        Matcher scriptMatcher = SCRIPT_BLOCK_PATTERN.matcher(text);
        while (scriptMatcher.find()) {
            int start = scriptMatcher.start(1);
            int end = scriptMatcher.end(1);
            if (offset >= start && offset <= end) {
                return true;
            }
        }
        return false;
    }

    public JavaScriptFunctionDefinition resolve(String functionName, IDocument document) {
        if (functionName == null || functionName.isBlank() || document == null) {
            return null;
        }

        for (JavaScriptFunctionDefinition definition : parseDefinitions(document.get())) {
            if (definition.getName().equals(functionName)) {
                return definition;
            }
        }
        return null;
    }

    public String findFunctionNameAt(IDocument document, int offset) {
        if (document == null || offset < 0 || document.getLength() == 0) {
            return null;
        }

        try {
            int cursor = Math.min(offset, document.getLength() - 1);
            char current = document.getChar(cursor);
            if (!isFunctionNamePart(current)) {
                if (cursor == 0) {
                    return null;
                }
                cursor--;
                current = document.getChar(cursor);
                if (!isFunctionNamePart(current)) {
                    return null;
                }
            }

            int start = cursor;
            while (start > 0 && isFunctionNamePart(document.getChar(start - 1))) {
                start--;
            }

            int end = cursor + 1;
            while (end < document.getLength() && isFunctionNamePart(document.getChar(end))) {
                end++;
            }

            if (end <= start) {
                return null;
            }

            int lookahead = end;
            while (lookahead < document.getLength() && Character.isWhitespace(document.getChar(lookahead))) {
                lookahead++;
            }
            if (lookahead >= document.getLength() || document.getChar(lookahead) != '(') {
                return null;
            }

            return document.get(start, end - start);
        } catch (BadLocationException e) {
            return null;
        }
    }

    public int[] findInvocationRegion(IDocument document, int offset) {
        if (document == null || offset < 0 || document.getLength() == 0) {
            return null;
        }

        try {
            int cursor = Math.min(offset, document.getLength() - 1);
            char current = document.getChar(cursor);
            if (!isFunctionNamePart(current)) {
                if (cursor == 0) {
                    return null;
                }
                cursor--;
                current = document.getChar(cursor);
                if (!isFunctionNamePart(current)) {
                    return null;
                }
            }

            int start = cursor;
            while (start > 0 && isFunctionNamePart(document.getChar(start - 1))) {
                start--;
            }
            int end = cursor + 1;
            while (end < document.getLength() && isFunctionNamePart(document.getChar(end))) {
                end++;
            }

            int lookahead = end;
            while (lookahead < document.getLength() && Character.isWhitespace(document.getChar(lookahead))) {
                lookahead++;
            }
            if (lookahead >= document.getLength() || document.getChar(lookahead) != '(') {
                return null;
            }
            return new int[] { start, end - start };
        } catch (BadLocationException e) {
            return null;
        }
    }

    private static void collectDefinitions(String scriptBody, int scriptBodyOffset, String fullText,
            Map<String, JavaScriptFunctionDefinition> definitions, Pattern pattern,
            int nameGroupIndex, int argsGroupIndex) {
        Matcher matcher = pattern.matcher(scriptBody);
        while (matcher.find()) {
            String functionName = matcher.group(nameGroupIndex);
            if (functionName == null || functionName.isBlank()) {
                continue;
            }

            int functionOffset = scriptBodyOffset + matcher.start();
            String comment = extractPrecedingLineComment(fullText, functionOffset);
            List<String> arguments = parseArguments(matcher.group(argsGroupIndex));
            definitions.putIfAbsent(functionName,
                    new JavaScriptFunctionDefinition(functionName, arguments, comment,
                            functionOffset, matcher.end() - matcher.start()));
        }
    }

    private static void collectArrowDefinitions(String scriptBody, int scriptBodyOffset, String fullText,
            Map<String, JavaScriptFunctionDefinition> definitions) {
        Matcher matcher = ARROW_FUNCTION_PATTERN.matcher(scriptBody);
        while (matcher.find()) {
            String functionName = matcher.group(1);
            if (functionName == null || functionName.isBlank()) {
                continue;
            }

            String rawArgs = matcher.group(2);
            if (rawArgs == null) {
                rawArgs = matcher.group(3);
            }

            int functionOffset = scriptBodyOffset + matcher.start();
            String comment = extractPrecedingLineComment(fullText, functionOffset);
            List<String> arguments = parseArguments(rawArgs);
            definitions.putIfAbsent(functionName,
                    new JavaScriptFunctionDefinition(functionName, arguments, comment,
                            functionOffset, matcher.end() - matcher.start()));
        }
    }

    private static List<String> parseArguments(String rawArgs) {
        if (rawArgs == null || rawArgs.isBlank()) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();
        String[] tokens = rawArgs.split(",");
        for (String token : tokens) {
            String value = token == null ? "" : token.trim();
            if (value.isEmpty()) {
                continue;
            }

            int defaultValueIndex = value.indexOf('=');
            if (defaultValueIndex >= 0) {
                value = value.substring(0, defaultValueIndex).trim();
            }
            if (!value.isEmpty()) {
                result.add(value);
            }
        }
        return result;
    }

    static String extractPrecedingLineComment(String text, int definitionStartOffset) {
        if (text == null || definitionStartOffset <= 0) {
            return "";
        }

        int definitionLineStart = text.lastIndexOf('\n', Math.max(0, definitionStartOffset - 1)) + 1;
        int cursor = definitionLineStart - 1;
        while (cursor >= 0 && (text.charAt(cursor) == '\n' || text.charAt(cursor) == '\r')) {
            cursor--;
        }
        if (cursor < 0) {
            return "";
        }

        int lineStart = text.lastIndexOf('\n', cursor) + 1;
        String previousLine = text.substring(lineStart, cursor + 1).trim();
        if (!previousLine.startsWith("##")) {
            return "";
        }

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

    private static boolean isFunctionNamePart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }
}