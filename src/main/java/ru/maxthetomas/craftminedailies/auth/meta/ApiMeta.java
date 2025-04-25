package ru.maxthetomas.craftminedailies.auth.meta;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import ru.maxthetomas.craftminedailies.CraftmineDailies;
import ru.maxthetomas.craftminedailies.mixin.common.ServerPlayerAccessor;

import java.util.List;
import java.util.UUID;

// Extra metadata to be stored
public record ApiMeta(
        long worldSeed, List<String> effects, UUID playerUUID, List<String> mods,
        int playerExperienceLevel, List<String> unlocks, long ticks, InventoryMeta inventoryMeta
) {
    public static ApiMeta createMeta(InventoryMeta inventoryMeta) {
        if (!CraftmineDailies.isInDaily()) return null;

        var server = Minecraft.getInstance().getSingleplayerServer();
        var level = server.theGame().getAllLevels().stream()
                .filter(ServerLevel::isMine).findFirst();

        List<String> worldEffects = List.of();
        if (level.isPresent()) {
            worldEffects = level.get().getActiveEffects().stream()
                    .map(v -> BuiltInRegistries.WORLD_EFFECT.getResourceKey(v).toString()).toList();
        }

        UUID playerId = Util.NIL_UUID;
        int xpLevel = -1;

        List<String> mods = FabricLoader.getInstance().getAllMods().stream()
                .map(v -> v.getMetadata().getId()).toList();

        List<String> unlocks = List.of();

        var player = server.theGame().playerList().getPlayers().getFirst();
        if (player != null) {
            playerId = player.getUUID();
            xpLevel = player.totalExperience;
            unlocks = ((ServerPlayerAccessor) player).getPlayerUnlocks().getActiveUnlocks()
                    .stream().map(Holder::getRegisteredName).toList();
        }

        var ticks = server.theGame().overworld().getGameTime();

        return new ApiMeta(
                server.theGame().overworld().getSeed(),
                worldEffects, playerId, mods, xpLevel, unlocks, ticks,
                inventoryMeta
        );
    }

    public JsonObject toJson(HolderLookup.Provider lookup) {
        var obj = new JsonObject();

        obj.addProperty("world_seed", worldSeed());
        obj.addProperty("player_xp", playerExperienceLevel());
        obj.addProperty("game_time", ticks());
        obj.addProperty("player_uuid", playerUUID().toString());
        obj.addProperty("mod_version", CraftmineDailies.VERSION);
        obj.addProperty("mod_version_string", CraftmineDailies.getStringVersion());
        obj.add("inventory", inventoryMeta.toJson(lookup));

        var effects = new JsonArray();
        this.effects().forEach(effects::add);
        obj.add("world_effects", effects);

        var unlocks = new JsonArray();
        this.unlocks().forEach(unlocks::add);
        obj.add("player_unlocks", unlocks);

        var mods = new JsonArray();
        this.mods().forEach(mods::add);
        obj.add("client_mods", mods);

        return obj;
    }
}
