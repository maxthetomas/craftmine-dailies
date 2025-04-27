package ru.maxthetomas.craftminedailies.mixin.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.maxthetomas.craftminedailies.CraftmineDailies;
import ru.maxthetomas.craftminedailies.util.DailiesUtil;
import ru.maxthetomas.craftminedailies.util.DailyTimeCalculator;

@Mixin(Gui.class)
public abstract class HudMixin {
    @Shadow
    protected abstract void renderHotbarAndDecorations(GuiGraphics guiGraphics, DeltaTracker deltaTracker);

    @Inject(method = "render", at = @At("RETURN"))
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!CraftmineDailies.shouldRenderInGameTimer())
            return;

        var minecraft = Minecraft.getInstance();

        var timeText = DailyTimeCalculator.getTimeText();
        var timeTextWidth = minecraft.font.width(timeText);
        guiGraphics.drawString(minecraft.font, timeText,
                guiGraphics.guiWidth() - 15 - timeTextWidth, guiGraphics.guiHeight() - 20, 0xFFFFFFFF);

        var xpText = DailiesUtil.getInventoryValueText();
        var xpTextWidth = minecraft.font.width(xpText);
        guiGraphics.drawString(minecraft.font, xpText, guiGraphics.guiWidth() - 15 - xpTextWidth, guiGraphics.guiHeight() - 30, 0xFFFFFFFF);

        var timeScaleText = DailyTimeCalculator.getTimeScaleText();
        var timeScaleTextWidth = minecraft.font.width(timeScaleText);

        var random = RandomSource.create();
        var ts = DailyTimeCalculator.getCurrentShakiness();
        guiGraphics.pose().pushPose();
        if (ts > 0) {
            ts /= 2f;
            guiGraphics.pose().translate(random.nextFloat() * ts, random.nextFloat() * ts, 0);
        }

        guiGraphics.drawString(minecraft.font,
                timeScaleText,
                guiGraphics.guiWidth() - 15 - timeTextWidth - 5 - timeScaleTextWidth,
                guiGraphics.guiHeight() - 20,
                0xFFFFFFFF);
        guiGraphics.pose().popPose();
    }
}
