package ru.maxthetomas.craftminedailies.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.ProfileKeyPair;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

public class ClientAuth {
    private static ProfileKeyPair keyPair;

    public static void create() {
        var mc = Minecraft.getInstance();

        if (keyPair == null || keyPair.dueRefresh())
            mc.getProfileKeyPairManager().prepareKeyPair().whenComplete((keyPair, error) -> {
                ClientAuth.keyPair = keyPair.get();
                tryAuth();
            });
    }


    public static void tryAuth() {
        if (keyPair == null) return;

        var payload = createGetTokenPayload();

        try {
            var request = HttpRequest.newBuilder().POST(HttpRequest.BodyPublishers.ofString(payload.toString())).uri(new URI("https://api.maxthetomas.ru/")).build();
            HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString()).whenComplete((str, err) -> {
                if (err != null) return;
                var data = JsonParser.parseString(str.body());
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
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
    static byte[] digest(long random) throws NoSuchAlgorithmException {
        var digest = MessageDigest.getInstance("SHA-256");
        digest.update(String.valueOf(random).getBytes(StandardCharsets.UTF_8));
        digest.update("24869".getBytes(StandardCharsets.UTF_8));
        return digest.digest();
    }
}
