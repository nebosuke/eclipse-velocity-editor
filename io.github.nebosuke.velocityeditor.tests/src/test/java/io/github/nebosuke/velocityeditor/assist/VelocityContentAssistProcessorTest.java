package io.github.nebosuke.velocityeditor.assist;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
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

import io.github.nebosuke.velocityeditor.Messages;

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

    @Mock
    private IProject project;

    @Mock
    private IProject referencedProject;

    @Mock
    private IProject transitiveReferencedProject;

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
        @DisplayName("#MACRO のような大文字定義も抽出できる")
        void parseMacroNamesSupportsUppercaseDirective() {
            var parsed = VelocityContentAssistProcessor.parseMacroNames(
                    "#MACRO(RenderUpper $x)\n#end\n#macro(renderLower $y)\n#end");

            assertEquals(2, parsed.size());
            assertEquals("RenderUpper", parsed.get(0));
            assertEquals("renderLower", parsed.get(1));
        }

        @Test
        @DisplayName("マクロ抽出はnullや空文字で空リストを返す")
        void parseMacroNamesReturnsEmptyForBlank() {
            assertTrue(VelocityContentAssistProcessor.parseMacroNames(null).isEmpty());
            assertTrue(VelocityContentAssistProcessor.parseMacroNames("   ").isEmpty());
        }

        @Test
        @DisplayName("Velocityテンプレート拡張子判定が正しく動作する")
        void velocityTemplateFileNameDetectionWorks() {
            assertTrue(VelocityContentAssistProcessor.isVelocityTemplateFileName("a.vm"));
            assertTrue(VelocityContentAssistProcessor.isVelocityTemplateFileName("b.vtl"));
            assertTrue(VelocityContentAssistProcessor.isVelocityTemplateFileName("C.VM"));
            assertTrue(VelocityContentAssistProcessor.isVelocityTemplateFileName("D.VTL"));

            assertFalse(VelocityContentAssistProcessor.isVelocityTemplateFileName("e.txt"));
            assertFalse(VelocityContentAssistProcessor.isVelocityTemplateFileName(""));
            assertFalse(VelocityContentAssistProcessor.isVelocityTemplateFileName(null));
        }
    }

    @Nested
    @DisplayName("依存プロジェクトの収集")
    class ProjectDependencyTraversal {

        @Test
        @DisplayName("参照プロジェクトを再帰的に収集し重複を除去する")
        void collectMacroProjectsTraversesReferencesRecursively() throws Exception {
            when(project.isAccessible()).thenReturn(true);
            when(referencedProject.isAccessible()).thenReturn(true);
            when(transitiveReferencedProject.isAccessible()).thenReturn(true);

            when(project.getReferencedProjects()).thenReturn(new IProject[] { referencedProject });
            when(referencedProject.getReferencedProjects())
                    .thenReturn(new IProject[] { transitiveReferencedProject });
            // 循環参照があっても無限ループしないことを確認
            when(transitiveReferencedProject.getReferencedProjects())
                    .thenReturn(new IProject[] { referencedProject });

            var collected = VelocityContentAssistProcessor.collectMacroProjects(project);

            assertEquals(3, collected.size());
            assertSame(project, collected.get(0));
            assertTrue(collected.contains(referencedProject));
            assertTrue(collected.contains(transitiveReferencedProject));
        }

        @Test
        @DisplayName("アクセス不可プロジェクトは収集対象外")
        void collectMacroProjectsSkipsInaccessibleProject() {
            when(project.isAccessible()).thenReturn(false);

            var collected = VelocityContentAssistProcessor.collectMacroProjects(project);

            assertTrue(collected.isEmpty());
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
            assertArrayContains(chars, '#');
            assertArrayContains(chars, '$');
            assertArrayContains(chars, '<');
            assertArrayContains(chars, 'a');
            assertArrayContains(chars, 'Z');
            assertArrayContains(chars, '_');
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
    @DisplayName("JavaScript関数の補完")
    class JavaScriptFunctionCompletion {

        @Test
        @DisplayName("scriptブロック内で関数候補と説明を返す")
        void returnsJavaScriptFunctionProposalsInScriptBlock() throws BadLocationException {
            String content = """
                    <script>
                    ## ユーザー表示
                    function greet(user, options = {}) {}

                    gre
                    </script>
                    """;
            int offset = content.indexOf("gre\n") + 3;
            setupMocks("gre", offset);
            when(document.get()).thenReturn(content);

            ICompletionProposal[] proposals = processor.computeCompletionProposals(viewer, offset);

            assertNotNull(proposals);
            boolean hasGreet = false;
            boolean hasInfo = false;
            for (ICompletionProposal proposal : proposals) {
                if ("greet".equals(proposal.getDisplayString())) {
                    hasGreet = true;
                    String info = proposal.getAdditionalProposalInfo();
                    hasInfo = info != null
                            && info.contains(Messages.MACRO_ARGS_LABEL + " user, options")
                            && info.contains("ユーザー表示");

                    Document completionTarget = new Document(content);
                    proposal.apply(completionTarget);
                    String expected = content.replace("gre\n", "greet(user, options)\n");
                    assertEquals(expected, completionTarget.get());
                    break;
                }
            }

            assertTrue(hasGreet, "JavaScript関数 greet の補完候補が含まれていません");
            assertTrue(hasInfo, "JavaScript関数 greet の補完説明が期待どおりではありません");
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
            when(document.get()).thenReturn("## ユーザーを描画\n#macro(renderUser $user $ctx)\n#end\n#macro(helper $x)\n#end");

            ICompletionProposal[] proposals = processor.computeCompletionProposals(viewer, 3);

            assertNotNull(proposals);
            boolean hasRenderUser = false;
            boolean hasRenderUserInfo = false;
            boolean hasRenderUserReplacement = false;
            for (ICompletionProposal proposal : proposals) {
                if ("#renderUser".equals(proposal.getDisplayString())) {
                    hasRenderUser = true;
                    String info = proposal.getAdditionalProposalInfo();
                    hasRenderUserInfo = info != null
                            && !info.startsWith("Macro\n")
                            && info.contains(Messages.MACRO_ARGS_LABEL + " $user, $ctx")
                            && info.contains("ユーザーを描画");

                    Document completionTarget = new Document("#re");
                    proposal.apply(completionTarget);
                    hasRenderUserReplacement = "#renderUser($user $ctx)".equals(completionTarget.get());
                    break;
                }
            }
            assertTrue(hasRenderUser, "同一ファイルマクロ #renderUser の補完候補が含まれていません");
            assertTrue(hasRenderUserInfo, "同一ファイルマクロ #renderUser の補完説明が期待どおりではありません");
            assertTrue(hasRenderUserReplacement, "同一ファイルマクロ #renderUser の補完挿入文字列が期待どおりではありません");
        }

        @Test
        @DisplayName("マクロコメントのMarkdownを補完説明で整形表示する")
        void formatsMarkdownCommentInMacroProposalInfo() throws BadLocationException {
            setupMocks("#render", 7);
            when(document.get()).thenReturn("## # 見出し\n## - **強調** と `code`\n#macro(renderMd $user)\n#end");

            ICompletionProposal[] proposals = processor.computeCompletionProposals(viewer, 7);

            assertNotNull(proposals);
            String info = null;
            for (ICompletionProposal proposal : proposals) {
                if ("#renderMd".equals(proposal.getDisplayString())) {
                    info = proposal.getAdditionalProposalInfo();
                    break;
                }
            }

            assertNotNull(info, "#renderMd の補完説明が見つかりません");
            assertTrue(info.contains("【見出し】"), "見出しMarkdownが整形されていません: " + info);
            assertTrue(info.contains("• 強調 と 'code'"), "リスト/強調/コードMarkdownが整形されていません: " + info);
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
            when(document.get()).thenReturn("");
            when(document.get(anyInt(), anyInt())).thenThrow(new BadLocationException("Test exception"));
            
            ICompletionProposal[] proposals = processor.computeCompletionProposals(viewer, 0);
            
            // Verify that no exception is thrown and an array is returned.
            assertNotNull(proposals);
        }
    }

    private void setupMocks(String prefix, int offset) throws BadLocationException {
        when(viewer.getDocument()).thenReturn(document);
        lenient().when(document.get()).thenReturn(prefix);
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
