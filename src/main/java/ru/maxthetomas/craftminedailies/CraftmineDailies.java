package ru.maxthetomas.craftminedailies;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerUnlock;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.mines.WorldEffect;
import ru.maxthetomas.craftminedailies.auth.ApiManager;
import ru.maxthetomas.craftminedailies.auth.ClientAuth;
import ru.maxthetomas.craftminedailies.auth.meta.ApiMeta;
import ru.maxthetomas.craftminedailies.auth.meta.InventoryMeta;
import ru.maxthetomas.craftminedailies.mixin.common.LivingEntityInvoker;
import ru.maxthetomas.craftminedailies.screens.LeaderboardScreen;
import ru.maxthetomas.craftminedailies.screens.NonDeathDailyEndScreen;
import ru.maxthetomas.craftminedailies.util.EndContext;
import ru.maxthetomas.craftminedailies.util.WorldCreationUtil;
import ru.maxthetomas.craftminedailies.util.ends.DeathEndContext;
import ru.maxthetomas.craftminedailies.util.ends.IllegitimateEndContext;
import ru.maxthetomas.craftminedailies.util.ends.TimeOutContext;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CraftmineDailies implements ModInitializer {
    public static final String MOD_ID = "craftminedailies";
    public static final String WORLD_NAME = "_cmd_daily";

    public static final int VERSION = 3;
    public static boolean HAS_UPDATES = false;

    private static Path lastDailySeedPath;
    private static long lastPlayedSeed = -1;

    public static final String DAILY_SERVER_BRAND = "_cm_daily";
    public static final String PLAYER_AWARDED_TAG = "_cm_daily_xp_awarded";
    public static final long MAX_GAME_TIME = 20 * 60 * 30;
    protected static long GAME_TIME_AT_START = -1;

    public static final float XP_MULT = 100;

    public static final Component BUTTON_TEXT_OK = Component.translatable("craftminedailies.button.start.active");
    public static final Component BUTTON_TEXT_PLAYED = Component.translatable("craftminedailies.button.start.played");
    public static final Component BUTTON_TEXT_LOADING = Component.translatable("craftminedailies.button.start.loading");
    public static final Component BUTTON_TEXT_NETWORK_ISSUE = Component.translatable("craftminedailies.button.start.issue");

    public static final Component REMINDER_TIME_10M = Component.translatable("craftminedailies.reminders.10m");
    public static final Component REMINDER_TIME_5M = Component.translatable("craftminedailies.reminders.5m");
    public static final Component REMINDER_TIME_1M = Component.translatable("craftminedailies.reminders.1m");

    public static long REMAINING_TIME_CACHE = -1;

    public static DeathEndContext LAST_DEATH_CONTEXT = null;

    // If run has ended for any reason.
    protected static boolean ENDED = false;
    protected static boolean WORLD_STARTED = false;

    private static int ticksToExpUpdate = 10;
    public static int CACHED_CURRENT_INV_EXP = 0;

    @Override
    public void onInitialize() {
        lastDailySeedPath = Path.of(FabricLoader.getInstance().getConfigDir().toString() + "/", ".cd_last_played_seed");
        ClientAuth.create();
        restoreLastPlayedSeed();
        fetchToday();

        checkForUpdates();

        ServerPlayConnectionEvents.JOIN.register((serverPlayer,
                                                  packetListener, server) -> {
            if (!isInDaily()) return;

            var player = serverPlayer.getPlayer();

            if (player.getTags().contains(PLAYER_AWARDED_TAG))
                return;


            forceUnlocks(serverPlayer.getPlayer());

            player.addTag(PLAYER_AWARDED_TAG);
            player.giveExperiencePoints(ApiManager.TodayDetails.xp());

            server.schedule(new TickTask(server.getTickCount() + 20, this::showErrorIfUnauthorized));

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

            var inventoryMeta = InventoryMeta.createForPlayer(player);

            var experience = getPlayerInventoryValue(player, player.serverLevel(), true, 0.5);
            var ctx = new DeathEndContext(experience, getRemainingTime(player.serverLevel().theGame().server()),
                    player, damageSource);

            dailyEnded(ctx, inventoryMeta);

            LAST_DEATH_CONTEXT = ctx;

            return true;
        });

        ServerPlayConnectionEvents.DISCONNECT.register((player, server) -> {
            if (isInDaily() && GAME_TIME_AT_START != -1) {
                var inv = InventoryMeta.createForPlayer(player.getPlayer());
                dailyEnded(new IllegitimateEndContext(getRemainingTime(server)), inv);
            }
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
                CACHED_CURRENT_INV_EXP = getPlayerInventoryValue(player, player.serverLevel(), false, 1.0);
            }

            var remainingTime = getRemainingTime(s.theGame().server());

            if (remainingTime == 20 * 60 * 10)
                s.theGame().playerList().broadcastSystemMessage(REMINDER_TIME_10M, false);
            if (remainingTime == 20 * 60 * 5) s.theGame().playerList().broadcastSystemMessage(REMINDER_TIME_5M, false);
            if (remainingTime == 20 * 60) s.theGame().playerList().broadcastSystemMessage(REMINDER_TIME_1M, false);

            if (remainingTime <= 0) {
                var player = Minecraft.getInstance().getSingleplayerServer().theGame().playerList().getPlayers().get(0);
                var inventoryMeta = InventoryMeta.createForPlayer(player);
                var context = new TimeOutContext(getPlayerInventoryValue(player, player.serverLevel(), true, 0.5));
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
        fetchToday();
    }

    public static void openLeaderboard() {
        openLeaderboard(0);
    }

    public static void dailyStarted(long gameTime) {
        // waiting for first tick with player
        if (Minecraft.getInstance().getSingleplayerServer().theGame().playerList().getPlayers().isEmpty())
            return;


        GAME_TIME_AT_START = gameTime;

        lastPlayedSeed = Minecraft.getInstance().getSingleplayerServer().theGame()
                .getWorldData().worldGenOptions().seed();

        storeLastPlayedSeed();

        ApiManager.submitRunStart(ApiMeta.createMeta(new InventoryMeta(List.of())));
    }

    public static void dailyEnded(EndContext endContext, InventoryMeta meta) {
        ENDED = true;

        ApiManager.submitRunEnd(endContext, ApiMeta.createMeta(meta));

        // Reset
        GAME_TIME_AT_START = -1;
        REMAINING_TIME_CACHE = -1;
    }

    public static int getRemainingTime(MinecraftServer server) {
        return (int) (MAX_GAME_TIME - server.theGame().overworld().getGameTime() + GAME_TIME_AT_START);
    }

    public static boolean shouldRenderInGameTimer() {
        if (GAME_TIME_AT_START == -1) return false;
        if (ENDED) return false;

        return true;
    }

    private static void refreshAllDailies() {

    }

    public static void showErrorMessage(Component component, boolean resetDaily) {
        Minecraft.getInstance().getChatListener().handleSystemMessage(Component.translatable("craftminedailies.errors.base" + (resetDaily ? "_with_reset" : ""), component)
                .withStyle(ChatFormatting.RED), false);

        if (resetDaily) {
            resetDailyProgression();
        }
    }

    private static void resetDailyProgression() {
        lastPlayedSeed = -1;
    }

    public static boolean shouldAllowDaily() {
        if (ApiManager.TodayDetails == null) return false;
        if (lastPlayedSeed == ApiManager.TodayDetails.seed()) return false;
        return true;
    }

    public static Component getStartDailyButtonText() {
        if (ApiManager.DailyFetchError)
            return BUTTON_TEXT_NETWORK_ISSUE;

        if (ApiManager.TodayDetails == null)
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
    public static int getPlayerInventoryValue(ServerPlayer player, ServerLevel level, boolean ignoreSelfPlacedWorldEffects, double extraScale) {
        double totalXp = 0F;
        for (ItemStack stack : player.getInventory()) {
            var worldMods = stack.get(DataComponents.WORLD_MODIFIERS);
            if (worldMods != null) continue;
            else if (stack.is(ItemTags.CARRY_OVER)) continue;

            var exchangeValueComponent = stack.getOrDefault(DataComponents.EXCHANGE_VALUE, Item.NO_EXCHANGE);
            totalXp += exchangeValueComponent.getValue(player, stack);
        }

        double multiplier = 1.0F;
        var todayEffects = ApiManager.TodayDetails.getEffects();
        for (WorldEffect effect : level.getActiveEffects()) {
            if (ignoreSelfPlacedWorldEffects) {
                // Check that the effect is not forced
                if (!todayEffects.contains(effect))
                    continue;
            }

            multiplier *= effect.experienceModifier();
        }

        multiplier *= player.getAttributeValue(Attributes.EXPERIENCE_GAIN_MODIFIER);
        return (int) (totalXp * multiplier * extraScale * XP_MULT);
    }

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

    private static void dumpData() {
        var json = new JsonObject();

        var effects = new JsonObject();
        BuiltInRegistries.WORLD_EFFECT.entrySet().forEach(entry -> {
            var effect = entry.getValue();
            var key = entry.getKey().location().toString();
            var effectJson = new JsonObject();
            effectJson.addProperty("weight", effect.randomWeight());
            effectJson.addProperty("multiplayer_only", effect.multiplayerOnly());
            effectJson.addProperty("unlock_mode", effect.unlockMode().toString().toLowerCase());
            effectJson.addProperty("xp_multiplier", effect.experienceModifier());

            var incompat = new JsonArray();
            effect.incompatibleWith().forEach(we -> {
                incompat.add("minecraft:" + we.key());
            });
            effectJson.add("incompatible_with", incompat);

            effects.add(key, effectJson);
        });
        json.add("effects", effects);

        var sets = new JsonObject();
        BuiltInRegistries.WORLD_EFFECT_SET.entrySet().forEach(entry -> {
            var setJson = new JsonObject();
            var key = entry.getKey().location().toString();

            setJson.addProperty("exclusive", entry.getValue().exclusive());

            var effectList = new JsonArray();
            entry.getValue().effects().forEach(eff -> effectList.add("minecraft:" + eff.key()));
            setJson.add("effects", effectList);
            sets.add(key, setJson);
        });
        json.add("effect_sets", sets);

        var unlocks = new JsonObject();
        BuiltInRegistries.PLAYER_UNLOCK.entrySet().forEach(entry -> {
            var unlockJson = new JsonObject();
            var key = entry.getKey().location().toString();

            var disablesArray = new JsonArray();
            entry.getValue().disables().forEach(disable -> {
                disablesArray.add(disable.getRegisteredName());
            });


            unlockJson.add("disables", disablesArray);
            unlockJson.addProperty("price", entry.getValue().unlockPrice());

            entry.getValue().parent().ifPresentOrElse((unlock) ->
                    unlockJson.addProperty("parent", unlock.getRegisteredName()), () -> unlockJson.addProperty("parent", "minecraft:empty"));

            unlocks.add(key, unlockJson);
        });
        json.add("unlocks", unlocks);

        try {
            Files.writeString(Path.of("./effect_json_data.json"), json.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
