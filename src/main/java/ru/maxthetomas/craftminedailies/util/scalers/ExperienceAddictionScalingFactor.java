package ru.maxthetomas.craftminedailies.util.scalers;

import net.minecraft.server.level.ServerPlayer;

public class ExperienceAddictionScalingFactor implements ITimeScalingFactor {
    private final ServerPlayer player;

    public ExperienceAddictionScalingFactor(ServerPlayer player) {
        this.player = player;
    }

    int tick = 0;
    int noXpPickupsTicks = 0;
    int lastXpLevel = -1;

    @Override
    public float getScale() {
        tick++;

        var currentXp = player.getTotalExperienceBasedOnLevels();
        if (currentXp > lastXpLevel) {
            noXpPickupsTicks = 0;
        }

        noXpPickupsTicks++;
        lastXpLevel = currentXp;

        var scale = 1f;
        if (noXpPickupsTicks > 200) {
            scale += (noXpPickupsTicks - 200f) / 1200f;
        }

        return scale;
    }
}
