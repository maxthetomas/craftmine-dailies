package ru.maxthetomas.craftminedailies.content;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.mines.WorldEffect;
import ru.maxthetomas.craftminedailies.CraftmineDailies;
import ru.maxthetomas.craftminedailies.mixin.common.WorldEffectBuilderInvoker;

public class DailyWorldEffects {
    public static Style DAILY_WORLD_EFFECT_STYLE = Style.EMPTY.withColor(ChatFormatting.GOLD);

    public static WorldEffect DAILY_BUT_FAST = register(WorldEffect.builder("daily_but_fast")
            .xpModifier(6f)
            .withNameStyle(DAILY_WORLD_EFFECT_STYLE)
            .withItemModelOf(Items.GLOWSTONE_DUST));

    public static WorldEffect NO_DEATH_PENALTY = register(WorldEffect.builder("no_death_penalty")
            .withNameStyle(DAILY_WORLD_EFFECT_STYLE)
            .withItemModelOf(Items.GRAVE_ADVANCEMENT));

    private static WorldEffect register(WorldEffect.Builder builder) {
        var effect = ((WorldEffectBuilderInvoker) builder).callBuild();

        if (!CraftmineDailies.EXPERIMENTAL)
            return effect;

        Registry.register(BuiltInRegistries.WORLD_EFFECT,
                ResourceLocation.fromNamespaceAndPath(CraftmineDailies.MOD_ID, effect.key()),
                effect);

        return effect;
    }

    public static WorldEffect bootstrap() {
        return DAILY_BUT_FAST;
    }

    public static long getDailyEndTime(ServerLevel mineLevel) {
        if (!CraftmineDailies.EXPERIMENTAL || !mineLevel.isMine())
            return CraftmineDailies.DEFAULT_MAX_GAME_TIME;

        var time = CraftmineDailies.DEFAULT_MAX_GAME_TIME;

        if (mineLevel.getActiveEffects().contains(DAILY_BUT_FAST))
            time /= 6;

        return time;
    }

    public static boolean shouldApplyDeathPenalty(ServerLevel mineLevel) {
        if (!CraftmineDailies.EXPERIMENTAL)
            return true;

        return !mineLevel.getActiveEffects().contains(NO_DEATH_PENALTY);
    }
}
