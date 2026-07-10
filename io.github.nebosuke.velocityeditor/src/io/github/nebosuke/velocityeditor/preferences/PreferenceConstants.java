package io.github.nebosuke.velocityeditor.preferences;

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

    public static final String INSERT_SPACES_FOR_TABS = "insertSpacesForTabs";

    public static final boolean DEFAULT_INSERT_SPACES_FOR_TABS = false;

    public static final String TAB_WIDTH = "tabWidth";

    public static final int DEFAULT_TAB_WIDTH = 4;
}
