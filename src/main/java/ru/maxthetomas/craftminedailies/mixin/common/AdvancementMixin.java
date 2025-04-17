package ru.maxthetomas.craftminedailies.mixin.common;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.maxthetomas.craftminedailies.CraftmineDailies;

import java.util.Optional;

@Mixin(Advancement.class)
public class AdvancementMixin {
    @Inject(at = @At("RETURN"), method = "display", cancellable = true)
    public void displayNothing(CallbackInfoReturnable<Optional<DisplayInfo>> cir) {
        if (CraftmineDailies.isInDaily())
            cir.setReturnValue(Optional.empty());
    }
}
