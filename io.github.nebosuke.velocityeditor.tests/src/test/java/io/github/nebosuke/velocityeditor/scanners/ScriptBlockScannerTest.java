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
 * Unit tests for ScriptBlockScanner.
 */
@DisplayName("ScriptBlockScanner")
@ExtendWith(MockitoExtension.class)
class ScriptBlockScannerTest {

    @Mock
    private ColorManager colorManager;

    private ScriptBlockScanner scanner;

    @BeforeEach
    void setUp() {
        when(colorManager.getColor(anyString())).thenReturn(null);
        scanner = new ScriptBlockScanner(colorManager);
    }

    @Test
    @DisplayName("JavaScriptキーワードを太字でハイライトする")
    void highlightsJavaScriptKeyword() {
        IToken token = firstTokenOf("const value = 1;");
        assertBoldToken(token);
    }

    @Test
    @DisplayName("VTLディレクティブを太字でハイライトする")
    void highlightsVtlDirective() {
        IToken token = firstTokenOf("#if($cond)");
        assertBoldToken(token);
    }

    @Test
    @DisplayName("JavaScript関数名を太字でハイライトする")
    void highlightsJavaScriptFunctionName() {
        IToken token = firstTokenOf("greet(user);");
        assertBoldToken(token);
    }

    @Test
    @DisplayName("通常テキストはデフォルト(非太字)トークン")
    void keepsPlainTextAsDefault() {
        IToken token = firstTokenOf("identifier");
        TextAttribute attribute = (TextAttribute) token.getData();
        assertNotNull(attribute);
        assertEquals(SWT.NORMAL, attribute.getStyle());
    }

    @Test
    @DisplayName("## コメントがディレクティブではなくコメントトークン(非太字)としてハイライトされる")
    void highlightsVtlSingleLineComment() {
        // Before the fix, the WordRule consumed '#' as a bold directive token.
        // After the fix, EndOfLineRule("##") fires first and returns the non-bold comment token.
        IToken token = firstTokenOf("## this is a VTL comment");
        TextAttribute attribute = (TextAttribute) token.getData();
        assertNotNull(attribute);
        assertEquals(SWT.NORMAL, attribute.getStyle());
    }

    @Test
    @DisplayName("#* ... *# ブロックコメントがコメントトークン(非太字)としてハイライトされる")
    void highlightsVtlBlockComment() {
        IToken token = firstTokenOf("#* block comment *#");
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