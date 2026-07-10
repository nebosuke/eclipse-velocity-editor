package io.github.nebosuke.velocityeditor.scanners;

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

import io.github.nebosuke.velocityeditor.preferences.ColorManager;

/**
 * Scanner for HTML tags
 * Handles tag names, attributes, and attribute values
 */
public class HTMLTagScanner extends RuleBasedScanner {

    public HTMLTagScanner(ColorManager colorManager) {
        // Create tokens
        IToken tagToken = new Token(new TextAttribute(
                colorManager.getColor(ColorManager.HTML_TAG), null, SWT.BOLD));
        IToken attributeToken = new Token(new TextAttribute(
                colorManager.getColor(ColorManager.HTML_ATTRIBUTE)));
        IToken attributeValueToken = new Token(new TextAttribute(
                colorManager.getColor(ColorManager.HTML_ATTRIBUTE_VALUE)));
        IToken variableToken = new Token(new TextAttribute(
                colorManager.getColor(ColorManager.VTL_VARIABLE)));

        List<IRule> rules = new ArrayList<>();

        // VTL variables inside HTML tags
        rules.add(new VelocityVariableRule(variableToken));

        // Attribute values (quoted strings)
        rules.add(new SingleLineRule("\"", "\"", attributeValueToken, '\\'));
        rules.add(new SingleLineRule("'", "'", attributeValueToken, '\\'));

        // HTML tag names and attributes
        WordRule tagRule = new WordRule(new HTMLWordDetector(), attributeToken);
        
        // Common HTML tags (these will be bold)
        String[] htmlTags = {
            "html", "head", "body", "div", "span", "p", "a", "img",
            "table", "tr", "td", "th", "thead", "tbody", "tfoot",
            "ul", "ol", "li", "dl", "dt", "dd",
            "form", "input", "button", "select", "option", "textarea", "label",
            "h1", "h2", "h3", "h4", "h5", "h6",
            "header", "footer", "nav", "main", "section", "article", "aside",
            "script", "style", "link", "meta", "title",
            "br", "hr", "pre", "code", "blockquote",
            "strong", "em", "b", "i", "u", "small", "sub", "sup",
            "iframe", "video", "audio", "canvas", "svg"
        };
        
        for (String tag : htmlTags) {
            tagRule.addWord(tag, tagToken);
            // Closing tags (e.g. </div>) are read as a single word "/div" by the
            // WordRule, so register the slash-prefixed form to bold them as well.
            tagRule.addWord("/" + tag, tagToken);
        }
        rules.add(tagRule);

        setDefaultReturnToken(tagToken);
        setRules(rules.toArray(new IRule[rules.size()]));
    }

    /**
     * Word detector for HTML tag names and attributes
     */
    private static class HTMLWordDetector implements IWordDetector {
        @Override
        public boolean isWordStart(char c) {
            return Character.isLetter(c) || c == '_' || c == '/';
        }

        @Override
        public boolean isWordPart(char c) {
            return Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == ':';
        }
    }
}
