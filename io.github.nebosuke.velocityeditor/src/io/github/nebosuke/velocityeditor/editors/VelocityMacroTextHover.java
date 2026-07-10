package io.github.nebosuke.velocityeditor.editors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;

import io.github.nebosuke.velocityeditor.Messages;
import io.github.nebosuke.velocityeditor.assist.JavaScriptFunctionResolver;
import io.github.nebosuke.velocityeditor.assist.MacroCommentFormatter;
import io.github.nebosuke.velocityeditor.assist.MacroDefinitionResolver;
import io.github.nebosuke.velocityeditor.assist.JavaScriptFunctionResolver.JavaScriptFunctionDefinition;
import io.github.nebosuke.velocityeditor.assist.MacroDefinitionResolver.MacroDefinition;

/**
 * Text hover for Velocity macro invocations.
 */
public class VelocityMacroTextHover implements ITextHover {

    private final MacroDefinitionResolver resolver;
    private final JavaScriptFunctionResolver javaScriptFunctionResolver;
    private final IProject project;
    private final IFile currentFile;

    public VelocityMacroTextHover(MacroDefinitionResolver resolver, IProject project, IFile currentFile) {
        this.resolver = resolver;
        this.javaScriptFunctionResolver = new JavaScriptFunctionResolver();
        this.project = project;
        this.currentFile = currentFile;
    }

    @Override
    public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
        if (textViewer == null || hoverRegion == null) {
            return null;
        }

        IDocument document = textViewer.getDocument();
        String text = document.get();
        if (javaScriptFunctionResolver.isOffsetInScriptBlock(text, hoverRegion.getOffset())) {
            String functionName = javaScriptFunctionResolver.findFunctionNameAt(document, hoverRegion.getOffset());
            if (functionName != null && !functionName.isBlank()) {
                JavaScriptFunctionDefinition definition = javaScriptFunctionResolver.resolve(functionName, document);
                if (definition != null) {
                    String args = definition.getArguments().isEmpty()
                            ? Messages.MACRO_ARGS_NONE_SHORT
                            : String.join(", ", definition.getArguments());
                    String comment = definition.getComment() == null || definition.getComment().isBlank()
                            ? Messages.JAVASCRIPT_FUNCTION_DEFINED
                            : MacroCommentFormatter.format(definition.getComment());
                    return Messages.format("javascript.hover.format",
                            definition.getName(), Messages.MACRO_ARGS_LABEL, args, comment);
                }
            }
        }

        String macroName = resolver.findMacroNameAt(document, hoverRegion.getOffset());
        if (macroName == null || macroName.isBlank()) {
            return null;
        }

        MacroDefinition definition = resolver.resolve(macroName, document, currentFile, project);
        if (definition == null) {
            return null;
        }

        String args = definition.getArguments().isEmpty()
                ? Messages.MACRO_ARGS_NONE_SHORT
                : String.join(", ", definition.getArguments());
        String comment = definition.getComment() == null || definition.getComment().isBlank()
                ? Messages.MACRO_COMMENT_NONE
                : MacroCommentFormatter.format(definition.getComment());

        return Messages.format("macro.hover.format",
                definition.getName(), Messages.MACRO_ARGS_LABEL, args, comment);
    }

    @Override
    public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
        if (textViewer == null) {
            return null;
        }

        IDocument document = textViewer.getDocument();
        if (javaScriptFunctionResolver.isOffsetInScriptBlock(document.get(), offset)) {
            int[] scriptRegion = javaScriptFunctionResolver.findInvocationRegion(document, offset);
            if (scriptRegion != null) {
                return new Region(scriptRegion[0], scriptRegion[1]);
            }
        }

        int[] region = resolver.findInvocationRegion(document, offset);
        if (region == null) {
            return null;
        }
        return new Region(region[0], region[1]);
    }
}