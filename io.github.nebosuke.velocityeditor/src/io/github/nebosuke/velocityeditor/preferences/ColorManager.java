package io.github.nebosuke.velocityeditor.preferences;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractTextEditor;

/**
 * Color manager for Velocity Editor with light/dark theme support
 */
public class ColorManager {

    // Color keys
    public static final String VTL_DIRECTIVE = "vtl_directive";
    public static final String VTL_VARIABLE = "vtl_variable";
    public static final String VTL_COMMENT = "vtl_comment";
    public static final String VTL_STRING = "vtl_string";
    public static final String HTML_TAG = "html_tag";
    public static final String HTML_ATTRIBUTE = "html_attribute";
    public static final String HTML_ATTRIBUTE_VALUE = "html_attribute_value";
    public static final String DEFAULT = "default";

    // Light theme colors
    private static final Map<String, RGB> LIGHT_COLORS = new HashMap<>();
    static {
        LIGHT_COLORS.put(VTL_DIRECTIVE, new RGB(123, 31, 162));       // #7B1FA2 Purple
        LIGHT_COLORS.put(VTL_VARIABLE, new RGB(21, 101, 192));        // #1565C0 Blue
        LIGHT_COLORS.put(VTL_COMMENT, new RGB(85, 139, 47));          // #558B2F Green
        LIGHT_COLORS.put(VTL_STRING, new RGB(46, 125, 50));           // #2E7D32 Green
        LIGHT_COLORS.put(HTML_TAG, new RGB(230, 81, 0));              // #E65100 Orange
        LIGHT_COLORS.put(HTML_ATTRIBUTE, new RGB(198, 40, 40));       // #C62828 Red
        LIGHT_COLORS.put(HTML_ATTRIBUTE_VALUE, new RGB(46, 125, 50)); // #2E7D32 Green
        LIGHT_COLORS.put(DEFAULT, new RGB(33, 33, 33));               // #212121 Dark gray
    }

    // Dark theme colors (higher contrast for readability)
    private static final Map<String, RGB> DARK_COLORS = new HashMap<>();
    static {
        DARK_COLORS.put(VTL_DIRECTIVE, new RGB(199, 146, 234));       // #C792EA Purple
        DARK_COLORS.put(VTL_VARIABLE, new RGB(130, 170, 255));        // #82AAFF Blue
        DARK_COLORS.put(VTL_COMMENT, new RGB(127, 132, 142));         // #7F848E Muted gray
        DARK_COLORS.put(VTL_STRING, new RGB(195, 232, 141));          // #C3E88D Green
        DARK_COLORS.put(HTML_TAG, new RGB(255, 203, 107));            // #FFCB6B Yellow/orange
        DARK_COLORS.put(HTML_ATTRIBUTE, new RGB(247, 140, 108));      // #F78C6C Orange
        DARK_COLORS.put(HTML_ATTRIBUTE_VALUE, new RGB(195, 232, 141));// #C3E88D Green
        DARK_COLORS.put(DEFAULT, new RGB(212, 212, 212));             // #D4D4D4 Light gray
    }

    private final Map<RGB, Color> colorCache = new HashMap<>();
    private Boolean cachedIsDarkTheme = null;
    private Display cachedDisplay = null;

    /**
     * Detect if the current Eclipse theme is dark
     */
    public boolean isDarkTheme() {
        Display display = Display.getCurrent();
        if (display == null) {
            display = Display.getDefault();
        }
        
        // Use cached value if display hasn't changed
        if (cachedDisplay == display && cachedIsDarkTheme != null) {
            return cachedIsDarkTheme;
        }
        
        cachedDisplay = display;

        Boolean themeBasedResult = detectDarkThemeFromWorkbenchTheme();
        if (themeBasedResult != null) {
            cachedIsDarkTheme = themeBasedResult;
            return cachedIsDarkTheme;
        }

        Boolean editorPrefResult = detectDarkThemeFromEditorPreferences();
        if (editorPrefResult != null) {
            cachedIsDarkTheme = editorPrefResult;
            return cachedIsDarkTheme;
        }
        
        // Fallback: infer from system background colors
        Color listBgColor = display.getSystemColor(SWT.COLOR_LIST_BACKGROUND);
        Color widgetBgColor = display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
        
        // Some environments report one color inaccurately, so use the darker one.
        float listLuminance = calculateLuminance(listBgColor.getRGB());
        float widgetLuminance = calculateLuminance(widgetBgColor.getRGB());
        float bgLuminance = Math.min(listLuminance, widgetLuminance);
        
        cachedIsDarkTheme = isDarkByLuminance(bgLuminance);
        return cachedIsDarkTheme;
    }

    static boolean isDarkByLuminance(float luminance) {
        return luminance < 0.45f;
    }

    static float calculateLuminance(RGB color) {
        return (color.red * 0.299f + color.green * 0.587f + color.blue * 0.114f) / 255f;
    }

    static Boolean detectDarkThemeFromThemeId(String themeId) {
        if (themeId == null) {
            return null;
        }
        String normalized = themeId.toLowerCase();
        if (normalized.contains("dark")) {
            return Boolean.TRUE;
        }
        if (normalized.contains("light")) {
            return Boolean.FALSE;
        }
        return null;
    }

    private Boolean detectDarkThemeFromWorkbenchTheme() {
        if (!PlatformUI.isWorkbenchRunning()) {
            return null;
        }
        try {
            String themeId = PlatformUI.getWorkbench()
                    .getThemeManager()
                    .getCurrentTheme()
                    .getId();
            return detectDarkThemeFromThemeId(themeId);
        } catch (IllegalStateException e) {
            return null;
        }
    }

    private Boolean detectDarkThemeFromEditorPreferences() {
        if (!PlatformUI.isWorkbenchRunning()) {
            return null;
        }
        try {
            RGB editorBg = PreferenceConverter.getColor(
                    EditorsUI.getPreferenceStore(),
                    AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND);
            float luminance = calculateLuminance(editorBg);
            return isDarkByLuminance(luminance);
        } catch (IllegalStateException e) {
            return null;
        }
    }

    /**
     * Get color for the given key, automatically selecting light/dark variant
     */
    public Color getColor(String key) {
        if (DEFAULT.equals(key)) {
            RGB editorForeground = getEditorForegroundPreference();
            if (editorForeground != null) {
                return getColor(editorForeground);
            }
        }

        RGB rgb = isDarkTheme() ? DARK_COLORS.get(key) : LIGHT_COLORS.get(key);
        if (rgb == null) {
            rgb = isDarkTheme() ? DARK_COLORS.get(DEFAULT) : LIGHT_COLORS.get(DEFAULT);
        }
        return getColor(rgb);
    }

    private RGB getEditorForegroundPreference() {
        if (!PlatformUI.isWorkbenchRunning()) {
            return null;
        }
        try {
            return PreferenceConverter.getColor(
                    EditorsUI.getPreferenceStore(),
                    AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND);
        } catch (IllegalStateException e) {
            return null;
        }
    }

    /**
     * Get or create a Color from RGB
     */
    private Color getColor(RGB rgb) {
        Color color = colorCache.get(rgb);
        if (color == null) {
            Display display = Display.getCurrent();
            if (display == null) {
                display = Display.getDefault();
            }
            color = new Color(display, rgb);
            colorCache.put(rgb, color);
        }
        return color;
    }

    /**
     * Reset theme cache (call when theme changes)
     */
    public void resetThemeCache() {
        cachedIsDarkTheme = null;
        cachedDisplay = null;
    }

    /**
     * Dispose all colors
     */
    public void dispose() {
        for (Color color : colorCache.values()) {
            if (!color.isDisposed()) {
                color.dispose();
            }
        }
        colorCache.clear();
        cachedIsDarkTheme = null;
        cachedDisplay = null;
    }
}
