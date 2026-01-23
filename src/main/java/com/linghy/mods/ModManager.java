package com.linghy.mods;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.linghy.env.Environment;
import com.linghy.mods.manifest.ModManifest;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class ModManager
{
    private final Gson gson;
    private final Path modsDir;

    public ModManager()
    {
        this.gson = new Gson();

        this.modsDir = Environment.getDefaultAppDir()
                .resolve("UserData")
                .resolve("Mods");

        try {
            Files.createDirectories(modsDir);
        } catch (IOException e) {
            System.err.println("Failed to create mod directories: " + e.getMessage());
        }
    }

    public List<InstalledMod> getInstalledMods()
    {
        List<InstalledMod> mods = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(modsDir, "*.jar")) {
            for (Path modFile : stream) {
                InstalledMod mod = loadModInfo(modFile);
                if (mod != null) {
                    mods.add(mod);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to list installed mods: " + e.getMessage());
        }

        return mods;
    }

    private ModManifest readManifest(Path jar) {
        try (FileSystem fs = FileSystems.newFileSystem(jar, (ClassLoader) null)) {
            Path manifest = fs.getPath("/manifest.json");
            if (!Files.exists(manifest))
                return null;

            String json = Files.readString(manifest);
            return gson.fromJson(json, ModManifest.class);

        } catch (Exception e) {
            return null;
        }
    }

    private InstalledMod loadModInfo(Path modFile)
    {
        try {
            ModManifest manifest = readManifest(modFile);
            if (manifest != null) {
                return new InstalledMod(
                        manifest.Name,
                        manifest.Name,
                        manifest.Version,
                        manifest.Authors != null && !manifest.Authors.isEmpty()
                                ? manifest.Authors.get(0).Name
                                : "Unknown",
                        !manifest.DisabledByDefault,
                        null,
                        0,
                        0
                );
            }

            Path metaFile = modFile.resolveSibling(modFile.getFileName() + ".meta");
            if (Files.exists(metaFile)) {
                return gson.fromJson(Files.readString(metaFile), InstalledMod.class);
            }

            return new InstalledMod(
                    modFile.getFileName().toString(),
                    modFile.getFileName().toString().replace(".jar", ""),
                    "Unknown",
                    "Unknown",
                    true,
                    null,
                    0,
                    0
            );

        } catch (Exception e) {
            return null;
        }
    }

    public void uninstallMod(String modFileName) throws IOException
    {
        Path modFile = modsDir.resolve(modFileName);
        Path metaFile = modFile.resolveSibling(modFile.getFileName() + ".meta");

        Files.deleteIfExists(modFile);
        Files.deleteIfExists(metaFile);
    }

    public void openModsFolder() throws IOException
    {
        if (java.awt.Desktop.isDesktopSupported()) {
            java.awt.Desktop.getDesktop().open(modsDir.toFile());
        }
    }

    public static class InstalledMod {
        public String id;
        public String name;
        public String version;
        public String author;
        public boolean enabled;
        public String iconUrl;
        public int curseForgeId;
        public int fileId;

        public InstalledMod(String id, String name, String version, String author,
                            boolean enabled, String iconUrl, int curseForgeId, int fileId) {
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