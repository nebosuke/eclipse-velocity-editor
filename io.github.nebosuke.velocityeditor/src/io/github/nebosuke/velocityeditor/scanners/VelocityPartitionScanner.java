package io.github.nebosuke.velocityeditor.scanners;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.Token;

/**
 * Partition scanner for Velocity templates
 * Divides the document into different content types:
 * - VTL comments (single-line and multi-line)
 * - HTML tags
 * - Default content
 */
public class VelocityPartitionScanner extends RuleBasedPartitionScanner {

    // Partition type constants
    public static final String VTL_COMMENT = "__vtl_comment";
    public static final String VTL_MULTILINE_COMMENT = "__vtl_multiline_comment";
    public static final String HTML_TAG = "__html_tag";
    public static final String SCRIPT_BLOCK = "__script_block";

    // All partition types
    public static final String[] PARTITION_TYPES = {
        VTL_COMMENT,
        VTL_MULTILINE_COMMENT,
        HTML_TAG,
        SCRIPT_BLOCK
    };

    public VelocityPartitionScanner() {
        // Create tokens for each partition type
        IToken vtlCommentToken = new Token(VTL_COMMENT);
        IToken vtlMultilineCommentToken = new Token(VTL_MULTILINE_COMMENT);
        IToken htmlTagToken = new Token(HTML_TAG);
        IToken scriptBlockToken = new Token(SCRIPT_BLOCK);

        List<IPredicateRule> rules = new ArrayList<>();

        // VTL multi-line comment: #* ... *#
        rules.add(new MultiLineRule("#*", "*#", vtlMultilineCommentToken));

        // VTL single-line comment: ## ...
        rules.add(new EndOfLineRule("##", vtlCommentToken));

        // Script blocks: <script ...> ... </script>
        // Put this before generic HTML tag rule so script content can be highlighted by a dedicated scanner.
        rules.add(new MultiLineRule("<script", "</script>", scriptBlockToken, (char) 0, true));

        // HTML tags: <...>
        // Using a custom rule to handle tags properly (including attributes)
        rules.add(new HTMLTagRule(htmlTagToken));

        setPredicateRules(rules.toArray(new IPredicateRule[rules.size()]));
    }
}
