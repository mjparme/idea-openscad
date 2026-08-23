package com.javampire.openscad.editor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Preview scene backgrounds aligned with OpenSCAD render color schemes.
 * Cornfield defaults from {@code src/glview/ColorMap.cc}; Clear Sky from {@code color-schemes/render/clearsky.json}.
 */
public enum PreviewBackground {
    CLEAR_SKY("clearsky", "Clear Sky", "#87ceeb", "#c9e9f6"),
    CORNFIELD("cornfield", "Cornfield", "#ffffe5", "#ffffe5"),
    DARK_GRADIENT("dark-gradient", "Dark Gradient", "#2d2d2d", "#1a1a1a");

    private final String id;
    private final String displayName;
    private final String topColor;
    private final String bottomColor;

    PreviewBackground(@NotNull final String id,
                      @NotNull final String displayName,
                      @NotNull final String topColor,
                      @NotNull final String bottomColor) {
        this.id = id;
        this.displayName = displayName;
        this.topColor = topColor;
        this.bottomColor = bottomColor;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    @NotNull
    public String getTopColor() {
        return topColor;
    }

    @NotNull
    public String getBottomColor() {
        return bottomColor;
    }

    @Nullable
    public static PreviewBackground fromId(@Nullable final String id) {
        if (id == null) {
            return null;
        }
        for (final PreviewBackground background : values()) {
            if (background.id.equals(id)) {
                return background;
            }
        }
        return null;
    }
}
