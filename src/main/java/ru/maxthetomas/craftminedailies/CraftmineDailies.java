package ru.maxthetomas.craftminedailies;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerUnlock;
import net.minecraft.util.Mth;
import net.minecraft.world.item.component.DeathProtection;
import ru.maxthetomas.craftminedailies.auth.ApiManager;
import ru.maxthetomas.craftminedailies.auth.ClientAuth;
import ru.maxthetomas.craftminedailies.auth.meta.ApiMeta;
import ru.maxthetomas.craftminedailies.auth.meta.InventoryMeta;
import ru.maxthetomas.craftminedailies.content.DailyWorldEffects;
import ru.maxthetomas.craftminedailies.mixin.common.LivingEntityInvoker;
import ru.maxthetomas.craftminedailies.mixin.common.ServerLevelAccessor;
import ru.maxthetomas.craftminedailies.screens.LeaderboardScreen;
import ru.maxthetomas.craftminedailies.screens.NonDeathDailyEndScreen;
import ru.maxthetomas.craftminedailies.util.*;
import ru.maxthetomas.craftminedailies.util.ends.DeathEndContext;
import ru.maxthetomas.craftminedailies.util.ends.IllegitimateEndContext;
import ru.maxthetomas.craftminedailies.util.ends.TimeOutContext;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CraftmineDailies implements ModInitializer {
    public static final String MOD_ID = "craftminedailies";
    public static final String WORLD_NAME = "_cmd_daily";

    public static final URI DISCORD_URL = URI.create("https://discord.gg/VAC7ZMTuPU");

    // Enables registration and use of experimental daily world components
    public static final boolean EXPERIMENTAL = false;

    public static final int VERSION = 13;
    private static String VERSION_STRING = String.valueOf(VERSION);
    public static boolean HAS_UPDATES = false;

    private static Path lastDailySeedPath;
    private static long lastPlayedSeed = -1;

    public static final String DAILY_SERVER_BRAND = "_cm_daily";
    public static final String PLAYER_AWARDED_TAG = "_cm_daily_xp_awarded";
    public static final long DEFAULT_MAX_GAME_TIME = 20 * 60 * 30;
    protected static long GAME_TIME_AT_START = -1;

    public static List<Component> END_TEXT;

    public static final float XP_MULT = 100;

    public static final Component BUTTON_TEXT_OK = Component.translatable("craftminedailies.button.start.active");
    public static final Component BUTTON_TEXT_PLAYED = Component.translatable("craftminedailies.button.start.played");
    public static final Component BUTTON_TEXT_LOADING = Component.translatable("craftminedailies.button.start.loading");
    public static final Component BUTTON_TEXT_NETWORK_ISSUE = Component.translatable("craftminedailies.button.start.issue");

    public static final Component REMINDER_TIME_10M = Component.translatable("craftminedailies.reminders.10m");
    public static final Component REMINDER_TIME_5M = Component.translatable("craftminedailies.reminders.5m");
    public static final Component REMINDER_TIME_1M = Component.translatable("craftminedailies.reminders.1m");

    public static long REMAINING_TIME_CACHE = -1;
    private static float previousRemainingTime = -1;

    // If run has ended for any reason.
    protected static boolean ENDED = false;

    private static int ticksToExpUpdate = 10;
    public static int CACHED_CURRENT_INV_EXP = 0;

    public static String getStringVersion() {
        return VERSION_STRING;
    }

    @Override
    public void onInitialize() {
        lastDailySeedPath = Path.of(FabricLoader.getInstance().getConfigDir().toString() + "/", ".cd_last_played_seed");

        // Load this mod by mod's ID
        VERSION_STRING = FabricLoader.getInstance().getModContainer(MOD_ID)
                .get().getMetadata().getVersion().getFriendlyString();

        DailyWorldEffects.bootstrap();

        ClientAuth.createApiPath();
        checkForUpdates();
        restoreLastPlayedSeed();
        fetchToday();

        ServerPlayConnectionEvents.JOIN.register((serverPlayer,
                                                  packetListener, server) -> {
            if (!isInDaily()) return;

            var player = serverPlayer.getPlayer();

            if (player.getTags().contains(PLAYER_AWARDED_TAG))
                return;


            forceUnlocks(serverPlayer.getPlayer());

            player.addTag(PLAYER_AWARDED_TAG);
            player.giveExperiencePoints(ApiManager.TodayDetails.xp());

            showErrorIfUnauthorized();

            ENDED = false;
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            if (!isInDaily()) {
                return;
            }
        });

        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmt) -> {
            if (!isInDaily()) return true;
            if (!(entity instanceof ServerPlayer player)) return true;

            // Totem edge case
            if (((LivingEntityInvoker) entity).callCheckTotemDeathProtection(damageSource))
                return true;

            if (EXPERIMENTAL
                    && player.serverLevel().isActive(DailyWorldEffects.STRONG_SPIRIT)) {
                var deathProtection = DeathProtection.TOTEM_OF_UNDYING;
                player.sendSystemMessage(Component.translatable("craftminedailies.strong_spirit.saved"));
                player.setHealth(1f);
                deathProtection.applyEffects(null, player);

                var effects = ((ServerLevelAccessor) player.serverLevel()).getActiveEffects();
                effects.remove(DailyWorldEffects.STRONG_SPIRIT);
                effects.add(DailyWorldEffects.USED_STRONG_SPIRIT);
                return false;
            }


            var inventoryMeta = InventoryMeta.createForPlayer(player);

            var hasPenalty = DailyWorldEffects.shouldApplyDeathPenalty(player.serverLevel());
            var xpScaler = 1f;
            if (hasPenalty) xpScaler *= 0.5f;

            if (EXPERIMENTAL && player.serverLevel().isActive(DailyWorldEffects.ALL_IN))
                xpScaler = 0f;

            var experience = DailiesUtil.getPlayerInventoryValue(player, player.serverLevel(),
                    hasPenalty, xpScaler);
            var ctx = new DeathEndContext(experience, DailyTimeCalculator.getActualPassedTime(player, (int) GAME_TIME_AT_START), player, damageSource);

            dailyEnded(ctx, inventoryMeta);

            return true;
        });

        ServerPlayConnectionEvents.DISCONNECT.register((player, server) -> {
            if (isInDaily() && GAME_TIME_AT_START != -1) {
                var inv = InventoryMeta.createForPlayer(player.getPlayer());
                dailyEnded(new IllegitimateEndContext(DailyTimeCalculator.getActualPassedTime(player.getPlayer(), (int) GAME_TIME_AT_START)), inv);
            }

            // Reset variables
            ENDED = true;
            GAME_TIME_AT_START = -1;
        });

        ServerTickEvents.END_WORLD_TICK.register(s -> {
            if (!isInDaily()) return;
            if (ENDED) return;
            if (!s.isMine()) return;
            if (s.isMineCompleted()) {
                // This is handled by ServerLevelMixin
                return;
            }

            if (GAME_TIME_AT_START == -1) {
                dailyStarted(s.getGameTime());
                return;
            }

            if (--ticksToExpUpdate < 0 && !s.players().isEmpty()) {
                ticksToExpUpdate = 10;
                var player = s.players().getFirst();
                CACHED_CURRENT_INV_EXP = DailiesUtil.getPlayerInventoryValue(player, player.serverLevel(), false, 1.0);
            }


            previousRemainingTime = DailyTimeCalculator.getRemainingTime(s.players().getFirst(), previousRemainingTime, (int) GAME_TIME_AT_START);
            var remainingTime = Mth.floor(previousRemainingTime);

            if (remainingTime == 20 * 60 * 10)
                s.theGame().playerList().broadcastSystemMessage(REMINDER_TIME_10M, false);
            if (remainingTime == 20 * 60 * 5) s.theGame().playerList().broadcastSystemMessage(REMINDER_TIME_5M, false);
            if (remainingTime == 20 * 60) s.theGame().playerList().broadcastSystemMessage(REMINDER_TIME_1M, false);

            if (remainingTime <= 0) {
                var player = Minecraft.getInstance().getSingleplayerServer().theGame().playerList().getPlayers().getFirst();
                var inventoryMeta = InventoryMeta.createForPlayer(player);

                var inventoryPrice = 0.5f;
                if (EXPERIMENTAL && player.serverLevel().isActive(DailyWorldEffects.ALL_IN))
                    inventoryPrice = 0f;

                var inventoryValue = DailiesUtil.getPlayerInventoryValue(player, player.serverLevel(), true, inventoryPrice);
                var context = new TimeOutContext(inventoryValue, DailyTimeCalculator.getActualPassedTime(player, (int) GAME_TIME_AT_START));

                dailyEnded(context, inventoryMeta);

                var minecraft = Minecraft.getInstance();
                Minecraft.getInstance().schedule(() -> {
                    if (minecraft.level != null) {
                        minecraft.level.disconnect();
                    }

                    minecraft.disconnect(new GenericMessageScreen(Component.translatable("menu.savingLevel")));
                    minecraft.setScreen(new NonDeathDailyEndScreen(context));
                });
            }

            REMAINING_TIME_CACHE = remainingTime;
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(stopped -> {
            WorldCreationUtil.tryDeleteWorld();
        });

        if (FabricLoader.getInstance().isDevelopmentEnvironment())
            DataDumper.dumpData();
    }

    private void showErrorIfUnauthorized() {
        if (ClientAuth.isAuthorized()) return;
        showErrorMessage(Component.translatable("craftminedailies.errors.unauthorized"), false);
    }

    public static void forceUnlocks(ServerPlayer player) {
        for (Holder.Reference<PlayerUnlock> forcedUnlock : ApiManager.TodayDetails.getForcedPlayerUnlocks()) {
            player.forceUnlock(forcedUnlock);

            DisplayInfo displayInfo = forcedUnlock.value().display();
            if (displayInfo.shouldAnnounceChat()) {
                player.sendSystemMessage(displayInfo.getType().createPlayerUnlockAnnouncement(forcedUnlock, player));
            }
        }
    }

    public static void fetchToday() {
        ApiManager.updateDailyDetails();
    }

    public static void startDaily() {
        if (!shouldAllowDaily()) return;
        ApiManager.login();
        WorldCreationUtil.createAndLoadDaily(ApiManager.TodayDetails);
    }

    public static void openLeaderboard(int startPage) {
        Minecraft.getInstance().setScreen(new LeaderboardScreen(startPage));
        DefaultDataPackLoader.tryLoadAsync();
        fetchToday();
    }

    public static void openLeaderboard() {
        openLeaderboard(0);
    }

    public static void dailyStarted(long gameTime) {
        // waiting for first tick with player
        if (Minecraft.getInstance().getSingleplayerServer().theGame().playerList().getPlayers().isEmpty())
            return;

        previousRemainingTime = -1;
        GAME_TIME_AT_START = gameTime;

        lastPlayedSeed = Minecraft.getInstance().getSingleplayerServer().theGame()
                .getWorldData().worldGenOptions().seed();

        storeLastPlayedSeed();

        ApiManager.submitRunStart(ApiMeta.createMeta(new InventoryMeta(List.of())));
    }

    public static void dailyEnded(EndContext endContext, InventoryMeta meta) {
        ENDED = true;

        END_TEXT = DailiesUtil.createRunDetails(endContext);

        ApiManager.submitRunEnd(endContext, ApiMeta.createMeta(meta));

        // Reset
        ENDED = true;
        GAME_TIME_AT_START = -1;
        previousRemainingTime = -1;
    }

    public static int getTimeAtStart() {
        return (int) GAME_TIME_AT_START;
    }

    public static boolean shouldRenderInGameTimer() {
        if (GAME_TIME_AT_START == -1) return false;
        if (previousRemainingTime == -1) return false;
        if (ENDED) return false;

        return true;
    }

    public static void showErrorMessage(Component component, boolean resetDaily) {
        GameOverlay.pushNotification(Component.translatable("craftminedailies.errors.base" + (resetDaily ? "_with_reset" : ""),
                component).withStyle(ChatFormatting.RED));

        if (resetDaily) {
            resetDailyProgression();
        }
    }

    private static void resetDailyProgression() {
        lastPlayedSeed = -1;
        storeLastPlayedSeed();
    }

    public static boolean shouldAllowDaily() {
        if (ApiManager.TodayDetails == null) return false;
        if (lastPlayedSeed == ApiManager.TodayDetails.seed()) return false;
        if (DefaultDataPackLoader.isLoadingData()) return false;

        return true;
    }

    public static Component getStartDailyButtonText() {
        if (ApiManager.DailyFetchError)
            return BUTTON_TEXT_NETWORK_ISSUE;

        if (ApiManager.TodayDetails == null
                || DefaultDataPackLoader.isLoadingData())
            return BUTTON_TEXT_LOADING;

        if (lastPlayedSeed == ApiManager.TodayDetails.seed())
            return BUTTON_TEXT_PLAYED;

        return BUTTON_TEXT_OK;
    }

    public static boolean isInDaily() {
        var server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) return false;
        var overworld = server.theGame().overworld();
        return isDailyWorld(overworld);
    }

    // Reimplement inventory value calculation for deaths.

    private static boolean isDailyWorld(ServerLevel level) {
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

    public void checkForUpdates() {
        ApiManager.fetchServerVersions().whenComplete((ver, thr) -> {
            if (thr != null) return;
            HAS_UPDATES = ver.clientVersion() > VERSION;
        });
    }

}
