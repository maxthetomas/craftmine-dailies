package ru.maxthetomas.craftminedailies.util;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.time.Instant;
import java.util.ArrayList;

public class GameOverlay {
    private static final ArrayList<Notification> notifications = new ArrayList<>();
    private static final ArrayList<Notification> notificationQueue = new ArrayList<>();

    static Instant lastFrame;

    public static void renderOverlay(GuiGraphics graphics) {
        if (lastFrame == null)
            lastFrame = Instant.now();

        var delta = (Instant.now().toEpochMilli() - lastFrame.toEpochMilli()) / 1000.0F;

        notifications.removeIf(Notification::isDone);

        while (notifications.size() < 6 && !notificationQueue.isEmpty()) {
            notifications.add(notificationQueue.removeFirst());
        }

        int height = 0;
        for (Notification notification : notifications) {
            height += notification.render(graphics, delta, 10, 10 + height);
        }

        lastFrame = Instant.now();
    }


    public static void pushNotification(Component text) {
        notificationQueue.add(new Notification(text));
    }

    public static void pushNotification(String text) {
        pushNotification(Component.literal(text));
    }

    static class Notification {
        Component text;
        private static final int MAX_WIDTH = 300;
        private static final float START_TIME = 8.5F;
        private static final float IN_FADE = .2F;

        public Notification(Component component) {
            this.text = component;
            remainingTime = START_TIME;

            LogUtils.getLogger().info("Notification: {}", component.getString());
        }

        float remainingTime;

        int render(GuiGraphics graphics, float delta, int x, int y) {
            var font = Minecraft.getInstance().font;

            if (!Minecraft.getInstance().isGameLoadFinished())
                return 0;

            remainingTime -= delta;

            var padding = 5;
            var lines = font.split(text, MAX_WIDTH);
            var width = lines.stream().map(font::width).max(Integer::compareTo).orElse(200);
            var height = font.lineHeight * lines.size();

            var alpha = 1f;
            if (remainingTime > START_TIME - IN_FADE) {
                alpha = START_TIME - remainingTime;
                alpha /= IN_FADE;
                alpha *= alpha;
                alpha *= alpha;
                height = (int) (height * alpha);
            }

            if (remainingTime < 1f) {
                alpha = remainingTime;
                alpha *= alpha;
                alpha *= alpha;
            }

            graphics.fill(RenderType.guiOverlay(), x, y, x + width + padding * 2, y + height + padding * 2 - 2,
                    toRgba(0x000000, alpha * 0.75F));
            graphics.renderOutline(x, y, width + padding * 2, height + padding * 2 - 2,
                    toRgba(0xAAAAEE, alpha * 0.75F));

            int line = 0;
            for (FormattedCharSequence text : lines) {
                graphics.drawString(font, text, x + padding, y + padding
                                + font.lineHeight * line,
                        toRgba(0xFFFFFF, Math.max(alpha, .1F)));
                line++;
            }

            int finalHeight = height + padding * 2 + 2;
            finalHeight = (int) (finalHeight * alpha);
            return finalHeight;
        }

        int toRgba(int color, float alpha) {
            return (color & 0xFFFFFF) | ((int) (Mth.clamp(alpha, 0f, 1f) * 255F) << 8 * 3);
        }

        boolean isDone() {
            return remainingTime < 0;
        }
    }

}
