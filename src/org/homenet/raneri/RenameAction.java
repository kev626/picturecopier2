package org.homenet.raneri;

import java.io.File;

public class RenameAction {

    private File jpegFrom;
    private File jpegTo;
    private File rawFrom;
    private File rawTo;
    private File xmpFrom;
    private File xmpTo;

    public RenameAction(File jpegFrom, File jpegTo, File rawFrom, File rawTo, File xmpFrom, File xmpTo) {
        this.jpegFrom = jpegFrom;
        this.jpegTo = jpegTo;
        this.rawFrom = rawFrom;
        this.rawTo = rawTo;
        this.xmpFrom = xmpFrom;
        this.xmpTo = xmpTo;
    }

    public boolean execute() {
        if (jpegFrom.renameTo(jpegTo)) {
            if (rawFrom != null && rawTo != null) {
                if (rawFrom.renameTo(rawTo)) {
                    return true;
                } else {
                    if (!jpegTo.renameTo(jpegFrom)) {   //Roll back jpeg if raw failed
                        System.out.println("Failed to rename RAW file " + rawFrom.getName() + ", and failed to roll back the corresponding JPEG file.");
                        System.out.println("Something should be seriously wrong if you ever see this message.");

                    }
                    return false;
                }
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    public boolean renameXMP() {
        if (xmpFrom != null && xmpTo != null) {
            return xmpFrom.renameTo(xmpTo);
        }
        return false;
    }

    public File getJpegFrom() {
        return jpegFrom;
    }

    public void setJpegFrom(File jpegFrom) {
        this.jpegFrom = jpegFrom;
    }

    public File getJpegTo() {
        return jpegTo;
    }

    public void setJpegTo(File jpegTo) {
        this.jpegTo = jpegTo;
    }

    public File getRawFrom() {
        return rawFrom;
    }

    public void setRawFrom(File rawFrom) {
        this.rawFrom = rawFrom;
    }

    public File getRawTo() {
        return rawTo;
    }

    public void setRawTo(File rawTo) {
        this.rawTo = rawTo;
    }

    public File getXmpFrom() {
        return xmpFrom;
    }

    public void setXmpFrom(File xmpFrom) {
        this.xmpFrom = xmpFrom;
    }

    public File getXmpTo() {
        return xmpTo;
    }

    public void setXmpTo(File xmpTo) {
        this.xmpTo = xmpTo;
    }
}
