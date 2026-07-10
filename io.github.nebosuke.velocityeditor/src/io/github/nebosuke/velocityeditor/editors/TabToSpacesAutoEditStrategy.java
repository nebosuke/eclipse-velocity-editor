package io.github.nebosuke.velocityeditor.editors;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.preference.IPreferenceStore;

import io.github.nebosuke.velocityeditor.Activator;
import io.github.nebosuke.velocityeditor.preferences.PreferenceConstants;

/**
 * Converts a plain Tab keystroke into spaces aligned to the next tab stop,
 * when enabled via the Velocity Editor preferences.
 */
public class TabToSpacesAutoEditStrategy implements IAutoEditStrategy {

    @Override
    public void customizeDocumentCommand(IDocument document, DocumentCommand command) {
        if (!"\t".equals(command.text)) {
            return;
        }

        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        if (!store.getBoolean(PreferenceConstants.INSERT_SPACES_FOR_TABS)) {
            return;
        }

        int tabWidth = store.getInt(PreferenceConstants.TAB_WIDTH);
        if (tabWidth <= 0) {
            tabWidth = PreferenceConstants.DEFAULT_TAB_WIDTH;
        }

        try {
            int line = document.getLineOfOffset(command.offset);
            int column = command.offset - document.getLineOffset(line);
            int spaces = spacesToNextStop(column, tabWidth);
            command.text = " ".repeat(spaces);
        } catch (BadLocationException e) {
            // Leave the command untouched; the caret position could not be resolved.
        }
    }

    static int spacesToNextStop(int column, int tabWidth) {
        int remainder = column % tabWidth;
        return tabWidth - remainder;
    }
}
