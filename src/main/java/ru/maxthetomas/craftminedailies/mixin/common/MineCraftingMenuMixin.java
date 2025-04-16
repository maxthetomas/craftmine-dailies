package ru.maxthetomas.craftminedailies.mixin.common;

import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MineCraftingMenu;
import net.minecraft.world.inventory.MineCraftingSlot;
import net.minecraft.world.level.mines.WorldEffects;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.maxthetomas.craftminedailies.CraftmineDailies;
import ru.maxthetomas.craftminedailies.auth.ApiManager;

import java.util.List;

@Mixin(MineCraftingMenu.class)
public abstract class MineCraftingMenuMixin {
    @Shadow
    @Final
    private List<MineCraftingSlot> craftingSlots;

    @Shadow
    private int nrOfRandomSlots;

    @Inject(method = "populateRandomEffects", at = @At("HEAD"), cancellable = true)
    private void populate(CallbackInfo ci) {
        if (!CraftmineDailies.isInDaily()) return;
        ci.cancel();

        boolean addedBase = false;
        for (MineCraftingSlot slot : this.craftingSlots) {
            if (!slot.randomSlot || slot.hasItem()) continue;

            if (!addedBase) {
                addedBase = true;
                slot.set(WorldEffects.createEffectItem(Component.translatableEscape("craftminedailies.item.base"),
                        false, List.copyOf(ApiManager.TodayDetails.getEffects())));
                continue;
            }

            slot.setActive(true);
        }
    }

    @Inject(at = @At("RETURN"), method = "getLevelName", cancellable = true)
    public void modifyReturnName(CallbackInfoReturnable<Component> cir) {
        if (!CraftmineDailies.isInDaily()) return;
        cir.setReturnValue(Component.translatable("craftminedailies.item.mine"));
    }

    @Inject(at = @At("RETURN"), method = "setAdditionalData")
    public void additionalData(List<Integer> list, CallbackInfo ci) {
        if (!CraftmineDailies.isInDaily()) return;
        this.nrOfRandomSlots = 1;
    }
}
