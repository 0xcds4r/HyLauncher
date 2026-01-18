package com.linghy.version;

public class GameVersion
{
    private final String name;
    private final String fileName;
    private final String downloadUrl;
    private final int patchNumber;
    private final long size;
    private final boolean installed;

    public GameVersion(String name, String fileName, String downloadUrl,
                       int patchNumber, long size, boolean installed)
    {
        this.name = name;
        this.fileName = fileName;
        this.downloadUrl = downloadUrl;
        this.patchNumber = patchNumber;
        this.size = size;
        this.installed = installed;
    }

    public String getName() {
        return name;
    }

    public String getFileName() {
        return fileName;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public int getPatchNumber() {
        return patchNumber;
    }

    public long getSize() {
        return size;
    }

    public boolean isInstalled() {
        return installed;
    }

    public String getFormattedSize()
    {
        if (size < 0) return "Unknown";

        double mb = size / (1024.0 * 1024.0);
        double gb = mb / 1024.0;

        if (gb >= 1.0) {
            return String.format("%.2f GB", gb);
        } else {
            return String.format("%.2f MB", mb);
        }
    }

    @Override
    public String toString() {
        return String.format("%s (%s)%s",
                name,
                getFormattedSize(),
                installed ? " [Installed]" : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameVersion that = (GameVersion) o;
        return patchNumber == that.patchNumber;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(patchNumber);
    }
}