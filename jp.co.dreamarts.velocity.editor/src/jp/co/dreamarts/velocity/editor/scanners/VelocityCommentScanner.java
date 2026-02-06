package jp.co.dreamarts.velocity.editor.scanners;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.swt.SWT;

import jp.co.dreamarts.velocity.editor.preferences.ColorManager;

/**
 * Scanner for VTL comments
 * Simply colors the entire comment region
 */
public class VelocityCommentScanner extends RuleBasedScanner {

    public VelocityCommentScanner(ColorManager colorManager) {
        IToken commentToken = new Token(new TextAttribute(
                colorManager.getColor(ColorManager.VTL_COMMENT), null, SWT.ITALIC));
        setDefaultReturnToken(commentToken);
    }
}
