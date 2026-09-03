package com.javampire.openscad.editor;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Collects font files from user-configured directories for WASM preview rendering.
 */
public final class OpenSCADPreviewFontCollector {

    private static final Logger LOG = Logger.getInstance(OpenSCADPreviewFontCollector.class);
    private static final String FONTS_ROOT = "/fonts/";
    private static final Set<String> FONT_EXTENSIONS = Set.of(".ttf", ".otf", ".ttc");
    private static final long MAX_FONT_BYTES = 64L * 1024 * 1024;

    private OpenSCADPreviewFontCollector() {
    }

    @NotNull
    public static Map<String, byte[]> collectUserFonts(@NotNull final List<String> directories) {
        final Map<String, byte[]> fonts = new LinkedHashMap<>();
        long totalBytes = 0;
        for (final String directoryPath : directories) {
            if (StringUtil.isEmptyOrSpaces(directoryPath)) {
                continue;
            }
            final var directory = java.nio.file.Path.of(directoryPath.trim());
            if (!Files.isDirectory(directory)) {
                LOG.warn("OpenSCAD preview: font directory does not exist: " + directoryPath);
                continue;
            }
            try {
                totalBytes = collectFontsFromDirectory(directory, fonts, totalBytes);
            }
            catch (final IOException ioe) {
                LOG.warn("OpenSCAD preview: failed to read fonts from " + directoryPath, ioe);
            }
        }
        return fonts;
    }

    private static long collectFontsFromDirectory(@NotNull final java.nio.file.Path directory,
                                                @NotNull final Map<String, byte[]> fonts,
                                                long totalBytes) throws IOException {
        try (var stream = Files.walk(directory)) {
            for (final java.nio.file.Path path : stream.filter(Files::isRegularFile).toList()) {
                if (!isFontFile(path)) {
                    continue;
                }
                final String fileName = path.getFileName().toString();
                final String virtualPath = FONTS_ROOT + fileName;
                if (fonts.containsKey(virtualPath)) {
                    LOG.warn("OpenSCAD preview: duplicate font file name '" + fileName + "'; using " + path);
                }
                final long size = Files.size(path);
                if (totalBytes + size > MAX_FONT_BYTES) {
                    LOG.warn("OpenSCAD preview: skipping font files above " + MAX_FONT_BYTES + " byte limit");
                    return totalBytes;
                }
                fonts.put(virtualPath, Files.readAllBytes(path));
                totalBytes += size;
            }
        }
        return totalBytes;
    }

    static boolean isFontFile(@NotNull final java.nio.file.Path path) {
        final String extension = FileUtil.getExtension(path.getFileName().toString()).toLowerCase(Locale.ROOT);
        return FONT_EXTENSIONS.contains("." + extension);
    }
}
