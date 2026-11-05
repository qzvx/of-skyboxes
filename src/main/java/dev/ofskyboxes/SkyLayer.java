package dev.ofskyboxes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

import java.util.Set;

/**
 * Immutable data object representing one OptiFine custom sky layer.
 */
public final class SkyLayer {

    public final ResourceLocation source;
    public final int blend;
    public final int startFadeIn;
    public final int endFadeIn;
    public final int startFadeOut;
    public final int endFadeOut;
    public final boolean rotate;
    public final float speed;
    public final float[] axis; // transformed axis: {z, y, -x}
    public final boolean weatherClear;
    public final boolean weatherRain;
    public final boolean weatherThunder;
    public final Set<Integer> days;
    public final int daysLoop;

    public SkyLayer(ResourceLocation source, int blend,
                    int startFadeIn, int endFadeIn, int startFadeOut, int endFadeOut,
                    boolean rotate, float speed, float[] axis,
                    boolean weatherClear, boolean weatherRain, boolean weatherThunder,
                    Set<Integer> days, int daysLoop) {
        this.source = source;
        this.blend = blend;
        this.startFadeIn = startFadeIn;
        this.endFadeIn = endFadeIn;
        this.startFadeOut = startFadeOut;
        this.endFadeOut = endFadeOut;
        this.rotate = rotate;
        this.speed = speed;
        this.axis = axis != null ? axis : new float[] {1.0F, 0.0F, 0.0F};
        this.weatherClear = weatherClear;
        this.weatherRain = weatherRain;
        this.weatherThunder = weatherThunder;
        this.days = days;
        this.daysLoop = daysLoop > 0 ? daysLoop : 8;
    }

    public boolean isActive(World world, int time) {
        if (this.startFadeIn >= 0 && timeBetween(time, this.endFadeOut, this.startFadeIn)) {
            return false;
        }
        if (this.days != null && !isDayActive(world.getWorldTime())) {
            return false;
        }
        return true;
    }

    public boolean isDayActive(long worldTime) {
        if (this.days == null) return true;
        long j = worldTime - (long) (this.startFadeIn >= 0 ? this.startFadeIn : 0);
        while (j < 0L) {
            j += (long) (24000 * this.daysLoop);
        }
        int dayIndex = (int) ((j / 24000L) % this.daysLoop);
        return this.days.contains(dayIndex);
    }

    public float getFadeBrightness(int time) {
        if (this.startFadeIn < 0) {
            return 1.0F; // no fade times specified → always on
        }
        if (timeBetween(time, this.startFadeIn, this.endFadeIn)) {
            int duration = normalizeTime(this.endFadeIn - this.startFadeIn);
            int elapsed = normalizeTime(time - this.startFadeIn);
            return duration == 0 ? 1.0F : (float) elapsed / (float) duration;
        } else if (timeBetween(time, this.endFadeIn, this.startFadeOut)) {
            return 1.0F;
        } else if (timeBetween(time, this.startFadeOut, this.endFadeOut)) {
            int duration = normalizeTime(this.endFadeOut - this.startFadeOut);
            int elapsed = normalizeTime(time - this.startFadeOut);
            return duration == 0 ? 0.0F : 1.0F - (float) elapsed / (float) duration;
        } else {
            return 0.0F;
        }
    }

    public void render(int time, float celestialAngle, float rainStrength, float thunderStrength) {
        float clearStrength = 1.0F - rainStrength;
        float rainOnlyStrength = rainStrength - thunderStrength;
        float weatherFactor = 0.0F;
        if (this.weatherClear) weatherFactor += clearStrength;
        if (this.weatherRain) weatherFactor += rainOnlyStrength;
        if (this.weatherThunder) weatherFactor += thunderStrength;
        weatherFactor = Math.max(0.0F, Math.min(1.0F, weatherFactor));

        float brightness = weatherFactor * this.getFadeBrightness(time);
        brightness = Math.max(0.0F, Math.min(1.0F, brightness));

        if (brightness >= 1.0E-4F) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(this.source);

            // Clamp texture coords to edge to prevent edge-bleeding seams between skybox faces
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, 0x812F); // GL_CLAMP_TO_EDGE
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, 0x812F);

            Blender.setupBlend(this.blend, brightness);
            GlStateManager.pushMatrix();

            if (this.rotate) {
                GlStateManager.rotate(celestialAngle * 360.0F * this.speed, this.axis[0], this.axis[1], this.axis[2]);
            }

            Tessellator tessellator = Tessellator.getInstance();
            GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(-90.0F, 0.0F, 0.0F, 1.0F);
            renderSide(tessellator, 4);

            GlStateManager.pushMatrix();
            GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
            renderSide(tessellator, 1);
            GlStateManager.popMatrix();

            GlStateManager.pushMatrix();
            GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
            renderSide(tessellator, 0);
            GlStateManager.popMatrix();

            GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
            renderSide(tessellator, 5);

            GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
            renderSide(tessellator, 2);

            GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
            renderSide(tessellator, 3);

            GlStateManager.popMatrix();
        }
    }

    private static void renderSide(Tessellator tessellator, int side) {
        WorldRenderer wr = tessellator.getWorldRenderer();
        double u0 = (double) (side % 3) / 3.0D;
        double v0 = (double) (side / 3) / 2.0D;
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        wr.pos(-100.0D, -100.0D, -100.0D).tex(u0, v0).endVertex();
        wr.pos(-100.0D, -100.0D,  100.0D).tex(u0, v0 + 0.5D).endVertex();
        wr.pos( 100.0D, -100.0D,  100.0D).tex(u0 + 1.0D / 3.0D, v0 + 0.5D).endVertex();
        wr.pos( 100.0D, -100.0D, -100.0D).tex(u0 + 1.0D / 3.0D, v0).endVertex();
        tessellator.draw();
    }

    public static int normalizeTime(int t) {
        while (t >= 24000) t -= 24000;
        while (t < 0) t += 24000;
        return t;
    }

    public static boolean timeBetween(int time, int from, int to) {
        return from <= to ? time >= from && time <= to : time >= from || time <= to;
    }
}
