package ru.maxthetomas.craftminedailies.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.ProfileKeyPair;
import org.slf4j.Logger;
import ru.maxthetomas.craftminedailies.CraftmineDailies;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

public class ClientAuth {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static ProfileKeyPair keyPair;
    public static final String DEFAULT_API_HOST = "cd.mxto.ru/api/v1";
    private static URI BASE_API;
    private static String apiAccessToken;

    public static void create() {
        BASE_API = URI.create(createApiUrl()).normalize();
    }

    static void tryAuthorizeApi() {
        if (keyPair == null || keyPair.dueRefresh())
            Minecraft.getInstance().getProfileKeyPairManager().prepareKeyPair().whenComplete((keyPair, error) -> {
                ClientAuth.keyPair = keyPair.get();
                tryAuthorizeApi();
            });

        if (keyPair == null) return;
        if (apiAccessToken != null) return;

        var payload = createGetTokenPayload();

        try (var client = HttpClient.newHttpClient()) {
            var request = createUnauthorizedRequestBuilder("/auth").POST(HttpRequest.BodyPublishers.ofString(payload.toString())).build();
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).whenComplete((str, err) -> {
                if (err != null) return;
                var data = JsonParser.parseString(str.body());
                apiAccessToken = data.getAsJsonObject().get("access_token").getAsString();
            });
        } catch (Exception e) {
            LOGGER.error("Cannot authorize craftmine dailies API", e);
        }
    }

    private static JsonObject createGetTokenPayload() {
        var random = new SecureRandom().nextLong();
        var keySignatureBytes = keyPair.publicKey().data().keySignature();
        var id = Minecraft.getInstance().getGameProfile().getId().toString();
        byte[] signature;

        try {
            signature = sign(random);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        var object = new JsonObject();

        object.addProperty("uuid", id);
        object.addProperty("nonce", String.valueOf(random));
        object.addProperty("key", Base64.getEncoder().encodeToString(keyPair.publicKey().data().key().getEncoded()));

        var mojangVerification = new JsonObject();
        mojangVerification.addProperty("signature", Base64.getEncoder().encodeToString(keySignatureBytes));
        mojangVerification.addProperty("expiry_at", String.valueOf(keyPair.publicKey().data().expiresAt().toEpochMilli()));
        object.add("mojang_verification", mojangVerification);

        object.addProperty("verification", Base64.getEncoder().encodeToString(signature));

        return object;
    }

    static HttpRequest.Builder createRequestBuilder(String path) {
        var builder = createUnauthorizedRequestBuilder(path);
        builder = updateRequestBuilder(builder);
        return builder;
    }

    public static boolean isAuthorized() {
        return keyPair != null;
    }

    static HttpRequest.Builder createUnauthorizedRequestBuilder(String path) {
        if (!path.startsWith("/"))
            path = "/" + path;

        return HttpRequest.newBuilder().header("User-Agent", String.format("maxthetomas/craftmine-dailies (%s)", CraftmineDailies.VERSION)).uri(URI.create(BASE_API.toString() + path));
    }

    static HttpRequest.Builder updateRequestBuilder(HttpRequest.Builder builder) {
        return builder.header("Authorization", "Bearer " + apiAccessToken);
    }

    public static URI getBaseApiUri() {
        return BASE_API;
    }

    // Creates and signs verification data using player's private key.
    private static byte[] sign(long random) throws SignatureException, NoSuchAlgorithmException, InvalidKeyException {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(keyPair.privateKey());
        signature.update(Minecraft.getInstance().getGameProfile().getId().toString()
                .getBytes(StandardCharsets.UTF_8));
        signature.update(Base64.getEncoder().encode(digest(random)));
        signature.update("24869".getBytes(StandardCharsets.UTF_8));
        return signature.sign();
    }

    // Creates a verification for nonce.
    private static byte[] digest(long random) throws NoSuchAlgorithmException {
        var digest = MessageDigest.getInstance("SHA-256");
        digest.update(String.valueOf(random).getBytes(StandardCharsets.UTF_8));
        digest.update("24869".getBytes(StandardCharsets.UTF_8));
        return digest.digest();
    }

    private static String createApiUrl() {
        boolean secure = true;

        String host = System.getProperty("cm_dailies_host", DEFAULT_API_HOST);

        if ((host.startsWith("localhost:") && !host.contains("@"))
                || host.equals("localhost"))
            secure = false;

        return (secure ? "https" : "http") + "://" + host;
    }
}
