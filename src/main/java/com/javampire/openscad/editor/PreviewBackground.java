package com.javampire.openscad.editor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Preview scene backgrounds aligned with OpenSCAD 3D-view color schemes.
 * Cornfield is built-in ({@code src/glview/ColorMap.cc}); the rest use
 * {@code background} / optional {@code background-stop} from
 * {@code color-schemes/render/*.json} (OpenSCAD master). Monotone is omitted
 * ({@code show-in-gui: false}). Dark Gradient is plugin-only.
 */
public enum PreviewBackground {
    CORNFIELD("cornfield", "Cornfield", "#ffffe5", "#ffffe5"),
    METALLIC("metallic", "Metallic", "#aaaaff", "#aaaaff"),
    SUNSET("sunset", "Sunset", "#aa4444", "#aa4444"),
    STARNIGHT("starnight", "Starnight", "#000000", "#000000"),
    BEFORE_DAWN("beforedawn", "BeforeDawn", "#333333", "#333333"),
    NATURE("nature", "Nature", "#fafafa", "#fafafa"),
    DAYLIGHT_GEM("daylight-gem", "Daylight Gem", "#f0f0f0", "#f0f0f0"),
    NOCTURNAL_GEM("nocturnal-gem", "Nocturnal Gem", "#0c0c0c", "#0c0c0c"),
    DEEP_OCEAN("deepocean", "DeepOcean", "#333333", "#333333"),
    SOLARIZED("solarized", "Solarized", "#fdf6e3", "#fdf6e3"),
    TOMORROW("tomorrow", "Tomorrow", "#f8f8f8", "#f8f8f8"),
    TOMORROW_NIGHT("tomorrow-night", "Tomorrow Night", "#1d1f21", "#1d1f21"),
    CLEAR_SKY("clearsky", "Clear Sky", "#87ceeb", "#c9e9f6"),
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
