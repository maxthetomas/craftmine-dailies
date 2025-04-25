package ru.maxthetomas.craftminedailies.mixin.common;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MineData;
import net.minecraft.world.level.mines.WorldEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(ServerLevel.class)
public interface ServerLevelAccessor {
    @Accessor
    MineData getMineData();

    @Accessor
    Set<WorldEffect> getActiveEffects();
}
