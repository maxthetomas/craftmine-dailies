package ru.maxthetomas.craftminedailies;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import ru.maxthetomas.craftminedailies.util.WorldCreationUtil;

public class CraftmineDailies implements ModInitializer {
    public static RandomSource RANDOM_EFFECT_SOURCE;

    public static final String DAILY_SERVER_BRAND = "_cm_daily";
    public static final String PLAYER_AWARDED_TAG = "_cm_daily_xp_awarded";
    public static final long MAX_GAME_TIME = 20 * 60 * 30;
    protected static long GAME_TIME_AT_START = -1;

    public static long REMAINING_TIME_CACHE = -1;

    // If run has ended for any reason.
    protected static boolean ENDED = false;

    @Override
    public void onInitialize() {
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

        ServerTickEvents.END_WORLD_TICK.register(s -> {
            if (!isDailyWorld(s.theGame().overworld())) return;
            if (ENDED) return;
            if (!s.isMine()) return;
            if (s.isMineCompleted()) {
                dailyEnded(s.isMineWon());
                return;
            }

            if (GAME_TIME_AT_START == -1)
                GAME_TIME_AT_START = s.getGameTime();


            var remainingTime = MAX_GAME_TIME - s.getGameTime() + GAME_TIME_AT_START;

            if (remainingTime <= 0) {
                dailyEnded(false);
            }

            REMAINING_TIME_CACHE = remainingTime;
        });
    }

    public static void startDaily() {
        WorldCreationUtil.createAndLoadDaily(
                "_daily",
                24869L
        );
    }

    public static void dailyEnded(boolean won) {
        ENDED = true;

        Minecraft.getInstance().getSingleplayerServer().theGame().tickRateManager().endTickWork();

        if (won) {
        } else {
        }
    }

    public static boolean isDailyWorld(ServerLevel level) {
        return level.theGame().getWorldData().getKnownServerBrands().stream().anyMatch(e -> e.equals(DAILY_SERVER_BRAND));
    }
}
