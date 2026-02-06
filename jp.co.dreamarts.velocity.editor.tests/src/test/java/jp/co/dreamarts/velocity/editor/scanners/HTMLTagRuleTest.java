package jp.co.dreamarts.velocity.editor.scanners;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for HTMLTagRule.
 */
@DisplayName("HTMLTagRule")
class HTMLTagRuleTest {

    private HTMLTagRule rule;
    private IToken successToken;

    @BeforeEach
    void setUp() {
        successToken = new Token("html-tag");
        rule = new HTMLTagRule(successToken);
    }

    @Nested
    @DisplayName("シンプルなタグ")
    class SimpleTags {

        @Test
        @DisplayName("開始タグを認識する")
        void recognizesOpeningTag() {
            StringCharacterScanner scanner = new StringCharacterScanner("<div>");
            IToken result = rule.evaluate(scanner);
            assertEquals(successToken, result);
        }

        @Test
        @DisplayName("閉じタグを認識する")
        void recognizesClosingTag() {
            StringCharacterScanner scanner = new StringCharacterScanner("</div>");
            IToken result = rule.evaluate(scanner);
            assertEquals(successToken, result);
        }

        @Test
        @DisplayName("自己閉じタグを認識する")
        void recognizesSelfClosingTag() {
            StringCharacterScanner scanner = new StringCharacterScanner("<br/>");
            IToken result = rule.evaluate(scanner);
            assertEquals(successToken, result);
        }

        @Test
        @DisplayName("スペース付き自己閉じタグを認識する")
        void recognizesSelfClosingTagWithSpace() {
            StringCharacterScanner scanner = new StringCharacterScanner("<br />");
            IToken result = rule.evaluate(scanner);
            assertEquals(successToken, result);
        }
    }

    @Nested
    @DisplayName("属性付きタグ")
    class TagsWithAttributes {

        @Test
        @DisplayName("単一属性のタグを認識する")
        void recognizesTagWithSingleAttribute() {
            StringCharacterScanner scanner = new StringCharacterScanner("<div class=\"container\">");
            IToken result = rule.evaluate(scanner);
            assertEquals(successToken, result);
        }

        @Test
        @DisplayName("複数属性のタグを認識する")
        void recognizesTagWithMultipleAttributes() {
            StringCharacterScanner scanner = new StringCharacterScanner("<input type=\"text\" name=\"username\" id=\"user-input\">");
            IToken result = rule.evaluate(scanner);
            assertEquals(successToken, result);
        }

        @Test
        @DisplayName("シングルクォートの属性を認識する")
        void recognizesTagWithSingleQuotedAttribute() {
            StringCharacterScanner scanner = new StringCharacterScanner("<div class='container'>");
            IToken result = rule.evaluate(scanner);
            assertEquals(successToken, result);
        }

        @Test
        @DisplayName("値のない属性を認識する")
        void recognizesTagWithValuelessAttribute() {
            StringCharacterScanner scanner = new StringCharacterScanner("<input disabled>");
            IToken result = rule.evaluate(scanner);
            assertEquals(successToken, result);
        }
    }

    @Nested
    @DisplayName("属性内の特殊文字")
    class SpecialCharactersInAttributes {

        @Test
        @DisplayName("属性値内の>を正しく処理する")
        void handlesGreaterThanInAttribute() {
            StringCharacterScanner scanner = new StringCharacterScanner("<div title=\"a > b\">");
            IToken result = rule.evaluate(scanner);
            assertEquals(successToken, result);
        }

        @Test
        @DisplayName("属性値内の<を正しく処理する")
        void handlesLessThanInAttribute() {
            StringCharacterScanner scanner = new StringCharacterScanner("<div title=\"a < b\">");
            IToken result = rule.evaluate(scanner);
            assertEquals(successToken, result);
        }
    }

    @Nested
    @DisplayName("複数行タグ")
    class MultiLineTags {

        @Test
        @DisplayName("複数行のタグを認識する")
        void recognizesMultiLineTag() {
            StringCharacterScanner scanner = new StringCharacterScanner("<div\n  class=\"container\"\n  id=\"main\">");
            IToken result = rule.evaluate(scanner);
            assertEquals(successToken, result);
        }
    }

    @Nested
    @DisplayName("除外パターン")
    class ExcludedPatterns {

        @Test
        @DisplayName("HTMLコメントは認識しない")
        void doesNotRecognizeHtmlComment() {
            StringCharacterScanner scanner = new StringCharacterScanner("<!-- comment -->");
            IToken result = rule.evaluate(scanner);
            assertEquals(Token.UNDEFINED, result);
        }

        @Test
        @DisplayName("XML処理命令は認識しない")
        void doesNotRecognizeProcessingInstruction() {
            StringCharacterScanner scanner = new StringCharacterScanner("<?xml version=\"1.0\"?>");
            IToken result = rule.evaluate(scanner);
            assertEquals(Token.UNDEFINED, result);
        }

        @Test
        @DisplayName("通常のテキストは認識しない")
        void doesNotRecognizeNormalText() {
            StringCharacterScanner scanner = new StringCharacterScanner("Hello World");
            IToken result = rule.evaluate(scanner);
            assertEquals(Token.UNDEFINED, result);
            assertEquals(0, scanner.getPosition());
        }
    }

    @Nested
    @DisplayName("不正なタグ")
    class InvalidTags {

        @Test
        @DisplayName("閉じていないタグはEOFまで読む")
        void handlesUnclosedTag() {
            StringCharacterScanner scanner = new StringCharacterScanner("<div");
            IToken result = rule.evaluate(scanner);
            // Since there is no closing bracket, UNDEFINED is returned and the position is reset.
            assertEquals(Token.UNDEFINED, result);
        }
    }

    @Nested
    @DisplayName("getSuccessToken")
    class GetSuccessToken {

        @Test
        @DisplayName("正しいトークンを返す")
        void returnsCorrectToken() {
            assertEquals(successToken, rule.getSuccessToken());
        }
    }

    @Nested
    @DisplayName("resumeモード")
    class ResumeMode {

        @Test
        @DisplayName("resume=trueでも正しく動作する")
        void worksWithResumeTrue() {
            StringCharacterScanner scanner = new StringCharacterScanner("<span>");
            IToken result = rule.evaluate(scanner, true);
            assertEquals(successToken, result);
        }
    }
}
