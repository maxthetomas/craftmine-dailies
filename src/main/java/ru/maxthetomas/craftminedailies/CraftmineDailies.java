package ru.maxthetomas.craftminedailies;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import ru.maxthetomas.craftminedailies.mixin.common.ServerLevelAccessor;
import ru.maxthetomas.craftminedailies.screens.LeaderboardScreen;
import ru.maxthetomas.craftminedailies.util.WorldCreationUtil;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

public class CraftmineDailies implements ModInitializer {
    public static RandomSource RANDOM_EFFECT_SOURCE;

    private static Path lastDailySeedPath;
    private static long lastPlayedSeed = -1;

    private static long todayDailySeed = 24871L;

    public static final String DAILY_SERVER_BRAND = "_cm_daily";
    public static final String PLAYER_AWARDED_TAG = "_cm_daily_xp_awarded";
    public static final long MAX_GAME_TIME = 20 * 60 * 30;
    protected static long GAME_TIME_AT_START = -1;

    public static long REMAINING_TIME_CACHE = -1;

    // If run has ended for any reason.
    protected static boolean ENDED = false;
    protected static boolean WORLD_STARTED = false;

    @Override
    public void onInitialize() {
        lastDailySeedPath = Path.of(FabricLoader.getInstance().getConfigDir().toString() + "/", ".cd_last_played_seed");
        restoreLastPlayedSeed();

        ServerPlayConnectionEvents.JOIN.register((serverPlayer,
                                                  packetListener, server) -> {
            if (isDailyWorld(server.theGame().overworld())) {
                var randomExperiencePts = RANDOM_EFFECT_SOURCE.nextInt(50, 400);
                var player = serverPlayer.getPlayer();

                if (player.getTags().contains(PLAYER_AWARDED_TAG))
                    return;

                player.addTag(PLAYER_AWARDED_TAG);
                player.giveExperiencePoints(randomExperiencePts);
                ENDED = false;
            }
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            if (!isDailyWorld(server.theGame().overworld())) {
                return;
            }

            if (server.theGame().getAllLevels().size() > 1) {
                // Re-join - Untrusted.
                server.stopServer(null);
                return;
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((player, server) -> {
            if (isDailyWorld(server.theGame().overworld())
                    && GAME_TIME_AT_START != -1) {
                dailyEnded(false, 0);
            }
        });

        ServerTickEvents.END_WORLD_TICK.register(s -> {
            if (!isDailyWorld(s.theGame().overworld())) return;
            if (ENDED) return;
            if (!s.isMine()) return;
            if (s.isMineCompleted()) {
                dailyEnded(s.isMineWon(),
                        ((ServerLevelAccessor) s.theGame().overworld()).getMineData()
                                .getExperienceToDrop());
                return;
            }

            if (GAME_TIME_AT_START == -1) {
                dailyStarted(s.getGameTime());
            }


            var remainingTime = MAX_GAME_TIME - s.getGameTime() + GAME_TIME_AT_START;

            if (remainingTime <= 0) {
                dailyEnded(false, 0);
            }

            REMAINING_TIME_CACHE = remainingTime;
        });
    }

    public static void startDaily() {
        WorldCreationUtil.createAndLoadDaily("_daily", todayDailySeed);
    }

    public static void openLeaderboard() {
        Minecraft.getInstance().setScreen(new LeaderboardScreen());
    }

    public static void dailyStarted(long gameTime) {
        GAME_TIME_AT_START = gameTime;

        lastPlayedSeed = Minecraft.getInstance().getSingleplayerServer().theGame()
                .getWorldData().worldGenOptions().seed();

        storeLastPlayedSeed();
    }

    public static void dailyEnded(boolean won, int xp) {
        ENDED = true;

        var server = Minecraft.getInstance().getSingleplayerServer();

        server.theGame().tickRateManager().endTickWork();
        var finalRemainingTime = MAX_GAME_TIME - server.theGame().overworld().getGameTime()
                + GAME_TIME_AT_START;

        if (won) {
        } else {
        }

        // Reset
        GAME_TIME_AT_START = -1;

        Minecraft.getInstance().disconnect();
    }

    public static boolean shouldRenderInGameTimer() {
        if (GAME_TIME_AT_START == -1) return false;
        if (ENDED) return false;

        return true;
    }

    private static void refreshAllDailies() {

    }

    public static boolean shouldAllowDaily() {
        if (lastPlayedSeed == todayDailySeed) return false;
        if (todayDailySeed == -1) return false;
        return true;
    }

    public static boolean isDailyWorld(ServerLevel level) {
        return level.theGame().getWorldData().getKnownServerBrands().stream().anyMatch(e -> e.equals(DAILY_SERVER_BRAND));
    }

    private static void storeLastPlayedSeed() {
        try {
            Files.writeString(lastDailySeedPath, String.format("%s", lastPlayedSeed), Charset.defaultCharset());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void restoreLastPlayedSeed() {
        try {
            if (!Files.exists(lastDailySeedPath)) {
                lastPlayedSeed = -1;
            }

            var stringSeed = Files.readString(lastDailySeedPath, Charset.defaultCharset()).trim();
            lastPlayedSeed = Long.parseLong(stringSeed);
        } catch (Exception e) {
            lastPlayedSeed = -1;
        }
    }
}
