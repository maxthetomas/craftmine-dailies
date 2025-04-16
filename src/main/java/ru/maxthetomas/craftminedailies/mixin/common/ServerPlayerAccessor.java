package ru.maxthetomas.craftminedailies.mixin.common;

import net.minecraft.server.ServerPlayerUnlocks;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerPlayer.class)
public interface ServerPlayerAccessor {
    @Accessor
    public ServerPlayerUnlocks getPlayerUnlocks();
}
