package ru.maxthetomas.craftminedailies.mixin.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
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

        var timeText = formatTimeWithoutHours(CraftmineDailies.REMAINING_TIME_CACHE / 20);
        var timeTextWidth = minecraft.font.width(timeText);
        guiGraphics.drawString(minecraft.font, timeText,
                guiGraphics.guiWidth() - 15 - timeTextWidth, guiGraphics.guiHeight() - 20, 0xFFFFFFFF);

        var xpText = Component.translatable("craftminedailies.hud.xp", CraftmineDailies.CACHED_CURRENT_INV_EXP);
        var xpTextWidth = minecraft.font.width(xpText);
        guiGraphics.drawString(minecraft.font, xpText, guiGraphics.guiWidth() - 15 - xpTextWidth, guiGraphics.guiHeight() - 30, 0xFFFFFFFF);
    }
}
