package io.github.nebosuke.velocityeditor.scanners;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.swt.SWT;

import io.github.nebosuke.velocityeditor.preferences.ColorManager;

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
