package ru.maxthetomas.craftminedailies.util;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Lifecycle;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.ErrorScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.DataPackReloadCookie;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContextMapper;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.slf4j.Logger;
import ru.maxthetomas.craftminedailies.CraftmineDailies;
import ru.maxthetomas.craftminedailies.auth.ApiManager;
import ru.maxthetomas.craftminedailies.mixin.common.PrimaryLevelDataAccessor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class WorldCreationUtil {

    public static Logger LOGGER = LogUtils.getLogger();

    // Add this method to your CreateWorldScreen class or a suitable utility class
    public static void createAndLoadDaily(
            ApiManager.DailyDetails dailyDetails
    ) {
        String worldName = CraftmineDailies.WORLD_NAME;
        Minecraft minecraft = Minecraft.getInstance();
        GameType gameType = GameType.SURVIVAL;
        Difficulty difficulty = Difficulty.HARD;
        boolean allowCommands = false;
        boolean hardcore = false;
        Path tempDataPackDir = null;
        long seed = dailyDetails.seed();
        CreateWorldScreen.queueLoadScreen(minecraft, Component.translatable("createWorld.preparing"));

        tryDeleteWorld();

        // 1. Load World Creation Context (similar to openCreateWorldScreen)
        PackRepository packRepository = new PackRepository(
                new ServerPacksSource(minecraft.directoryValidator())
        );
        // Use default datapacks for this example
        WorldDataConfiguration worldDataConfiguration =
                WorldDataConfiguration.DEFAULT;
        WorldLoader.InitConfig initConfig = CreateWorldScreen.createDefaultLoadConfig(
                packRepository,
                worldDataConfiguration
        );

        // Use default world preset (NORMAL) and context mapping
        Function<WorldLoader.DataLoadContext, WorldGenSettings> worldGenSettingsFunction =
                (dataLoadContext) -> new WorldGenSettings(
                        new WorldOptions(seed, true, false), // Use fixed seed, generate structures, no bonus chest
                        WorldPresets.createNormalWorldDimensions(
                                dataLoadContext.datapackWorldgen()
                        )
                );
        WorldCreationContextMapper worldCreationContextMapper = (
                reloadableServerResources,
                layeredRegistryAccess,
                dataPackReloadCookie
        ) -> new WorldCreationContext(
                dataPackReloadCookie.worldGenSettings(),
                layeredRegistryAccess,
                reloadableServerResources,
                dataPackReloadCookie.dataConfiguration()
        );

        CompletableFuture<WorldCreationContext> contextFuture = WorldLoader.load(
                initConfig,
                (dataLoadContext) -> new WorldLoader.DataLoadOutput<>(
                        new DataPackReloadCookie(
                                worldGenSettingsFunction.apply(dataLoadContext),
                                dataLoadContext.dataConfiguration()
                        ),
                        dataLoadContext.datapackDimensions()
                ),
                (
                        closeableResourceManager,
                        reloadableServerResources,
                        layeredRegistryAccess,
                        dataPackReloadCookie
                ) -> {
                    closeableResourceManager.close();
                    return worldCreationContextMapper.apply(
                            reloadableServerResources,
                            layeredRegistryAccess,
                            dataPackReloadCookie
                    );
                },
                Util.backgroundExecutor(),
                minecraft
        );

        Objects.requireNonNull(contextFuture);
        minecraft.managedBlock(contextFuture::isDone); // Wait for context loading
        WorldCreationContext worldCreationContext;
        try {
            worldCreationContext = contextFuture.join();
        } catch (Exception e) {
            LOGGER.error("Failed to load world generation context", e);
            minecraft.setScreen(
                    new ErrorScreen(
                            Component.translatable("selectWorld.load_folder_access"),
                            Component.literal(e.getMessage())
                    )
            ); // Show an error screen
            return;
        }

        // 2. Define LevelSettings & WorldOptions programmatically
        LevelSettings levelSettings = new LevelSettings(
                worldName,
                gameType,
                hardcore,
                difficulty,
                allowCommands,
                new GameRules(WorldDataConfiguration.DEFAULT.enabledFeatures()), // Use default game rules
                worldCreationContext.dataConfiguration()
        );
        // WorldOptions are already part of worldCreationContext.options() via worldGenSettingsFunction

        // 3. Prepare PrimaryLevelData
        WorldDimensions.Complete worldDimensions = worldCreationContext
                .selectedDimensions()
                .bake(worldCreationContext.datapackDimensions());
        LayeredRegistryAccess<RegistryLayer> layeredRegistryAccess = worldCreationContext
                .worldgenRegistries()
                .replaceFrom(
                        RegistryLayer.DIMENSIONS,
                        worldDimensions.dimensionsRegistryAccess()
                );
        Lifecycle worldLifecycle = Lifecycle.stable();
        Lifecycle registryLifecycle = layeredRegistryAccess
                .compositeAccess()
                .allRegistriesLifecycle();
        Lifecycle finalLifecycle = registryLifecycle.add(worldLifecycle);

        PrimaryLevelData primaryLevelData = new PrimaryLevelData(
                levelSettings,
                worldCreationContext.options(), // Contains the seed
                worldDimensions.specialWorldProperty(),
                finalLifecycle
        );

        updateLevelData(primaryLevelData, dailyDetails);

        // 4. Create World Directory
        String targetFolder = worldName;

        Optional<LevelStorageSource.LevelStorageAccess> storageAccessOpt =
                CreateWorldScreen.createNewWorldDirectory(minecraft, targetFolder, tempDataPackDir);

        if (storageAccessOpt.isEmpty()) {
            LOGGER.error("Failed to create directory for world '{}'", targetFolder);
            SystemToast.onPackCopyFailure(minecraft, targetFolder); // Reuse existing toast
            // Optionally set an error screen:
            // minecraft.setScreen(new ErrorScreen(Component.translatable("selectWorld.unable_to_create"), Component.literal(targetFolder)));
            return;
        }


        // 5. Initiate World Loading
        minecraft
                .createWorldOpenFlows()
                .createLevelFromExistingSettings(
                        storageAccessOpt.get(),
                        worldCreationContext.dataPackResources(),
                        layeredRegistryAccess,
                        primaryLevelData
                );

    }

    private static void updateLevelData(PrimaryLevelData primaryLevelData, ApiManager.DailyDetails dailyDetails) {
        primaryLevelData.setDifficultyLocked(true);
        primaryLevelData.setModdedInfo(CraftmineDailies.DAILY_SERVER_BRAND, true);
        dailyDetails.getUnlockedEffects().forEach(primaryLevelData::unlockEffect);
        dailyDetails.getEffects().forEach(primaryLevelData::unlockEffect);
        ((PrimaryLevelDataAccessor) primaryLevelData).setMineCrafterLevel(dailyDetails.mineCrafterLevel());
    }

    public static void tryDeleteWorld() {
        try (var access = Minecraft.getInstance().getLevelSource().createAccess(CraftmineDailies.WORLD_NAME)) {
            access.deleteLevel();
        } catch (IOException e) {
        }
    }
}
