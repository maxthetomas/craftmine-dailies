package ru.maxthetomas.craftminedailies.screens;

import com.mojang.realmsclient.RealmsMainScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

public class ConfirmExitScreen extends Screen {
    private final PauseScreen back;

    public ConfirmExitScreen(PauseScreen back) {
        super(Component.translatableWithFallback("craftminedailies.screen.confirm_exit", "Confirm Exit"));
        this.back = back;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(
                Component.translatable("craftminedailies.confirm_exit"),
                (b) -> {
                    forceDisconnect();
                }
        ).bounds(this.width / 2 - 102, this.height / 4 + 21 * 4 - 3, 204, 20).build());
        addRenderableWidget(Button.builder(
                Component.translatable("craftminedailies.back"),
                (b) -> {
                    onClose();
                }
        ).bounds(this.width / 2 - 102, this.height / 4 + 21 * 5, 204, 20).build());
    }

    private static final Component SAVING_LEVEL = Component.translatable("menu.savingLevel");

    // Literally copied from pause screen :)
    private void forceDisconnect() {
        boolean bl = this.minecraft.isLocalServer();
        ServerData serverData = this.minecraft.getCurrentServer();
        this.minecraft.level.disconnect();
        if (bl) {
            this.minecraft.disconnect(new GenericMessageScreen(SAVING_LEVEL));
        } else {
            this.minecraft.disconnect();
        }

        TitleScreen titleScreen = new TitleScreen();
        if (bl) {
            this.minecraft.setScreen(titleScreen);
        } else if (serverData != null && serverData.isRealm()) {
            this.minecraft.setScreen(new RealmsMainScreen(titleScreen));
        } else {
            this.minecraft.setScreen(new JoinMultiplayerScreen(titleScreen));
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);

        var h = this.height / 4;
        guiGraphics.drawCenteredString(this.font, Component.translatable("craftminedailies.screen.exit.title").withStyle(ChatFormatting.BOLD), this.width / 2, h, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, Component.translatable("craftminedailies.screen.exit.description"), this.width / 2, h + 15, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(back);
    }
}
