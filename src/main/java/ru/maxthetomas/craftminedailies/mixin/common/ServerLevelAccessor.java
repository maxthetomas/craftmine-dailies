package ru.maxthetomas.craftminedailies.mixin.common;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MineData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerLevel.class)
public interface ServerLevelAccessor {
    @Accessor
    MineData getMineData();
}
