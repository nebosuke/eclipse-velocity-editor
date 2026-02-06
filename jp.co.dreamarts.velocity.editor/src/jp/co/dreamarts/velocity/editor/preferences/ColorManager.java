package jp.co.dreamarts.velocity.editor.preferences;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

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

    // Dark theme colors
    private static final Map<String, RGB> DARK_COLORS = new HashMap<>();
    static {
        DARK_COLORS.put(VTL_DIRECTIVE, new RGB(206, 147, 216));       // #CE93D8 Light purple
        DARK_COLORS.put(VTL_VARIABLE, new RGB(100, 181, 246));        // #64B5F6 Light blue
        DARK_COLORS.put(VTL_COMMENT, new RGB(129, 199, 132));         // #81C784 Light green
        DARK_COLORS.put(VTL_STRING, new RGB(165, 214, 167));          // #A5D6A7 Light green
        DARK_COLORS.put(HTML_TAG, new RGB(255, 183, 77));             // #FFB74D Light orange
        DARK_COLORS.put(HTML_ATTRIBUTE, new RGB(239, 154, 154));      // #EF9A9A Light red
        DARK_COLORS.put(HTML_ATTRIBUTE_VALUE, new RGB(165, 214, 167));// #A5D6A7 Light green
        DARK_COLORS.put(DEFAULT, new RGB(238, 238, 238));             // #EEEEEE Light gray
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
        
        // Get system background color to determine theme
        Color bgColor = display.getSystemColor(SWT.COLOR_LIST_BACKGROUND);
        
        // Calculate luminance using relative luminance formula
        float luminance = (bgColor.getRed() * 0.299f + 
                          bgColor.getGreen() * 0.587f + 
                          bgColor.getBlue() * 0.114f) / 255f;
        
        cachedIsDarkTheme = luminance < 0.5f;
        return cachedIsDarkTheme;
    }

    /**
     * Get color for the given key, automatically selecting light/dark variant
     */
    public Color getColor(String key) {
        RGB rgb = isDarkTheme() ? DARK_COLORS.get(key) : LIGHT_COLORS.get(key);
        if (rgb == null) {
            rgb = isDarkTheme() ? DARK_COLORS.get(DEFAULT) : LIGHT_COLORS.get(DEFAULT);
        }
        return getColor(rgb);
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
