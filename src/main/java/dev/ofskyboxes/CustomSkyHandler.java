package dev.ofskyboxes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.IRenderHandler;

import java.util.List;

/**
 * Custom IRenderHandler hooked into the WorldProvider via Forge.
 * Renders the vanilla sky, followed by all active custom sky layers.
 */
public final class CustomSkyHandler extends IRenderHandler {

    public static final CustomSkyHandler INSTANCE = new CustomSkyHandler();

    private CustomSkyHandler() {}

    @Override
    public void render(float partialTicks, WorldClient world, Minecraft mc) {
        int dim = world.provider.getDimensionId();
        List<SkyLayer> layers = SkyManager.getLayersForDimension(dim);

        // 1. Render the base vanilla sky by temporarily unsetting the sky renderer
        world.provider.setSkyRenderer(null);
        try {
            mc.renderGlobal.renderSky(partialTicks, 0);
        } finally {
            world.provider.setSkyRenderer(this);
        }

        // 2. If no custom sky layers exist for this dimension, we're done
        if (layers == null || layers.isEmpty()) {
            return;
        }

        // 3. Render custom sky layers safely inside try-finally to guarantee clean GL state restoration
        try {
            long worldTime = world.getWorldTime();
            int time = (int) (worldTime % 24000L);
            float celestialAngle = world.getCelestialAngle(partialTicks);
            float rainStrength = world.getRainStrength(partialTicks);
            float thunderStrength = world.getThunderStrength(partialTicks);
            if (rainStrength > 0.0F) {
                thunderStrength /= rainStrength;
            }

            GlStateManager.depthMask(false);
            GlStateManager.enableTexture2D();
            GlStateManager.disableFog();
            GlStateManager.pushMatrix();

            // OptiFine pre-rotation around Y axis
            GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);

            for (SkyLayer layer : layers) {
                if (layer.isActive(world, time)) {
                    layer.render(time, celestialAngle, rainStrength, thunderStrength);
                }
            }

            GlStateManager.popMatrix();
        } finally {
            // Reset blend and GL state cleanly for subsequent rendering passes (terrain, clouds, entities)
            GlStateManager.disableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.enableTexture2D();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.enableFog();
            GlStateManager.depthMask(true);
        }
    }
}
