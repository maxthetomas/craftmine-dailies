package ru.maxthetomas.craftminedailies.mixin.common;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.server.PlayerAdvancements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

@Mixin(PlayerAdvancements.class)
public class PlayerAdvancementsMixin {
    @Redirect(method = "award", at = @At(value = "INVOKE", target = "Lnet/minecraft/advancements/Advancement;display()Ljava/util/Optional;"))
    public Optional<DisplayInfo> redir(Advancement instance) {
        return Optional.empty();
    }
}
