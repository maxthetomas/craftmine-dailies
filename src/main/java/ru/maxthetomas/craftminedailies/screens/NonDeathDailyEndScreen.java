package ru.maxthetomas.craftminedailies.screens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import ru.maxthetomas.craftminedailies.CraftmineDailies;
import ru.maxthetomas.craftminedailies.util.EndContext;

public class NonDeathDailyEndScreen extends Screen {
    private final EndContext context;

    public NonDeathDailyEndScreen(EndContext context) {
        super(Component.translatable("craftminedailies.screen.end"));
        this.context = context;
    }


    @Override
    protected void init() {
        addRenderableWidget(Button.builder(
                Component.translatable("craftminedailies.button.leaderboards"),
                (bu) -> {
                    CraftmineDailies.openLeaderboard();
                }
        ).bounds(this.width / 2 - 150 / 2, this.height / 4 + 76,
                Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT).build());

        addRenderableWidget(Button.builder(
                Component.translatableWithFallback("craftminedailies.button.returnToMenu", "Back to Title"),
                (bu) -> {
                    this.minecraft.setScreen(new TitleScreen());
                }
        ).bounds(this.width / 2 - 150 / 2, this.height / 4 + 100,
                Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT).build());

        addRenderableWidget(Button.builder(
                Component.translatable("menu.quit"),
                (bu) -> {
                    minecraft.close();
                }
        ).bounds(this.width / 2 - 150 / 2, this.height - 50,
                Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT).build());

        super.init();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(2f, 2f, 2f);

        guiGraphics.drawCenteredString(this.font,
                Component.translatableWithFallback("craftminedailies.screen.end.title." + context.getStringName(), "Daily completed!"),
                this.width / 4, 20, 0xFFFFFF);

        guiGraphics.pose().popPose();

        for (int t = 0; t < CraftmineDailies.END_TEXT.size(); t++) {
            guiGraphics.drawCenteredString(minecraft.font, CraftmineDailies.END_TEXT.get(t),
                    this.width / 2, 75 + 12 * t, 0xFFFFFF);
        }
    }
}
