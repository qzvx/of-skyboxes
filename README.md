# of-skyboxes

of-skyboxes is a Minecraft 1.8.9 Forge client mod that renders custom skies from OptiFine and MCPatcher resource packs.

I made this project because I could not find any working alternatives for Forge 1.8.9. AI wrote the majority of the code.

## What it does

The mod loads sky properties and textures from resource packs at:
- `assets/minecraft/optifine/sky/world<dimension>/sky<index>.properties`
- `assets/minecraft/mcpatcher/sky/world<dimension>/sky<index>.properties`

The mod reads rotation, fade times, weather conditions, active days, and blend modes. It draws the custom skybox over the vanilla sky.

## Building

Run Gradle to compile the mod:

```bash
./gradlew build
```

The output jar file is in `build/libs/OfSkyboxes-1.0.0.jar`.
