package ru.maxthetomas.craftminedailies.mixin.common;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.mines.WorldEffect;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.maxthetomas.craftminedailies.CraftmineDailies;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {
    @Inject(method = "dropUnlockEffect", at = @At("HEAD"), cancellable = true)
    void dropEffect(Vec3 vec3, WorldEffect worldEffect, ServerPlayer serverPlayer, CallbackInfo ci) {
        // no inventory rubbish
        if (CraftmineDailies.isInDaily())
            ci.cancel();
    }

    @Inject(method = "cleanInventoryAndReward", at = @At("HEAD"), cancellable = true)
    void cleanInventory(ServerPlayer serverPlayer, float f, CallbackInfo ci) {

    }
}
