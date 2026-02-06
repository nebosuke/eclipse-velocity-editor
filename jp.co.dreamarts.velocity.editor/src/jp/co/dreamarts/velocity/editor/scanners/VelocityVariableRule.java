package jp.co.dreamarts.velocity.editor.scanners;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

/**
 * Rule for detecting Velocity variables
 * Matches: $variable, ${variable}, $!variable, $!{variable}
 * Also handles method calls: $variable.method(), ${variable.method()}
 */
public class VelocityVariableRule implements IRule {

    private final IToken successToken;

    public VelocityVariableRule(IToken token) {
        this.successToken = token;
    }

    @Override
    public IToken evaluate(ICharacterScanner scanner) {
        int c = scanner.read();

        if (c == '$') {
            int readCount = 1;
            
            // Check for silent reference modifier (!)
            c = scanner.read();
            readCount++;
            if (c == '!') {
                c = scanner.read();
                readCount++;
            }

            // Check for formal reference syntax ({)
            boolean formal = false;
            if (c == '{') {
                formal = true;
                c = scanner.read();
                readCount++;
            }

            // Variable name must start with letter or underscore
            if (Character.isLetter(c) || c == '_') {
                readCount++;
                
                // Read variable name
                while (true) {
                    c = scanner.read();
                    if (Character.isLetterOrDigit(c) || c == '_' || c == '-') {
                        readCount++;
                        continue;
                    }
                    
                    // Handle property access (.)
                    if (c == '.') {
                        int next = scanner.read();
                        if (Character.isLetter(next) || next == '_') {
                            readCount += 2;
                            // Read property/method name
                            while (true) {
                                c = scanner.read();
                                if (Character.isLetterOrDigit(c) || c == '_') {
                                    readCount++;
                                    continue;
                                }
                                // Handle method call ()
                                if (c == '(') {
                                    readCount++;
                                    int parenDepth = 1;
                                    while (parenDepth > 0) {
                                        c = scanner.read();
                                        if (c == ICharacterScanner.EOF) {
                                            break;
                                        }
                                        readCount++;
                                        if (c == '(') parenDepth++;
                                        if (c == ')') parenDepth--;
                                    }
                                    c = scanner.read();
                                }
                                break;
                            }
                            // Check for another property access
                            if (c == '.') {
                                scanner.unread();
                                continue;
                            }
                        } else {
                            scanner.unread();
                        }
                    }
                    break;
                }
                
                // For formal syntax, read until closing brace
                if (formal) {
                    while (c != '}' && c != ICharacterScanner.EOF) {
                        c = scanner.read();
                        readCount++;
                    }
                    if (c == '}') {
                        return successToken;
                    }
                } else {
                    scanner.unread();
                    return successToken;
                }
            }

            // Not a valid variable, unread everything
            for (int i = 0; i < readCount; i++) {
                scanner.unread();
            }
            return Token.UNDEFINED;
        }

        scanner.unread();
        return Token.UNDEFINED;
    }
}
