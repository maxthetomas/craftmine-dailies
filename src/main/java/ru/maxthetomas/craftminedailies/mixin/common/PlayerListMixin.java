package ru.maxthetomas.craftminedailies.mixin.common;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.maxthetomas.craftminedailies.CraftmineDailies;

import java.util.List;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    @Shadow
    @Final
    private List<ServerPlayer> players;

    @Inject(at = @At("HEAD"), method = "setAllowCommandsForAllPlayers", cancellable = true)
    public void setAllowCommands(boolean bl, CallbackInfo ci) {
        if (!this.players.isEmpty() && CraftmineDailies.isInDaily())
            ci.cancel();
    }
}
