package ru.maxthetomas.craftminedailies.mixin.common;

import net.minecraft.world.level.storage.PrimaryLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PrimaryLevelData.class)
public interface PrimaryLevelDataAccessor {
    @Accessor
    public void setMineCrafterLevel(int level);
}
