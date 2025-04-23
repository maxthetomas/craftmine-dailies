package ru.maxthetomas.craftminedailies.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.mines.WorldEffect;
import ru.maxthetomas.craftminedailies.CraftmineDailies;
import ru.maxthetomas.craftminedailies.auth.ApiManager;

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
        return (int) (totalXp * multiplier * extraScale * CraftmineDailies.XP_MULT);
    }
}
