package org.homenet.raneri;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);

        if (args.length != 2) {
            System.out.println("Usage: java -jar PictureRenamer.jar <jpeg path> <raw path>");
            System.exit(1);
            return;
        }

        //Prompt user for paths to get/save pictures from/to
        String jpegdir = args[0];
        if (jpegdir.endsWith("\\")) jpegdir = jpegdir.substring(0, jpegdir.length()-1);

        String rawdir = args[1];
        if (rawdir.endsWith("\\")) rawdir = rawdir.substring(0, rawdir.length()-1);


        List<File> allFiles = new ArrayList<>();

        List<File> jpegFiles;
        jpegFiles = getFilesByExtensionType(jpegdir, ".jpg");
        allFiles.addAll(jpegFiles);
        if (jpegFiles.size() > 0) {
            System.out.println("Found " + jpegFiles.size() + " JPEG images");
        } else {
            System.out.println("Didn't find any JPEG images.");
            System.exit(0);
        }

        List<File> rawFiles;
        rawFiles = getFilesByExtensionType(rawdir, ".ARW");
        allFiles.addAll(rawFiles);
        if (rawFiles.size() > 0) {
            System.out.println("Found " + rawFiles.size() + " RAW images");
        } else {
            System.out.println("Didn't find any RAW images.");
        }

        Collections.sort(jpegFiles);
        Collections.sort(rawFiles);

        //Find pictures which have matches

        System.out.println("Checking to make sure all pictures have matches...");
        List<File> extraJPEGs = new ArrayList<>();
        for (File jpegFile : jpegFiles) {
            String jpeg = removeFileExtension(jpegFile.getName());
            boolean matchFound = false;
            for (File rawFile : rawFiles) {
                String raw = removeFileExtension(rawFile.getName());
                if (jpeg.equals(raw)) matchFound = true;
            }

            if (!matchFound) {
                extraJPEGs.add(jpegFile);
                //System.out.println("JPEG file " + jpeg + " does not have a corresponding RAW file!");
            }
        }

        List<File> extraRAWs = new ArrayList<>();
        for (File rawFile : rawFiles) {
            String raw = removeFileExtension(rawFile.getName());
            boolean matchFound = false;
            for (File jpegFile : jpegFiles) {
                String jpeg = removeFileExtension(jpegFile.getName());
                if (raw.equals(jpeg)) matchFound = true;
            }

            if (!matchFound) {
                extraRAWs.add(rawFile);
                //System.out.println("RAW file " + raw + " does not have a corresponding JPEG file!");
            }
        }

        if (extraJPEGs.size() == 0 && extraRAWs.size() == 0) {
            System.out.println("All files are in perfect JPEG/RAW pairs.");
        } else if (extraRAWs.size() > 0) {
            System.out.println("There are " + extraRAWs.size() + " extra RAWs which will not be renamed.");
            for (File raw : extraRAWs) rawFiles.remove(raw);
        } else {
            System.out.println("There are " + extraJPEGs.size() + " extra JPEGs and " + extraRAWs.size() + " extra RAWs.");
            System.out.print("Would you like to rename these files as well? (Raws will not be renamed) [Y/n] ");
            boolean renameExtras = input.nextLine().equalsIgnoreCase("Y");
            if (!renameExtras) {
                for (File jpeg : extraJPEGs) jpegFiles.remove(jpeg);
                for (File raw : extraRAWs) rawFiles.remove(raw);
            } else {
                for (File raw : extraRAWs) rawFiles.remove(raw);
            }
        }



        //Read EXIF data

        System.out.println("Reading EXIF data...");

        Map<File, Metadata> exifMap = getMetadata(jpegFiles);
        System.out.println("Done reading EXIF data                                        ");
        if (exifMap.size() < jpegFiles.size()) {
            System.out.println("Could not get EXIF data for " + (jpegFiles.size() - exifMap.size()) + " files. These will not be renamed.");
        }


        //Process rename actions
        List<RenameAction> renameActions = new ArrayList<>();

        DateCounter dateCounter = new DateCounter();
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd hh.mm.ss");
        for (int i = 0; i < jpegFiles.size(); i++) {
            File jpegFile = jpegFiles.get(i);
            File rawFile = getRawFileByName(rawFiles, jpegFile.getName());
            Metadata metadata = exifMap.get(jpegFile);

            ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);

            String camera = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class).getString(ExifIFD0Directory.TAG_MODEL);
            camera = camera.replace("ILCE-", "A");

            int n = dateCounter.getCountForDate(date);
            String pictureNumber = "";
            String targetFilenameNoExt = "";

            boolean nextRawFound = false;
            while (!nextRawFound) {
                if (n < 10) pictureNumber = "0" + n;
                else if (n > 99) continue;
                else pictureNumber = n + "";
                targetFilenameNoExt = dateFormatter.format(date) + "-" + pictureNumber + " " + camera.toUpperCase();
                boolean fileExistsWithSameFilename = false;
                for (File file : allFiles) {
                    if (removeFileExtension(file.getName()).equals(targetFilenameNoExt) && file != jpegFile && file != rawFile) {
                        fileExistsWithSameFilename = true;
                    }
                }

                if (fileExistsWithSameFilename) n = dateCounter.getCountForDate(date);
                else nextRawFound = true;
            }

            if (n < 10) pictureNumber = "0" + n;
            else if (n > 99) continue;
            else pictureNumber = n + "";
            targetFilenameNoExt = dateFormatter.format(date) + "-" + pictureNumber + " " + camera.toUpperCase();
            String targetFilenameJpeg = targetFilenameNoExt + ".jpg";
            String targetFilenameRaw = targetFilenameNoExt + ".ARW";

            if (rawFile == null) {
                renameActions.add(new RenameAction(
                        jpegFile,
                        new File(jpegdir + "\\" + targetFilenameJpeg),
                        null,
                        null
                ));
            } else {
                renameActions.add(new RenameAction(
                        jpegFile,
                        new File(jpegdir + "\\" + targetFilenameJpeg),
                        rawFile,
                        new File(rawdir + "\\" + targetFilenameRaw)
                ));
            }

        }

        for (int i = renameActions.size()-1; i >= 0; i--) {
            RenameAction action = renameActions.get(i);
            if (action.getJpegFrom().getName().equals(action.getJpegTo().getName()))
                renameActions.remove(i);
        }

        System.out.println("Will rename " + renameActions.size() + " image files.");
        System.out.print("Do you wish to continue? [Y/n] ");
        if (!input.nextLine().equalsIgnoreCase("Y")) return;

        //Rename files

        System.out.println("Renaming files...");

        boolean isDone = false;
        while (!isDone) {
            List<RenameAction> completedActions = new ArrayList<>();
            for (RenameAction action : renameActions) {
                if (action.execute()) completedActions.add(action);
            }
            for (RenameAction action : completedActions) {
                renameActions.remove(action);
            }

            isDone = completedActions.size() == 0 || renameActions.size() == 0;
        }

        System.out.println("Done.");

        //TODO: Retry if any conflicts occurred that would result in a file unable to be renamed: Old file with old name still needs to be processed, etc.
        //TODO: Do not recalculate the entire list of events, just execute all actions again, until all have completed or none succeeded.
        //TODO: Always do it in pairs!!!!!!!!

    }

    public static File getRawFileByName(List<File> rawFiles, String jpegName) {
        String name = removeFileExtension(jpegName);
        for (File f : rawFiles) {
            if (removeFileExtension(f.getName()).equals(name)) return f;
        }
        return null;
    }

    public static Map<File, Metadata> getMetadata(List<File> jpegFiles) {
        Map<File, Metadata> exifMap = new HashMap<>();
        double completed = 0;
        double total = jpegFiles.size();
        for (File file : jpegFiles) {
            try {
                exifMap.put(file, ImageMetadataReader.readMetadata(file));
            } catch (IOException | ImageProcessingException e) { } finally {
                completed++;
                printProgressBar(completed / total);
            }
        }
        return exifMap;
    }

    public static void printProgressBar(double progress) {
        int length = 48;
        System.out.print("[");
        for (int i = 0; i < length; i++)
            System.out.print(progress*length < i ? " " : "=");
        System.out.print("] " + Math.round(progress * 1000)/10d + "%\r");
    }


    public static String removeFileExtension(String file) {
        return file.substring(0, file.lastIndexOf("."));
    }


    public static List<File> getFilesByExtensionType(String dirpath, String ext) {
        List<File> fileList = new ArrayList<>();

        File dir = new File(dirpath);
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File file : directoryListing)
                if (file.getName().endsWith(ext))
                    fileList.add(file);

            return fileList;
        } else {
            System.err.println(dirpath + " is not a directory!");
            System.exit(1);
        }
        return null;
    }
}
