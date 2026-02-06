package jp.co.dreamarts.velocity.editor.scanners;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

/**
 * Rule for detecting HTML tags including their attributes
 * Handles <tag>, </tag>, and <tag attr="value">
 */
public class HTMLTagRule implements IPredicateRule {

    private final IToken successToken;

    public HTMLTagRule(IToken successToken) {
        this.successToken = successToken;
    }

    @Override
    public IToken evaluate(ICharacterScanner scanner) {
        return evaluate(scanner, false);
    }

    @Override
    public IToken evaluate(ICharacterScanner scanner, boolean resume) {
        int c = scanner.read();

        if (c == '<') {
            // Check for comment or special tags
            int next = scanner.read();
            
            // Skip <!-- comments --> and <!DOCTYPE>
            if (next == '!') {
                int third = scanner.read();
                if (third == '-') {
                    // HTML comment <!-- -->
                    scanner.unread();
                    scanner.unread();
                    scanner.unread();
                    return Token.UNDEFINED;
                }
                scanner.unread();
            }
            
            // Skip <? processing instructions ?>
            if (next == '?') {
                scanner.unread();
                scanner.unread();
                return Token.UNDEFINED;
            }
            
            scanner.unread();

            // Read until we find the closing >
            boolean inString = false;
            char stringChar = 0;
            int readCount = 1; // Already read '<'

            while (true) {
                c = scanner.read();
                readCount++;

                if (c == ICharacterScanner.EOF) {
                    // Unread everything and return undefined
                    for (int i = 0; i < readCount; i++) {
                        scanner.unread();
                    }
                    return Token.UNDEFINED;
                }

                // Handle strings within tags
                if (!inString && (c == '"' || c == '\'')) {
                    inString = true;
                    stringChar = (char) c;
                } else if (inString && c == stringChar) {
                    inString = false;
                }

                // Found closing bracket (not inside a string)
                if (!inString && c == '>') {
                    return successToken;
                }

                // Line break without closing - might not be a valid tag
                if (c == '\n' || c == '\r') {
                    // Allow multi-line tags (common in HTML with attributes)
                    continue;
                }
            }
        }

        scanner.unread();
        return Token.UNDEFINED;
    }

    @Override
    public IToken getSuccessToken() {
        return successToken;
    }
}
