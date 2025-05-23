package ru.maxthetomas.craftminedailies.content;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.mines.WorldEffect;
import ru.maxthetomas.craftminedailies.CraftmineDailies;
import ru.maxthetomas.craftminedailies.mixin.common.WorldEffectBuilderInvoker;

public class DailyWorldEffects {
    public static Style DAILY_WORLD_EFFECT_STYLE = Style.EMPTY.withColor(ChatFormatting.GOLD);

    // Removes the death penalty
    public static final WorldEffect NO_DEATH_PENALTY = register(WorldEffect.builder("no_death_penalty")
            .withItemModelOf(Items.GRAVE_ADVANCEMENT)
            .withNameStyle(DAILY_WORLD_EFFECT_STYLE));

    // 2x on win, 0x on death
    public static final WorldEffect ALL_IN = register(WorldEffect.builder("all_in")
            .withItemModelOf(Items.DIAMOND).xpModifier(2f)
            .withNameStyle(DAILY_WORLD_EFFECT_STYLE).incompatibleWith(NO_DEATH_PENALTY));

    // Time passes quicker when you don't pick up XP orbs,
    // Player's XP counts towards final score
    public static final WorldEffect XP_ADDICTION = register(WorldEffect.builder("xp_addiction")
            .withItemModelOf(Items.EXPERIENCE_BOTTLE).incompatibleWith(NO_DEATH_PENALTY)
            .withNameStyle(DAILY_WORLD_EFFECT_STYLE));

    // You can die. Once.
    public static final WorldEffect STRONG_SPIRIT = register(WorldEffect.builder("strong_spirit")
            .withItemModelOf(Items.TOTEM_OF_UNDYING)
            .withNameStyle(DAILY_WORLD_EFFECT_STYLE));

    public static final WorldEffect USED_STRONG_SPIRIT = register(WorldEffect.builder("used_strong_spirit")
            .withItemModelOf(Items.GRAVE_ADVANCEMENT)
            .withNameStyle(DAILY_WORLD_EFFECT_STYLE).neverUnlocked().notRandomizable());

    public static final WorldEffect RANDOM_TICK_SPEED = register(WorldEffect.builder("random_tick_speed")
            .withItemModelOf(Items.CLOCK)
            .onMineEnter(e -> e.getGameRules()
                    .getRule(GameRules.RULE_RANDOMTICKING).set(30, e.theGame()))
            .onMineLeave(e -> e.getGameRules()
                    .getRule(GameRules.RULE_RANDOMTICKING).set(3, e.theGame()))
            .withNameStyle(DAILY_WORLD_EFFECT_STYLE).neverUnlocked().notRandomizable());

    private static WorldEffect register(WorldEffect.Builder builder, String itemModel) {
        var builderAccessor = ((WorldEffectBuilderInvoker) builder);
        if (itemModel != null)
            builderAccessor.setItemModel(ResourceLocation.fromNamespaceAndPath(CraftmineDailies.MOD_ID, itemModel));
        var effect = builderAccessor.callBuild();

        if (!CraftmineDailies.EXPERIMENTAL)
            return effect;

        Registry.register(BuiltInRegistries.WORLD_EFFECT,
                ResourceLocation.fromNamespaceAndPath(CraftmineDailies.MOD_ID, effect.key()),
                effect);

        return effect;
    }

    private static WorldEffect register(WorldEffect.Builder builder) {
        return register(builder, null);
    }

    public static WorldEffect bootstrap() {
        return NO_DEATH_PENALTY;
    }

    public static boolean shouldApplyDeathPenalty(ServerLevel mineLevel) {
        if (!CraftmineDailies.EXPERIMENTAL)
            return true;

        return !mineLevel.getActiveEffects().contains(NO_DEATH_PENALTY);
    }
}
