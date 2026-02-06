package jp.co.dreamarts.velocity.editor.editors;

import org.eclipse.ui.editors.text.TextEditor;

import jp.co.dreamarts.velocity.editor.preferences.ColorManager;

/**
 * Main editor class for Velocity Template files
 */
public class VelocityEditor extends TextEditor {

    private ColorManager colorManager;

    public VelocityEditor() {
        super();
        colorManager = new ColorManager();
        setSourceViewerConfiguration(new VelocityConfiguration(colorManager));
        setDocumentProvider(new VelocityDocumentProvider());
    }

    @Override
    public void dispose() {
        colorManager.dispose();
        super.dispose();
    }
}
