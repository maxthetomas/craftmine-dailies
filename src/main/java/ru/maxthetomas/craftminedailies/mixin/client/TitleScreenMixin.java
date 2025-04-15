package ru.maxthetomas.craftminedailies.mixin.client;

import com.mojang.realmsclient.gui.screens.RealmsNotificationsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.joml.Math;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.maxthetomas.craftminedailies.CraftmineDailies;

import static ru.maxthetomas.craftminedailies.util.TimeFormatters.formatTime;
import static ru.maxthetomas.craftminedailies.util.TimeFormatters.secondsUntilNextDaily;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {
    @Shadow
    private float panoramaFade;

    private static Button startButton;

    @Redirect(method = "createNormalMenuOptions",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/TitleScreen;addRenderableWidget(Lnet/minecraft/client/gui/components/events/GuiEventListener;)Lnet/minecraft/client/gui/components/events/GuiEventListener;",
                    ordinal = 2))
    private GuiEventListener preventThirdWidgetAddition(TitleScreen instance, GuiEventListener guiEventListener) {
        startButton = ((ScreenInvoker) instance).callAddRenderableWidget(
                Button.builder(Component.translatable("craftminedailies.screens.title.start"),
                        (but) -> {
                            CraftmineDailies.startDaily();
                        }).bounds(instance.width / 2 - 100, instance.height / 4 + 48 + 24 * 2, 200, 20).build()
        );

        ((ScreenInvoker) instance).callAddRenderableWidget(
                Button.builder(Component.translatable("craftminedailies.screens.title.leaderboard"),
                        (but) -> {
                            CraftmineDailies.openLeaderboard();
                        }).bounds(instance.width / 2 - 124, instance.height / 4 + 48 + 24 * 2, 20, 20).build()
        );

        return guiEventListener;
    }

    @Inject(at = @At("RETURN"), method = "render")
    public void render(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        var screen = (Screen) (Object) this;
        var minecraft = Minecraft.getInstance();
        var x = screen.width / 2 + 100 + 5;
        var y = screen.height / 4 + 48 + 24 * 2 + 8;

        startButton.active = CraftmineDailies.shouldAllowDaily();

        // Background
        guiGraphics.fill(x - 3, y - 5, x + 44, y + 9, 0xAA000000);
        // Timer text
        guiGraphics.drawString(minecraft.font, formatTime(secondsUntilNextDaily()),
                x, y - 1, 0x00FFFFFF | ((int) Math.lerp(0x0, 0xFF, this.panoramaFade) << (8 * 3)));
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/realmsclient/gui/screens/RealmsNotificationsScreen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    public void realmsRedirect(RealmsNotificationsScreen instance, GuiGraphics guiGraphics, int i, int j, float f) {
        // Ignore realms
    }
}
