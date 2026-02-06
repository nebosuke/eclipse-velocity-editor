package jp.co.dreamarts.velocity.editor.preferences;

/**
 * Preference keys and default values for Velocity Editor
 */
public final class PreferenceConstants {

    private PreferenceConstants() {
    }

    public static final String CONTENT_ASSIST_VARIABLES = "contentAssistVariables";

    public static final String DEFAULT_CONTENT_ASSIST_VARIABLES = String.join("\n",
            "variable",
            "item",
            "collection",
            "user",
            "request",
            "response");
}
