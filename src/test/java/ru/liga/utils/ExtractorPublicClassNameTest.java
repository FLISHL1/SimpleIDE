package ru.liga.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExtractorPublicClassNameTest {

    @Test
    void testExtractClassName() {
        // Arrange
        ExtractorPublicClassName extractor = new ExtractorPublicClassName();
        String code = "public class HelloWorld { public static void main(String[] args) {} }";
        String expectedClassName = "HelloWorld";

        // Act
        String actualClassName = extractor.extractClassName(code);

        // Assert
        assertEquals(expectedClassName, actualClassName, "The extracted class name should match the expected value.");
    }

    @Test
    void testExtractClassNameWithNoPublicClass() {
        // Arrange
        ExtractorPublicClassName extractor = new ExtractorPublicClassName();
        String code = "class HelloWorld { public static void main(String[] args) {} }";

        // Act
        String actualClassName = extractor.extractClassName(code);

        // Assert
        assertNull(actualClassName, "If no public class is found, the result should be null.");
    }

    @Test
    void testExtractClassNameWithMultipleClasses() {
        // Arrange
        ExtractorPublicClassName extractor = new ExtractorPublicClassName();
        String code = "public class HelloWorld { public static void main(String[] args) {} } class AnotherClass {}";
        String expectedClassName = "HelloWorld";

        // Act
        String actualClassName = extractor.extractClassName(code);

        // Assert
        assertEquals(expectedClassName, actualClassName, "The extracted class name should match the first public class.");
    }

    @Test
    void testExtractClassNameWithDifferentSpacing() {
        // Arrange
        ExtractorPublicClassName extractor = new ExtractorPublicClassName();
        String code = "public  class    HelloWorld { public static void main(String[] args) {} }";
        String expectedClassName = "HelloWorld";

        // Act
        String actualClassName = extractor.extractClassName(code);

        // Assert
        assertEquals(expectedClassName, actualClassName, "The extractor should handle different spacing correctly.");
    }
}
