package io.github.nebosuke.velocityeditor.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.swt.graphics.RGB;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ColorManager")
class ColorManagerTest {

    @Test
    @DisplayName("テーマIDに dark を含む場合はダークテーマ判定")
    void detectsDarkThemeId() {
        assertEquals(Boolean.TRUE, ColorManager.detectDarkThemeFromThemeId("org.eclipse.e4.ui.css.theme.e4_dark"));
    }

    @Test
    @DisplayName("テーマIDに light を含む場合はライトテーマ判定")
    void detectsLightThemeId() {
        assertEquals(Boolean.FALSE, ColorManager.detectDarkThemeFromThemeId("org.eclipse.e4.ui.css.theme.e4_default_light"));
    }

    @Test
    @DisplayName("テーマIDが不明な場合は null を返す")
    void returnsNullForUnknownThemeId() {
        assertNull(ColorManager.detectDarkThemeFromThemeId("com.example.customtheme"));
    }

    @Test
    @DisplayName("暗い輝度はダークテーマ扱い")
    void darkLuminanceIsDarkTheme() {
        assertTrue(ColorManager.isDarkByLuminance(0.30f));
    }

    @Test
    @DisplayName("明るい輝度はダークテーマではない")
    void brightLuminanceIsNotDarkTheme() {
        assertFalse(ColorManager.isDarkByLuminance(0.70f));
    }

    @Test
    @DisplayName("RGBから輝度を計算できる")
    void calculatesLuminanceFromRgb() {
        float luminance = ColorManager.calculateLuminance(new RGB(0, 0, 0));
        assertEquals(0.0f, luminance, 0.0001f);
    }
}
