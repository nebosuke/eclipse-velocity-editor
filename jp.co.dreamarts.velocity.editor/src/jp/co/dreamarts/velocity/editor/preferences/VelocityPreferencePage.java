package jp.co.dreamarts.velocity.editor.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import jp.co.dreamarts.velocity.editor.Activator;

/**
 * Preference page for Velocity Editor settings
 */
public class VelocityPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    private Text contentAssistVariablesText;

    public VelocityPreferencePage() {
        super("Velocity Editor");
        setDescription("Settings for Velocity Template Editor");
    }

    @Override
    public void init(IWorkbench workbench) {
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        Label infoLabel = new Label(composite, SWT.NONE);
        infoLabel.setText("Velocity Template Editor v1.0.0\n\n" +
                "Supported file extensions: .vtl, .vm\n\n" +
                "Features:\n" +
                "  • VTL syntax highlighting (directives, variables, comments)\n" +
                "  • HTML syntax highlighting (tags, attributes)\n" +
                "  • Content assist for VTL and HTML\n" +
                "  • Double-click word selection\n" +
                "  • Light/Dark theme support");

        Label variableLabel = new Label(composite, SWT.NONE);
        variableLabel.setText("Content Assist variables for '$' (one variable name per line):");
        variableLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        contentAssistVariablesText = new Text(composite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
        GridData textGridData = new GridData(SWT.FILL, SWT.FILL, true, false);
        textGridData.heightHint = 120;
        contentAssistVariablesText.setLayoutData(textGridData);
        contentAssistVariablesText.setText(getPreferenceStore()
                .getString(PreferenceConstants.CONTENT_ASSIST_VARIABLES));

        return composite;
    }

    @Override
    public boolean performOk() {
        getPreferenceStore().setValue(
                PreferenceConstants.CONTENT_ASSIST_VARIABLES,
                contentAssistVariablesText.getText());
        return super.performOk();
    }

    @Override
    protected void performDefaults() {
        contentAssistVariablesText.setText(PreferenceConstants.DEFAULT_CONTENT_ASSIST_VARIABLES);
        super.performDefaults();
    }
}
