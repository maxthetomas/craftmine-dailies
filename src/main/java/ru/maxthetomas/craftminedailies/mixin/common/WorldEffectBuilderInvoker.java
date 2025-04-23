package ru.maxthetomas.craftminedailies.mixin.common;

import net.minecraft.world.level.mines.WorldEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(WorldEffect.Builder.class)
public interface WorldEffectBuilderInvoker {
    @Invoker
    WorldEffect callBuild();
}
