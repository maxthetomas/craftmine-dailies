package ru.maxthetomas.craftminedailies;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerUnlock;
import ru.maxthetomas.craftminedailies.auth.ApiManager;
import ru.maxthetomas.craftminedailies.mixin.common.ServerLevelAccessor;
import ru.maxthetomas.craftminedailies.screens.LeaderboardScreen;
import ru.maxthetomas.craftminedailies.util.WorldCreationUtil;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class CraftmineDailies implements ModInitializer {
    public static final String MOD_ID = "craftminedailies";

    private static Path lastDailySeedPath;
    private static long lastPlayedSeed = -1;

    private static long todayDailySeed = -1;

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
                var randomExperiencePts = ApiManager.TodayDetails.xp();
                var player = serverPlayer.getPlayer();

                if (player.getTags().contains(PLAYER_AWARDED_TAG))
                    return;

                forceUnlocks(serverPlayer.getPlayer());

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

        ServerLivingEntityEvents.AFTER_DEATH.register((died, source) -> {
            if (!(died instanceof ServerPlayer player)) return;
            var str = source.getLocalizedDeathMessage(died).getString();
            // todo
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((a, b, resp) -> {
            if (!isDailyWorld(a.serverLevel().theGame().overworld())) return;
            a.disconnect();
            b.disconnect();
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

    public static void forceUnlocks(ServerPlayer player) {
        for (Holder.Reference<PlayerUnlock> forcedUnlock : ApiManager.TodayDetails.getForcedPlayerUnlocks()) {
            player.forceUnlock(forcedUnlock);

            DisplayInfo displayInfo = ((PlayerUnlock) forcedUnlock.value()).display();
            if (displayInfo.shouldAnnounceChat()) {
                player.sendSystemMessage(displayInfo.getType().createPlayerUnlockAnnouncement(forcedUnlock, player));
            }
        }
    }

    public static void fetchToday() {
        ApiManager.updateDailyDetails().whenComplete((data, error) -> {
            if (error != null) {
                todayDailySeed = -1;
                return;
            }
            todayDailySeed = data.seed();
        });
    }

    public static void startDaily() {
        WorldCreationUtil.createAndLoadDaily("_daily", ApiManager.TodayDetails);
    }

    public static void openLeaderboard() {
        Minecraft.getInstance().setScreen(new LeaderboardScreen());
        fetchToday();
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
        REMAINING_TIME_CACHE = -1;
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
