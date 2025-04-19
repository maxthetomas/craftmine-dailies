package ru.maxthetomas.craftminedailies.screens;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.players.PlayerUnlock;
import net.minecraft.server.players.PlayerUnlocks;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.mines.WorldEffect;
import net.minecraft.world.level.mines.WorldEffects;
import ru.maxthetomas.craftminedailies.auth.ApiManager;
import ru.maxthetomas.craftminedailies.auth.RunDetails;
import ru.maxthetomas.craftminedailies.util.TimeFormatters;

import java.util.List;
import java.util.Optional;

import static ru.maxthetomas.craftminedailies.screens.LeaderboardScreen.getNameFromUUID;
import static ru.maxthetomas.craftminedailies.screens.LeaderboardScreen.getOrAddCache;

public class RunDetailsScreen extends Screen {
    private static final ResourceLocation CONTAINER_BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final ResourceLocation ADVANCEMENT_BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/sprites/advancements/task_frame_unobtained.png");
    private final LeaderboardScreen parent;

    public RunDetailsScreen(LeaderboardScreen parent, int runId) {
        super(Component.translatableWithFallback("craftminedailies.screen.runDetails", "Run Details"));
        this.parent = parent;
        ApiManager.fetchRunDetails(runId).whenComplete((runDetails, throwable) -> {
            if (throwable != null) {
                // todo handle better
                return;
            }

            this.details = runDetails;
        });
    }

    private RunDetails details = null;

    private boolean isLoading() {
        return details == null;
    }

    @Override
    protected void init() {
        super.init();

        addRenderableWidget(Button.builder(Component.translatable("craftminedailies.close"), (b) -> {
            onClose();
        }).bounds(10, this.height - 30, 50, 20).build());

        xBase = this.width / 2 - 9 * 9 - xOffset;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent == null ? new TitleScreen() : parent);
    }

    int xMouse = 0;
    int yMouse = 0;

    int xOffset = 24 / 2;
    int xBase = 0;

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);

        this.xMouse = i;
        this.yMouse = j;

        if (isLoading()) {
            guiGraphics.drawCenteredString(this.font, Component.translatableWithFallback("craftminedailies.run.loading", "Loading..."),
                    this.width / 2, this.height / 2, 0xFFFFFF);
            return;
        }

        renderPlayerInfo(guiGraphics);
        renderRunStats(guiGraphics);
        int y = renderUnlocks(guiGraphics);
        y = renderWorldEffects(guiGraphics, y);
        renderInventory(guiGraphics, y);
    }

    private void renderPlayerInfo(GuiGraphics guiGraphics) {
        PlayerFaceRenderer.draw(guiGraphics, getOrAddCache(minecraft, details.playerUuid()), xBase, 24, 16);
        guiGraphics.drawString(this.font, getNameFromUUID(details.playerUuid(), details.playerOfflineName()), xBase + 16 + 6, 24 + 4, 0xFFFFFF);

    }

    private void renderRunStats(GuiGraphics guiGraphics) {
        int baseHeight = 28;
        var runState = Component.translatableWithFallback("craftminedailies.run.time", "%s at %s",
                details.state().getTranslatable(), TimeFormatters.formatTimeWithoutHours(details.time() / 20));

        if (details.deathMessage().isPresent())
            runState = runState.withStyle(ChatFormatting.UNDERLINE);

        renderRightAligned(guiGraphics, runState, baseHeight);
        renderRightAligned(guiGraphics, Component.translatableWithFallback("craftminedailies.run.score", "XP: %s",
                details.score()), baseHeight + 11);

        var rightAlign = xBase + 18 * 10 + 3;
        var x = rightAlign - this.font.width(runState);
        if (isInBounds(x, baseHeight, this.font.width(runState), 10) && details.deathMessage().isPresent()) {
            guiGraphics.renderTooltip(this.font, details.deathMessage().get(), xMouse, yMouse);
        }
    }

    private void renderRightAligned(GuiGraphics guiGraphics, Component cmp, int y) {
        var rightAlign = xBase + 18 * 10 + 3;
        var x = rightAlign - this.font.width(cmp);
        guiGraphics.drawString(this.font, cmp, x, y, 0xFFFFFF);
    }

    private int renderWorldEffects(GuiGraphics guiGraphics, int baseY) {
        baseY += 12;

        guiGraphics.drawString(this.font, Component.translatableWithFallback("craftminedailies.run.effects", "World ingredients"), xBase, baseY, 0xFFFFFF);

        int accumulatedX = xBase;
        for (WorldEffect effect : details.addedWorldEffects()) {
            renderAdvancementLike(guiGraphics, accumulatedX, baseY + 12, WorldEffects.createEffectItem(effect, true),
                    effect.name(), effect.description());
            accumulatedX += 16;

            if (accumulatedX > xBase + 18 * 9) {
                accumulatedX = xBase;
                baseY += 18;
            }
        }

        if (details.forcedWorldEffects().isEmpty())
            return baseY + 40;

        guiGraphics.vLine(accumulatedX + 1, baseY + 14, baseY + 14 + 10, 0xFFFFFFFF);

        accumulatedX += 4;

        var effectItem = WorldEffects.createEffectItem(Component.translatableEscape("craftminedailies.item.base"),
                false, List.copyOf(details.forcedWorldEffects()));
        renderAdvancementLike(guiGraphics, accumulatedX, baseY + 12, effectItem,
                getTooltipFromItem(minecraft, effectItem));

        return baseY + 40;
    }

    private int renderUnlocks(GuiGraphics guiGraphics) {
        var baseY = 24 + 16 + 14;

        guiGraphics.drawString(this.font, Component.translatableWithFallback("craftminedailies.run.unlocks", "Player unlocks"), xBase, baseY, 0xFFFFFF);

        int accumulatedX = xBase;
        for (PlayerUnlock unlock : details.addedPlayerUnlocks()) {
            renderUnlock(guiGraphics, accumulatedX, baseY + 12, unlock.display());
            accumulatedX += 16;

            if (accumulatedX > xBase + 18 * 9) {
                accumulatedX = xBase;
                baseY += 18;
            }
        }

        if (details.forcedPlayerUnlocks().isEmpty())
            return baseY + 16 + 4;

        guiGraphics.vLine(accumulatedX + 1, baseY + 14, baseY + 14 + 10, 0xFFFFFFFF);

        accumulatedX += 4;

        for (PlayerUnlock unlock : details.forcedPlayerUnlocks()) {
            renderUnlock(guiGraphics, accumulatedX, baseY + 12, unlock.display());
            accumulatedX += 16;

            if (accumulatedX > xBase + 18 * 9) {
                accumulatedX = xBase;
                baseY += 18;
            }
        }

        return baseY + 16 + 4;
    }


    private void renderUnlock(GuiGraphics guiGraphics, int x, int y, DisplayInfo display) {
        var stack = display.getIcon();
        renderAdvancementLike(guiGraphics, x, y, stack, display.getTitle(), display.getDescription());
    }

    private void renderAdvancementLike(GuiGraphics guiGraphics, int x, int y, ItemStack stack, Component title, Component description) {
        var tooltip = List.of(
                title.copy().withStyle(ChatFormatting.BOLD),
                description
        );

        renderAdvancementLike(guiGraphics, x, y, stack, tooltip);
    }

    private void renderAdvancementLike(GuiGraphics guiGraphics, int x, int y, ItemStack stack, List<Component> tooltip) {
        guiGraphics.blit(RenderType::guiTextured, ADVANCEMENT_BACKGROUND,
                x, y, 0, 0, 16, 16, 16, 16);

        guiGraphics.pose().pushPose();
        float scale = 0.75f;
        guiGraphics.pose().scale(scale, scale, scale);
        guiGraphics.renderItem(stack, (int) ((x + 2) * (1f / scale)), (int) ((y + 2) * (1f / scale)));
        guiGraphics.pose().popPose();

        if (isInBounds(x, y, 16, 16)) {
            guiGraphics.renderTooltip(this.font, tooltip, Optional.empty(), xMouse, yMouse);
        }
    }

    private void renderInventory(GuiGraphics guiGraphics, int baseY) {
        int yBase = baseY + 10;

        if (details.inventory().isEmpty()) {
            guiGraphics.drawString(this.font, Component.translatable("craftminedailies.run.inventoryEmpty").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY),
                    xBase + 2, yBase - 12, 0xFFFFFF);
            return;
        }

        guiGraphics.drawString(this.font, Component.translatable("container.inventory"), xBase + 2, yBase - 12, 0xFFFFFF);
        renderSlotSprite(guiGraphics, xBase, yBase, 9, 3);
        renderSlotSprite(guiGraphics, xBase, yBase + 18 * 3 + 3, 9, 1);
        renderSlotSprite(guiGraphics, xBase + 18 * 9 + 3, yBase, 1, 4);
        renderSlotSprite(guiGraphics, xBase + 18 * 10 + 3 * 2, yBase, 1, 1);

        // Main inventory
        for (int row = 0; row < 3; row++) {
            if (row >= details.getNumInventoryRows())
                renderLockedSpriteRow(guiGraphics, xBase, yBase + 18 * row);

            for (int column = 0; column < 9; column++) {
                int finalX = xBase + 18 * column + 1;
                int finalY = yBase + 18 * row + 1;
                int effectiveSlotId = 9 + row * 9 + column;
                renderItem(guiGraphics, finalX, finalY, effectiveSlotId);
            }
        }

        // Hotbar
        for (int column = 0; column < 9; column++) {
            int finalX = xBase + 18 * column + 1;
            int finalY = yBase + 18 * 3 + 3 + 1;
            renderItem(guiGraphics, finalX, finalY, column);
        }


        if (!details.hasUnlock(PlayerUnlocks.ARMAMENTS))
            renderLockedArmor(guiGraphics, xBase, yBase);

        var eqSlots = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
        // Armor
        for (int row = 0; row < 4; row++) {
            int finalX = xBase + 18 * 9 + 3;
            int finalY = yBase + row * 18 + 1;
//            int effectiveSlotId = 103 - row;
            renderItem(guiGraphics, finalX, finalY, eqSlots.get(row).getIndex(36));
        }

        // Offhand
        renderItem(guiGraphics, xBase + 18 * 10 + 3 * 2 + 1, yBase + 1, Inventory.SLOT_OFFHAND);
    }

    private void renderSlotSprite(GuiGraphics guiGraphics, int x, int y, int guiWidth, int guiHeight) {
        guiGraphics.blit(RenderType::guiTextured, CONTAINER_BACKGROUND, x, y, 7, 17, 18 * guiWidth, 18 * guiHeight, 256, 256);
    }

    private void renderLockedSpriteRow(GuiGraphics guiGraphics, int xBase, int yLevel) {
        for (int i = 0; i < 9; i++) {
            renderLockedSprite(guiGraphics, xBase + 18 * i, yLevel);
        }
    }

    private void renderLockedArmor(GuiGraphics guiGraphics, int xBase, int yBase) {
        for (int i = 0; i < 4; i++) {
            renderLockedSprite(guiGraphics, xBase + 18 * 9 + 3, yBase + 18 * i);
        }
    }

    private void renderLockedSprite(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blitSprite(RenderType::guiTextured, AbstractContainerScreen.DISABLED_SLOT_SPRITE, x, y, 18, 18);
    }

    public void renderItem(GuiGraphics guiGraphics, int x, int y, int slotId) {
        var slot = details.getItemInSlot(slotId);
        guiGraphics.renderItem(slot, x, y);
        guiGraphics.renderItemDecorations(this.font, slot, x, y);
        if (!slot.isEmpty() && isSlotSelected(x, y)) {
            var tt = Screen.getTooltipFromItem(minecraft, slot).stream().filter(v -> !(v.getContents() instanceof TranslatableContents tc) ||
                    !tc.getKey().equals("world.effect.convert")).toList();
            guiGraphics.renderTooltip(font, tt, slot.getTooltipImage(), xMouse, yMouse,
                    slot.get(DataComponents.TOOLTIP_STYLE));
        }
    }

    private boolean isSlotSelected(int x, int y) {
        return isInBounds(x, y, 18, 18);
    }

    private boolean isInBounds(int x, int y, int w, int h) {
        return this.xMouse > x && this.xMouse < x + w
                && this.yMouse > y && this.yMouse < y + h;
    }
}
