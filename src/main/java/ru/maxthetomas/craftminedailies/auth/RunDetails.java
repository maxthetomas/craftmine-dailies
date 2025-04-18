package ru.maxthetomas.craftminedailies.auth;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.util.UndashedUuid;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.players.PlayerUnlock;
import net.minecraft.server.players.PlayerUnlocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.mines.WorldEffect;
import ru.maxthetomas.craftminedailies.auth.meta.InventoryMeta;
import ru.maxthetomas.craftminedailies.screens.LeaderboardScreen;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record RunDetails(UUID playerUuid, String playerOfflineName, LeaderboardScreen.ResultState state,
                         int runId, int score, int time, Instant startTime, Instant endTime,
                         List<WorldEffect> forcedWorldEffects, List<WorldEffect> addedWorldEffects,
                         List<PlayerUnlock> forcedPlayerUnlocks, List<PlayerUnlock> addedPlayerUnlocks,
                         Optional<Component> deathMessage, List<InventoryMeta.SlotItem> inventory) {
    public static final MapCodec<RunDetails> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("player_uuid").xmap(UndashedUuid::fromStringLenient, UUID::toString).forGetter(RunDetails::playerUuid),
            Codec.STRING.fieldOf("player_username").forGetter(RunDetails::playerOfflineName),
            Codec.STRING.fieldOf("state").xmap(LeaderboardScreen.ResultState::valueOf, Enum::toString).forGetter(RunDetails::state),
            Codec.INT.fieldOf("run_id").forGetter(RunDetails::runId),
            Codec.INT.fieldOf("score").forGetter(RunDetails::score),
            Codec.INT.fieldOf("time").forGetter(RunDetails::time),
            Codec.STRING.fieldOf("start_time").xmap(Instant::parse, Instant::toString).forGetter(RunDetails::startTime),
            Codec.STRING.fieldOf("end_time").xmap(Instant::parse, Instant::toString).forGetter(RunDetails::endTime),
            Codec.STRING.xmap(RunDetails::worldEffectFromString, RunDetails::worldEffectToString)
                    .listOf().fieldOf("forced_world_effects").forGetter(RunDetails::forcedWorldEffects),
            Codec.STRING.xmap(RunDetails::worldEffectFromString, RunDetails::worldEffectToString)
                    .listOf().fieldOf("added_world_effects").forGetter(RunDetails::addedWorldEffects),
            Codec.STRING.xmap(RunDetails::playerUnlockFromString, RunDetails::playerUnlockToString)
                    .listOf().fieldOf("forced_player_unlocks").forGetter(RunDetails::forcedPlayerUnlocks),
            Codec.STRING.xmap(RunDetails::playerUnlockFromString, RunDetails::playerUnlockToString)
                    .listOf().fieldOf("added_player_unlocks").forGetter(RunDetails::addedPlayerUnlocks),
            ComponentSerialization.CODEC.lenientOptionalFieldOf("death_message").forGetter(RunDetails::deathMessage),
            InventoryMeta.SlotItem.CODEC.codec().listOf().fieldOf("inventory").forGetter(RunDetails::inventory)
    ).apply(instance, RunDetails::new));

    private static WorldEffect worldEffectFromString(String id) {
        return BuiltInRegistries.WORLD_EFFECT.get(ResourceLocation.parse(id)).get().value();
    }

    private static String worldEffectToString(WorldEffect e) {
        return BuiltInRegistries.WORLD_EFFECT.getResourceKey(e).get().location().toString();
    }

    private static PlayerUnlock playerUnlockFromString(String id) {
        return BuiltInRegistries.PLAYER_UNLOCK.get(ResourceLocation.parse(id)).get().value();
    }

    private static String playerUnlockToString(PlayerUnlock e) {
        return BuiltInRegistries.PLAYER_UNLOCK.getResourceKey(e).get().location().toString();
    }

    public static RunDetails fromJson(JsonObject object) {
        return CODEC.decoder().decode(JsonOps.INSTANCE, object).getOrThrow().getFirst();
    }


    public boolean hasUnlock(PlayerUnlock unlock) {
        return addedPlayerUnlocks().contains(unlock) || forcedPlayerUnlocks().contains(unlock);
    }

    public boolean hasUnlock(Holder<PlayerUnlock> unlock) {
        return this.hasUnlock(unlock.value());
    }

    public int getNumInventoryRows() {
        int count = 0;

        if (hasUnlock(PlayerUnlocks.INVENTORY_SLOTS_1))
            count++;

        if (hasUnlock(PlayerUnlocks.INVENTORY_SLOTS_2))
            count++;

        if (hasUnlock(PlayerUnlocks.INVENTORY_SLOTS_3))
            count++;

        return count;
    }

    public ItemStack getItemInSlot(int slot) {
        return inventory.stream().filter(sl -> sl.slot() == slot).map(InventoryMeta.SlotItem::stack)
                .findFirst().orElse(ItemStack.EMPTY);
    }
}
