package ru.maxthetomas.craftminedailies.auth;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.players.PlayerUnlock;
import net.minecraft.world.level.mines.WorldEffect;
import ru.maxthetomas.craftminedailies.auth.meta.ApiMeta;
import ru.maxthetomas.craftminedailies.util.EndContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ApiManager {
    public static DailyDetails TodayDetails;
    public static boolean DailyFetchError;

    public static void login() {
        ClientAuth.create();
    }


    public static CompletableFuture<DailyDetails> updateDailyDetails() {
        var future = new CompletableFuture<DailyDetails>();
        DailyFetchError = false;
        TodayDetails = null;

        try (var client = HttpClient.newHttpClient()) {
            client.sendAsync(HttpRequest.newBuilder().uri(URI.create("http://localhost:2486/today"))
                    .GET().build(), HttpResponse.BodyHandlers.ofString()).whenComplete((data, error) -> {
                if (error != null) {
                    DailyFetchError = true;
                    TodayDetails = null;
                }

                var json = JsonParser.parseString(data.body());
                TodayDetails = DailyDetails.fromJson(json.getAsJsonObject());
                DailyFetchError = false;
                future.complete(TodayDetails);
            });
        } catch (Exception e) {
            future.completeExceptionally(e);
        }

        return future;
    }

    public static void submitRunStart(ApiMeta meta) {

    }

    public static void submitRunEnd(EndContext context, ApiMeta meta) {

    }

    public record DailyDetails(int xp, int mineCrafterLevel, long seed,
                               List<String> effects, List<String> unlockedEffects, List<String> playerUnlocks) {
        public List<WorldEffect> getEffects() {
            return effects.stream().map(v -> BuiltInRegistries.WORLD_EFFECT
                    .get(ResourceLocation.parse(v)).get().value()).toList();
        }

        public List<WorldEffect> getUnlockedEffects() {
            return unlockedEffects.stream().map(v -> BuiltInRegistries.WORLD_EFFECT
                    .get(ResourceLocation.parse(v)).get().value()).toList();
        }

        public List<Holder.Reference<PlayerUnlock>> getForcedPlayerUnlocks() {
            return playerUnlocks().stream().map(v -> BuiltInRegistries.PLAYER_UNLOCK.get(ResourceLocation.parse(v)).get()).toList();
        }

        public static DailyDetails fromJson(JsonObject object) {
            var seed = Long.parseLong(object.get("seed").getAsString());
            var crafterLevel = object.get("mine_crafter_level").getAsInt();
            var xp = object.get("xp").getAsInt();

            var effects = object.getAsJsonArray("effects");
            var unlocked = object.getAsJsonArray("unlocked_effects");
            var playerUnlocks = object.getAsJsonArray("player_unlocks");

            return new DailyDetails(
                    xp, crafterLevel, seed,
                    effects.asList().stream().map(JsonElement::getAsString).toList(),
                    unlocked.asList().stream().map(JsonElement::getAsString).toList(),
                    playerUnlocks.asList().stream().map(JsonElement::getAsString).toList()
            );
        }
    }
}
