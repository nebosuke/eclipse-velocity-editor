package jp.co.dreamarts.velocity.editor.editors;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextViewer;

/**
 * Double-click strategy for Velocity editor
 * Handles word selection including VTL variables and HTML elements
 */
public class VelocityDoubleClickStrategy implements ITextDoubleClickStrategy {

    protected ITextViewer textViewer;

    @Override
    public void doubleClicked(ITextViewer viewer) {
        int offset = viewer.getSelectedRange().x;

        if (offset < 0) {
            return;
        }

        textViewer = viewer;

        IDocument document = viewer.getDocument();
        try {
            // Try to select VTL variable first ($var, ${var}, $!var, $!{var})
            if (selectVelocityVariable(document, offset)) {
                return;
            }

            // Try to select HTML tag name
            if (selectHTMLTagName(document, offset)) {
                return;
            }

            // Default: select word
            selectWord(document, offset);

        } catch (BadLocationException e) {
            // Ignore and use default behavior
        }
    }

    /**
     * Select a Velocity variable ($var, ${var}, $!var, $!{var})
     */
    private boolean selectVelocityVariable(IDocument document, int offset) throws BadLocationException {
        int docLength = document.getLength();
        
        // Look backwards for $ sign
        int start = offset;
        while (start > 0) {
            char c = document.getChar(start - 1);
            if (c == '$') {
                start--;
                break;
            }
            if (!isVelocityVariableChar(c) && c != '!' && c != '{') {
                break;
            }
            start--;
        }
        
        // Check if we found a $ sign
        if (start >= docLength || document.getChar(start) != '$') {
            return false;
        }
        
        // Now find the end of the variable
        int end = start + 1;
        boolean inBraces = false;
        
        // Skip ! if present
        if (end < docLength && document.getChar(end) == '!') {
            end++;
        }
        
        // Check for braces
        if (end < docLength && document.getChar(end) == '{') {
            inBraces = true;
            end++;
        }
        
        // Find the end of variable name
        while (end < docLength) {
            char c = document.getChar(end);
            if (inBraces) {
                if (c == '}') {
                    end++;
                    break;
                }
            } else {
                if (!isVelocityVariableChar(c)) {
                    break;
                }
            }
            end++;
        }
        
        // Check if we have a valid variable
        if (end > start + 1) {
            textViewer.setSelectedRange(start, end - start);
            return true;
        }
        
        return false;
    }

    /**
     * Select an HTML tag name
     */
    private boolean selectHTMLTagName(IDocument document, int offset) throws BadLocationException {
        int docLength = document.getLength();
        
        // Check if we're inside an HTML tag
        int tagStart = -1;
        for (int i = offset; i >= 0; i--) {
            char c = document.getChar(i);
            if (c == '<') {
                tagStart = i;
                break;
            }
            if (c == '>') {
                break;
            }
        }
        
        if (tagStart < 0) {
            return false;
        }
        
        // Find the tag name start (after < and optional /)
        int nameStart = tagStart + 1;
        if (nameStart < docLength && document.getChar(nameStart) == '/') {
            nameStart++;
        }
        
        // Skip whitespace
        while (nameStart < docLength && Character.isWhitespace(document.getChar(nameStart))) {
            nameStart++;
        }
        
        // Find the end of tag name
        int nameEnd = nameStart;
        while (nameEnd < docLength) {
            char c = document.getChar(nameEnd);
            if (!Character.isLetterOrDigit(c) && c != '-' && c != '_' && c != ':') {
                break;
            }
            nameEnd++;
        }
        
        // Check if offset is within the tag name
        if (offset >= nameStart && offset <= nameEnd && nameEnd > nameStart) {
            textViewer.setSelectedRange(nameStart, nameEnd - nameStart);
            return true;
        }
        
        return false;
    }

    /**
     * Select a word at the given offset
     */
    private void selectWord(IDocument document, int offset) throws BadLocationException {
        int docLength = document.getLength();
        
        // Find word start
        int start = offset;
        while (start > 0) {
            char c = document.getChar(start - 1);
            if (!isWordChar(c)) {
                break;
            }
            start--;
        }
        
        // Find word end
        int end = offset;
        while (end < docLength) {
            char c = document.getChar(end);
            if (!isWordChar(c)) {
                break;
            }
            end++;
        }
        
        if (end > start) {
            textViewer.setSelectedRange(start, end - start);
        }
    }

    /**
     * Check if character is valid in a Velocity variable name
     */
    private boolean isVelocityVariableChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.';
    }

    /**
     * Check if character is valid in a word
     */
    private boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
