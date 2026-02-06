package jp.co.dreamarts.velocity.editor.editors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.graphics.Point;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for VelocityDoubleClickStrategy.
 */
@DisplayName("VelocityDoubleClickStrategy")
@ExtendWith(MockitoExtension.class)
class VelocityDoubleClickStrategyTest {

    private VelocityDoubleClickStrategy strategy;

    @Mock
    private ITextViewer viewer;

    @Mock
    private IDocument document;

    @BeforeEach
    void setUp() {
        strategy = new VelocityDoubleClickStrategy();
    }

    @Nested
    @DisplayName("Velocity変数の選択")
    class VelocityVariableSelection {

        @Test
        @DisplayName("シンプルな変数を選択する ($name)")
        void selectsSimpleVariable() throws BadLocationException {
            String content = "$name";
            setupMocks(content, 2); // Double-click at the position of "n".
            
            when(document.getChar(anyInt())).thenAnswer(invocation -> {
                int pos = invocation.getArgument(0);
                return pos >= 0 && pos < content.length() ? content.charAt(pos) : '\0';
            });

            strategy.doubleClicked(viewer);

            ArgumentCaptor<Integer> startCaptor = ArgumentCaptor.forClass(Integer.class);
            ArgumentCaptor<Integer> lengthCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(viewer).setSelectedRange(startCaptor.capture(), lengthCaptor.capture());
            
            assertEquals(0, startCaptor.getValue());
            assertEquals(5, lengthCaptor.getValue());
        }

        @Test
        @DisplayName("サイレント変数を選択する ($!name)")
        void selectsSilentVariable() throws BadLocationException {
            String content = "$!name";
            setupMocks(content, 3); // Double-click at the position of "n".
            
            when(document.getChar(anyInt())).thenAnswer(invocation -> {
                int pos = invocation.getArgument(0);
                return pos >= 0 && pos < content.length() ? content.charAt(pos) : '\0';
            });

            strategy.doubleClicked(viewer);

            verify(viewer).setSelectedRange(anyInt(), anyInt());
        }

        @Test
        @DisplayName("フォーマル変数を選択する (${name})")
        void selectsFormalVariable() throws BadLocationException {
            String content = "${name}";
            setupMocks(content, 3); // Double-click at the position of "n".
            
            when(document.getChar(anyInt())).thenAnswer(invocation -> {
                int pos = invocation.getArgument(0);
                return pos >= 0 && pos < content.length() ? content.charAt(pos) : '\0';
            });

            strategy.doubleClicked(viewer);

            verify(viewer).setSelectedRange(anyInt(), anyInt());
        }
    }

    @Nested
    @DisplayName("HTMLタグ名の選択")
    class HTMLTagNameSelection {

        @Test
        @DisplayName("開始タグ名を選択する")
        void selectsOpeningTagName() throws BadLocationException {
            String content = "<div class=\"test\">";
            setupMocks(content, 2); // Double-click at the position of "i".
            
            when(document.getChar(anyInt())).thenAnswer(invocation -> {
                int pos = invocation.getArgument(0);
                return pos >= 0 && pos < content.length() ? content.charAt(pos) : '\0';
            });

            strategy.doubleClicked(viewer);

            verify(viewer).setSelectedRange(anyInt(), anyInt());
        }

        @Test
        @DisplayName("閉じタグ名を選択する")
        void selectsClosingTagName() throws BadLocationException {
            String content = "</div>";
            setupMocks(content, 3); // Double-click at the position of "i".
            
            when(document.getChar(anyInt())).thenAnswer(invocation -> {
                int pos = invocation.getArgument(0);
                return pos >= 0 && pos < content.length() ? content.charAt(pos) : '\0';
            });

            strategy.doubleClicked(viewer);

            verify(viewer).setSelectedRange(anyInt(), anyInt());
        }
    }

    @Nested
    @DisplayName("通常の単語選択")
    class NormalWordSelection {

        @Test
        @DisplayName("通常の単語を選択する")
        void selectsNormalWord() throws BadLocationException {
            String content = "hello world";
            setupMocks(content, 2); // Double-click at the position of "l".
            
            when(document.getChar(anyInt())).thenAnswer(invocation -> {
                int pos = invocation.getArgument(0);
                return pos >= 0 && pos < content.length() ? content.charAt(pos) : '\0';
            });

            strategy.doubleClicked(viewer);

            ArgumentCaptor<Integer> startCaptor = ArgumentCaptor.forClass(Integer.class);
            ArgumentCaptor<Integer> lengthCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(viewer).setSelectedRange(startCaptor.capture(), lengthCaptor.capture());
            
            assertEquals(0, startCaptor.getValue());
            assertEquals(5, lengthCaptor.getValue()); // "hello"
        }

        @Test
        @DisplayName("数字を含む単語を選択する")
        void selectsWordWithNumbers() throws BadLocationException {
            String content = "test123 other";
            setupMocks(content, 3); // Double-click at the position of "t".
            
            when(document.getChar(anyInt())).thenAnswer(invocation -> {
                int pos = invocation.getArgument(0);
                return pos >= 0 && pos < content.length() ? content.charAt(pos) : '\0';
            });

            strategy.doubleClicked(viewer);

            ArgumentCaptor<Integer> startCaptor = ArgumentCaptor.forClass(Integer.class);
            ArgumentCaptor<Integer> lengthCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(viewer).setSelectedRange(startCaptor.capture(), lengthCaptor.capture());
            
            assertEquals(0, startCaptor.getValue());
            assertEquals(7, lengthCaptor.getValue()); // "test123"
        }

        @Test
        @DisplayName("アンダースコアを含む単語を選択する")
        void selectsWordWithUnderscore() throws BadLocationException {
            String content = "my_variable";
            setupMocks(content, 5); // Double-click at the position of "a".
            
            when(document.getChar(anyInt())).thenAnswer(invocation -> {
                int pos = invocation.getArgument(0);
                return pos >= 0 && pos < content.length() ? content.charAt(pos) : '\0';
            });

            strategy.doubleClicked(viewer);

            ArgumentCaptor<Integer> startCaptor = ArgumentCaptor.forClass(Integer.class);
            ArgumentCaptor<Integer> lengthCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(viewer).setSelectedRange(startCaptor.capture(), lengthCaptor.capture());
            
            assertEquals(0, startCaptor.getValue());
            assertEquals(11, lengthCaptor.getValue()); // "my_variable"
        }
    }

    @Nested
    @DisplayName("エッジケース")
    class EdgeCases {

        @Test
        @DisplayName("オフセットが負の場合は何もしない")
        void doesNothingForNegativeOffset() {
            when(viewer.getSelectedRange()).thenReturn(new Point(-1, 0));
            
            strategy.doubleClicked(viewer);

            verify(viewer, never()).setSelectedRange(anyInt(), anyInt());
        }

        @Test
        @DisplayName("BadLocationExceptionが発生しても例外をスローしない")
        void handlesExceptionGracefully() throws BadLocationException {
            setupMocks("test", 0);
            when(document.getChar(anyInt())).thenThrow(new BadLocationException("Test exception"));

            assertDoesNotThrow(() -> strategy.doubleClicked(viewer));
        }
    }

    private void setupMocks(String content, int offset) {
        when(viewer.getSelectedRange()).thenReturn(new Point(offset, 0));
        when(viewer.getDocument()).thenReturn(document);
        when(document.getLength()).thenReturn(content.length());
    }
}
