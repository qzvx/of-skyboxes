package dev.ofskyboxes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = OfSkyboxes.MODID, name = OfSkyboxes.NAME, version = OfSkyboxes.VERSION, clientSideOnly = true)
public class OfSkyboxes {

    public static final String MODID = "ofskyboxes";
    public static final String NAME = "OfSkyboxes";
    public static final String VERSION = "1.0.0";

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        SkyManager manager = new SkyManager();
        MinecraftForge.EVENT_BUS.register(manager);

        IResourceManager rm = Minecraft.getMinecraft().getResourceManager();
        if (rm instanceof IReloadableResourceManager) {
            ((IReloadableResourceManager) rm).registerReloadListener(manager);
        }
    }
}
