package jp.co.dreamarts.velocity.editor.assist;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for VelocityContentAssistProcessor.
 */
@DisplayName("VelocityContentAssistProcessor")
@ExtendWith(MockitoExtension.class)
class VelocityContentAssistProcessorTest {

    private VelocityContentAssistProcessor processor;

    @Mock
    private ITextViewer viewer;

    @Mock
    private IDocument document;

    @BeforeEach
    void setUp() {
        processor = new VelocityContentAssistProcessor();
    }

    @Nested
    @DisplayName("変数名パース")
    class VariableParsing {

        @Test
        @DisplayName("改行区切り・カンマ区切りを正規化して重複を除去する")
        void parseVariableNamesNormalizesAndDeduplicates() {
            var parsed = VelocityContentAssistProcessor.parseVariableNames(
                    "user\n$item\n${user},$!{response}, invalid-name");

            assertEquals(3, parsed.size());
            assertEquals("user", parsed.get(0));
            assertEquals("item", parsed.get(1));
            assertEquals("response", parsed.get(2));
        }

        @Test
        @DisplayName("nullや空文字では空リストを返す")
        void parseVariableNamesReturnsEmptyForBlank() {
            assertTrue(VelocityContentAssistProcessor.parseVariableNames(null).isEmpty());
            assertTrue(VelocityContentAssistProcessor.parseVariableNames("   ").isEmpty());
        }

        @Test
        @DisplayName("マクロ定義を抽出して重複を除去する")
        void parseMacroNamesExtractsAndDeduplicates() {
            var parsed = VelocityContentAssistProcessor.parseMacroNames(
                    "#macro(renderUser $user)\n#end\n#macro(renderItem $item)\n#end\n#macro(renderUser $user2)\n#end");

            assertEquals(2, parsed.size());
            assertEquals("renderUser", parsed.get(0));
            assertEquals("renderItem", parsed.get(1));
        }

        @Test
        @DisplayName("マクロ抽出はnullや空文字で空リストを返す")
        void parseMacroNamesReturnsEmptyForBlank() {
            assertTrue(VelocityContentAssistProcessor.parseMacroNames(null).isEmpty());
            assertTrue(VelocityContentAssistProcessor.parseMacroNames("   ").isEmpty());
        }
    }

    @Nested
    @DisplayName("自動起動文字")
    class AutoActivationCharacters {

        @Test
        @DisplayName("補完候補の自動起動文字が正しい")
        void hasCorrectAutoActivationCharacters() {
            char[] chars = processor.getCompletionProposalAutoActivationCharacters();
            assertNotNull(chars);
            assertEquals(3, chars.length);
            assertArrayContains(chars, '#');
            assertArrayContains(chars, '$');
            assertArrayContains(chars, '<');
        }

        @Test
        @DisplayName("コンテキスト情報の自動起動文字はnull")
        void contextInfoAutoActivationCharactersIsNull() {
            assertNull(processor.getContextInformationAutoActivationCharacters());
        }

        private void assertArrayContains(char[] array, char value) {
            boolean found = false;
            for (char c : array) {
                if (c == value) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "配列に '" + value + "' が含まれていません");
        }
    }

    @Nested
    @DisplayName("VTLディレクティブの補完")
    class VtlDirectiveCompletion {

        @Test
        @DisplayName("#で始まる補完候補を返す")
        void returnsDirectiveProposals() throws BadLocationException {
            setupMocks("#", 1);
            
            ICompletionProposal[] proposals = processor.computeCompletionProposals(viewer, 1);
            
            assertNotNull(proposals);
            assertTrue(proposals.length > 0);
            
            // Verify that #if is included.
            boolean hasIf = false;
            for (ICompletionProposal proposal : proposals) {
                if (proposal.getDisplayString().contains("#if")) {
                    hasIf = true;
                    break;
                }
            }
            assertTrue(hasIf, "#ifの補完候補が含まれていません");
        }

        @Test
        @DisplayName("#ifにマッチする補完候補を返す")
        void returnsMatchingDirectiveProposals() throws BadLocationException {
            setupMocks("#if", 3);
            
            ICompletionProposal[] proposals = processor.computeCompletionProposals(viewer, 3);
            
            assertNotNull(proposals);
            assertTrue(proposals.length > 0);
            
            // Verify that all proposals include #if.
            for (ICompletionProposal proposal : proposals) {
                String displayString = proposal.getDisplayString().toLowerCase();
                assertTrue(displayString.contains("#if") || displayString.contains("#elseif"),
                    "候補が#ifにマッチしていません: " + displayString);
            }
        }

        @Test
        @DisplayName("同一ファイルで定義したマクロが#補完候補に含まれる")
        void returnsCurrentFileMacroProposals() throws BadLocationException {
            setupMocks("#re", 3);
            when(document.get()).thenReturn("#macro(renderUser $user)\n#end\n#macro(helper $x)\n#end");

            ICompletionProposal[] proposals = processor.computeCompletionProposals(viewer, 3);

            assertNotNull(proposals);
            boolean hasRenderUser = false;
            for (ICompletionProposal proposal : proposals) {
                if ("#renderUser".equals(proposal.getDisplayString())) {
                    hasRenderUser = true;
                    break;
                }
            }
            assertTrue(hasRenderUser, "同一ファイルマクロ #renderUser の補完候補が含まれていません");
        }
    }

    @Nested
    @DisplayName("VTL変数の補完")
    class VtlVariableCompletion {

        @Test
        @DisplayName("$で始まる補完候補を返す")
        void returnsVariableProposals() throws BadLocationException {
            setupMocks("$", 1);
            
            ICompletionProposal[] proposals = processor.computeCompletionProposals(viewer, 1);
            
            assertNotNull(proposals);
            assertTrue(proposals.length > 0);
            
            // Verify that variable proposals are included.
            boolean hasVariable = false;
            for (ICompletionProposal proposal : proposals) {
                if (proposal.getDisplayString().contains("$")) {
                    hasVariable = true;
                    break;
                }
            }
            assertTrue(hasVariable, "変数の補完候補が含まれていません");
        }

        @Test
        @DisplayName("$foreachで始まる補完候補を返す")
        void returnsForeachVariableProposals() throws BadLocationException {
            setupMocks("$foreach", 8);
            
            ICompletionProposal[] proposals = processor.computeCompletionProposals(viewer, 8);
            
            assertNotNull(proposals);
            assertTrue(proposals.length > 0);
        }
    }

    @Nested
    @DisplayName("HTMLタグの補完")
    class HtmlTagCompletion {

        @Test
        @DisplayName("<で始まる補完候補を返す")
        void returnsHtmlTagProposals() throws BadLocationException {
            setupMocks("<", 1);
            
            ICompletionProposal[] proposals = processor.computeCompletionProposals(viewer, 1);
            
            assertNotNull(proposals);
            assertTrue(proposals.length > 0);
            
            // Verify that <div> is included.
            boolean hasDiv = false;
            for (ICompletionProposal proposal : proposals) {
                if (proposal.getDisplayString().contains("<div>")) {
                    hasDiv = true;
                    break;
                }
            }
            assertTrue(hasDiv, "<div>の補完候補が含まれていません");
        }

        @Test
        @DisplayName("</で始まる閉じタグの補完候補を返す")
        void returnsClosingTagProposals() throws BadLocationException {
            setupMocks("</", 2);
            
            ICompletionProposal[] proposals = processor.computeCompletionProposals(viewer, 2);
            
            assertNotNull(proposals);
            assertTrue(proposals.length > 0);
        }

        @Test
        @DisplayName("<divにマッチする補完候補を返す")
        void returnsMatchingHtmlTagProposals() throws BadLocationException {
            setupMocks("<div", 4);
            
            ICompletionProposal[] proposals = processor.computeCompletionProposals(viewer, 4);
            
            assertNotNull(proposals);
            assertTrue(proposals.length > 0);
        }
    }

    @Nested
    @DisplayName("空のプレフィックス")
    class EmptyPrefix {

        @Test
        @DisplayName("空のプレフィックスですべての候補を返す")
        void returnsAllProposalsForEmptyPrefix() throws BadLocationException {
            setupMocks("", 0);
            
            ICompletionProposal[] proposals = processor.computeCompletionProposals(viewer, 0);
            
            assertNotNull(proposals);
            assertTrue(proposals.length > 0);
        }
    }

    @Nested
    @DisplayName("コンテキスト情報")
    class ContextInformation {

        @Test
        @DisplayName("コンテキスト情報はnullを返す")
        void contextInformationReturnsNull() {
            assertNull(processor.computeContextInformation(viewer, 0));
        }

        @Test
        @DisplayName("コンテキスト情報バリデータはnullを返す")
        void contextInformationValidatorReturnsNull() {
            assertNull(processor.getContextInformationValidator());
        }
    }

    @Nested
    @DisplayName("エラーメッセージ")
    class ErrorMessage {

        @Test
        @DisplayName("エラーメッセージはnullを返す")
        void errorMessageReturnsNull() {
            assertNull(processor.getErrorMessage());
        }
    }

    @Nested
    @DisplayName("例外処理")
    class ExceptionHandling {

        @Test
        @DisplayName("BadLocationExceptionが発生しても例外をスローしない")
        void handlesExceptionGracefully() throws BadLocationException {
            when(viewer.getDocument()).thenReturn(document);
            when(document.get(anyInt(), anyInt())).thenThrow(new BadLocationException("Test exception"));
            
            ICompletionProposal[] proposals = processor.computeCompletionProposals(viewer, 0);
            
            // Verify that no exception is thrown and an array is returned.
            assertNotNull(proposals);
        }
    }

    private void setupMocks(String prefix, int offset) throws BadLocationException {
        when(viewer.getDocument()).thenReturn(document);
        when(document.get(anyInt(), anyInt())).thenReturn(prefix);
        
        // Mock behavior for getPrefix.
        if (offset > 0) {
            for (int i = 0; i < offset; i++) {
                final int pos = i;
                lenient().when(document.getChar(i)).thenAnswer(invocation -> {
                    return pos < prefix.length() ? prefix.charAt(pos) : ' ';
                });
            }
        }
    }
}
