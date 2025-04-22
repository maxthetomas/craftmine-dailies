package ru.maxthetomas.craftminedailies.mixin.common;

import net.minecraft.world.level.mines.WorldEffect;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.maxthetomas.craftminedailies.util.WorldCreationUtil;

import java.util.List;

@Mixin(PrimaryLevelData.class)
public class PrimaryLevelDataMixin {

    @Redirect(method = "<init>(Lnet/minecraft/nbt/CompoundTag;ZLnet/minecraft/core/BlockPos;FJJIIIZIZZZLnet/minecraft/world/level/border/WorldBorder$Settings;IILjava/util/UUID;Ljava/util/Set;Ljava/util/Set;Lnet/minecraft/world/level/timers/TimerQueue;Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/world/level/dimension/end/EndDragonFight$Data;Lnet/minecraft/world/level/LevelSettings;Lnet/minecraft/world/level/levelgen/WorldOptions;Lnet/minecraft/world/level/storage/PrimaryLevelData$SpecialWorldProperty;Ljava/util/Optional;Lcom/mojang/serialization/Lifecycle;Ljava/util/List;Ljava/util/List;Ljava/util/List;Lit/unimi/dsi/fastutil/objects/Object2IntOpenHashMap;IIILjava/util/Map;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 0))
    public boolean modifyUnlockMode(List<WorldEffect> instance, Object e) {
        if (WorldCreationUtil.IS_CREATING_DAILY)
            return true;

        return instance.add((WorldEffect) e);
    }
}
