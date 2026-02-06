package jp.co.dreamarts.velocity.editor.assist;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

import jp.co.dreamarts.velocity.editor.Activator;
import jp.co.dreamarts.velocity.editor.preferences.PreferenceConstants;

/**
 * Content assist processor for Velocity templates
 * Provides auto-completion for VTL directives, variables, and HTML tags
 */
public class VelocityContentAssistProcessor implements IContentAssistProcessor {

    // VTL Directive templates
    private static final String[][] VTL_DIRECTIVES = {
        { "#if", "#if($condition)\n\t\n#end", "Conditional statement" },
        { "#elseif", "#elseif($condition)\n\t", "Else-if condition" },
        { "#else", "#else\n\t", "Else clause" },
        { "#end", "#end", "End block" },
        { "#foreach", "#foreach($item in $collection)\n\t$item\n#end", "Loop through collection" },
        { "#set", "#set($variable = value)", "Set variable" },
        { "#macro", "#macro(name $arg1 $arg2)\n\t\n#end", "Define macro" },
        { "#parse", "#parse(\"template.vm\")", "Parse and include template" },
        { "#include", "#include(\"file.txt\")", "Include file as-is" },
        { "#stop", "#stop", "Stop template processing" },
        { "#break", "#break", "Break from foreach loop" },
        { "#evaluate", "#evaluate($dynamicVTL)", "Evaluate VTL string" },
        { "#define", "#define($block)\n\t\n#end", "Define reusable block" },
        { "#literal", "#literal()\n\t\n#end", "Output literal text (no VTL processing)" }
    };

    // Built-in VTL variables
    private static final String[][] BUILTIN_VTL_VARIABLES = {
        { "$foreach.count", "$foreach.count", "Current iteration count (1-based)" },
        { "$foreach.index", "$foreach.index", "Current iteration index (0-based)" },
        { "$foreach.hasNext", "$foreach.hasNext", "True if more elements" },
        { "$foreach.first", "$foreach.first", "True if first iteration" },
        { "$foreach.last", "$foreach.last", "True if last iteration" }
    };

    // Common HTML tags
    private static final String[][] HTML_TAGS = {
        { "<div>", "<div>\n\t\n</div>", "Division container" },
        { "<span>", "<span></span>", "Inline container" },
        { "<p>", "<p></p>", "Paragraph" },
        { "<a>", "<a href=\"\"></a>", "Anchor/link" },
        { "<img>", "<img src=\"\" alt=\"\" />", "Image" },
        { "<ul>", "<ul>\n\t<li></li>\n</ul>", "Unordered list" },
        { "<ol>", "<ol>\n\t<li></li>\n</ol>", "Ordered list" },
        { "<li>", "<li></li>", "List item" },
        { "<table>", "<table>\n\t<tr>\n\t\t<td></td>\n\t</tr>\n</table>", "Table" },
        { "<tr>", "<tr>\n\t<td></td>\n</tr>", "Table row" },
        { "<td>", "<td></td>", "Table cell" },
        { "<th>", "<th></th>", "Table header cell" },
        { "<form>", "<form action=\"\" method=\"post\">\n\t\n</form>", "Form" },
        { "<input>", "<input type=\"text\" name=\"\" />", "Input field" },
        { "<button>", "<button type=\"button\"></button>", "Button" },
        { "<select>", "<select name=\"\">\n\t<option value=\"\"></option>\n</select>", "Dropdown" },
        { "<textarea>", "<textarea name=\"\" rows=\"4\" cols=\"50\"></textarea>", "Text area" },
        { "<h1>", "<h1></h1>", "Heading 1" },
        { "<h2>", "<h2></h2>", "Heading 2" },
        { "<h3>", "<h3></h3>", "Heading 3" },
        { "<header>", "<header>\n\t\n</header>", "Header section" },
        { "<footer>", "<footer>\n\t\n</footer>", "Footer section" },
        { "<nav>", "<nav>\n\t\n</nav>", "Navigation section" },
        { "<main>", "<main>\n\t\n</main>", "Main content" },
        { "<section>", "<section>\n\t\n</section>", "Section" },
        { "<article>", "<article>\n\t\n</article>", "Article" },
        { "<aside>", "<aside>\n\t\n</aside>", "Sidebar content" },
        { "<script>", "<script>\n\t\n</script>", "JavaScript" },
        { "<style>", "<style>\n\t\n</style>", "CSS styles" },
        { "<link>", "<link rel=\"stylesheet\" href=\"\" />", "External stylesheet" },
        { "<meta>", "<meta name=\"\" content=\"\" />", "Meta information" }
    };

    @Override
    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
        List<ICompletionProposal> proposals = new ArrayList<>();
        IDocument document = viewer.getDocument();

        try {
            // Get the text before cursor to determine context
            String prefix = getPrefix(document, offset);
            int prefixLength = prefix.length();
            int replacementOffset = offset - prefixLength;

            // VTL Directives (triggered by #)
            if (prefix.startsWith("#") || prefix.isEmpty()) {
                for (String[] directive : VTL_DIRECTIVES) {
                    if (directive[0].toLowerCase().startsWith(prefix.toLowerCase()) || prefix.isEmpty()) {
                        proposals.add(createProposal(directive[0], directive[1], directive[2],
                                replacementOffset, prefixLength, offset));
                    }
                }
            }

            // VTL Variables (triggered by $)
            if (prefix.startsWith("$") || prefix.isEmpty()) {
                for (String[] variable : getVtlVariables()) {
                    if (variable[0].toLowerCase().startsWith(prefix.toLowerCase()) || prefix.isEmpty()) {
                        proposals.add(createProposal(variable[0], variable[1], variable[2],
                                replacementOffset, prefixLength, offset));
                    }
                }
            }

            // HTML Tags (triggered by <)
            if (prefix.startsWith("<") || prefix.isEmpty()) {
                for (String[] tag : HTML_TAGS) {
                    if (tag[0].toLowerCase().startsWith(prefix.toLowerCase()) || prefix.isEmpty()) {
                        proposals.add(createProposal(tag[0], tag[1], tag[2],
                                replacementOffset, prefixLength, offset));
                    }
                }
            }

            // HTML closing tags (triggered by </)
            if (prefix.startsWith("</")) {
                String[] closingTags = { "</div>", "</span>", "</p>", "</a>", "</ul>", "</ol>", 
                        "</li>", "</table>", "</tr>", "</td>", "</th>", "</form>", 
                        "</select>", "</textarea>", "</h1>", "</h2>", "</h3>",
                        "</header>", "</footer>", "</nav>", "</main>", "</section>",
                        "</article>", "</aside>", "</script>", "</style>" };
                for (String tag : closingTags) {
                    if (tag.toLowerCase().startsWith(prefix.toLowerCase())) {
                        proposals.add(new CompletionProposal(tag, replacementOffset, prefixLength,
                                tag.length(), null, tag, null, "Closing tag"));
                    }
                }
            }

        } catch (BadLocationException e) {
            // Ignore and return empty proposals
        }

        return proposals.toArray(new ICompletionProposal[proposals.size()]);
    }

    /**
     * Get the prefix (partial text) before the cursor
     */
    private String getPrefix(IDocument document, int offset) throws BadLocationException {
        int start = offset;
        while (start > 0) {
            char c = document.getChar(start - 1);
            if (Character.isWhitespace(c) || c == '\n' || c == '\r') {
                break;
            }
            // Stop at certain delimiters but include them in prefix
            if (c == '#' || c == '$' || c == '<') {
                start--;
                break;
            }
            start--;
        }
        return document.get(start, offset - start);
    }

    private List<String[]> getVtlVariables() {
        List<String[]> variables = new ArrayList<>();

        for (String variableName : parseVariableNames(getConfiguredVariablesRaw())) {
            String variable = "$" + variableName;
            variables.add(new String[] { variable, variable, "Custom variable" });
        }

        for (String[] builtinVariable : BUILTIN_VTL_VARIABLES) {
            variables.add(builtinVariable);
        }

        return variables;
    }

    private String getConfiguredVariablesRaw() {
        if (Activator.getDefault() == null) {
            return PreferenceConstants.DEFAULT_CONTENT_ASSIST_VARIABLES;
        }
        return Activator.getDefault().getPreferenceStore()
                .getString(PreferenceConstants.CONTENT_ASSIST_VARIABLES);
    }

    static List<String> parseVariableNames(String rawVariables) {
        Set<String> variableNames = new LinkedHashSet<>();
        if (rawVariables == null || rawVariables.isBlank()) {
            return new ArrayList<>();
        }

        String[] tokens = rawVariables.split("[\\r\\n,]+");
        for (String token : tokens) {
            String normalized = normalizeVariableName(token);
            if (isValidVariableName(normalized)) {
                variableNames.add(normalized);
            }
        }

        return new ArrayList<>(variableNames);
    }

    private static String normalizeVariableName(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.startsWith("$!{")) {
            normalized = normalized.substring(3);
        } else if (normalized.startsWith("${")) {
            normalized = normalized.substring(2);
        } else if (normalized.startsWith("$!")) {
            normalized = normalized.substring(2);
        } else if (normalized.startsWith("$")) {
            normalized = normalized.substring(1);
        }
        if (normalized.endsWith("}")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.trim();
    }

    private static boolean isValidVariableName(String variableName) {
        return variableName != null && variableName.matches("[A-Za-z_][A-Za-z0-9_]*");
    }

    /**
     * Create a completion proposal
     */
    private ICompletionProposal createProposal(String displayString, String replacementString,
            String description, int replacementOffset, int replacementLength, int cursorOffset) {
        
        // Calculate cursor position (find first placeholder or end)
        int cursorPosition = replacementString.length();
        int placeholderPos = replacementString.indexOf("$");
        if (placeholderPos == -1) {
            placeholderPos = replacementString.indexOf("\"\"");
            if (placeholderPos != -1) {
                cursorPosition = placeholderPos + 1;
            }
        }

        return new CompletionProposal(
                replacementString,
                replacementOffset,
                replacementLength,
                cursorPosition,
                null,
                displayString,
                null,
                description
        );
    }

    @Override
    public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
        return null;
    }

    @Override
    public char[] getCompletionProposalAutoActivationCharacters() {
        return new char[] { '#', '$', '<' };
    }

    @Override
    public char[] getContextInformationAutoActivationCharacters() {
        return null;
    }

    @Override
    public String getErrorMessage() {
        return null;
    }

    @Override
    public IContextInformationValidator getContextInformationValidator() {
        return null;
    }
}
