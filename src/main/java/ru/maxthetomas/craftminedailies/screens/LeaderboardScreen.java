package ru.maxthetomas.craftminedailies.screens;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import ru.maxthetomas.craftminedailies.util.TimeFormatters;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LeaderboardScreen extends Screen {
    public LeaderboardScreen(boolean showSelf) {
        super(Component.translatable("craftminedailies.screen.leaderboard.title"));
    }

    Button backButton;
    Button frontButton;

    int currentPageIdx = 0;
    int maxPageIdx = 4;

    @Override
    protected void init() {
        super.init();

        addRenderableWidget(Button.builder(Component.literal("<"), (b) -> {
        }).bounds(this.width / 2 - 100, this.height - 30, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), (b) -> {
        }).bounds(this.width / 2 + 80, this.height - 30, 20, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("craftminedailies.close"), (b) -> {
            minecraft.setScreen(new TitleScreen());
        }).bounds(10, this.height - 30, 50, 20).build());
    }

    record ProfileData(String name, PlayerSkin skin) {
    }

    static HashMap<UUID, ProfileData> profiles = new HashMap<>();

    static PlayerSkin getOrAddCache(Minecraft minecraft, UUID uuid) {
        if (profiles.containsKey(uuid)) {
            var data = profiles.get(uuid);
            if (data == null) {
                return minecraft.getSkinManager().getInsecureSkin(new GameProfile(uuid, ""));
            }

            return data.skin();
        }

        profiles.put(uuid, null);

        var future = new CompletableFuture<ProfileData>();
        future.whenComplete((d, t) -> {
            profiles.compute(uuid, (u, pd) -> d);
        });

        // todo: fetch skins when fetching leaderboard data
        var profile = minecraft.getMinecraftSessionService().fetchProfile(uuid, true);
        minecraft.getSkinManager().getOrLoad(profile.profile()).whenComplete(((playerSkin, throwable) -> {
            future.complete(new ProfileData(profile.profile().getName(), playerSkin.orElse(
                    minecraft.getSkinManager().getInsecureSkin(profile.profile())
            )));
        }));

        return minecraft.getSkinManager().getInsecureSkin(new GameProfile(uuid, ""));
    }

    private static List<Result> results = List.of(
            new Result(UUID.fromString("5eef34b5-cb0c-4956-896b-f9b75ca6ba00"),
                    "altthetomas", 24865, 20 * 60 * 24869, ResultState.DNF)
    );


    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);

        guiGraphics.drawCenteredString(this.font, Component.translatable("craftminedailies.leaderboards.title"), this.width / 2, 15, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, Component.translatable("craftminedailies.leaderboards.page", currentPageIdx + 1, maxPageIdx + 1), this.width / 2, this.height - 25, 0xFFFFFF);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(0.5F, 0.5F, 0.5F);

        int titleX = this.width - 110 * 2;
        int titleY = 65;
        guiGraphics.drawString(this.font, Component.translatableWithFallback("craftminedailies.leaderboards.player", "Player"), titleX + 4 * 2, titleY, 0xFFFFFF);
        guiGraphics.drawString(this.font, Component.translatableWithFallback("craftminedailies.leaderboards.score", "Score"), titleX + 130 * 2, titleY, 0xFFFFFF);
        guiGraphics.drawString(this.font, Component.translatableWithFallback("craftminedailies.leaderboards.time", "Time"), titleX + 172 * 2, titleY, 0xFFFFFF);
        guiGraphics.drawString(this.font, Component.translatableWithFallback("craftminedailies.leaderboards.state", "State"), titleX + 210 * 2, titleY, 0xFFFFFF);
        guiGraphics.pose().popPose();

        for (int a = 0; a < 10; a++) {
            var y = 38 + a * 17;
            renderResultAt(guiGraphics, y, a + 1, results.get(a % results.size()));
        }
    }

    private String getNameFromUUID(UUID uuid, String fallback) {
        if (profiles.containsKey(uuid) && profiles.getOrDefault(uuid, null) != null)
            return profiles.get(uuid).name;

        return fallback;
    }

    private Component getTime(Result result) {
        var state = result.state;
        var ticks = result.gameTime;
        if (state == ResultState.TIME_OUT)
            return Component.translatableWithFallback("craftminedailies.time.dnf", "--:--");
        if (state == ResultState.DNF)
            return Component.translatableWithFallback("craftminedailies.time.dnf", "--:--");
        return Component.literal(TimeFormatters.formatTimeWithoutHours(ticks / 20));
    }

    private void renderResultAt(GuiGraphics graphics, int y, int order, Result result) {
        int x = this.width / 2 - 110;
        int yText = y + 3 + 4;
        graphics.drawString(this.font, Component.literal(String.format("%s.", order)), x - 4 - 20, yText, 0xFFFFFF);
        PlayerFaceRenderer.draw(graphics, getOrAddCache(minecraft, result.playerId), x + 4, y + 4, 12);
        graphics.drawString(this.font, Component.literal(getNameFromUUID(result.playerId, result.offlineName)), x + 7 + 16, yText, 0xFFFFFF);
        graphics.drawString(this.font, Component.literal(String.valueOf(result.xp)), x + 130, yText, 0xFFFFFF);
        graphics.drawString(this.font, getTime(result), x + 172, yText, 0xFFFFFF);
        graphics.drawString(this.font, result.state.getTranslatable(), x + 210, yText, 0xFFFFFF);
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (i == GLFW.GLFW_KEY_RIGHT) {
            return true;
        } else if (i == GLFW.GLFW_KEY_LEFT) {
            return true;
        }


        return super.keyPressed(i, j, k);
    }

    public record Result(UUID playerId, String offlineName, int xp, int gameTime, ResultState state) {
    }

    public enum ResultState {
        WIN(Component.translatable("craftminedailies.run.state.win")),
        TIME_OUT(Component.translatable("craftminedailies.run.state.time_out")),
        DEATH(Component.translatable("craftminedailies.run.state.death")),
        DNF(Component.translatable("craftminedailies.run.state.dnf")),
        ;

        private final Component style;

        ResultState(Component style) {
            this.style = style;
        }

        public Component getTranslatable() {
            return style;
        }
    }
}
