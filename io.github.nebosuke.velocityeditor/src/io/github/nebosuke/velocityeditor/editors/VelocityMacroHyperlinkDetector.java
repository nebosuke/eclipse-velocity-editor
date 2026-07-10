package io.github.nebosuke.velocityeditor.editors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetectorExtension2;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import io.github.nebosuke.velocityeditor.Messages;
import io.github.nebosuke.velocityeditor.assist.MacroDefinitionResolver;
import io.github.nebosuke.velocityeditor.assist.MacroDefinitionResolver.MacroDefinition;

/**
 * Hyperlink detector for Velocity macro invocations.
 */
public class VelocityMacroHyperlinkDetector extends AbstractHyperlinkDetector
        implements IHyperlinkDetectorExtension2 {

    private final MacroDefinitionResolver resolver;
    private final IProject project;
    private final IFile currentFile;

    public VelocityMacroHyperlinkDetector(MacroDefinitionResolver resolver,
            IProject project, IFile currentFile) {
        this.resolver = resolver;
        this.project = project;
        this.currentFile = currentFile;
    }

    @Override
    public int getStateMask() {
        return SWT.MOD1;
    }

    @Override
    public IHyperlink[] detectHyperlinks(org.eclipse.jface.text.ITextViewer textViewer,
            IRegion region, boolean canShowMultipleHyperlinks) {
        if (textViewer == null || region == null) {
            return null;
        }

        IDocument document = textViewer.getDocument();
        String macroName = resolver.findMacroNameAt(document, region.getOffset());
        if (macroName == null || macroName.isBlank()) {
            return null;
        }

        MacroDefinition definition = resolver.resolve(macroName, document, currentFile, project);
        if (definition == null) {
            return null;
        }

        int[] invocationRegion = resolver.findInvocationRegion(document, region.getOffset());
        if (invocationRegion == null) {
            return null;
        }

        IRegion hyperlinkRegion = new Region(invocationRegion[0], invocationRegion[1]);
        IHyperlink hyperlink = new IHyperlink() {
            @Override
            public IRegion getHyperlinkRegion() {
                return hyperlinkRegion;
            }

            @Override
            public String getTypeLabel() {
                return Messages.MACRO_HYPERLINK_TYPE_LABEL;
            }

            @Override
            public String getHyperlinkText() {
                return Messages.format("macro.hyperlink.text.format", definition.getName());
            }

            @Override
            public void open() {
                openDefinition(definition);
            }
        };
        return new IHyperlink[] { hyperlink };
    }

    private void openDefinition(MacroDefinition definition) {
        if (definition.getFile() == null) {
            return;
        }

        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow() == null
                ? null
                : PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        if (page == null) {
            return;
        }

        try {
            IEditorInput editorInput = createFileEditorInput(definition.getFile());
            if (editorInput == null) {
                return;
            }
            IEditorPart part = page.openEditor(editorInput, VelocityEditor.EDITOR_ID);
            if (part instanceof ITextEditor) {
                ((ITextEditor) part).selectAndReveal(definition.getOffset(), definition.getLength());
            }
        } catch (PartInitException e) {
            // Ignore.
        }
    }

    private IEditorInput createFileEditorInput(IFile file) {
        try {
            Class<?> clazz = Class.forName("org.eclipse.ui.part.FileEditorInput");
            Object instance = clazz.getConstructor(IFile.class).newInstance(file);
            if (instance instanceof IEditorInput) {
                return (IEditorInput) instance;
            }
        } catch (ReflectiveOperationException e) {
            // Ignore and return null.
        }
        return null;
    }

    public static VelocityMacroHyperlinkDetector createCommandClickDetector(
            MacroDefinitionResolver resolver, IProject project, IFile currentFile) {
        return new VelocityMacroHyperlinkDetector(resolver, project, currentFile);
    }
}