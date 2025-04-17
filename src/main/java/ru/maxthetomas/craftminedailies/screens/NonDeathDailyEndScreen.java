package ru.maxthetomas.craftminedailies.screens;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import ru.maxthetomas.craftminedailies.CraftmineDailies;
import ru.maxthetomas.craftminedailies.auth.ApiManager;
import ru.maxthetomas.craftminedailies.util.EndContext;
import ru.maxthetomas.craftminedailies.util.TimeFormatters;

public class NonDeathDailyEndScreen extends Screen {
    private final EndContext context;
    private final Component scoreText;
    private final Component timeText;

    public NonDeathDailyEndScreen(EndContext context) {
        super(Component.translatable("craftminedailies.screen.end"));
        this.context = context;

        scoreText = Component.translatable("craftminedailies.screen.end.score",
                Component.literal(String.valueOf(this.context.getExperience()))
                        .withStyle(ChatFormatting.YELLOW));
        timeText = Component.translatable("craftminedailies.screen.end.time",
                Component.literal(TimeFormatters.formatTimeWithoutHours(this.context.getRemainingTime() / 20))
                        .withStyle(ChatFormatting.YELLOW));
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


        guiGraphics.drawCenteredString(this.font, scoreText, this.width / 2, 75, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, timeText, this.width / 2, 90, 0xFFFFFF);

        guiGraphics.drawCenteredString(this.font, Component.translatableWithFallback("craftminedailies.screen.end.place", "Your place: %s",
                        Component.literal(String.valueOf(
                                ApiManager.CachedCurrentLeaderboardPlace != -1 ? ApiManager.CachedCurrentLeaderboardPlace + 1 : "???"
                        )).withStyle(ChatFormatting.YELLOW)),
                this.width / 2, 105, 0xFFFFFF);
    }
}
