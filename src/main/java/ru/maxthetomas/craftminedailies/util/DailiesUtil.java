package ru.maxthetomas.craftminedailies.util;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.mines.WorldEffect;
import ru.maxthetomas.craftminedailies.CraftmineDailies;
import ru.maxthetomas.craftminedailies.auth.ApiManager;
import ru.maxthetomas.craftminedailies.content.DailyWorldEffects;
import ru.maxthetomas.craftminedailies.util.scalers.ExperienceAddictionScalingFactor;

import java.util.ArrayList;
import java.util.List;

public class DailiesUtil {
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
        for (WorldEffect effect : level.getActiveEffects()) {
            if (shouldIgnore(ignoreSelfPlacedWorldEffects, effect))
                continue;

            multiplier *= effect.experienceModifier();
        }

        if (CraftmineDailies.EXPERIMENTAL &&
                !shouldIgnore(ignoreSelfPlacedWorldEffects, DailyWorldEffects.XP_ADDICTION) &&
                level.isActive(DailyWorldEffects.XP_ADDICTION)) {
            totalXp += ((ExperienceAddictionScalingFactor)
                    DailyTimeCalculator.getScalingHandler()).getActualXpLevel() / 20f;
        }

        multiplier *= player.getAttributeValue(Attributes.EXPERIENCE_GAIN_MODIFIER);
        return (int) (totalXp * multiplier * extraScale * CraftmineDailies.XP_MULT);
    }

    public static boolean shouldIgnore(boolean shouldIgnoreSelfPlaced, WorldEffect effect) {
        return isSelfPlaced(effect) && shouldIgnoreSelfPlaced;
    }

    public static boolean isSelfPlaced(WorldEffect effect) {
        return !ApiManager.TodayDetails.getEffects().contains(effect);
    }

    public static Component getInventoryValueText() {
        return Component.translatable("craftminedailies.hud.xp", CraftmineDailies.CACHED_CURRENT_INV_EXP);
    }

    public static List<Component> createRunDetails(EndContext context) {
        var list = new ArrayList<Component>();

        list.add(Component.translatable("craftminedailies.screen.end.score",
                Component.literal(String.valueOf(context.getExperience()))
                        .withStyle(ChatFormatting.YELLOW)));

        list.add(Component.translatable("craftminedailies.screen.end.time",
                Component.literal(TimeFormatters.formatTimeWithoutHours(CraftmineDailies.REMAINING_TIME_CACHE / 20))
                        .withStyle(ChatFormatting.YELLOW)));

        if (DailyTimeCalculator.getScalingHandler() != null &&
                DailyTimeCalculator.getScalingHandler() instanceof ExperienceAddictionScalingFactor) {
            list.add(Component.translatable("craftminedailies.screen.end.passed_time",
                    Component.literal(TimeFormatters.formatTimeWithoutHours(context.getPassedTime() / 20))
                            .withStyle(ChatFormatting.YELLOW)));
        }

        list.add(Component.translatableWithFallback("craftminedailies.screen.end.place", "Your place: %s",
                Component.literal(String.valueOf(
                        ApiManager.CachedCurrentLeaderboardPlace != -1 ? ApiManager.CachedCurrentLeaderboardPlace + 1 : "???"
                )).withStyle(ChatFormatting.YELLOW)));

        return list;
    }
}
