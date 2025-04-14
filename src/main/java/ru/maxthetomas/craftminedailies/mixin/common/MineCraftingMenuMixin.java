package ru.maxthetomas.craftminedailies.mixin.common;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.inventory.MineCraftingMenu;
import net.minecraft.world.level.mines.WorldEffect;
import net.minecraft.world.level.mines.WorldEffectSet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.maxthetomas.craftminedailies.CraftmineDailies;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Mixin(MineCraftingMenu.class)
public class MineCraftingMenuMixin {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Shadow
    @Final
    private Optional<ServerLevel> serverLevel;

    @ModifyVariable(method = "getMustHaveEffects", at = @At("STORE"))
    private RandomSource mustHaveEffects(RandomSource source) {
        if (!CraftmineDailies.isDailyWorld(this.serverLevel.get()))
            return source;

        return CraftmineDailies.RANDOM_EFFECT_SOURCE;
    }

    @Inject(method = "getRandomEffect", at = @At("HEAD"), cancellable = true)
    private static void randomEffect(ServerLevel serverLevel, List<WorldEffect> list, Set<WorldEffect> set, CallbackInfoReturnable<Optional<WorldEffect>> cir) {
        if (!CraftmineDailies.isDailyWorld(serverLevel))
            return;

        WeightedList.Builder<WorldEffect> builder = WeightedList.builder();

        for (WorldEffect worldEffect : BuiltInRegistries.WORLD_EFFECT) {
            if (!worldEffect.inSets().stream().anyMatch(WorldEffectSet::exclusive) && worldEffect.randomWeight() > 0 && worldEffect.isValidWith(list) && !set.contains(worldEffect) && worldEffect.canRandomize(serverLevel)) {
                boolean bl = serverLevel.isEffectUnlocked(worldEffect);
                if (bl) {
                    builder.add(worldEffect, worldEffect.randomWeight());
                } else {
                    builder.add(worldEffect, (int) ((float) worldEffect.randomWeight() * 0.1F));
                }
            }
        }

        cir.setReturnValue(builder.build().getRandom(CraftmineDailies.RANDOM_EFFECT_SOURCE));
    }
}
