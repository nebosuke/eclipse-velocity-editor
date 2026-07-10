package io.github.nebosuke.velocityeditor.scanners;

import org.eclipse.jface.text.rules.ICharacterScanner;

/**
 * ICharacterScanner implementation for tests.
 * A simple scanner implementation over a String.
 */
public class StringCharacterScanner implements ICharacterScanner {

    private final String content;
    private int position;
    private int mark;

    public StringCharacterScanner(String content) {
        this.content = content;
        this.position = 0;
        this.mark = 0;
    }

    @Override
    public char[][] getLegalLineDelimiters() {
        return new char[][] { { '\n' }, { '\r', '\n' }, { '\r' } };
    }

    @Override
    public int getColumn() {
        int column = 0;
        for (int i = position - 1; i >= 0; i--) {
            if (content.charAt(i) == '\n' || content.charAt(i) == '\r') {
                break;
            }
            column++;
        }
        return column;
    }

    @Override
    public int read() {
        if (position >= content.length()) {
            return EOF;
        }
        return content.charAt(position++);
    }

    @Override
    public void unread() {
        if (position > 0) {
            position--;
        }
    }

    /**
     * Marks the current position.
     */
    public void mark() {
        mark = position;
    }

    /**
     * Resets to the marked position.
     */
    public void reset() {
        position = mark;
    }

    /**
     * Returns the current position.
     */
    public int getPosition() {
        return position;
    }

    /**
     * Sets the current position.
     */
    public void setPosition(int position) {
        this.position = position;
    }

    /**
     * Returns the number of characters read.
     */
    public int getReadCount() {
        return position - mark;
    }
    
    /**
     * Returns the content length.
     */
    public int getLength() {
        return content.length();
    }
}
