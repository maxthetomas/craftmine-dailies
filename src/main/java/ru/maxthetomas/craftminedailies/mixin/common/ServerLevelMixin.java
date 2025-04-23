package ru.maxthetomas.craftminedailies.mixin.common;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.TheGame;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.mines.WorldEffect;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.maxthetomas.craftminedailies.CraftmineDailies;
import ru.maxthetomas.craftminedailies.auth.meta.InventoryMeta;
import ru.maxthetomas.craftminedailies.screens.NonDeathDailyEndScreen;
import ru.maxthetomas.craftminedailies.util.ends.WinEndContext;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Shadow
    @Final
    private TheGame theGame;

    @Shadow
    public abstract boolean mayInteract(Entity entity, BlockPos blockPos);

    @Shadow
    public abstract void levelEvent(@Nullable Entity entity, int i, BlockPos blockPos, int j);

    @Inject(method = "dropUnlockEffect", at = @At("HEAD"), cancellable = true)
    void dropEffect(Vec3 vec3, WorldEffect worldEffect, ServerPlayer serverPlayer, CallbackInfo ci) {
        // no inventory rubbish
        if (CraftmineDailies.isInDaily())
            ci.cancel();
    }

    @Inject(method = "cleanInventoryAndReward", at = @At("HEAD"), cancellable = true)
    void cleanInventory(ServerPlayer serverPlayer, float f, CallbackInfo ci) {
        if (!CraftmineDailies.isInDaily()) return;

        ci.cancel();

        var inventoryMeta = InventoryMeta.createForPlayer(serverPlayer);
        var ctx = new WinEndContext(
                CraftmineDailies.getPlayerInventoryValue(serverPlayer, (ServerLevel) (Object) this, false, 1.0),
                CraftmineDailies.getRemainingTime(this.theGame.server()));
        CraftmineDailies.dailyEnded(ctx, inventoryMeta);

        var minecraft = Minecraft.getInstance();
        var server = serverPlayer.serverLevel().theGame().server();

        minecraft.schedule(() -> {
            if (minecraft.level != null) {
                minecraft.level.disconnect();
            }

            minecraft.disconnect(new GenericMessageScreen(Component.translatable("menu.savingLevel")));
            minecraft.setScreen(new NonDeathDailyEndScreen(ctx));
        });
    }
}
