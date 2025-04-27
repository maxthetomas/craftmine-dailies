package ru.maxthetomas.craftminedailies.util.scalers;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

public class ExperienceAddictionScalingFactor implements ITimeScalingFactor {
    private final ServerPlayer player;

    public ExperienceAddictionScalingFactor(ServerPlayer player) {
        this.player = player;
    }

    int tick = 0;
    int noXpPickupsTicks = 0;
    int effectiveNoXpPickupTicks = 0;
    int lastXpLevel = -1;

    int actualXpLevel = 0;

    @Override
    public float getScale() {
        tick++;


        var currentXp = player.getTotalExperienceBasedOnLevels();

        var diff = currentXp - lastXpLevel;
        if (lastXpLevel != -1)
            actualXpLevel = Math.max(0, actualXpLevel + diff);

        if (currentXp > lastXpLevel) {
            noXpPickupsTicks = 0;
        }

        noXpPickupsTicks++;
        effectiveNoXpPickupTicks = Math.max(noXpPickupsTicks - 20 * 20, 0);
        lastXpLevel = currentXp;

        float scale = 1f + Mth.floor(effectiveNoXpPickupTicks / (20f * 60f));

        if (effectiveNoXpPickupTicks % (20 * 60) > 20 * 40) {
            scale += (effectiveNoXpPickupTicks % (20f * 60f) - 20f * 40f) / (20f * 20f);
        }

        return scale;
    }

    @Override
    public float getShakiness() {
        float scale = Mth.floor(effectiveNoXpPickupTicks / (20f * 60f));

        scale += Math.clamp((effectiveNoXpPickupTicks % (20f * 60f)) / (20f * 40f), 0f, 1f);

        return scale;
    }

    public int getActualXpLevel() {
        return actualXpLevel;
    }
}
