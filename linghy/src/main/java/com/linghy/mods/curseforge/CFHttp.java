package com.linghy.mods.curseforge;

import java.net.http.*;
import java.net.*;
import java.nio.file.Path;
import java.time.Duration;

public final class CFHttp
{
    private static final String BASE = "https://api.curseforge.com/v1/";
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static HttpResponse<String> get(String path) throws Exception
    {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .header("Accept", "application/json")
                .header("x-api-key", CurseForgeAPI.getApiKey())
                .GET()
                .build();

        return CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
    }

    public static HttpResponse<Path> download(String url, Path out) throws Exception
    {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        return CLIENT.send(req, HttpResponse.BodyHandlers.ofFile(out));
    }
}
