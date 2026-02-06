package jp.co.dreamarts.velocity.editor.scanners;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for VelocityPartitionScanner.
 */
@DisplayName("VelocityPartitionScanner")
class VelocityPartitionScannerTest {

    private VelocityPartitionScanner scanner;

    @BeforeEach
    void setUp() {
        scanner = new VelocityPartitionScanner();
    }

    @Nested
    @DisplayName("パーティション定数")
    class PartitionConstants {

        @Test
        @DisplayName("VTL_COMMENTが正しい値を持つ")
        void vtlCommentConstant() {
            assertEquals("__vtl_comment", VelocityPartitionScanner.VTL_COMMENT);
        }

        @Test
        @DisplayName("VTL_MULTILINE_COMMENTが正しい値を持つ")
        void vtlMultilineCommentConstant() {
            assertEquals("__vtl_multiline_comment", VelocityPartitionScanner.VTL_MULTILINE_COMMENT);
        }

        @Test
        @DisplayName("HTML_TAGが正しい値を持つ")
        void htmlTagConstant() {
            assertEquals("__html_tag", VelocityPartitionScanner.HTML_TAG);
        }
    }

    @Nested
    @DisplayName("PARTITION_TYPES配列")
    class PartitionTypesArray {

        @Test
        @DisplayName("正しい数のパーティションタイプを持つ")
        void hasCorrectNumberOfPartitionTypes() {
            assertEquals(3, VelocityPartitionScanner.PARTITION_TYPES.length);
        }

        @Test
        @DisplayName("VTL_COMMENTを含む")
        void containsVtlComment() {
            assertArrayContains(VelocityPartitionScanner.PARTITION_TYPES, VelocityPartitionScanner.VTL_COMMENT);
        }

        @Test
        @DisplayName("VTL_MULTILINE_COMMENTを含む")
        void containsVtlMultilineComment() {
            assertArrayContains(VelocityPartitionScanner.PARTITION_TYPES, VelocityPartitionScanner.VTL_MULTILINE_COMMENT);
        }

        @Test
        @DisplayName("HTML_TAGを含む")
        void containsHtmlTag() {
            assertArrayContains(VelocityPartitionScanner.PARTITION_TYPES, VelocityPartitionScanner.HTML_TAG);
        }

        private void assertArrayContains(String[] array, String value) {
            boolean found = false;
            for (String item : array) {
                if (item.equals(value)) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Array does not contain " + value);
        }
    }

    @Nested
    @DisplayName("スキャナーの初期化")
    class ScannerInitialization {

        @Test
        @DisplayName("スキャナーがnullでないこと")
        void scannerIsNotNull() {
            assertNotNull(scanner);
        }

        @Test
        @DisplayName("インスタンス生成が例外をスローしないこと")
        void instanceCreationDoesNotThrow() {
            assertDoesNotThrow(() -> new VelocityPartitionScanner());
        }
    }

    @Nested
    @DisplayName("パーティションタイプの一意性")
    class PartitionTypeUniqueness {

        @Test
        @DisplayName("すべてのパーティションタイプがユニークであること")
        void allPartitionTypesAreUnique() {
            String[] types = VelocityPartitionScanner.PARTITION_TYPES;
            for (int i = 0; i < types.length; i++) {
                for (int j = i + 1; j < types.length; j++) {
                    assertNotEquals(types[i], types[j], 
                        "Duplicate partition types found: " + types[i] + " and " + types[j]);
                }
            }
        }

        @Test
        @DisplayName("パーティションタイプがnullでないこと")
        void partitionTypesAreNotNull() {
            for (String type : VelocityPartitionScanner.PARTITION_TYPES) {
                assertNotNull(type);
            }
        }

        @Test
        @DisplayName("パーティションタイプが空文字列でないこと")
        void partitionTypesAreNotEmpty() {
            for (String type : VelocityPartitionScanner.PARTITION_TYPES) {
                assertFalse(type.isEmpty(), "Partition type must not be empty");
            }
        }
    }

    @Nested
    @DisplayName("パーティションタイプの命名規則")
    class PartitionTypeNaming {

        @Test
        @DisplayName("パーティションタイプがダブルアンダースコアで始まること")
        void partitionTypesStartWithDoubleUnderscore() {
            for (String type : VelocityPartitionScanner.PARTITION_TYPES) {
                assertTrue(type.startsWith("__"), 
                    "Partition type does not start with '__': " + type);
            }
        }
    }
}
