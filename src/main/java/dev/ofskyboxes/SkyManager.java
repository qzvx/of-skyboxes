package dev.ofskyboxes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Manages custom sky layers, resource pack scanning, and hook registration with WorldProvider.
 */
public final class SkyManager implements IResourceManagerReloadListener {

    private static final Map<Integer, List<SkyLayer>> DIMENSION_LAYERS = new HashMap<Integer, List<SkyLayer>>();

    public static List<SkyLayer> getLayersForDimension(int dimension) {
        List<SkyLayer> list = DIMENSION_LAYERS.get(dimension);
        if (list == null) {
            list = loadLayers(dimension);
            DIMENSION_LAYERS.put(dimension, list);
        }
        return list;
    }

    public static boolean hasLayersFor(int dimension) {
        List<SkyLayer> list = getLayersForDimension(dimension);
        return list != null && !list.isEmpty();
    }

    public static void clearCache() {
        DIMENSION_LAYERS.clear();
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        clearCache();
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld != null) {
            int dim = mc.theWorld.provider.getDimensionId();
            if (hasLayersFor(dim)) {
                mc.theWorld.provider.setSkyRenderer(CustomSkyHandler.INSTANCE);
            } else {
                mc.theWorld.provider.setSkyRenderer(null);
            }
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (event.world.isRemote && event.world instanceof WorldClient) {
            WorldClient world = (WorldClient) event.world;
            int dim = world.provider.getDimensionId();
            if (hasLayersFor(dim)) {
                world.provider.setSkyRenderer(CustomSkyHandler.INSTANCE);
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Minecraft mc = Minecraft.getMinecraft();
        WorldClient world = mc.theWorld;
        if (world == null) return;

        int dim = world.provider.getDimensionId();
        if (hasLayersFor(dim)) {
            if (world.provider.getSkyRenderer() != CustomSkyHandler.INSTANCE) {
                world.provider.setSkyRenderer(CustomSkyHandler.INSTANCE);
            }
        } else if (world.provider.getSkyRenderer() == CustomSkyHandler.INSTANCE) {
            world.provider.setSkyRenderer(null);
        }
    }

    /**
     * Scans for sky properties files across both optifine/sky and mcpatcher/sky directories.
     * Validates that the referenced source texture exists in the resource pack to prevent
     * missing-texture (black and magenta) artifacts.
     */
    private static List<SkyLayer> loadLayers(int dimension) {
        List<SkyLayer> list = new ArrayList<SkyLayer>();
        IResourceManager rm = Minecraft.getMinecraft().getResourceManager();

        String[] prefixes = {
            "optifine/sky/world" + dimension + "/sky",
            "mcpatcher/sky/world" + dimension + "/sky"
        };

        for (String prefix : prefixes) {
            int misses = 0;
            for (int k = 0; k < 200; k++) {
                ResourceLocation loc = new ResourceLocation("minecraft", prefix + k + ".properties");
                try {
                    IResource res = rm.getResource(loc);
                    InputStream in = res.getInputStream();
                    try {
                        SkyLayer layer = SkyPropertiesParser.parse(in, loc);
                        if (layer != null) {
                            // Validate that the texture resource actually exists in the pack!
                            // Prevents missing texture (black/magenta checkerboard) from rendering.
                            try {
                                IResource texRes = rm.getResource(layer.source);
                                if (texRes != null) {
                                    InputStream texIn = texRes.getInputStream();
                                    if (texIn != null) {
                                        texIn.close();
                                        list.add(layer);
                                    }
                                }
                            } catch (IOException e) {
                                // Texture file is missing from pack — skip this layer (matches OptiFine)
                            }
                        }
                    } finally {
                        in.close();
                    }
                    misses = 0;
                } catch (IOException e) {
                    misses++;
                    if (!list.isEmpty() && misses >= 10) {
                        break;
                    }
                }
            }
            if (!list.isEmpty()) {
                // If layers were found under optifine, do not also load mcpatcher
                break;
            }
        }

        DIMENSION_LAYERS.put(dimension, list);
        return list;
    }
}
