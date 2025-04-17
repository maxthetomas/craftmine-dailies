package ru.maxthetomas.craftminedailies.mixin.common;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.maxthetomas.craftminedailies.CraftmineDailies;

@Mixin(PlayerAdvancements.class)
public class PlayerAdvancementsMixin {
    @Inject(method = "award", cancellable = true, at = @At("HEAD"))
    public void cancelAwardIfDaily(AdvancementHolder advancementHolder, String string, CallbackInfoReturnable<Boolean> cir) {
        if (CraftmineDailies.isInDaily())
            cir.cancel();
    }
}
