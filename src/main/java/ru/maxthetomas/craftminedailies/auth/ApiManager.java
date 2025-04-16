package ru.maxthetomas.craftminedailies.auth;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.players.PlayerUnlock;
import net.minecraft.world.level.mines.WorldEffect;
import org.slf4j.Logger;
import ru.maxthetomas.craftminedailies.CraftmineDailies;
import ru.maxthetomas.craftminedailies.auth.meta.ApiMeta;
import ru.maxthetomas.craftminedailies.util.EndContext;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class ApiManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static DailyDetails TodayDetails;
    public static boolean DailyFetchError;
    public static int ongoingRunId = -1;

    public static void login() {
        ClientAuth.tryAuthorizeApi();
    }

    public static void updateDailyDetails() {
        DailyFetchError = false;
        TodayDetails = null;

        try (var client = HttpClient.newHttpClient()) {
            client.sendAsync(ClientAuth.createUnauthorizedRequestBuilder("/today").GET().build(),
                    HttpResponse.BodyHandlers.ofString()).whenComplete((data, error) -> {
                if (error != null) {
                    LOGGER.error("Cannot fetch today daily run details!", error);
                    DailyFetchError = true;
                    TodayDetails = null;
                }

                var json = JsonParser.parseString(data.body());
                TodayDetails = DailyDetails.fromJson(json.getAsJsonObject());
                DailyFetchError = false;
            });
        } catch (Exception e) {
            LOGGER.error("Cannot fetch today daily run details!", e);
        }
    }

    public static void submitRunStart(ApiMeta meta) {
        var reqJson = new JsonObject();
        reqJson.add("meta", meta.toJson());

        var request = ClientAuth.createRequestBuilder("/run").POST(HttpRequest.BodyPublishers.ofString(reqJson.toString())).build();
        try (var client = HttpClient.newHttpClient()) {
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).whenComplete((resp, error) -> {
                var json = processResponse(resp, error);

                if (json == null) {
                    return;
                }

                ongoingRunId = json.get("run_id").getAsInt();
            });
        } catch (Exception error) {
            LOGGER.error("Could not submit run start!", error);
            CraftmineDailies.showErrorMessage(Component.translatable("craftminedailies.errors.general_networking")
                    .withStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Component.literal(error.getMessage())))), true);
        }
    }

    public static void submitRunEnd(EndContext context, ApiMeta meta) {
        if (ongoingRunId == -1) {
            LOGGER.warn("Unknown run id!");
        }

        var reqJson = new JsonObject();
        reqJson.add("meta", meta.toJson());
        reqJson.add("ctx", context.getAsJson());
        reqJson.addProperty("run_id", ongoingRunId);

        var request = ClientAuth.createRequestBuilder("/run").PUT(HttpRequest.BodyPublishers.ofString(reqJson.toString())).build();
        try (var client = HttpClient.newHttpClient()) {
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).whenComplete((resp, error) -> {
                var json = processResponse(resp, error);

                if (json == null) {
                    return;
                }
            });
        } catch (Exception error) {
            LOGGER.error("Could not submit run end!", error);
            CraftmineDailies.showErrorMessage(Component.translatable("craftminedailies.errors.general_networking")
                    .withStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Component.literal(error.getMessage())))), true);
        }

        ongoingRunId = -1;
    }

    private static JsonObject processResponse(HttpResponse<String> response, Throwable error) {
        if (error != null) {
            LOGGER.error("An error while sending request!", error);
            CraftmineDailies.showErrorMessage(Component.translatable("craftminedailies.errors.general_networking")
                    .withStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Component.literal(error.getMessage())))), true);
            return null;
        }

        var jsonResp = JsonParser.parseString(response.body()).getAsJsonObject();

        var code = response.statusCode();
        if (code != 200) {
            if (jsonResp.has("error")) {
                var errorObj = jsonResp.getAsJsonObject("error");
                var reset = errorObj.has("reset") && errorObj.get("reset").getAsBoolean();
                var message = errorObj.get("message").getAsString();
                CraftmineDailies.showErrorMessage(Component.literal(message), reset);
                return null;
            }

            switch (code) {
                case 401 -> CraftmineDailies.showErrorMessage(
                        Component.translatable("craftminedailies.errors.unauthorized"), true);
                case 403 -> CraftmineDailies.showErrorMessage(
                        Component.translatable("craftminedailies.errors.already_attempted"), false);
            }

            return null;
        }

        return jsonResp;
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
