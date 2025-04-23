package ru.maxthetomas.craftminedailies.util.ends;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import ru.maxthetomas.craftminedailies.util.EndContext;

import java.util.Optional;

public class DeathEndContext extends EndContext {
    private final ServerPlayer player;
    private final DamageSource source;

    public DeathEndContext(int experience, int passedTime, ServerPlayer player, DamageSource source) {
        super(experience, passedTime);
        this.player = player;
        this.source = source;
    }

    @Override
    public boolean isWin() {
        return false;
    }

    @Override
    public String getStringName() {
        return "death";
    }

    @Override
    public JsonObject getAsJson() {
        var json = super.getAsJson();

        json.add("death_message", ComponentSerialization.CODEC.encode(source.getLocalizedDeathMessage(player),
                JsonOps.INSTANCE, JsonOps.INSTANCE.emptyMap()).getOrThrow());
        json.addProperty("damage_type", source.type().msgId());
        Optional.ofNullable(source.getEntity()).ifPresent((e) -> json.addProperty("damager_entity", e.getType().getDescriptionId()));

        return json;
    }
}
