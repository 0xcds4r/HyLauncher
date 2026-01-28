package com.linghy.mods;

import com.google.gson.Gson;
import com.linghy.env.Environment;
import com.linghy.mods.curseforge.CurseForgeAPI;
import com.linghy.mods.manifest.ModManifest;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ModManager
{
    private final Gson gson;
    private final Path modsDir;
    private final Map<String, ModManifest> manifestCache;

    public ModManager()
    {
        this.gson = new Gson();
        this.manifestCache = new ConcurrentHashMap<>();

        this.modsDir = Environment.getDefaultAppDir()
                .resolve("UserData")
                .resolve("Mods");

        try {
            Files.createDirectories(modsDir);
        } catch (IOException e) {
            System.err.println("Failed to create mod directories: " + e.getMessage());
        }
    }

    public CompletableFuture<List<InstalledMod>> getInstalledModsAsync()
    {
        return CompletableFuture.supplyAsync(() -> getInstalledMods());
    }

    public List<InstalledMod> getInstalledMods()
    {
        List<InstalledMod> mods = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(modsDir, "*.jar"))
        {
            for (Path modFile : stream)
            {
                InstalledMod mod = loadModInfo(modFile);

                if (mod != null) {
                    mods.add(mod);
                }
            }
        } catch (IOException e)
        {
            System.err.println("Failed to list installed mods: " + e.getMessage());
        }

        return mods;
    }

    private ModManifest readManifest(Path jar)
    {
        String jarPath = jar.toString();

        if (manifestCache.containsKey(jarPath)) {
            return manifestCache.get(jarPath);
        }

        try (ZipFile zip = new ZipFile(jar.toFile()))
        {
            ZipEntry entry = zip.getEntry("manifest.json");
            if (entry == null) return null;

            try (InputStream is = zip.getInputStream(entry))
            {
                String json = new String(is.readAllBytes());
                ModManifest manifest = gson.fromJson(json, ModManifest.class);
                manifestCache.put(jarPath, manifest);
                return manifest;
            }
        } catch (Exception e)
        {
            return null;
        }
    }

    private InstalledMod loadModInfo(Path modFile)
    {
        try
        {
            Path metaFile = modFile.resolveSibling(modFile.getFileName() + ".cfmeta");
            int curseForgeId = 0;
            int fileId = 0;
            String iconUrl = null;

            if (Files.exists(metaFile))
            {
                try {
                    String metaJson = Files.readString(metaFile);
                    ModMetadata meta = gson.fromJson(metaJson, ModMetadata.class);
                    if (meta != null)
                    {
                        curseForgeId = meta.curseForgeId;
                        fileId = meta.fileId;
                        iconUrl = meta.iconUrl;
                    }
                } catch (Exception e) {
                    // ...
                }
            }

            ModManifest manifest = readManifest(modFile);

            if (manifest != null)
            {
                return new InstalledMod(
                        modFile.getFileName().toString(),
                        manifest.Name,
                        manifest.Version != null ? manifest.Version : "Unknown",
                        manifest.Authors != null && !manifest.Authors.isEmpty()
                                ? manifest.Authors.get(0).Name
                                : "Unknown",
                        !manifest.DisabledByDefault,
                        iconUrl,
                        curseForgeId,
                        fileId
                );
            }

            String fileName = modFile.getFileName().toString();

            return new InstalledMod(
                    fileName,
                    fileName.replace(".jar", ""),
                    "Unknown",
                    "Unknown",
                    true,
                    iconUrl,
                    curseForgeId,
                    fileId
            );

        } catch (Exception e) {
            return null;
        }
    }

    public CompletableFuture<Void> downloadAndInstallAsync(CurseForgeAPI.ModFile file, ModProgressListener listener)
    {
        return CompletableFuture.runAsync(() ->
        {
            try {
                downloadAndInstall(file, listener);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void downloadAndInstall(CurseForgeAPI.ModFile file, ModProgressListener listener) throws IOException
    {
        downloadAndInstall(file, 0, null, listener);
    }

    public void downloadAndInstall(CurseForgeAPI.ModFile file, int curseForgeId, String iconUrl, ModProgressListener listener) throws IOException
    {
        String fileName = file.fileName;
        Path outPath = modsDir.resolve(fileName);

        if (listener != null) {
            listener.onProgress(0, "Starting...");
        }

        int maxRetries = 3;
        IOException lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++)
        {
            try {
                if (attempt > 1)
                {
                    if (listener != null) {
                        listener.onProgress(0, "Retry " + attempt + "...");
                    }

                    Thread.sleep(2000 * attempt);
                }

                URL url = new URL(file.downloadUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                connection.setInstanceFollowRedirects(true);
                connection.setConnectTimeout(60000);
                connection.setReadTimeout(60000);

                int responseCode = connection.getResponseCode();

                int redirectCount = 0;
                while ((responseCode == 301 || responseCode == 302 || responseCode == 303) && redirectCount < 5)
                {
                    String newUrl = connection.getHeaderField("Location");
                    connection.disconnect();
                    connection = (HttpURLConnection) new URL(newUrl).openConnection();
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                    connection.setInstanceFollowRedirects(true);
                    connection.setConnectTimeout(60000);
                    connection.setReadTimeout(60000);
                    responseCode = connection.getResponseCode();
                    redirectCount++;
                }

                if (responseCode != 200)
                {
                    throw new IOException("HTTP " + responseCode);
                }

                long contentLength = connection.getContentLengthLong();

                try (ReadableByteChannel rbc = Channels.newChannel(connection.getInputStream());
                     FileChannel fc = FileChannel.open(outPath,
                             StandardOpenOption.CREATE,
                             StandardOpenOption.WRITE,
                             StandardOpenOption.TRUNCATE_EXISTING))
                {
                    long totalRead = 0;
                    long startTime = System.currentTimeMillis();
                    long lastUpdate = startTime;
                    long chunkSize = 1024 * 1024;

                    while (totalRead < contentLength || contentLength <= 0)
                    {
                        long transferred = fc.transferFrom(rbc, totalRead, chunkSize);
                        if (transferred <= 0) break;

                        totalRead += transferred;

                        long now = System.currentTimeMillis();
                        if (listener != null && (now - lastUpdate > 16))
                        {
                            double percent = contentLength > 0 ? (totalRead * 100.0) / contentLength : 0;
                            double elapsed = (now - startTime) / 1000.0;
                            double speed = elapsed > 0 ? (totalRead / 1024.0 / 1024.0) / elapsed : 0;

                            listener.onProgress(percent, String.format("%.1f MB/s", speed));
                            lastUpdate = now;
                        }
                    }

                    if (contentLength > 0 && totalRead != contentLength)
                    {
                        throw new IOException("Incomplete");
                    }
                }

                connection.disconnect();

                if (listener != null) {
                    listener.onProgress(100, "Done");
                }

                if (curseForgeId > 0)
                {
                    Path metaFile = outPath.resolveSibling(fileName + ".cfmeta");
                    String metaJson = gson.toJson(new ModMetadata(curseForgeId, file.id, iconUrl));
                    Files.writeString(metaFile, metaJson);
                }

                manifestCache.remove(outPath.toString());
                return;

            } catch (IOException e)
            {
                lastException = e;

                try {
                    Files.deleteIfExists(outPath);
                } catch (IOException ex) {}

                if (attempt == maxRetries) break;

            } catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted", e);
            }
        }

        throw new IOException("Failed after " + maxRetries + " attempts", lastException);
    }

    private static class ModMetadata
    {
        int curseForgeId;
        int fileId;
        String iconUrl;

        ModMetadata(int curseForgeId, int fileId, String iconUrl)
        {
            this.curseForgeId = curseForgeId;
            this.fileId = fileId;
            this.iconUrl = iconUrl;
        }
    }

    public void uninstallMod(String modFileName) throws IOException
    {
        Path modFile = modsDir.resolve(modFileName);
        Path metaFile = modFile.resolveSibling(modFileName + ".cfmeta");

        manifestCache.remove(modFile.toString());

        Files.deleteIfExists(modFile);
        Files.deleteIfExists(metaFile);
    }

    public void openModsFolder() throws IOException
    {
        if (java.awt.Desktop.isDesktopSupported()) {
            java.awt.Desktop.getDesktop().open(modsDir.toFile());
        }
    }

    public static class InstalledMod
    {
        public String id;
        public String name;
        public String version;
        public String author;
        public boolean enabled;
        public String iconUrl;
        public int curseForgeId;
        public int fileId;

        public InstalledMod(String id, String name, String version, String author, boolean enabled, String iconUrl, int curseForgeId, int fileId)
        {
            this.id = id;
            this.name = name;
            this.version = version;
            this.author = author;
            this.enabled = enabled;
            this.iconUrl = iconUrl;
            this.curseForgeId = curseForgeId;
            this.fileId = fileId;
        }
    }

    public interface ModProgressListener {
        void onProgress(double percent, String message);
    }
}