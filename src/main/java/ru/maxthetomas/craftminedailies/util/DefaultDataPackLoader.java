package ru.maxthetomas.craftminedailies.util;

import com.mojang.logging.LogUtils;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.world.level.WorldDataConfiguration;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class DefaultDataPackLoader {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static LayeredRegistryAccess<RegistryLayer> registries;
    private static CompletableFuture<LayeredRegistryAccess<RegistryLayer>> future;

    public static void tryLoadAsync() {
        if (future != null || registries != null) return;
        var start = Instant.now();
        LOGGER.info("Loading default data pack...");
        future = CompletableFuture.supplyAsync(DefaultDataPackLoader::loadDefaultRegistries).whenComplete((access, error) -> {
            registries = access;
            var now = Instant.now();
            var millis = now.toEpochMilli() - start.toEpochMilli();
            LOGGER.info("Loading default data pack took {} ms", millis);
        });
    }

    public static LayeredRegistryAccess<RegistryLayer> getRegistriesOrBlock() {
        if (registries != null)
            return registries;

        tryLoadAsync();
        return future.join();
    }

    public static boolean isLoadingData() {
        if (registries != null) return false;
        return future != null;
    }


    /**
     * Loads the default server registries using the WorldLoader mechanism.
     * This involves loading default datapacks.
     * <p>
     * NOTE: This performs I/O and can be slow. It blocks the calling thread
     * until loading is complete. Consider running it asynchronously if needed.
     *
     * @return A LayeredRegistryAccess containing the loaded default registries,
     * or null if loading fails.
     */
    public static LayeredRegistryAccess<RegistryLayer> loadDefaultRegistries() {
        Minecraft minecraft = Minecraft.getInstance(); // Needed for paths and executor
        Executor backgroundExecutor = Util.backgroundExecutor();

        // 1. Set up Pack Repository (to find default server packs)
        PackRepository packRepository = new PackRepository(
                new ServerPacksSource(minecraft.directoryValidator())
        );

        // 2. Define World Data Configuration (use default)
        // This determines which features/packs are enabled initially.
        WorldDataConfiguration worldDataConfiguration = WorldDataConfiguration.DEFAULT;

        // 3. Create Initial Load Configuration
        // We reuse the logic from CreateWorldScreen for simplicity
        WorldLoader.InitConfig initConfig = createDefaultLoadConfig(
                packRepository,
                worldDataConfiguration
        );

        // 4. Define the loading process using WorldLoader
        CompletableFuture<LayeredRegistryAccess<RegistryLayer>> future =
                WorldLoader.load(
                        initConfig,
                        // DataLoadContext -> DataLoadOutput: Just pass the config through
                        (dataLoadContext) -> new WorldLoader.DataLoadOutput<>(
                                dataLoadContext.dataConfiguration(),
                                dataLoadContext.datapackDimensions() // Need this intermediate step
                        ),
                        // Final stage: Process loaded resources and return the registry access
                        (
                                closeableResourceManager,
                                reloadableServerResources,
                                layeredRegistryAccess,
                                loadedConfig // The config passed from the previous step
                        ) -> {
                            // *** CRITICAL: Close the resource manager ***
                            closeableResourceManager.close();
                            // We only want the registry access in this case
                            return layeredRegistryAccess;
                        },
                        backgroundExecutor,
                        minecraft // Executor for main thread tasks if needed
                );

        // 5. Wait for completion and handle errors
        try {
            // Block until the future completes
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Failed to load default registries", e);
            // Propagate crash report if available
            if (e.getCause() instanceof ReportedException reportedException) {
                throw reportedException;
            }
            // Otherwise, return null or throw a new exception
            return null;
        } finally {
            // Ensure repository is closed if it holds resources (optional but good practice)
            // packRepository.close(); // PackRepository doesn't directly implement Closeable
        }
    }

    /**
     * Helper method to create the InitConfig.
     * Adapted from CreateWorldScreen.createDefaultLoadConfig.
     */
    private static WorldLoader.InitConfig createDefaultLoadConfig(
            PackRepository packRepository,
            WorldDataConfiguration worldDataConfiguration
    ) {
        WorldLoader.PackConfig packConfig = new WorldLoader.PackConfig(
                packRepository,
                worldDataConfiguration,
                false, // safeMode - set to true to ignore broken packs
                true // initMode - forces initial load logic
        );
        return new WorldLoader.InitConfig(
                packConfig,
                Commands.CommandSelection.DEDICATED, // Or INTEGRATED, affects command registration
                2 // functionCompilationLevel - Default for dedicated servers
        );
    }

}
