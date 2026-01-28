package com.linghy.mods.manifest;

import java.util.List;
import java.util.Map;

public class ModManifest
{
    public String Group;
    public String Name;
    public String Version;
    public String Description;
    public List<Author> Authors;
    public String ServerVersion;
    public Map<String, String> Dependencies;
    public Map<String, String> OptionalDependencies;
    public boolean DisabledByDefault;
    public String Main;
    public boolean IncludesAssetPack;

    public static class Author
    {
        public String Name;
        public String Website;
    }
}
