package com.linghy.mods.curseforge;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.linghy.utils.CryptoUtil;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

public class CurseForgeAPI
{
    public static final int GAME_ID = 70216; // hytale game id
    private static final Gson gson = new GsonBuilder().create();

    public enum SortField
    {
        FEATURED(1),
        POPULARITY(2),
        LAST_UPDATED(3),
        NAME(4),
        AUTHOR(5),
        TOTAL_DOWNLOADS(6);

        private final int value;
        SortField(int value) { this.value = value; }
        public int getValue() { return value; }
    }

    public enum SortOrder
    {
        ASCENDING("asc"),
        DESCENDING("desc");

        private final String value;
        SortOrder(String value) { this.value = value; }
        public String getValue() { return value; }
    }

    public static String getApiKey()
    {
        try
        {
            String url = "https://raw.githubusercontent.com/0xcds4r/LingHy-Launcher/main/keys/cf.key";
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() != 200)
            {
                System.err.println("Failed to fetch API key, status: " + response.statusCode());
                return "";
            }

            return CryptoUtil.readEncryptedBytes(response.body());
        }
        catch (Exception e)
        {
            System.err.println("Error fetching API key: " + e.getMessage());
            e.printStackTrace();
        }

        return "";
    }

    public static void init()
    {

    }

    public static List<Mod> searchMods(String query) throws Exception
    {
        return searchMods(query, SortField.POPULARITY, SortOrder.DESCENDING);
    }

    public static List<Mod> searchMods(String query, SortField sortField, SortOrder sortOrder) throws Exception
    {
        StringBuilder urlBuilder = new StringBuilder("mods/search?gameId=" + GAME_ID);

        if (query != null && !query.trim().isEmpty()) {
            urlBuilder.append("&searchFilter=").append(URLEncoder.encode(query, StandardCharsets.UTF_8));
        }

        urlBuilder.append("&sortField=").append(sortField.getValue());
        urlBuilder.append("&sortOrder=").append(sortOrder.getValue());
        urlBuilder.append("&pageSize=50");

        String url = urlBuilder.toString();
        String resp = CFHttp.get(url).body();
        ApiResponse<Mod[]> apiResp = gson.fromJson(resp, ApiResponseModArray.class);

        return List.of(apiResp.data);
    }

    public static Mod getMod(int modId) throws Exception
    {
        String url = "mods/" + modId;
        String resp = CFHttp.get(url).body();
        ApiResponse<Mod> apiResp = gson.fromJson(resp, ApiResponseMod.class);
        return apiResp.data;
    }

    public static List<ModFile> getModFiles(int modId) throws Exception
    {
        String url = "mods/" + modId + "/files";
        String resp = CFHttp.get(url).body();
        ApiResponse<ModFile[]> apiResp = gson.fromJson(resp, ApiResponseModFileArray.class);
        return List.of(apiResp.data);
    }

    public static ModFile getLatestFile(int modId) throws Exception
    {
        List<ModFile> files = getModFiles(modId);
        if (files.isEmpty()) return null;
        ModFile latest = files.get(0);
        for (ModFile f : files) {
            if (f.fileDate.getTime() > latest.fileDate.getTime()) {
                latest = f;
            }
        }
        return latest;
    }

    public static void downloadFile(ModFile file, Path out) throws Exception {
        CFHttp.download(file.downloadUrl, out);
    }

    public static class ApiResponse<T> {
        public T data;
    }

    private static class ApiResponseMod extends ApiResponse<Mod> {}
    private static class ApiResponseModArray extends ApiResponse<Mod[]> {}
    private static class ApiResponseModFileArray extends ApiResponse<ModFile[]> {}

    public static class Mod
    {
        public int id;
        public String name;
        public String slug;
        public String summary;
        public List<Author> authors;
        public List<Category> categories;
        @SerializedName("mainFileId")
        public int mainFileId;
        @SerializedName("downloadCount")
        public long downloadCount;
        @SerializedName("dateCreated")
        public String dateCreated;
        @SerializedName("dateModified")
        public String dateModified;
        public Logo logo;
        public List<Screenshot> screenshots;
    }

    public static class Logo
    {
        public int id;
        public int modId;
        public String title;
        public String description;
        public String thumbnailUrl;
        public String url;
    }

    public static class Screenshot
    {
        public int id;
        public int modId;
        public String title;
        public String description;
        public String thumbnailUrl;
        public String url;
    }

    public static class Author
    {
        public int id;
        public String name;
        public String url;
    }

    public static class Category
    {
        public int id;
        public String name;
        public String url;
    }

    public static class ModFile
    {
        public int id;
        public String fileName;
        public String displayName;
        public String downloadUrl;
        @SerializedName("fileDate")
        public java.util.Date fileDate;
        public long fileLength;
        public String[] gameVersions;
    }
}