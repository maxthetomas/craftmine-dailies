package ru.maxthetomas.craftminedailies.mixin.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.maxthetomas.craftminedailies.CraftmineDailies;

@Mixin(DeathScreen.class)
public abstract class DeathScreenMixin extends Screen {
    @Shadow
    @Nullable
    private Button respawnButton;

    @Mutable
    @Final
    @Shadow
    private boolean hardcore;

    protected DeathScreenMixin(Component component) {
        super(component);
    }

    @Inject(at = @At("RETURN"), method = "init", cancellable = true)
    public void init(CallbackInfo ci) {
        if (!CraftmineDailies.isInDaily())
            return;

        removeWidget(this.respawnButton);
        this.hardcore = true;
    }
}
