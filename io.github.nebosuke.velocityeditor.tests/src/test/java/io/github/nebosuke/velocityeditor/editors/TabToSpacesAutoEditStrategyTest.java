package io.github.nebosuke.velocityeditor.editors;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for TabToSpacesAutoEditStrategy.
 */
@DisplayName("TabToSpacesAutoEditStrategy")
class TabToSpacesAutoEditStrategyTest {

    @Test
    @DisplayName("行頭 (column 0) ではタブ幅分のスペースを返す")
    void returnsFullTabWidthAtLineStart() {
        assertEquals(4, TabToSpacesAutoEditStrategy.spacesToNextStop(0, 4));
    }

    @Test
    @DisplayName("タブストップの境界ではタブ幅分のスペースを返す")
    void returnsFullTabWidthAtStopBoundary() {
        assertEquals(4, TabToSpacesAutoEditStrategy.spacesToNextStop(8, 4));
    }

    @Test
    @DisplayName("タブストップの途中では次のストップまでのスペース数を返す")
    void returnsRemainingSpacesMidStop() {
        assertEquals(2, TabToSpacesAutoEditStrategy.spacesToNextStop(2, 4));
        assertEquals(1, TabToSpacesAutoEditStrategy.spacesToNextStop(3, 4));
    }

    @Test
    @DisplayName("タブ幅が1の場合は常に1スペース")
    void tabWidthOfOneAlwaysReturnsOne() {
        for (int column = 0; column < 5; column++) {
            assertEquals(1, TabToSpacesAutoEditStrategy.spacesToNextStop(column, 1));
        }
    }
}
