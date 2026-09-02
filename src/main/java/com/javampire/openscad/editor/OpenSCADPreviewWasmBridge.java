package com.javampire.openscad.editor;

import org.cef.browser.CefBrowser;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Sends preview source payloads from Java to the JCEF JavaScript preview runtime.
 */
public final class OpenSCADPreviewWasmBridge {

    private OpenSCADPreviewWasmBridge() {
    }

    public static void render(@NotNull final CefBrowser browser,
                              @NotNull final OpenSCADPreviewSourceCollector.PreviewSources sources) {
        final String json = toJson(sources);
        final String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        final String frameUrl = browser.getURL();
        browser.executeJavaScript(
                "(function(){"
                        + "var payload=JSON.parse(atob('" + encoded + "'));"
                        + "if(typeof window.renderPreview==='function'){window.renderPreview(payload);}"
                        + "else{console.error('renderPreview is not available');}"
                        + "})();",
                frameUrl,
                0
        );
    }

    @NotNull
    private static String toJson(@NotNull final OpenSCADPreviewSourceCollector.PreviewSources sources) {
        final StringBuilder json = new StringBuilder();
        json.append("{\"mainPath\":").append(quote(sources.mainPath()))
                .append(",\"loadPreviewFonts\":").append(sources.loadPreviewFonts())
                .append(",\"enableTextMetrics\":").append(sources.enableTextMetrics())
                .append(",\"files\":{");
        boolean first = true;
        for (final Map.Entry<String, String> entry : sources.files().entrySet()) {
            if (!first) {
                json.append(',');
            }
            first = false;
            json.append(quote(entry.getKey())).append(':').append(quote(entry.getValue()));
        }
        json.append("}}");
        return json.toString();
    }

    @NotNull
    private static String quote(@NotNull final String value) {
        final StringBuilder quoted = new StringBuilder(value.length() + 2);
        quoted.append('"');
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            switch (c) {
                case '"', '\\' -> quoted.append('\\').append(c);
                case '\n' -> quoted.append("\\n");
                case '\r' -> quoted.append("\\r");
                case '\t' -> quoted.append("\\t");
                default -> {
                    if (c < 0x20) {
                        quoted.append(String.format("\\u%04x", (int)c));
                    }
                    else {
                        quoted.append(c);
                    }
                }
            }
        }
        quoted.append('"');
        return quoted.toString();
    }
}
