package io.github.nebosuke.velocityeditor.assist;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formats macro comments written in Markdown into readable plain text.
 */
public final class MacroCommentFormatter {

    private static final Pattern HEADER_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$");
    private static final Pattern ORDERED_LIST_PATTERN = Pattern.compile("^(\\d+)\\.\\s+(.+)$");
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^\\]]+)]\\(([^)]+)\\)");
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)");
    private static final Pattern INLINE_CODE_PATTERN = Pattern.compile("`([^`]+)`");

    private MacroCommentFormatter() {
    }

    public static String format(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }

        String normalized = markdown.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\\n", -1);
        List<String> formatted = new ArrayList<>();

        boolean inCodeBlock = false;
        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();

            if (trimmed.startsWith("```")) {
                inCodeBlock = !inCodeBlock;
                continue;
            }

            if (inCodeBlock) {
                formatted.add("    " + trimmed);
                continue;
            }

            if (trimmed.isEmpty()) {
                appendEmptyLine(formatted);
                continue;
            }

            Matcher headerMatcher = HEADER_PATTERN.matcher(trimmed);
            if (headerMatcher.matches()) {
                int level = headerMatcher.group(1).length();
                String text = applyInlineMarkdown(headerMatcher.group(2).trim());
                formatted.add(formatHeading(level, text));
                continue;
            }

            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                formatted.add("• " + applyInlineMarkdown(trimmed.substring(2).trim()));
                continue;
            }

            Matcher orderedListMatcher = ORDERED_LIST_PATTERN.matcher(trimmed);
            if (orderedListMatcher.matches()) {
                formatted.add(orderedListMatcher.group(1) + ") "
                        + applyInlineMarkdown(orderedListMatcher.group(2).trim()));
                continue;
            }

            if (trimmed.startsWith(">")) {
                String quoteBody = trimmed.length() > 1 ? trimmed.substring(1).trim() : "";
                formatted.add("❝ " + applyInlineMarkdown(quoteBody));
                continue;
            }

            formatted.add(applyInlineMarkdown(trimmed));
        }

        return String.join("\n", formatted).trim();
    }

    private static void appendEmptyLine(List<String> lines) {
        if (!lines.isEmpty() && !lines.get(lines.size() - 1).isEmpty()) {
            lines.add("");
        }
    }

    private static String formatHeading(int level, String text) {
        if (level <= 1) {
            return "【" + text + "】";
        }
        return "■ " + text;
    }

    private static String applyInlineMarkdown(String line) {
        String value = line;
        value = replacePattern(value, LINK_PATTERN, "$1 ($2)");
        value = replacePattern(value, BOLD_PATTERN, "$1");
        value = replacePattern(value, ITALIC_PATTERN, "$1");
        value = replacePattern(value, INLINE_CODE_PATTERN, "'$1'");
        return value;
    }

    private static String replacePattern(String input, Pattern pattern, String replacement) {
        Matcher matcher = pattern.matcher(input);
        if (!matcher.find()) {
            return input;
        }
        return matcher.replaceAll(replacement);
    }
}