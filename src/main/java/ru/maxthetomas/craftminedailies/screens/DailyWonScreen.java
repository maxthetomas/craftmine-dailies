package ru.maxthetomas.craftminedailies.screens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ru.maxthetomas.craftminedailies.util.ends.WinEndContext;

public class DailyWonScreen extends Screen {
    private final WinEndContext context;

    public DailyWonScreen(WinEndContext context) {
        super(Component.translatable("screens.won"));
        this.context = context;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);


    }
}
