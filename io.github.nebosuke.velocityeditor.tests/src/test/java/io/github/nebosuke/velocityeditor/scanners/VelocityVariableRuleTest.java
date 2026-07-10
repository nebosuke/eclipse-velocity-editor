package io.github.nebosuke.velocityeditor.scanners;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for VelocityVariableRule.
 */
@DisplayName("VelocityVariableRule")
class VelocityVariableRuleTest {

    private VelocityVariableRule rule;
    private IToken successToken;

    @BeforeEach
    void setUp() {
        successToken = new Token("variable");
        rule = new VelocityVariableRule(successToken);
    }

    @Nested
    @DisplayName("シンプルな変数 ($variable)")
    class SimpleVariable {

        @Test
        @DisplayName("基本的な変数を認識する")
        void recognizesBasicVariable() {
            StringCharacterScanner scanner = new StringCharacterScanner("$name");
            IToken result = rule.evaluate(scanner);
            assertEquals(successToken, result);
        }

        @Test
        @DisplayName("アンダースコアで始まる変数を認識する")
        void recognizesUnderscoreVariable() {
            StringCharacterScanner scanner = new StringCharacterScanner("$_private");
            IToken result = rule.evaluate(scanner);
            assertEquals(successToken, result);
        }

        @Test
        @DisplayName("数字を含む変数を認識する")
        void recognizesVariableWithNumbers() {
            StringCharacterScanner scanner = new StringCharacterScanner("$var123");
            IToken result = rule.evaluate(scanner);
            assertEquals(successToken, result);
        }

        @Test
        @DisplayName("ハイフンを含む変数を認識する")
        void recognizesVariableWithHyphen() {
            StringCharacterScanner scanner = new StringCharacterScanner("$my-var");
            IToken result = rule.evaluate(scanner);
            assertEquals(successToken, result);
        }
    }

    @Nested
    @DisplayName("フォーマル変数 (${variable})")
    class FormalVariable {

        @Test
        @DisplayName("フォーマル変数を認識する")
        void recognizesFormalVariable() {
            StringCharacterScanner scanner = new StringCharacterScanner("${name}");
            IToken result = rule.evaluate(scanner);
            assertEquals(successToken, result);
        }

        @Test
        @DisplayName("フォーマル変数内のプロパティアクセスを認識する")
        void recognizesFormalVariableWithProperty() {
            StringCharacterScanner scanner = new StringCharacterScanner("${user.name}");
            IToken result = rule.evaluate(scanner);
            assertEquals(successToken, result);
        }
    }

    @Nested
    @DisplayName("サイレント変数 ($!variable)")
    class SilentVariable {

        @Test
        @DisplayName("サイレント変数を認識する")
        void recognizesSilentVariable() {
            StringCharacterScanner scanner = new StringCharacterScanner("$!name");
            IToken result = rule.evaluate(scanner);
            assertEquals(successToken, result);
        }

        @Test
        @DisplayName("サイレントフォーマル変数を認識する")
        void recognizesSilentFormalVariable() {
            StringCharacterScanner scanner = new StringCharacterScanner("$!{name}");
            IToken result = rule.evaluate(scanner);
            assertEquals(successToken, result);
        }
    }

    @Nested
    @DisplayName("プロパティアクセス ($variable.property)")
    class PropertyAccess {

        @Test
        @DisplayName("プロパティアクセスを認識する")
        void recognizesPropertyAccess() {
            StringCharacterScanner scanner = new StringCharacterScanner("$user.name");
            IToken result = rule.evaluate(scanner);
            assertEquals(successToken, result);
        }

        @Test
        @DisplayName("ネストしたプロパティアクセスを認識する")
        void recognizesNestedPropertyAccess() {
            StringCharacterScanner scanner = new StringCharacterScanner("$user.address.city");
            IToken result = rule.evaluate(scanner);
            assertEquals(successToken, result);
        }
    }

    @Nested
    @DisplayName("メソッド呼び出し ($variable.method())")
    class MethodCall {

        @Test
        @DisplayName("メソッド呼び出しを認識する")
        void recognizesMethodCall() {
            StringCharacterScanner scanner = new StringCharacterScanner("$user.getName()");
            IToken result = rule.evaluate(scanner);
            assertEquals(successToken, result);
        }

        @Test
        @DisplayName("引数付きメソッド呼び出しを認識する")
        void recognizesMethodCallWithArgs() {
            StringCharacterScanner scanner = new StringCharacterScanner("$list.get(0)");
            IToken result = rule.evaluate(scanner);
            assertEquals(successToken, result);
        }

        @Test
        @DisplayName("ネストした括弧のメソッド呼び出しを認識する")
        void recognizesMethodCallWithNestedParens() {
            StringCharacterScanner scanner = new StringCharacterScanner("$obj.method(fn(a))");
            IToken result = rule.evaluate(scanner);
            assertEquals(successToken, result);
        }
    }

    @Nested
    @DisplayName("非変数パターン")
    class NonVariablePatterns {

        @Test
        @DisplayName("$だけでは変数として認識しない")
        void doesNotRecognizeDollarOnly() {
            StringCharacterScanner scanner = new StringCharacterScanner("$ ");
            IToken result = rule.evaluate(scanner);
            assertEquals(Token.UNDEFINED, result);
            assertEquals(0, scanner.getPosition()); // The scanner position is reset.
        }

        @Test
        @DisplayName("$に続く数字は変数として認識しない")
        void doesNotRecognizeDollarNumber() {
            StringCharacterScanner scanner = new StringCharacterScanner("$123");
            IToken result = rule.evaluate(scanner);
            assertEquals(Token.UNDEFINED, result);
        }

        @Test
        @DisplayName("通常のテキストは変数として認識しない")
        void doesNotRecognizeNormalText() {
            StringCharacterScanner scanner = new StringCharacterScanner("hello");
            IToken result = rule.evaluate(scanner);
            assertEquals(Token.UNDEFINED, result);
            assertEquals(0, scanner.getPosition());
        }
    }

    @Nested
    @DisplayName("文中の変数")
    class VariableInText {

        @Test
        @DisplayName("テキストの後の変数を認識する")
        void recognizesVariableAfterText() {
            StringCharacterScanner scanner = new StringCharacterScanner("Hello $name!");
            // Move the scanner to the position of '$'.
            scanner.setPosition(6);
            IToken result = rule.evaluate(scanner);
            assertEquals(successToken, result);
        }
    }
}
