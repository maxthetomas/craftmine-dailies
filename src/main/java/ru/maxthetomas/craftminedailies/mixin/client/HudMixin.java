package ru.maxthetomas.craftminedailies.mixin.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.maxthetomas.craftminedailies.CraftmineDailies;

import static ru.maxthetomas.craftminedailies.util.TimeFormatters.formatTimeWithoutHours;

@Mixin(Gui.class)
public class HudMixin {
    @Inject(method = "render", at = @At("RETURN"))
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!CraftmineDailies.shouldRenderInGameTimer())
            return;

        var minecraft = Minecraft.getInstance();
        if (CraftmineDailies.REMAINING_TIME_CACHE != -1)
            guiGraphics.drawString(minecraft.font, formatTimeWithoutHours(CraftmineDailies.REMAINING_TIME_CACHE / 20),
                    guiGraphics.guiWidth() - 45, guiGraphics.guiHeight() - 20, 0xFFFFFFFF);
    }
}
