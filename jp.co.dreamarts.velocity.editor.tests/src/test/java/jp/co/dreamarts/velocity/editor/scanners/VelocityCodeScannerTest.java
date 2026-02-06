package jp.co.dreamarts.velocity.editor.scanners;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.swt.SWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.co.dreamarts.velocity.editor.preferences.ColorManager;

/**
 * Unit tests for VelocityCodeScanner.
 */
@DisplayName("VelocityCodeScanner")
@ExtendWith(MockitoExtension.class)
class VelocityCodeScannerTest {

    @Mock
    private ColorManager colorManager;

    private VelocityCodeScanner scanner;

    @BeforeEach
    void setUp() {
        // TextAttribute can work with null colors in unit tests.
        when(colorManager.getColor(anyString())).thenReturn(null);
        scanner = new VelocityCodeScanner(colorManager);
    }

    @Nested
    @DisplayName("ディレクティブ/マクロのハイライト")
    class DirectiveHighlight {

        @Test
        @DisplayName("既知のディレクティブ #if を太字トークンで返す")
        void highlightsKnownDirective() {
            IToken token = firstTokenOf("#if($cond)");

            assertBoldToken(token);
        }

        @Test
        @DisplayName("#から始まるカスタムマクロも太字トークンで返す")
        void highlightsCustomMacro() {
            IToken token = firstTokenOf("#myMacro($arg)");

            assertBoldToken(token);
        }
    }

    @Test
    @DisplayName("通常テキストはデフォルト(非太字)トークンで返す")
    void keepsPlainTextAsDefault() {
        IToken token = firstTokenOf("plainText");

        TextAttribute attribute = (TextAttribute) token.getData();
        assertNotNull(attribute);
        assertEquals(SWT.NORMAL, attribute.getStyle());
    }

    private IToken firstTokenOf(String source) {
        Document document = new Document(source);
        scanner.setRange(document, 0, source.length());
        return scanner.nextToken();
    }

    private void assertBoldToken(IToken token) {
        TextAttribute attribute = (TextAttribute) token.getData();
        assertNotNull(attribute);
        assertEquals(SWT.BOLD, attribute.getStyle());
    }
}
