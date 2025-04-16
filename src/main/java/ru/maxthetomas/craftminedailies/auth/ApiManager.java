package ru.maxthetomas.craftminedailies.auth;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.mines.WorldEffect;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ApiManager {
    public static DailyDetails TodayDetails;

    public static void login() {
        ClientAuth.create();
    }


    public static CompletableFuture<DailyDetails> updateDailyDetails() {
        var future = new CompletableFuture<DailyDetails>();

        try (var client = HttpClient.newHttpClient()) {
            client.sendAsync(HttpRequest.newBuilder().uri(URI.create("http://localhost:2486/today"))
                    .GET().build(), HttpResponse.BodyHandlers.ofString()).whenComplete((data, error) -> {
                var json = JsonParser.parseString(data.body());
                TodayDetails = DailyDetails.fromJson(json.getAsJsonObject());
                future.complete(TodayDetails);
            });
        } catch (Exception e) {
            future.completeExceptionally(e);
        }

        return future;
    }

    public record DailyDetails(int xp, int mineCrafterLevel, long seed,
                               List<String> effects, List<String> unlockedEffects) {
        public List<WorldEffect> getEffects() {
            return effects.stream().map(v -> BuiltInRegistries.WORLD_EFFECT
                    .get(ResourceLocation.parse(v)).get().value()).toList();
        }

        public List<WorldEffect> getUnlockedEffects() {
            return unlockedEffects.stream().map(v -> BuiltInRegistries.WORLD_EFFECT
                    .get(ResourceLocation.parse(v)).get().value()).toList();
        }

        public static DailyDetails fromJson(JsonObject object) {
            var seed = Long.parseLong(object.get("seed").getAsString());
            var crafterLevel = object.get("mine_crafter_level").getAsInt();
            var xp = object.get("xp").getAsInt();

            var effects = object.getAsJsonArray("effects");
            var unlocked = object.getAsJsonArray("unlocked_effects");

            return new DailyDetails(
                    xp, crafterLevel, seed,
                    effects.asList().stream().map(JsonElement::getAsString).toList(),
                    unlocked.asList().stream().map(JsonElement::getAsString).toList()
            );
        }
    }
}
