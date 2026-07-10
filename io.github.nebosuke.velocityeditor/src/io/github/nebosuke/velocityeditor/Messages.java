package io.github.nebosuke.velocityeditor;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Message access utility for i18n.
 */
public final class Messages {

    private static final String BUNDLE_NAME = "io.github.nebosuke.velocityeditor.messages";
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(
            BUNDLE_NAME, Locale.getDefault(), Messages.class.getClassLoader());

    public static final String MACRO_ARGS_LABEL = getString("macro.args.label");
    public static final String MACRO_ARGS_NONE = getString("macro.args.none");
    public static final String MACRO_ARGS_NONE_SHORT = getString("macro.args.none.short");
    public static final String MACRO_COMMENT_NONE = getString("macro.comment.none");
    public static final String MACRO_PROJECT_DEFINED = getString("macro.project.defined");
    public static final String MACRO_HYPERLINK_TYPE_LABEL = getString("macro.hyperlink.typeLabel");
    public static final String MACRO_REFRESH_JOB_NAME = getString("macro.refresh.job.name");
    public static final String JAVASCRIPT_FUNCTION_DEFINED = getString("javascript.function.defined");

    private Messages() {
    }

    public static String getString(String key) {
        try {
            return BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    public static String format(String key, Object... arguments) {
        return MessageFormat.format(getString(key), arguments);
    }
}
