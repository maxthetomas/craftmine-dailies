package ru.maxthetomas.craftminedailies.mixin.client;

import com.mojang.realmsclient.gui.screens.RealmsNotificationsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.maxthetomas.craftminedailies.CraftmineDailies;

import static ru.maxthetomas.craftminedailies.util.TimeFormatters.formatTime;
import static ru.maxthetomas.craftminedailies.util.TimeFormatters.secondsUntilNextDaily;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    @Shadow
    protected abstract void fadeWidgets(float f);

    @Shadow
    private boolean fading;
    @Unique
    private static Button startDailyButton;

    protected TitleScreenMixin(Component component) {
        super(component);
    }

    @Redirect(method = "createNormalMenuOptions",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/TitleScreen;addRenderableWidget(Lnet/minecraft/client/gui/components/events/GuiEventListener;)Lnet/minecraft/client/gui/components/events/GuiEventListener;",
                    ordinal = 2))
    private GuiEventListener preventThirdWidgetAddition(TitleScreen instance, GuiEventListener guiEventListener) {
        startDailyButton = ((ScreenInvoker) instance).callAddRenderableWidget(createStartDailyButton(instance.width, instance.height));
        ((ScreenInvoker) instance).callAddRenderableWidget(createLeaderboardButton(instance.width, instance.height));

        startDailyButton.setMessage(CraftmineDailies.getStartDailyButtonText());
        startDailyButton.active = CraftmineDailies.shouldAllowDaily();

        return guiEventListener;
    }

    @Unique
    private float widgetAlpha = 0F;

    @Unique
    private static Button createStartDailyButton(int screenWidth, int screenHeight) {
        return Button.builder(Component.translatable("craftminedailies.button.start.loading"),
                (but) -> {
                    CraftmineDailies.startDaily();
                }).bounds(screenWidth / 2 - 100, screenHeight / 4 + 48 + 24 * 2, 200, 20).build();
    }

    @Unique
    private static Button createLeaderboardButton(int screenWidth, int screenHeight) {
        var button = SpriteIconButton.builder(
                        Component.translatable("craftminedailies.button.leaderboards"),
                        (but) -> {
                            CraftmineDailies.openLeaderboard();
                        }, true)
                .sprite(ResourceLocation.fromNamespaceAndPath(CraftmineDailies.MOD_ID, "icon/leaderboards"),
                        15, 15)
                .build();

        var x = screenWidth / 2 - 124;
        var y = screenHeight / 4 + 48 + 24 * 2;
        button.setRectangle(20, 20, x, y);

        return button;
    }

    @Inject(at = @At("RETURN"), method = "render")
    public void render(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        var screen = (Screen) (Object) this;
        var minecraft = Minecraft.getInstance();
        var x = screen.width / 2 + 100 + 5;
        var y = screen.height / 4 + 48 + 24 * 2 + 8;

        startDailyButton.setMessage(CraftmineDailies.getStartDailyButtonText());
        startDailyButton.active = CraftmineDailies.shouldAllowDaily();

        if (!this.fading)
            widgetAlpha = 1f;
        var whiteWithAlpha = 0x00FFFFFF | ((int) (widgetAlpha * 255f) << 8 * 3);

        if (widgetAlpha <= 0.01f)
            return;

        // Background
        guiGraphics.fill(x - 3, y - 5, x + 44, y + 9, 0xAA000000);
        // Timer text
        guiGraphics.drawString(minecraft.font, formatTime(secondsUntilNextDaily()),
                x, y - 1, whiteWithAlpha);

        // Left bottom
        guiGraphics.drawString(minecraft.font, Component.translatable("craftminedailies.version", CraftmineDailies.getStringVersion()),
                2, this.height - 20, whiteWithAlpha);

        renderUpdate(guiGraphics, whiteWithAlpha);
    }

    @Unique
    void renderUpdate(GuiGraphics gui, int color) {
        if (!CraftmineDailies.HAS_UPDATES) return;

        var updateTextHeight = this.height / 4 + 48 + 24 * 3;
        gui.drawCenteredString(this.font, Component.translatable("craftminedailies.updateAvailable"), this.width / 2, updateTextHeight, color);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/realmsclient/gui/screens/RealmsNotificationsScreen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    public void realmsRedirect(RealmsNotificationsScreen instance, GuiGraphics guiGraphics, int i, int j, float f) {
        // Ignores realms
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/TitleScreen;fadeWidgets(F)V"))
    public void redirect(TitleScreen instance, float f) {
        this.fadeWidgets(f);
        widgetAlpha = f;
    }


}
