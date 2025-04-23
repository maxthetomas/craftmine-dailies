package ru.maxthetomas.craftminedailies.screens;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;
import ru.maxthetomas.craftminedailies.CraftmineDailies;
import ru.maxthetomas.craftminedailies.auth.ApiManager;
import ru.maxthetomas.craftminedailies.util.GameOverlay;
import ru.maxthetomas.craftminedailies.util.Month;
import ru.maxthetomas.craftminedailies.util.TimeFormatters;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LeaderboardScreen extends Screen {
    public LeaderboardScreen(int startPage) {
        super(Component.translatable("craftminedailies.screen.leaderboard.title"));
        this.currentPageIdx = startPage;
        actualRefresh();
    }

    public LeaderboardScreen(String apiDay, Screen parent) {
        super(Component.translatable("craftminedailies.screen.leaderboard.title"));
        this.currentPageIdx = 0;
        this.apiDay = apiDay;
        this.parent = parent;
        actualRefresh();
    }

    Screen parent;
    String apiDay = null;

    Button backButton;
    Button frontButton;

    int currentPageIdx = 0;
    int pageCount = 1;

    @Override
    protected void init() {
        super.init();

        this.backButton = addRenderableWidget(Button.builder(Component.literal("<"), (b) -> {
            attemptListLeft();
        }).bounds(this.width / 2 - 100, this.height - 30, 20, 20).build());
        this.frontButton = addRenderableWidget(Button.builder(Component.literal(">"), (b) -> {
            attemptListRight();
        }).bounds(this.width / 2 + 80, this.height - 30, 20, 20).build());

        if (parent == null) {
            addRenderableWidget(Button.builder(Component.translatable("craftminedailies.close"), (b) -> {
                minecraft.setScreen(new TitleScreen());
            }).bounds(10, this.height - 30, 50, 20).build());

            addHistory();
        } else {
            addRenderableWidget(Button.builder(Component.translatable("craftminedailies.back"), (b) -> {
                minecraft.setScreen(parent);
            }).bounds(10, this.height - 30, 50, 20).build());
        }
    }

    void addHistory() {
        var btn = SpriteIconButton.builder(
                        Component.translatable("craftminedailies.button.leaderboards"),
                        (but) -> {
                            this.minecraft.setScreen(new LeaderboardCalendarScreen(Month.current(), this));
                        }, true)
                .sprite(ResourceLocation.fromNamespaceAndPath(CraftmineDailies.MOD_ID, "icon/history"),
                        16, 16)
                .width(20)
                .tooltip(Tooltip.create(Component.translatable("craftminedailies.button.history")))
                .build();

        btn.setRectangle(20, 20, 10, this.height - 30 - 20 - 2);

        addRenderableWidget(btn);
    }

    record ProfileData(String name, GameProfile profile, PlayerSkin skin) {
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

        CompletableFuture.runAsync(() -> {
            var profile = minecraft.getMinecraftSessionService().fetchProfile(uuid, true);
            minecraft.getSkinManager().getOrLoad(profile.profile()).whenComplete(((playerSkin, throwable) -> {
                future.complete(new ProfileData(profile.profile().getName(), profile.profile(), playerSkin.orElse(
                        minecraft.getSkinManager().getInsecureSkin(profile.profile())
                )));
            }));
        });

        return minecraft.getSkinManager().getInsecureSkin(new GameProfile(uuid, ""));
    }

    private List<Result> results = null;
    private CompletableFuture<ApiManager.LeaderboardFetch> futureGetter;

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);

        GameOverlay.customScreenRenderTimer(minecraft, guiGraphics, width, height);

        if (apiDay != null) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("craftminedailies.leaderboards.title.withDay",
                    Month.fromApiDay(apiDay).getComponentRepresentation()), this.width / 2, 15, 0xFFFFFF);
        } else {
            guiGraphics.drawCenteredString(this.font, Component.translatable("craftminedailies.leaderboards.title"), this.width / 2, 15, 0xFFFFFF);
        }

        if (backButton != null)
            backButton.active = canListLeft();
        if (frontButton != null)
            frontButton.active = canListRight();

        if (results == null || futureGetter != null) {
            guiGraphics.drawCenteredString(this.font,
                    Component.translatable("craftminedailies.leaderboards.loading").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
                    this.width / 2, this.height - 25, 0xFFFFFF);

            guiGraphics.drawCenteredString(this.font,
                    Component.translatable("craftminedailies.leaderboards.loading").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
                    this.width / 2, 65, 0xFFFFFF);
            return;
        }

        if (results.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("craftminedailies.leaderboards.empty"), this.width / 2, 65, 0xFFFFFF);
            return;
        }

        guiGraphics.drawCenteredString(this.font, Component.translatable("craftminedailies.leaderboards.page", currentPageIdx + 1, pageCount), this.width / 2, this.height - 25, 0xFFFFFF);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(0.5F, 0.5F, 0.5F);

        int titleX = this.width - 110 * 2;
        int titleY = 65;
        guiGraphics.drawString(this.font, Component.translatableWithFallback("craftminedailies.leaderboards.player", "Player"), titleX + 4 * 2, titleY, 0xFFFFFF);
        guiGraphics.drawString(this.font, Component.translatableWithFallback("craftminedailies.leaderboards.score", "Score"), titleX + 130 * 2, titleY, 0xFFFFFF);
        guiGraphics.drawString(this.font, Component.translatableWithFallback("craftminedailies.leaderboards.time", "Time"), titleX + 172 * 2, titleY, 0xFFFFFF);
        guiGraphics.drawString(this.font, Component.translatableWithFallback("craftminedailies.leaderboards.state", "State"), titleX + 210 * 2, titleY, 0xFFFFFF);
        guiGraphics.pose().popPose();

        var selectedRun = getCurrentlySelectedRun(i, j);
        for (int a = 0; a < Math.min(10, results.size()); a++) {
            var y = 38 + a * 17;
            renderResultAt(guiGraphics, y, currentPageIdx * 10 + a + 1, results.get(a), selectedRun == a);
        }
    }

    @Override
    public void onClose() {
        super.onClose();

        if (this.parent != null)
            minecraft.setScreen(parent);
    }

    public static String getNameFromUUID(UUID uuid, String fallback) {
        if (profiles.containsKey(uuid) && profiles.getOrDefault(uuid, null) != null) {
            if (profiles.get(uuid).name == null)
                return fallback;

            return profiles.get(uuid).name;
        }

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

    private void renderResultAt(GuiGraphics graphics, int y, int order, Result result, boolean selected) {
        int x = this.width / 2 - 110;
        int yText = y + 3 + 4;
        var style = Style.EMPTY.withUnderlined(selected);
        graphics.drawString(this.font,
                Component.literal(String.format("%s.", order)), x - 4 - 20, yText, 0xFFFFFF);
        GameOverlay.drawPlayerHead(graphics, getOrAddCache(minecraft, result.playerId), x + 4, y + 4, 12);
        graphics.drawString(this.font, Component.literal(getNameFromUUID(result.playerId, result.offlineName)).withStyle(style), x + 7 + 16, yText, 0xFFFFFF);
        graphics.drawString(this.font, Component.literal(String.valueOf(result.xp)), x + 130, yText, 0xFFFFFF);
        graphics.drawString(this.font, getTime(result), x + 172, yText, 0xFFFFFF);
        graphics.drawString(this.font, result.state.getTranslatable(), x + 210, yText, 0xFFFFFF);
    }

    public int getCurrentlySelectedRun(int mouseX, int mouseY) {
        int x = this.width / 2 - 110;
        if (mouseX < x || mouseX > x + 220) return -1;
        if (mouseY < 38 || mouseY > 38 + 17 * 10) return -1;

        var y = mouseY - 38;
        y /= 17;

        return y;
    }

    @Override
    public boolean mouseClicked(double d, double e, int i) {
        if (i == 0) {
            var currentRun = getCurrentlySelectedRun((int) d, (int) e);
            if (currentRun != -1 && currentRun < results.size()) {
                var runId = results.get(currentRun).runId;
                this.minecraft.setScreen(new RunDetailsScreen(this, runId, apiDay));
                return true;
            }
        }

        return super.mouseClicked(d, e, i);
    }

    boolean canListLeft() {
        return currentPageIdx != 0;
    }

    boolean canListRight() {
        return currentPageIdx < pageCount - 1;
    }

    void attemptListLeft() {
        if (futureGetter != null) return;
        if (currentPageIdx == 0) return;
        currentPageIdx--;

        refreshList();
    }

    void attemptListRight() {
        if (futureGetter != null) return;
        if (currentPageIdx + 1 == pageCount) return;
        currentPageIdx++;

        refreshList();
    }

    void refreshList() {
        if (futureGetter != null) return;
        results = null;
        actualRefresh();
    }

    void actualRefresh() {
        futureGetter = ApiManager.fetchLeaderboardPage(currentPageIdx, apiDay);
        futureGetter.whenComplete((v, er) -> {
            futureGetter = null;
            if (er != null) {
                LogUtils.getLogger().error("Could not fetch leaderboard", er);
                return;
            }
            results = v.results();
            pageCount = v.maxPages();
        });
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (i == GLFW.GLFW_KEY_RIGHT) {
            attemptListRight();
            return true;
        } else if (i == GLFW.GLFW_KEY_LEFT) {
            attemptListLeft();
            return true;
        }


        return super.keyPressed(i, j, k);
    }

    public record Result(UUID playerId, String offlineName, int xp, int gameTime, ResultState state, int runId) {
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
