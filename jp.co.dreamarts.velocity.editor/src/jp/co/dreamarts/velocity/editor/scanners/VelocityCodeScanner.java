package jp.co.dreamarts.velocity.editor.scanners;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.swt.SWT;

import jp.co.dreamarts.velocity.editor.preferences.ColorManager;

/**
 * Scanner for Velocity code (directives and variables)
 * Handles #if, #foreach, #set, $variable, ${variable}, etc.
 */
public class VelocityCodeScanner extends RuleBasedScanner {

    // VTL Directives
    private static final String[] DIRECTIVES = {
        "#if", "#else", "#elseif", "#end",
        "#foreach", "#set", "#macro", "#parse", "#include",
        "#stop", "#break", "#evaluate", "#define", "#literal"
    };

    public VelocityCodeScanner(ColorManager colorManager) {
        // Create tokens
        IToken directiveToken = new Token(new TextAttribute(
                colorManager.getColor(ColorManager.VTL_DIRECTIVE), null, SWT.BOLD));
        IToken variableToken = new Token(new TextAttribute(
                colorManager.getColor(ColorManager.VTL_VARIABLE)));
        IToken stringToken = new Token(new TextAttribute(
                colorManager.getColor(ColorManager.VTL_STRING)));
        IToken defaultToken = new Token(new TextAttribute(
                colorManager.getColor(ColorManager.DEFAULT)));

        List<IRule> rules = new ArrayList<>();

        // String rules (double and single quotes)
        rules.add(new SingleLineRule("\"", "\"", stringToken, '\\'));
        rules.add(new SingleLineRule("'", "'", stringToken, '\\'));

        // VTL Variable rules ($var, ${var}, $!var, $!{var})
        rules.add(new VelocityVariableRule(variableToken));

        // VTL Directive rules
        WordRule directiveRule = new WordRule(new VelocityDirectiveDetector(), Token.UNDEFINED);
        for (String directive : DIRECTIVES) {
            directiveRule.addWord(directive, directiveToken);
        }
        rules.add(directiveRule);

        setDefaultReturnToken(defaultToken);
        setRules(rules.toArray(new IRule[rules.size()]));
    }

    /**
     * Word detector for VTL directives (#if, #foreach, etc.)
     */
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
}
