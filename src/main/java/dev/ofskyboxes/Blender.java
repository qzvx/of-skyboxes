package dev.ofskyboxes;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

/**
 * Implements OptiFine's blend modes for custom sky layers.
 */
public final class Blender {

    public static final int BLEND_ALPHA = 0;
    public static final int BLEND_ADD = 1;
    public static final int BLEND_SUBTRACT = 2;
    public static final int BLEND_MULTIPLY = 3;
    public static final int BLEND_DODGE = 4;
    public static final int BLEND_BURN = 5;
    public static final int BLEND_SCREEN = 6;
    public static final int BLEND_OVERLAY = 7;
    public static final int BLEND_REPLACE = 8;
    public static final int BLEND_DEFAULT = BLEND_ADD;

    private Blender() {}

    public static int parseBlend(String str) {
        if (str == null) return BLEND_DEFAULT;
        str = str.toLowerCase().trim();
        if (str.equals("alpha") || str.equals("normal")) return BLEND_ALPHA;
        if (str.equals("add")) return BLEND_ADD;
        if (str.equals("subtract")) return BLEND_SUBTRACT;
        if (str.equals("multiply")) return BLEND_MULTIPLY;
        if (str.equals("dodge")) return BLEND_DODGE;
        if (str.equals("burn")) return BLEND_BURN;
        if (str.equals("screen")) return BLEND_SCREEN;
        if (str.equals("overlay")) return BLEND_OVERLAY;
        if (str.equals("replace")) return BLEND_REPLACE;
        return BLEND_DEFAULT;
    }

    public static void setupBlend(int blend, float alpha) {
        switch (blend) {
            case BLEND_ALPHA:
                GlStateManager.disableAlpha();
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
                break;
            case BLEND_ADD:
                GlStateManager.disableAlpha();
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
                break;
            case BLEND_SUBTRACT:
                GlStateManager.disableAlpha();
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL11.GL_ONE_MINUS_SRC_COLOR, GL11.GL_ZERO);
                GlStateManager.color(alpha, alpha, alpha, 1.0F);
                break;
            case BLEND_MULTIPLY:
                GlStateManager.disableAlpha();
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL11.GL_DST_COLOR, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GlStateManager.color(alpha, alpha, alpha, alpha);
                break;
            case BLEND_DODGE:
                GlStateManager.disableAlpha();
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL11.GL_ONE, GL11.GL_ONE);
                GlStateManager.color(alpha, alpha, alpha, 1.0F);
                break;
            case BLEND_BURN:
                GlStateManager.disableAlpha();
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL11.GL_ZERO, GL11.GL_ONE_MINUS_SRC_COLOR);
                GlStateManager.color(alpha, alpha, alpha, 1.0F);
                break;
            case BLEND_SCREEN:
                GlStateManager.disableAlpha();
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_COLOR);
                GlStateManager.color(alpha, alpha, alpha, 1.0F);
                break;
            case BLEND_OVERLAY:
                GlStateManager.disableAlpha();
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL11.GL_DST_COLOR, GL11.GL_SRC_COLOR);
                GlStateManager.color(alpha, alpha, alpha, 1.0F);
                break;
            case BLEND_REPLACE:
                GlStateManager.enableAlpha();
                GlStateManager.disableBlend();
                GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
                break;
        }
        GlStateManager.enableTexture2D();
    }
}
