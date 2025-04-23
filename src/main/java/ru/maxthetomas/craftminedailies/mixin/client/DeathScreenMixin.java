package ru.maxthetomas.craftminedailies.mixin.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.maxthetomas.craftminedailies.CraftmineDailies;
import ru.maxthetomas.craftminedailies.util.TimeFormatters;

import java.util.List;

@Mixin(DeathScreen.class)
public abstract class DeathScreenMixin extends Screen {
    @Shadow
    @Nullable
    private Button respawnButton;

    @Mutable
    @Final
    @Shadow
    private boolean hardcore;

    @Shadow
    private Component deathScore;

    @Shadow
    protected abstract void exitToTitleScreen();

    @Shadow
    @Final
    private List<Button> exitButtons;

    protected DeathScreenMixin(Component component) {
        super(component);
    }

    @Unique
    private Component timeText;

    @Inject(at = @At("RETURN"), method = "init")
    public void init(CallbackInfo ci) {
        if (!CraftmineDailies.isInDaily())
            return;

        removeWidget(this.respawnButton);

        var btn = Button.builder(
                Component.translatable("craftminedailies.button.leaderboards"),
                (b) -> {
                    exitToTitleScreen();
                    CraftmineDailies.openLeaderboard();
                }
        ).bounds(this.width / 2 - 100, this.height / 4 + 72, 200, 20).build();

        this.exitButtons.add(btn);
        addRenderableWidget(btn);

        this.hardcore = true;

        this.deathScore = Component.translatable("craftminedailies.death.score",
                Component.literal(String.valueOf(CraftmineDailies.LAST_DEATH_CONTEXT.getExperience()))
                        .withStyle(ChatFormatting.YELLOW));
        this.timeText = Component.translatable("craftminedailies.death.time",
                Component.literal(TimeFormatters.formatTimeWithoutHours(CraftmineDailies.LAST_DEATH_CONTEXT.getPassedTime() / 20))
                        .withStyle(ChatFormatting.YELLOW));
    }

    @Inject(at = @At("RETURN"), method = "render")
    public void render(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        if (!CraftmineDailies.isInDaily())
            return;

        guiGraphics.drawCenteredString(this.font, this.timeText, this.width / 2, 112, 0xffffff);
    }
}
