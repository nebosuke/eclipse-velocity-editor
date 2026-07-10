package io.github.nebosuke.velocityeditor.scanners;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.nebosuke.velocityeditor.preferences.ColorManager;

/**
 * Unit tests for HTMLTagScanner.
 */
@DisplayName("HTMLTagScanner")
@ExtendWith(MockitoExtension.class)
class HTMLTagScannerTest {

    @Mock
    private ColorManager colorManager;

    private HTMLTagScanner scanner;

    @BeforeEach
    void setUp() {
        when(colorManager.getColor(anyString())).thenReturn(null);
        scanner = new HTMLTagScanner(colorManager);
    }

    @Test
    @DisplayName("開きタグ名を太字でハイライトする")
    void highlightsOpeningTagName() {
        // First token is "<" (default token), second is the tag name "div".
        IToken token = tokenAt("<div>", 1);
        assertBoldToken(token);
    }

    @Test
    @DisplayName("閉じタグ名を太字でハイライトする")
    void highlightsClosingTagName() {
        // First token is "<" (default token), second is the word "/div".
        // Before the fix "/div" was not a registered key and fell back to the
        // non-bold attribute token.
        IToken token = tokenAt("</div>", 1);
        assertBoldToken(token);
    }

    @Test
    @DisplayName("属性名は非太字トークン")
    void keepsAttributeNameNonBold() {
        // Tokens: "<", "div", " ", "class", ... — index 3 is the attribute name.
        IToken token = tokenAt("<div class=\"x\">", 3);
        TextAttribute attribute = (TextAttribute) token.getData();
        assertNotNull(attribute);
        assertEquals(SWT.NORMAL, attribute.getStyle());
    }

    private IToken tokenAt(String source, int index) {
        Document document = new Document(source);
        scanner.setRange(document, 0, source.length());
        IToken token = scanner.nextToken();
        for (int i = 0; i < index; i++) {
            token = scanner.nextToken();
        }
        return token;
    }

    private void assertBoldToken(IToken token) {
        TextAttribute attribute = (TextAttribute) token.getData();
        assertNotNull(attribute);
        assertEquals(SWT.BOLD, attribute.getStyle());
    }
}
