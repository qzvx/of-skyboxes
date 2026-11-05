package dev.ofskyboxes;

import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Parses OptiFine / MCPatcher sky*.properties files into {@link SkyLayer} objects.
 */
public final class SkyPropertiesParser {

    private SkyPropertiesParser() {}

    public static SkyLayer parse(InputStream in, ResourceLocation propsPath) throws IOException {
        Properties props = new Properties();
        props.load(in);

        // --- source ---
        ResourceLocation source;
        String srcVal = props.getProperty("source");
        if (srcVal != null) {
            source = resolveTexturePath(srcVal.trim(), propsPath);
        } else {
            // Default: replace .properties with .png in the same directory
            String path = propsPath.getResourcePath();
            String defaultPng = path.replace(".properties", ".png");
            source = new ResourceLocation(propsPath.getResourceDomain(), defaultPng);
        }

        // --- blend ---
        int blend = Blender.parseBlend(props.getProperty("blend"));

        // --- fade times ---
        int startFadeIn = parseTime(props.getProperty("startFadeIn"));
        int endFadeIn = parseTime(props.getProperty("endFadeIn"));
        int startFadeOut = parseTime(props.getProperty("startFadeOut"));
        int endFadeOut = parseTime(props.getProperty("endFadeOut"));

        // Derive startFadeOut if absent
        if (startFadeIn >= 0 && endFadeIn >= 0 && endFadeOut >= 0) {
            if (startFadeOut < 0) {
                int fadeDuration = SkyLayer.normalizeTime(endFadeIn - startFadeIn);
                startFadeOut = SkyLayer.normalizeTime(endFadeOut - fadeDuration);
                if (SkyLayer.timeBetween(startFadeOut, startFadeIn, endFadeIn)) {
                    startFadeOut = endFadeIn;
                }
            }
        }

        // --- rotation ---
        boolean rotate = parseBoolean(props.getProperty("rotate"), true);
        float speed = parseFloat(props.getProperty("speed"), 1.0F);

        // --- axis (OptiFine transform: {z, y, -x}) ---
        float[] axis = parseAxis(props.getProperty("axis"));

        // --- weather ---
        boolean weatherClear = false;
        boolean weatherRain = false;
        boolean weatherThunder = false;
        String weatherVal = props.getProperty("weather");
        if (weatherVal != null) {
            for (String w : weatherVal.trim().split("\\s+")) {
                w = w.toLowerCase(Locale.ROOT);
                if (w.equals("clear")) weatherClear = true;
                if (w.equals("rain")) weatherRain = true;
                if (w.equals("thunder")) weatherThunder = true;
            }
        } else {
            weatherClear = true; // default clear
        }

        // --- days ---
        Set<Integer> days = parseDays(props.getProperty("days"));
        int daysLoop = parseInt(props.getProperty("daysLoop"), 8);

        return new SkyLayer(source, blend, startFadeIn, endFadeIn, startFadeOut, endFadeOut,
                rotate, speed, axis, weatherClear, weatherRain, weatherThunder, days, daysLoop);
    }

    public static int parseTime(String time) {
        if (time == null) return -1;
        time = time.replace("\\", "").trim();
        String[] parts = time.split(":");
        if (parts.length != 2) return -1;
        try {
            int h = Integer.parseInt(parts[0].trim());
            int m = Integer.parseInt(parts[1].trim());
            if (h < 0 || h > 23 || m < 0 || m > 59) return -1;
            h -= 6;
            if (h < 0) h += 24;
            return h * 1000 + (int) ((double) m / 60.0D * 1000.0D);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static float[] parseAxis(String axisVal) {
        if (axisVal == null) {
            return new float[] {1.0F, 0.0F, 0.0F}; // default south
        }
        String[] parts = axisVal.trim().split("\\s+");
        if (parts.length != 3) {
            return new float[] {1.0F, 0.0F, 0.0F};
        }
        try {
            float x = Float.parseFloat(parts[0]);
            float y = Float.parseFloat(parts[1]);
            float z = Float.parseFloat(parts[2]);
            if (x * x + y * y + z * z < 1.0E-5F) {
                return new float[] {1.0F, 0.0F, 0.0F};
            }
            // OptiFine transforms axis: {z, y, -x}
            return new float[] {z, y, -x};
        } catch (NumberFormatException e) {
            return new float[] {1.0F, 0.0F, 0.0F};
        }
    }

    private static ResourceLocation resolveTexturePath(String path, ResourceLocation propsPath) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.startsWith("assets/minecraft/")) {
            path = path.substring("assets/minecraft/".length());
        }
        if (path.startsWith("./")) {
            String dir = propsPath.getResourcePath();
            int lastSlash = dir.lastIndexOf('/');
            if (lastSlash >= 0) {
                dir = dir.substring(0, lastSlash + 1);
            } else {
                dir = "";
            }
            return new ResourceLocation(propsPath.getResourceDomain(), dir + path.substring(2));
        }
        if (path.startsWith("~/")) {
            String base = propsPath.getResourcePath().startsWith("mcpatcher/") ? "mcpatcher/" : "optifine/";
            return new ResourceLocation(propsPath.getResourceDomain(), base + path.substring(2));
        }
        if (path.contains(":")) {
            String[] parts = path.split(":", 2);
            return new ResourceLocation(parts[0], parts[1]);
        }
        return new ResourceLocation("minecraft", path);
    }

    private static Set<Integer> parseDays(String value) {
        if (value == null) return null;
        Set<Integer> set = new HashSet<Integer>();
        for (String part : value.trim().split("\\s+")) {
            if (part.contains("-")) {
                String[] range = part.split("-", 2);
                try {
                    int lo = Integer.parseInt(range[0].trim());
                    int hi = Integer.parseInt(range[1].trim());
                    for (int i = lo; i <= hi; i++) {
                        set.add(i);
                    }
                } catch (NumberFormatException ignored) {}
            } else {
                try {
                    set.add(Integer.parseInt(part.trim()));
                } catch (NumberFormatException ignored) {}
            }
        }
        return set.isEmpty() ? null : set;
    }

    private static boolean parseBoolean(String value, boolean def) {
        if (value == null) return def;
        String s = value.trim().toLowerCase(Locale.ROOT);
        if (s.equals("true")) return true;
        if (s.equals("false")) return false;
        return def;
    }

    private static float parseFloat(String value, float def) {
        if (value == null) return def;
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static int parseInt(String value, int def) {
        if (value == null) return def;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
