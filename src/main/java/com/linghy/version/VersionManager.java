package com.linghy.version;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.linghy.env.Environment;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

public class VersionManager
{
    private static final String PATCHES_BASE_URL = "https://game-patches.hytale.com/patches";
    private static final int MAX_PATCH_SCAN = 100;
    private static final int SCAN_THREADS = 10;

    private final Path versionsFile;
    private final Path installedVersionsFile;
    private final HttpClient httpClient;
    private final Gson gson;

    public VersionManager()
    {
        Path appDir = Environment.getDefaultAppDir();
        this.versionsFile = appDir.resolve("available_versions.json");
        this.installedVersionsFile = appDir.resolve("installed_versions.json");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.gson = new Gson();
    }


    public List<GameVersion> scanAvailableVersions(ProgressListener listener) throws Exception
    {
        String os = Environment.getOS();
        String arch = Environment.getArch();

        List<GameVersion> versions = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(SCAN_THREADS);
        List<Future<GameVersion>> futures = new ArrayList<>();

        listener.onProgress(0, "Scanning version...");

        for (int i = 0; i <= MAX_PATCH_SCAN; i++)
        {
            final int patchNumber = i;
            futures.add(executor.submit(() -> checkPatchExists(os, arch, patchNumber)));
        }

        int completed = 0;
        for (Future<GameVersion> future : futures)
        {
            try {
                GameVersion version = future.get();
                if (version != null) {
                    versions.add(version);
                }
            } catch (Exception e) {

            }

            completed++;
            int progress = (completed * 100) / futures.size();
            listener.onProgress(progress, "count: " + versions.size());
        }

        executor.shutdown();

        versions.sort(Comparator.comparingInt(GameVersion::getPatchNumber).reversed());

        saveAvailableVersions(versions);

        listener.onProgress(100, "count: " + versions.size());
        return versions;
    }

    private GameVersion checkPatchExists(String os, String arch, int patchNumber)
    {
        String fileName = patchNumber + ".pwr";
        String url = String.format("%s/%s/%s/release/0/%s",
                PATCHES_BASE_URL, os, arch, fileName);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<Void> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.discarding());

            if (response.statusCode() == 200) {
                long size = response.headers()
                        .firstValueAsLong("Content-Length")
                        .orElse(-1);

                return new GameVersion(
                        "Release " + patchNumber,
                        fileName,
                        url,
                        patchNumber,
                        size,
                        false
                );
            }
        } catch (Exception e) {

        }

        return null;
    }

    public List<GameVersion> loadCachedVersions()
    {
        try {
            if (Files.exists(versionsFile)) {
                String json = Files.readString(versionsFile);
                return gson.fromJson(json, new TypeToken<List<GameVersion>>(){}.getType());
            }
        } catch (IOException e) {
            System.err.println("Failed to load installed versions cache: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    private void saveAvailableVersions(List<GameVersion> versions)
    {
        try {
            String json = gson.toJson(versions);
            Files.writeString(versionsFile, json);
        } catch (IOException e) {
            System.err.println("Failed to save installed versions cache: " + e.getMessage());
        }
    }

    public List<GameVersion> getInstalledVersions()
    {
        try {
            if (Files.exists(installedVersionsFile)) {
                String json = Files.readString(installedVersionsFile);
                return gson.fromJson(json, new TypeToken<List<GameVersion>>(){}.getType());
            }
        } catch (IOException e) {
            System.err.println("Failed to load installed versions: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    public void markVersionInstalled(GameVersion version)
    {
        List<GameVersion> installed = getInstalledVersions();

        installed.removeIf(v -> v.getPatchNumber() == version.getPatchNumber());

        GameVersion installedVersion = new GameVersion(
                version.getName(),
                version.getFileName(),
                version.getDownloadUrl(),
                version.getPatchNumber(),
                version.getSize(),
                true
        );
        installed.add(installedVersion);

        try {
            String json = gson.toJson(installed);
            Files.writeString(installedVersionsFile, json);
        } catch (IOException e) {
            System.err.println("Failed to save installed versions: " + e.getMessage());
        }
    }

    public boolean isVersionInstalled(int patchNumber)
    {
        Path gameDir = Environment.getDefaultAppDir()
                .resolve("release").resolve("package")
                .resolve("game").resolve("patch-" + patchNumber);

        String clientName = Environment.getOS().equals("windows")
                ? "HytaleClient.exe" : "HytaleClient";
        Path clientPath = gameDir.resolve("Client").resolve(clientName);

        return Files.exists(clientPath);
    }

    public Path getVersionDirectory(int patchNumber)
    {
        return Environment.getDefaultAppDir()
                .resolve("release").resolve("package")
                .resolve("game").resolve("patch-" + patchNumber);
    }

    public void deleteVersion(int patchNumber) throws IOException
    {
        Path versionDir = getVersionDirectory(patchNumber);

        if (Files.exists(versionDir)) {
            deleteRecursively(versionDir);
        }

        List<GameVersion> installed = getInstalledVersions();
        installed.removeIf(v -> v.getPatchNumber() == patchNumber);

        String json = gson.toJson(installed);
        Files.writeString(installedVersionsFile, json);
    }

    private void deleteRecursively(Path path) throws IOException
    {
        if (!Files.exists(path)) return;

        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        System.err.println("Fail to delete: " + p);
                    }
                });
    }

    public interface ProgressListener {
        void onProgress(int percent, String message);
    }
}