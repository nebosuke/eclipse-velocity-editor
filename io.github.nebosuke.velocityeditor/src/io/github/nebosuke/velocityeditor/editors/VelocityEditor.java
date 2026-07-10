package io.github.nebosuke.velocityeditor.editors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.editors.text.TextEditor;

import io.github.nebosuke.velocityeditor.preferences.ColorManager;

/**
 * Main editor class for Velocity Template files
 */
public class VelocityEditor extends TextEditor {

    public static final String EDITOR_ID = "io.github.nebosuke.velocityeditor.VelocityEditor";

    private final ColorManager colorManager;
    private final VelocityConfiguration configuration;

    public VelocityEditor() {
        super();
        colorManager = new ColorManager();
        configuration = new VelocityConfiguration(colorManager, null);
        setSourceViewerConfiguration(configuration);
        setDocumentProvider(new VelocityDocumentProvider());
    }

    @Override
    protected boolean isTabsToSpacesConversionEnabled() {
        // The platform-wide "Insert spaces for tabs" preference (General > Editors > Text
        // Editors) would otherwise convert Tab to spaces regardless of this editor's own
        // "Insert spaces for tabs" setting; disable it so TabToSpacesAutoEditStrategy is
        // the sole authority over Tab-key behavior in the Velocity editor.
        return false;
    }

    @Override
    protected void doSetInput(IEditorInput input) throws CoreException {
        super.doSetInput(input);
        configuration.setFile(resolveFile(input));
        configuration.setProject(resolveProject(input));
    }

    private IFile resolveFile(IEditorInput input) {
        if (input == null) {
            return null;
        }
        return input.getAdapter(IFile.class);
    }

    private IProject resolveProject(IEditorInput input) {
        IFile file = input == null ? null : input.getAdapter(IFile.class);
        if (file != null) {
            return file.getProject();
        }
        return null;
    }

    @Override
    public void dispose() {
        colorManager.dispose();
        super.dispose();
    }
}
