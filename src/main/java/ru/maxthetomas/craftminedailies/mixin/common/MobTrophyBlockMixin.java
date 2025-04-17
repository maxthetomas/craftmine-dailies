package ru.maxthetomas.craftminedailies.mixin.common;

import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.MobTrophyBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.maxthetomas.craftminedailies.CraftmineDailies;

@Mixin(MobTrophyBlock.class)
public class MobTrophyBlockMixin {
    @Inject(at = @At("RETURN"), method = "generateForMob", cancellable = true)
    private static void generateForMob(Holder<EntityType<?>> holder, RandomSource randomSource, CallbackInfoReturnable<ItemStack> cir) {
        if (!CraftmineDailies.isInDaily()) return;
        cir.setReturnValue(ItemStack.EMPTY);
    }
}
