package io.github.nebosuke.velocityeditor.assist;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MacroCommentFormatter")
class MacroCommentFormatterTest {

    @Test
    @DisplayName("代表的なMarkdown記法を読みやすい形式へ整形する")
    void formatMarkdownToReadableText() {
        String markdown = "# Title\n"
                + "- **bold** and `code`\n"
                + "1. first\n"
                + "> quote\n"
                + "[link](https://example.com)";

        String actual = MacroCommentFormatter.format(markdown);

        assertEquals("【Title】\n"
                + "• bold and 'code'\n"
                + "1) first\n"
                + "❝ quote\n"
                + "link (https://example.com)", actual);
    }

    @Test
    @DisplayName("空文字やnullは空文字を返す")
    void returnsEmptyForBlankInput() {
        assertEquals("", MacroCommentFormatter.format(null));
        assertEquals("", MacroCommentFormatter.format("   \n\t"));
    }
}