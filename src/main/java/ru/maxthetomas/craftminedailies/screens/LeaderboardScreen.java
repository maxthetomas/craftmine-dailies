package ru.maxthetomas.craftminedailies.screens;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LeaderboardScreen extends Screen {
    public LeaderboardScreen() {
        super(Component.translatable("craftminedailies.screen.leaderboard.title"));
    }

    @Override
    protected void init() {
        super.init();
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

//        var profile = minecraft.getMinecraftSessionService().fetchProfile(uuid, true);
//        minecraft.getSkinManager().getOrLoad(profile.profile()).whenComplete(((playerSkin, throwable) -> {
//            future.complete(new ProfileData(profile.profile().getName(), playerSkin.orElse(
//                    minecraft.getSkinManager().getInsecureSkin(profile.profile())
//            )));
//        }));

        return minecraft.getSkinManager().getInsecureSkin(new GameProfile(uuid, ""));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);

        PlayerFaceRenderer.draw(guiGraphics, getOrAddCache(minecraft, UUID.fromString("5eef34b5-cb0c-4956-896b-f9b75ca6ba00")), 10, 10, 32);
    }
}
