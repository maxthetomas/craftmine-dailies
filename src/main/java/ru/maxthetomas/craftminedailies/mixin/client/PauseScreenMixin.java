package ru.maxthetomas.craftminedailies.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.maxthetomas.craftminedailies.CraftmineDailies;
import ru.maxthetomas.craftminedailies.screens.ConfirmExitScreen;

@Mixin(PauseScreen.class)
public class PauseScreenMixin {
    @Redirect(
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;"
                    , ordinal = 5), method = "createPauseMenu")
    <T extends LayoutElement> T create(GridLayout.RowHelper instance, T layoutElement) {
        if (!CraftmineDailies.isInDaily())
            return instance.addChild(layoutElement);

        var btn = Button.builder(Component.translatable("craftminedailies.publish.button"),
                button -> {
                }).width(98).build();
        btn.active = false;

        instance.addChild(btn);
        return null;
    }

    @Inject(at = @At("HEAD"), method = "onDisconnect", cancellable = true)
    public void disconnect(CallbackInfo ci) {
        if (!CraftmineDailies.isInDaily()) return;
        if (!Minecraft.getInstance().player.level().isMine()) return;

        ci.cancel();
        Minecraft.getInstance().setScreen(new ConfirmExitScreen((PauseScreen) (Object) this));
    }
}
