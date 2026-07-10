package io.github.nebosuke.velocityeditor.scanners;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.swt.SWT;

import io.github.nebosuke.velocityeditor.preferences.ColorManager;

/**
 * Scanner for script blocks.
 * Highlights JavaScript syntax while keeping VTL directives/variables prioritized.
 */
public class ScriptBlockScanner extends RuleBasedScanner {

    private static final String[] JAVASCRIPT_KEYWORDS = {
            "const", "let", "var", "function", "return", "if", "else", "for", "while",
            "switch", "case", "break", "continue", "try", "catch", "finally", "throw",
            "new", "class", "extends", "import", "from", "export", "default", "await",
            "async", "true", "false", "null", "undefined"
    };

    public ScriptBlockScanner(ColorManager colorManager) {
        IToken htmlTagToken = new Token(new TextAttribute(
                colorManager.getColor(ColorManager.HTML_TAG), null, SWT.BOLD));
        IToken vtlDirectiveToken = new Token(new TextAttribute(
                colorManager.getColor(ColorManager.VTL_DIRECTIVE), null, SWT.BOLD));
        IToken vtlVariableToken = new Token(new TextAttribute(
                colorManager.getColor(ColorManager.VTL_VARIABLE)));

        IToken jsKeywordToken = new Token(new TextAttribute(
                colorManager.getColor(ColorManager.VTL_DIRECTIVE), null, SWT.BOLD));
        IToken jsFunctionToken = new Token(new TextAttribute(
                colorManager.getColor(ColorManager.VTL_VARIABLE), null, SWT.BOLD));
        IToken jsCommentToken = new Token(new TextAttribute(
                colorManager.getColor(ColorManager.VTL_COMMENT)));
        IToken jsStringToken = new Token(new TextAttribute(
                colorManager.getColor(ColorManager.VTL_STRING)));
        IToken defaultToken = new Token(new TextAttribute(
                colorManager.getColor(ColorManager.DEFAULT)));

        List<IRule> rules = new ArrayList<>();

        // Keep script tag boundaries highlighted as HTML tags.
        rules.add(new MultiLineRule("<script", ">", htmlTagToken, (char) 0, true));
        rules.add(new MultiLineRule("</script", ">", htmlTagToken, (char) 0, true));

        // Prioritize VTL syntax inside script blocks.
        rules.add(new VelocityVariableRule(vtlVariableToken));

        // Velocity and JavaScript comments (must come before the directive WordRule to
        // prevent the WordRule from consuming the leading '#' of "##" before this rule matches).
        rules.add(new EndOfLineRule("##", jsCommentToken));
        rules.add(new MultiLineRule("#*", "*#", jsCommentToken));

        WordRule directiveRule = new WordRule(new VelocityDirectiveDetector(), vtlDirectiveToken);
        rules.add(directiveRule);
        rules.add(new EndOfLineRule("//", jsCommentToken));
        rules.add(new MultiLineRule("/*", "*/", jsCommentToken));
        rules.add(new SingleLineRule("\"", "\"", jsStringToken, '\\'));
        rules.add(new SingleLineRule("'", "'", jsStringToken, '\\'));
        rules.add(new SingleLineRule("`", "`", jsStringToken, '\\'));

        // JavaScript keywords.
        WordRule keywordRule = new WordRule(new JavaScriptWordDetector(), Token.UNDEFINED);
        for (String keyword : JAVASCRIPT_KEYWORDS) {
            keywordRule.addWord(keyword, jsKeywordToken);
        }
        rules.add(keywordRule);

        // JavaScript function names (e.g. greet(), obj.method()).
        rules.add(new JavaScriptFunctionRule(jsFunctionToken));

        setDefaultReturnToken(defaultToken);
        setRules(rules.toArray(new IRule[rules.size()]));
    }

    private static class JavaScriptWordDetector implements IWordDetector {
        @Override
        public boolean isWordStart(char c) {
            return Character.isLetter(c) || c == '_' || c == '$';
        }

        @Override
        public boolean isWordPart(char c) {
            return Character.isLetterOrDigit(c) || c == '_' || c == '$';
        }
    }

    private static class VelocityDirectiveDetector implements IWordDetector {
        @Override
        public boolean isWordStart(char c) {
            return c == '#';
        }

        @Override
        public boolean isWordPart(char c) {
            return Character.isLetterOrDigit(c) || c == '_';
        }
    }

    private static class JavaScriptFunctionRule implements IRule {
        private final IToken successToken;

        JavaScriptFunctionRule(IToken successToken) {
            this.successToken = successToken;
        }

        @Override
        public IToken evaluate(ICharacterScanner scanner) {
            int c = scanner.read();
            int readCount = 1;

            if (!(Character.isLetter(c) || c == '_' || c == '$')) {
                scanner.unread();
                return Token.UNDEFINED;
            }

            while (true) {
                c = scanner.read();
                readCount++;
                if (!(Character.isLetterOrDigit(c) || c == '_' || c == '$')) {
                    break;
                }
            }

            int lookaheadCount = 0;
            while (Character.isWhitespace(c)) {
                c = scanner.read();
                readCount++;
                lookaheadCount++;
            }

            if (c == '(') {
                // Keep token range on function name only.
                scanner.unread();
                for (int i = 0; i < lookaheadCount; i++) {
                    scanner.unread();
                }
                return successToken;
            }

            for (int i = 0; i < readCount; i++) {
                scanner.unread();
            }
            return Token.UNDEFINED;
        }
    }
}