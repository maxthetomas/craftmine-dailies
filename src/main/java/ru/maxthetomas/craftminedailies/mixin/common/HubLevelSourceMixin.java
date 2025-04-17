package ru.maxthetomas.craftminedailies.mixin.common;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.HubLevelSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ru.maxthetomas.craftminedailies.CraftmineDailies;
import ru.maxthetomas.craftminedailies.auth.ApiManager;

@Mixin(HubLevelSource.class)
public class HubLevelSourceMixin {
    @ModifyVariable(method = "plaseChunk", argsOnly = true, at = @At("HEAD"), ordinal = 0)
    ResourceLocation placeChunk(ResourceLocation value, WorldGenLevel instance) {
        if (ApiManager.TodayDetails == null ||
                instance.getSeed() != ApiManager.TodayDetails.seed())
            return value;

        if (!value.equals(HubLevelSource.CENTER_BASE_STRUCTURE))
            return value;

        return ResourceLocation.fromNamespaceAndPath(CraftmineDailies.MOD_ID, "hub/daily_center_base");
    }
}
