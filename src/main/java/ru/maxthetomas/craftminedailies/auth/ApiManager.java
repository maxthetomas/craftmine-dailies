package ru.maxthetomas.craftminedailies.auth;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.mojang.util.UndashedUuid;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.players.PlayerUnlock;
import net.minecraft.world.level.mines.WorldEffect;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import ru.maxthetomas.craftminedailies.CraftmineDailies;
import ru.maxthetomas.craftminedailies.auth.meta.ApiMeta;
import ru.maxthetomas.craftminedailies.screens.LeaderboardScreen;
import ru.maxthetomas.craftminedailies.util.EndContext;
import ru.maxthetomas.craftminedailies.util.GameOverlay;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/*
 * Hey there, source code reader!
 *
 * I want to politely ask not to use the information from this class,
 * and other networking classes, to try and "hack" the run submission and leaderboards.
 *
 * That would be greatly appreciated!
 * Thank you for understanding that we're here just to have some fun with this blocky videogame!
 * */

@SuppressWarnings("resource")
public class ApiManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static DailyDetails TodayDetails;
    public static boolean DailyFetchError;
    public static int ongoingRunId = -1;

    public static int CachedCurrentLeaderboardPlace = -1;
    public static int CachedCurrentLeaderboardPage = -1;

    public static void login() {
        ClientAuth.tryAuthorizeApi();
    }

    public static void updateDailyDetails() {
        DailyFetchError = false;
        TodayDetails = null;

        try {
            HttpClient.newHttpClient().sendAsync(ClientAuth.createUnauthorizedRequestBuilder("/today").GET().build(),
                    HttpResponse.BodyHandlers.ofString()).whenComplete((data, error) -> {
                var json = processResponse(data, error);

                if (error != null) {
                    LOGGER.error("Cannot fetch today daily run details!", error);
                    DailyFetchError = true;
                    TodayDetails = null;
                    GameOverlay.pushNotification(Component.translatable("craftminedailies.check_status"));
                    return;
                }

                TodayDetails = DailyDetails.fromJson(json.getAsJsonObject());
                DailyFetchError = false;
            });
        } catch (Exception e) {
            LOGGER.error("Cannot fetch today daily run details!", e);
        }
    }

    public static void submitRunStart(ApiMeta meta) {
        var reqJson = new JsonObject();

        RegistryLayer.createRegistryAccess().getAccessFrom(RegistryLayer.STATIC);

        reqJson.add("meta", meta.toJson(Minecraft.getInstance()
                .getSingleplayerServer().theGame().registryAccess()));

        CachedCurrentLeaderboardPage = -1;
        CachedCurrentLeaderboardPlace = -1;

        var request = ClientAuth.createRequestBuilder("/run").POST(HttpRequest.BodyPublishers.ofString(reqJson.toString())).build();
        try {
            HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString()).whenComplete((resp, error) -> {
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
        reqJson.add("meta", meta.toJson(Minecraft.getInstance().getSingleplayerServer()
                .theGame().registryAccess()));

        reqJson.add("ctx", context.getAsJson());
        reqJson.addProperty("run_id", ongoingRunId);

        var request = ClientAuth.createRequestBuilder("/run").PUT(HttpRequest.BodyPublishers.ofString(reqJson.toString())).build();
        try {
            HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString()).whenComplete((resp, error) -> {
                var json = processResponse(resp, error);

                if (json == null) {
                    return;
                }

                CachedCurrentLeaderboardPlace = json.get("leaderboard_place").getAsInt();
                CachedCurrentLeaderboardPage = json.get("leaderboard_page").getAsInt();
            });
        } catch (Exception error) {
            LOGGER.error("Could not submit run end!", error);
            CraftmineDailies.showErrorMessage(Component.translatable("craftminedailies.errors.general_networking")
                    .withStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Component.literal(error.getMessage())))), true);
        }

        ongoingRunId = -1;
    }

    public record LeaderboardFetch(int maxPages, List<LeaderboardScreen.Result> results) {
    }

    public static CompletableFuture<LeaderboardFetch> fetchLeaderboardPage(int page, @Nullable String apiDay) {
        var future = new CompletableFuture<LeaderboardFetch>();

        try {
            var path = "/leaderboard?page=" + page;
            if (apiDay != null)
                path = path + "&date=" + apiDay;
            var request = ClientAuth.createUnauthorizedRequestBuilder(path).GET().build();
            HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString()).whenComplete((resp, error) -> {

                var json = processResponse(resp, error);

                if (error != null) {
                    future.completeExceptionally(error);
                    return;
                }

                if (json == null) {
                    future.completeExceptionally(new Exception("Error processing json"));
                    return;
                }

                var list = new ArrayList<LeaderboardScreen.Result>();
                for (JsonElement elem : json.getAsJsonArray("leaderboard")) {
                    var obj = elem.getAsJsonObject();

                    int score = obj.get("score").getAsInt();
                    String state = obj.get("state").getAsString();
                    int gameTime = obj.get("game_time").getAsInt();
                    String uuid = obj.get("player_uuid").getAsString();
                    String offlineUsername = obj.get("player_username").getAsString();
                    int runId = obj.get("run_id").getAsInt();

                    var result = new LeaderboardScreen.Result(UndashedUuid.fromStringLenient(uuid), offlineUsername, score,
                            gameTime, LeaderboardScreen.ResultState.valueOf(state), runId);
                    list.add(result);
                }

                var pageCount = json.get("page_count").getAsInt();
                future.complete(new LeaderboardFetch(pageCount, list));
            });
        } catch (Exception error) {
            LOGGER.error("Could not get leaderboard data!", error);
            future.completeExceptionally(error);
        }

        return future;
    }

    public static CompletableFuture<RunDetails> fetchRunDetails(int runId) {
        var future = new CompletableFuture<RunDetails>();

        try {
            var request = ClientAuth.createUnauthorizedRequestBuilder("/run/" + runId).GET().build();
            HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString()).whenComplete((resp, error) -> {
                var json = processResponse(resp, error);

                if (error != null) {
                    future.completeExceptionally(error);
                    return;
                }

                if (json == null) {
                    future.completeExceptionally(new Exception("Error processing json"));
                    return;
                }

                future.complete(RunDetails.fromJson(json));
            });
        } catch (Exception error) {
            LOGGER.error("Could not get leaderboard data!", error);
            future.completeExceptionally(error);
        }

        return future;
    }

    public record ServerVersions(int serverVersion, int clientVersion) {
    }

    public static CompletableFuture<ServerVersions> fetchServerVersions() {
        var future = new CompletableFuture<ServerVersions>();

        try {
            var request = ClientAuth.createUnauthorizedRequestBuilder("/ver").GET().build();
            HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString()).whenComplete((resp, error) -> {
                var json = processResponse(resp, error);

                if (error != null) {
                    future.completeExceptionally(error);
                    return;
                }

                if (json == null) {
                    future.completeExceptionally(new Exception("Error processing json"));
                    return;
                }

                var srv = json.get("server").getAsInt();
                var cli = json.get("client").getAsInt();

                future.complete(new ServerVersions(srv, cli));
            });
        } catch (Exception error) {
            LOGGER.error("Could not get leaderboard data!", error);
            future.completeExceptionally(error);
        }

        return future;
    }


    private static JsonObject processResponse(HttpResponse<String> response, Throwable error) {
        if (error != null) {
            LOGGER.error("An error while sending request!", error);
            CraftmineDailies.showErrorMessage(Component.translatable("craftminedailies.errors.general_networking")
                            .withStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Component.literal(error.getMessage())))),
                    CraftmineDailies.isInDaily());

            return null;
        }

        JsonObject jsonResp = null;

        try {
            jsonResp = JsonParser.parseString(response.body()).getAsJsonObject();
        } catch (Exception e) {
            LOGGER.error("Could not parse response JSON", e);
            CraftmineDailies.showErrorMessage(Component.translatable("craftminedailies.errors.general_networking"), false);
        }

        var code = response.statusCode();
        if (code != 200 && jsonResp == null) {
            if (code == 401) {
                CraftmineDailies.showErrorMessage(
                        Component.translatable("craftminedailies.errors.unauthorized"), true);
            }

            return null;
        }

        if (jsonResp != null) {
            if (jsonResp.has("error")) {
                var errorObj = jsonResp.getAsJsonObject("error");
                var reset = errorObj.has("reset") && errorObj.get("reset").getAsBoolean();
                var message = ComponentSerialization.CODEC.decode(JsonOps.INSTANCE, errorObj.get("message"));
                CraftmineDailies.showErrorMessage(message.getOrThrow().getFirst(), reset);
                return null;
            }

            if (jsonResp.has("overlay_messages")) {
                ComponentSerialization.CODEC.listOf().fieldOf("overlay_messages")
                        .decoder().decode(JsonOps.INSTANCE, jsonResp).ifSuccess(s -> {
                            for (Component msg : s.getFirst()) {
                                GameOverlay.pushNotification(msg);
                            }
                        });
            }
        }

        return jsonResp;
    }

    public record DailyDetails(int xp, int mineCrafterLevel, long seed,
                               List<String> effects, List<String> unlockedEffects, List<String> playerUnlocks,
                               boolean isSpecial, JsonElement extraData) {
        public List<WorldEffect> getEffects() {
            return effects.stream().map(v -> BuiltInRegistries.WORLD_EFFECT
                    .get(ResourceLocation.parse(v)).get().value()).toList();
        }

        public List<WorldEffect> getUnlockedEffects() {
            return unlockedEffects.stream().map(v -> BuiltInRegistries.WORLD_EFFECT
                            .get(ResourceLocation.parse(v)).orElse(null))
                    .filter(Objects::nonNull).map(Holder.Reference::value).toList();
        }

        public List<Holder.Reference<PlayerUnlock>> getForcedPlayerUnlocks() {
            return playerUnlocks().stream().map(v -> BuiltInRegistries.PLAYER_UNLOCK.get(ResourceLocation.parse(v)).get()).toList();
        }

        public JsonObject getDataOrEmpty() {
            if (extraData != null && extraData.isJsonObject())
                return extraData.getAsJsonObject();

            return new JsonObject();
        }

        public static DailyDetails fromJson(JsonObject object) {
            var seed = Long.parseLong(object.get("seed").getAsString());
            var crafterLevel = object.get("mine_crafter_level").getAsInt();
            var xp = object.get("xp").getAsInt();

            var effects = object.getAsJsonArray("effects");
            var unlocked = object.getAsJsonArray("unlocked_effects");
            var playerUnlocks = object.getAsJsonArray("player_unlocks");
            var isSpecial = object.has("special") && object.get("special").getAsBoolean();
            var extraData = object.has("data") ? object.get("data") : null;

            return new DailyDetails(
                    xp, crafterLevel, seed,
                    effects.asList().stream().map(JsonElement::getAsString).toList(),
                    unlocked.asList().stream().map(JsonElement::getAsString).toList(),
                    playerUnlocks.asList().stream().map(JsonElement::getAsString).toList(),
                    isSpecial, extraData
            );
        }
    }
}
