package ru.maxthetomas.craftminedailies.mixin.client;

import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.TheGame;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.maxthetomas.craftminedailies.CraftmineDailies;

@Mixin(IntegratedServer.class)
public class IntegratedServerMixin {
    @Inject(at = @At("HEAD"), method = "publishServer", cancellable = true)
    public void publishGame(TheGame theGame, GameType gameType, boolean bl, int i, CallbackInfoReturnable<Boolean> cir) {
        if (CraftmineDailies.isDailyWorld(theGame.overworld())) {
            theGame.playerList().broadcastSystemMessage(Component.translatable("craftminedailies.publish"), true);
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
