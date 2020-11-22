package org.homenet.raneri;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FileGroup {

    private List<File> files;
    private Map<File, String> finalPaths;

    private String prefixName;
    private boolean ignored = false;
    private Date timestamp;
    private long sequenceNumber;
    private String camera;
    private String finalName;

    public FileGroup(String prefixName) {
        this.prefixName = prefixName;
        files = new ArrayList<>();
        finalPaths = new HashMap<>();
    }

    public List<File> getFiles() {
        return files;
    }

    public String getPrefixName() {
        return prefixName;
    }

    public void ignore() {
        ignored = true;
    }

    public boolean isIgnored() {
        return ignored;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getCamera() {
        return camera;
    }

    public void setCamera(String camera) {
        this.camera = camera;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getFinalName() {
        return finalName;
    }

    public Map<File, String> getFinalPaths() {
        return finalPaths;
    }

    public void setFinalName(String finalName) {
        this.finalName = finalName;
    }

    @Override
    public String toString() {
        return String.format("%s - Date=%s, seq=%d, cam=%s",
                files.stream().map(f -> f.getAbsolutePath()).collect(Collectors.joining(",")),
                timestamp.toString(),
                sequenceNumber,
                camera);
    }
}
