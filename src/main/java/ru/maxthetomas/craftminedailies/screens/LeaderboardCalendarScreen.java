package ru.maxthetomas.craftminedailies.screens;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ru.maxthetomas.craftminedailies.util.GameOverlay;
import ru.maxthetomas.craftminedailies.util.Month;

public class LeaderboardCalendarScreen extends Screen {
    private Month month;
    private Screen parent;

    protected LeaderboardCalendarScreen(Month month, Screen parent) {
        super(Component.literal("Leaderboard calendar"));
        this.month = month;
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.clearWidgets();

        super.init();

        int day = 0;
        for (int r = 0; r < 5; r++) {
            var effectiveHeight = 70 + r * 22;
            for (int d = 0; d < 7; d++) {
                var effectiveWidth = this.width / 2 - 11 * 7 + 22 * d;

                if (r == 0 && d < month.startDayOfWeek()) {
                    continue;
                }

                if (day > month.endDay()) {
                    continue;
                }

                int finalDay = day;
                var text = Component.literal(String.valueOf(day + 1));

                if (month.isDayToday(day))
                    text = text.withStyle(ChatFormatting.BOLD);

                var button = Button.builder(text, (b) -> {
//                            if (month.isDayToday(finalDay)) {
//                                onClose();
//                                return;
//                            }
                            var apiDay = month.getApiDay(finalDay);
                            this.minecraft.setScreen(new LeaderboardScreen(apiDay, this));
                        })
                        .pos(effectiveWidth, effectiveHeight).size(20, 20).build();

                button.active = !month.isDayInFuture(day) && !month.isDateBeforeModCreation(day);

                addRenderableWidget(button);

                day++;
            }
        }

        addRenderableWidget(Button.builder(Component.translatable("craftminedailies.back"), (b) -> {
            onClose();
        }).bounds(10, this.height - 30, 50, 20).build());

        var backMonth = addRenderableWidget(Button.builder(Component.translatable("craftminedailies.calendar.back"), (b) -> {
            this.month = month.previous();
            init();
        }).bounds(this.width / 2 - 50, this.height - 30, 40, 20).build());
        backMonth.active = !Month.isMonthBeforeCreation(this.month.previous());

        addRenderableWidget(Button.builder(Component.translatable("craftminedailies.calendar.next"), (b) -> {
            this.month = month.next();
            init();
        }).bounds(this.width / 2 + 10, this.height - 30, 40, 20).build());
    }


    @Override
    public void onClose() {
        super.onClose();
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);

        GameOverlay.customScreenRenderTimer(minecraft, guiGraphics, width, height);

        guiGraphics.drawCenteredString(this.font, Component.translatable("craftminedailies.calendar.title"), this.width / 2, 20, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, month.getComponentRepresentation(), this.width / 2, 32, 0xFFFFFF);
    }
}
