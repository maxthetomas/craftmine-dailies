package ru.maxthetomas.craftminedailies.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.maxthetomas.craftminedailies.util.GameOverlay;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;flush()V", ordinal = 1))
    public void injectRenderDetails(GuiGraphics instance) {
        GameOverlay.renderOverlay(instance);
        instance.flush();
    }
}
