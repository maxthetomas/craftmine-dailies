package ru.maxthetomas.craftminedailies.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import ru.maxthetomas.craftminedailies.CraftmineDailies;
import ru.maxthetomas.craftminedailies.content.DailyWorldEffects;
import ru.maxthetomas.craftminedailies.util.scalers.ExperienceAddictionScalingFactor;
import ru.maxthetomas.craftminedailies.util.scalers.ITimeScalingFactor;

import static ru.maxthetomas.craftminedailies.util.TimeFormatters.formatTimeWithoutHours;

public class DailyTimeCalculator {

    public static long getDailyEndTime(ServerPlayer player) {
        if (!CraftmineDailies.EXPERIMENTAL || !player.serverLevel().isMine())
            return CraftmineDailies.DEFAULT_MAX_GAME_TIME;

        return CraftmineDailies.DEFAULT_MAX_GAME_TIME;
    }

    private static float scalingFactor = 1.0f;
    private static ITimeScalingFactor scalingHandler;

    public static float getRemainingTime(ServerPlayer player, float previousTime, int startTime) {
        if (previousTime == -1) {
            reset(player);
            return (float) getDailyEndTime(player);
        }

        scalingFactor = scalingHandler != null ? scalingHandler.getScale() : 1f;

        // Don't allow to go over actual time limit
        return Math.min(previousTime - scalingFactor,
                getDailyEndTime(player) - getActualPassedTime(player, startTime));
    }

    private static int lerpColor(int from, int to, float f) {
        var r = Mth.lerpInt(f, (from >> 16) & 0xFF, (to >> 16) & 0xFF);
        var g = Mth.lerpInt(f, (from >> 8) & 0xFF, (to >> 8) & 0xFF);
        var b = Mth.lerpInt(f, from & 0xFF, to & 0xFF);

        return (r << 16) | (g << 8) | b;
    }

    public static int getColorForScalingFactor(float sf) {
        var fac = ((sf - 1f) / 2f);
        return lerpColor(0x44FF44, 0xFF3333, Mth.clamp(fac, 0f, 1f));
    }

    public static int getActualPassedTime(ServerPlayer player, int startTime) {
        var mineLevel = player.serverLevel();
        return (int) (mineLevel.getGameTime() - startTime);
    }

    private static void reset(ServerPlayer player) {
        if (!CraftmineDailies.EXPERIMENTAL)
            return;

        scalingHandler = null;

        if (player.serverLevel().isActive(DailyWorldEffects.XP_ADDICTION))
            scalingHandler = new ExperienceAddictionScalingFactor(player);
    }

    public static float getCurrentTimeScale() {
        return scalingFactor;
    }

    public static Component getTimeText() {
        return Component.literal(formatTimeWithoutHours(CraftmineDailies.REMAINING_TIME_CACHE / 20));
    }

    public static Component getTimeScaleText() {
        if (scalingHandler == null)
            return Component.empty();

        return Component.literal(String.format("x%.1f", scalingFactor))
                .withColor(getColorForScalingFactor(scalingFactor));
    }
}
