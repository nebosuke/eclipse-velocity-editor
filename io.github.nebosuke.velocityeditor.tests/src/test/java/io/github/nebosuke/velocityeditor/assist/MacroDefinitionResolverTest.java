package io.github.nebosuke.velocityeditor.assist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.jface.text.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.nebosuke.velocityeditor.assist.MacroDefinitionResolver.MacroDefinition;

@DisplayName("MacroDefinitionResolver")
class MacroDefinitionResolverTest {

    private final MacroDefinitionResolver resolver = new MacroDefinitionResolver();

    @Test
    @DisplayName("macro定義から名前と引数を抽出できる")
    void parseDefinitionsExtractsNameAndArguments() {
        String text = "#macro(renderUser $user $!{ctx})\n#end";

        List<MacroDefinition> definitions = MacroDefinitionResolver.parseDefinitions(text, null);

        assertEquals(1, definitions.size());
        MacroDefinition definition = definitions.get(0);
        assertEquals("renderUser", definition.getName());
        assertEquals(List.of("$user", "$!{ctx}"), definition.getArguments());
    }

    @Test
    @DisplayName("直上の##コメントを抽出できる")
    void extractPrecedingLineComments() {
        String text = "## line1\n## line2\n#macro(render $x)\n#end";

        List<MacroDefinition> definitions = MacroDefinitionResolver.parseDefinitions(text, null);

        assertEquals(1, definitions.size());
        assertEquals("line1\nline2", definitions.get(0).getComment());
    }

    @Test
    @DisplayName("直上の#* *#コメントを抽出できる")
    void extractPrecedingBlockComment() {
        String text = "#*\n * block line1\n * block line2\n *#\n#macro(render $x)\n#end";

        List<MacroDefinition> definitions = MacroDefinitionResolver.parseDefinitions(text, null);

        assertEquals(1, definitions.size());
        assertEquals("block line1\nblock line2", definitions.get(0).getComment());
    }

    @Test
    @DisplayName("カーソル位置からマクロ名と領域を解決できる")
    void findMacroNameAndRegionAtInvocation() {
        String text = "#renderUser($user)";
        Document document = new Document(text);
        int offsetInsideName = text.indexOf("User");

        String macroName = resolver.findMacroNameAt(document, offsetInsideName);
        int[] region = resolver.findInvocationRegion(document, offsetInsideName);

        assertEquals("renderUser", macroName);
        assertNotNull(region);
        assertEquals(0, region[0]);
        assertEquals("#renderUser".length(), region[1]);
    }

    @Test
    @DisplayName("組み込みディレクティブはマクロ名として扱わない")
    void ignoresBuiltInDirectives() {
        Document document = new Document("#if($x)");

        String macroName = resolver.findMacroNameAt(document, 2);

        assertNull(macroName);
    }

    @Test
    @DisplayName("空文字では定義抽出は空")
    void parseDefinitionsReturnsEmptyForBlank() {
        assertTrue(MacroDefinitionResolver.parseDefinitions("", null).isEmpty());
        assertTrue(MacroDefinitionResolver.parseDefinitions("   ", null).isEmpty());
    }
}