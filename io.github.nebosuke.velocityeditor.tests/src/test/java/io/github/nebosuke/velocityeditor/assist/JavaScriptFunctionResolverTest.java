package io.github.nebosuke.velocityeditor.assist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.jface.text.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JavaScriptFunctionResolver")
class JavaScriptFunctionResolverTest {

    private final JavaScriptFunctionResolver resolver = new JavaScriptFunctionResolver();

    @Test
    @DisplayName("scriptブロック内の関数定義と直上##コメントを抽出する")
    void parsesDefinitionsWithComments() {
        String text = """
                <script>
                ## ユーザー表示
                function greet(user, options = {}) {}

                ## 保存処理
                const save = (item) => {};
                </script>
                """;

        List<JavaScriptFunctionResolver.JavaScriptFunctionDefinition> definitions = resolver.parseDefinitions(text);

        assertEquals(2, definitions.size());

        JavaScriptFunctionResolver.JavaScriptFunctionDefinition greet = definitions.get(0);
        assertEquals("greet", greet.getName());
        assertEquals(List.of("user", "options"), greet.getArguments());
        assertEquals("ユーザー表示", greet.getComment());

        JavaScriptFunctionResolver.JavaScriptFunctionDefinition save = definitions.get(1);
        assertEquals("save", save.getName());
        assertEquals(List.of("item"), save.getArguments());
        assertEquals("保存処理", save.getComment());
    }

    @Test
    @DisplayName("カーソル位置がscriptブロック内か判定できる")
    void detectsScriptBlockOffset() {
        String text = "before\n<script>\nfunction greet() {}\n</script>\nafter";
        int inScript = text.indexOf("greet");
        int outScript = text.indexOf("before");

        assertTrue(resolver.isOffsetInScriptBlock(text, inScript));
        assertFalse(resolver.isOffsetInScriptBlock(text, outScript));
    }

    @Test
    @DisplayName("関数呼び出し上で関数名と領域を解決できる")
    void resolvesFunctionAtInvocation() {
        String text = """
                <script>
                function greet(user) {}
                greet(user);
                </script>
                """;
        Document document = new Document(text);
        int offset = text.indexOf("greet(user);") + 2;

        String functionName = resolver.findFunctionNameAt(document, offset);
        int[] region = resolver.findInvocationRegion(document, offset);

        assertEquals("greet", functionName);
        assertNotNull(region);
        assertEquals("greet", text.substring(region[0], region[0] + region[1]));
    }
}