package io.github.nebosuke.velocityeditor.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import io.github.nebosuke.velocityeditor.Activator;

/**
 * Preference page for Velocity Editor settings
 */
public class VelocityPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    private Text contentAssistVariablesText;
    private Button insertSpacesForTabsButton;
    private Spinner tabWidthSpinner;

    public VelocityPreferencePage() {
        super("Velocity Editor");
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

        Label variableLabel = new Label(composite, SWT.NONE);
        variableLabel.setText("Content Assist variables for '$' (one variable name per line):");
        variableLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        contentAssistVariablesText = new Text(composite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
        GridData textGridData = new GridData(SWT.FILL, SWT.FILL, true, false);
        textGridData.heightHint = 120;
        contentAssistVariablesText.setLayoutData(textGridData);
        contentAssistVariablesText.setText(getPreferenceStore()
                .getString(PreferenceConstants.CONTENT_ASSIST_VARIABLES));

        insertSpacesForTabsButton = new Button(composite, SWT.CHECK);
        insertSpacesForTabsButton.setText("Insert spaces for tabs");
        insertSpacesForTabsButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        insertSpacesForTabsButton.setSelection(getPreferenceStore()
                .getBoolean(PreferenceConstants.INSERT_SPACES_FOR_TABS));

        Label tabWidthLabel = new Label(composite, SWT.NONE);
        tabWidthLabel.setText("Tab width:");
        tabWidthLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        tabWidthSpinner = new Spinner(composite, SWT.BORDER);
        tabWidthSpinner.setMinimum(1);
        tabWidthSpinner.setMaximum(16);
        tabWidthSpinner.setSelection(getPreferenceStore().getInt(PreferenceConstants.TAB_WIDTH));
        tabWidthSpinner.setEnabled(insertSpacesForTabsButton.getSelection());

        insertSpacesForTabsButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                tabWidthSpinner.setEnabled(insertSpacesForTabsButton.getSelection());
            }
        });

        return composite;
    }

    @Override
    public boolean performOk() {
        getPreferenceStore().setValue(
                PreferenceConstants.CONTENT_ASSIST_VARIABLES,
                contentAssistVariablesText.getText());
        getPreferenceStore().setValue(
                PreferenceConstants.INSERT_SPACES_FOR_TABS,
                insertSpacesForTabsButton.getSelection());
        getPreferenceStore().setValue(
                PreferenceConstants.TAB_WIDTH,
                tabWidthSpinner.getSelection());
        return super.performOk();
    }

    @Override
    protected void performDefaults() {
        contentAssistVariablesText.setText(PreferenceConstants.DEFAULT_CONTENT_ASSIST_VARIABLES);
        insertSpacesForTabsButton.setSelection(PreferenceConstants.DEFAULT_INSERT_SPACES_FOR_TABS);
        tabWidthSpinner.setSelection(PreferenceConstants.DEFAULT_TAB_WIDTH);
        tabWidthSpinner.setEnabled(PreferenceConstants.DEFAULT_INSERT_SPACES_FOR_TABS);
        super.performDefaults();
    }
}
