package com.javampire.openscad.editor;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class OpenSCADPreviewFontCollectorTest extends BasePlatformTestCase {

    public void testCollectUserFontsFromDirectory() throws IOException {
        final Path directory = Files.createTempDirectory("preview-fonts");
        try {
            Files.write(directory.resolve("MyFont.ttf"), new byte[]{1, 2, 3});
            Files.write(directory.resolve("notes.txt"), new byte[]{9});

            final Map<String, byte[]> fonts = OpenSCADPreviewFontCollector.collectUserFonts(List.of(directory.toString()));

            assertEquals(1, fonts.size());
            assertTrue(fonts.containsKey("/fonts/MyFont.ttf"));
            assertTrue(Arrays.equals(new byte[]{1, 2, 3}, fonts.get("/fonts/MyFont.ttf")));
        }
        finally {
            FileUtil.delete(directory);
        }
    }

    public void testCollectUserFontsFromNestedDirectory() throws IOException {
        final Path directory = Files.createTempDirectory("preview-fonts-nested");
        try {
            final Path nested = Files.createDirectory(directory.resolve("subdir"));
            Files.write(nested.resolve("Nested.otf"), new byte[]{4, 5});

            final Map<String, byte[]> fonts = OpenSCADPreviewFontCollector.collectUserFonts(List.of(directory.toString()));

            assertEquals(1, fonts.size());
            assertTrue(fonts.containsKey("/fonts/Nested.otf"));
        }
        finally {
            FileUtil.delete(directory);
        }
    }

    public void testCollectUserFontsSkipsMissingDirectory() {
        final Map<String, byte[]> fonts = OpenSCADPreviewFontCollector.collectUserFonts(
                List.of("/path/that/does/not/exist")
        );
        assertTrue(fonts.isEmpty());
    }

    public void testIsFontFileRecognizesSupportedExtensions() {
        assertTrue(OpenSCADPreviewFontCollector.isFontFile(Path.of("Arial.ttf")));
        assertTrue(OpenSCADPreviewFontCollector.isFontFile(Path.of("Serif.otf")));
        assertTrue(OpenSCADPreviewFontCollector.isFontFile(Path.of("Collection.ttc")));
        assertFalse(OpenSCADPreviewFontCollector.isFontFile(Path.of("readme.txt")));
    }
}
