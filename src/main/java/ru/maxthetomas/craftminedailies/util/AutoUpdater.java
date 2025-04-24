package ru.maxthetomas.craftminedailies.util;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.version.VersionComparisonOperator;
import org.apache.logging.log4j.core.config.plugins.convert.HexConverter;
import ru.maxthetomas.craftminedailies.CraftmineDailies;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

public class AutoUpdater {


    public static void downloadAndInstallUpdate() {
        try {
            var current = Version.parse(CraftmineDailies.getStringVersion());
            var latest = Version.parse(CraftmineDailies.LATEST_VERSION_STRING);
            var hasUpdate = VersionComparisonOperator.GREATER.test(latest, current);

            if (!hasUpdate)
                return;

            var currentMod = FabricLoader.getInstance().getModContainer(CraftmineDailies.MOD_ID).get();
            var path = currentMod.getOrigin().getPaths().getFirst().toFile();

            if (!path.isFile() || !path.canWrite()) {
                // Probably running in IDE
                return;
            }

            var sha512 = HttpClient.newHttpClient().sendAsync(buildSha512Request(latest.getFriendlyString()), HttpResponse.BodyHandlers.ofString()).get();

            var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
            client.sendAsync(buildRequest(latest.getFriendlyString()),
                    HttpResponse.BodyHandlers.ofByteArray()).whenCompleteAsync(((httpResponse, throwable) -> {

                if (httpResponse.statusCode() != 200) {
                    return;
                }

                MessageDigest digest = null;
                try {
                    digest = MessageDigest.getInstance("SHA-512");
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
                digest.update(httpResponse.body());

                var result = digest.digest();
                var hexBinary = HexConverter.parseHexBinary(sha512.body());

                if (!Arrays.equals(result, hexBinary)) {
                    return;
                }

                var parentPath = path.getParent();

                var isSuccessful = path.delete();
                if (!isSuccessful) return;

                var finalPath = Path.of(FabricLoader.getInstance().getGameDir().toString(),
                        "/mods/", MODRINTH_MOD_ID + "-" + latest + ".jar");
                try {
                    Files.write(finalPath, httpResponse.body());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }));
        } catch (VersionParsingException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void showDownloadingUpdateScreen() {

    }

    private static final String MODRINTH_MOD_ID = "craftmine-dailies";

    private static HttpRequest buildRequest(String version) {
        return HttpRequest.newBuilder().GET().uri(URI.create("https://api.modrinth.com/maven/maven/modrinth/" + MODRINTH_MOD_ID + "/"
                + version + "/" + MODRINTH_MOD_ID + "-" + version + ".jar")).build();
    }

    private static HttpRequest buildSha512Request(String version) {
        return HttpRequest.newBuilder().GET().uri(URI.create("https://api.modrinth.com/maven/maven/modrinth/" + MODRINTH_MOD_ID + "/"
                + version + "/" + MODRINTH_MOD_ID + "-" + version + ".jar.sha512")).build();
    }
}
