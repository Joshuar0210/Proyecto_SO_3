package model;

import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class FileInfo {
    private String name;
    private long size;
    private FileTime lastModified;
    private double columnView;

    public FileInfo(String name, long size, FileTime lastModified) {
        this.name = name;
        this.size = size;
        this.lastModified = lastModified;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public FileTime getLastModified() {
        return lastModified;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setLastModified(FileTime lastModified) {
        this.lastModified = lastModified;
    }

    public String getFormattedDate() {
        if (lastModified == null) return "";
        Instant instant = lastModified.toInstant();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                                       .withZone(ZoneId.systemDefault());
        return formatter.format(instant);
    }

    @Override
    public String toString() {
        return "FileInfo{" +
               "name='" + name + '\'' +
               ", size=" + size +
               ", lastModified=" + getFormattedDate() +
               '}';
    }
}
