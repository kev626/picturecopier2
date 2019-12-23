package org.homenet.raneri;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileGroup {

    private List<File> files;
    private Map<File, String> finalPaths;

    private String prefixName;
    private boolean ignored = false;
    private Date timestamp;
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

    public String getFinalName() {
        return finalName;
    }

    public Map<File, String> getFinalPaths() {
        return finalPaths;
    }

    public void setFinalName(String finalName) {
        this.finalName = finalName;
    }
}
