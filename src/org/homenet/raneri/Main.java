package org.homenet.raneri;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class Main {

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);

        if (args.length != 1) {
            System.out.println("Usage: java -jar PictureRenamer.jar <path>");
            System.exit(1);
            return;
        }

        //Prompt user for paths to get/save pictures from/to
        String path = args[0];

        List<File> allFiles = new ArrayList<>();

        List<File> jpegFiles;
        jpegFiles = getFilesByExtensionType(path, ".JPG");
        allFiles.addAll(jpegFiles);
        if (jpegFiles.size() > 0) {
            System.out.println("Found " + jpegFiles.size() + " JPEG images");
        } else {
            System.out.println("Didn't find any JPEG images.");
            System.exit(0);
        }

        List<File> rawFiles;
        rawFiles = getFilesByExtensionType(path, ".ARW");
        allFiles.addAll(rawFiles);
        if (rawFiles.size() > 0) {
            System.out.println("Found " + rawFiles.size() + " RAW images");
        } else {
            System.out.println("Didn't find any RAW images.");
        }

        List<File> xmpFiles;
        xmpFiles = getFilesByExtensionType(path, ".XMP");
        allFiles.addAll(xmpFiles);
        if (xmpFiles.size() > 0) {
            System.out.println("Found " + xmpFiles.size() + " XMP files");
        }



        //Find pictures which have matches

        Map<String, FileGroup> fileGroups = new HashMap<>();

        allFiles.forEach((file) -> {
            String prefixName = removeFileExtension(file.getName());
            if (fileGroups.containsKey(prefixName)) {
                fileGroups.get(prefixName).getFiles().add(file);
            } else {
                FileGroup group = new FileGroup(prefixName);
                group.getFiles().add(file);
                fileGroups.put(prefixName, group);
            }
        });

        fileGroups.values().forEach((group) -> {
            List<File> jpegs = group.getFiles().stream()
                    .filter((file) -> file.getName().endsWith(".JPG"))
                    .collect(Collectors.toList());

            if (jpegs.size() == 0) {
                group.ignore();
                System.out.println(format("Group %s has no JPEG files. Ignoring", group.getPrefixName()));
            } else {
                jpegs.forEach((jpegFile) -> {
                    try {
                        Metadata meta = ImageMetadataReader.readMetadata(jpegFile);

                        ExifSubIFDDirectory directory = meta.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
                        Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);

                        String camera = meta.getFirstDirectoryOfType(ExifIFD0Directory.class).getString(ExifIFD0Directory.TAG_MODEL);
                        camera = camera.replace("ILCE-", "A");

                        if (group.getTimestamp() != null && date != null && !group.getTimestamp().equals(date)) {
                            System.out.println(format("Group %s has a date mismatch!", group.getPrefixName()));
                            group.ignore();
                            return;
                        }

                        group.setTimestamp(date);
                        group.setCamera(camera);

                    } catch (ImageProcessingException | IOException e) {
                        System.out.println(format("Unable to read EXIF data for group %s", group.getPrefixName()));
                    }
                });

                if (group.getTimestamp() == null) {
                    System.out.println(format("Group %s had no date in any JPEG file."));
                    group.ignore();
                    return;
                }
            }
        });

        DateCounter dateCounter = new DateCounter();
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
        dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));

        fileGroups.values().stream().filter((group) -> !group.isIgnored())
                .forEach((group) -> {
            int pictureNumber = dateCounter.getCountForDate(group.getTimestamp());
            group.setFinalName(format("%s-%02d %s",
                    dateFormatter.format(group.getTimestamp()),
                    pictureNumber,
                    group.getCamera().toUpperCase()));
        });

        fileGroups.values().stream().filter((group) -> !group.isIgnored()).forEach((group) -> {
            group.getFiles().forEach((file) -> {
                String finalPath = removeFileExtension(file.getAbsolutePath());
                String[] pieces = finalPath.split("\\\\");
                String[] extensionSplit = file.getAbsolutePath().split("\\.");
                pieces[pieces.length - 1] = group.getFinalName();
                pieces[pieces.length - 1] += "." + extensionSplit[extensionSplit.length - 1];
                finalPath = String.join("\\", pieces);

                System.out.println(finalPath);

                group.getFinalPaths().put(file, finalPath);

                if (new File(finalPath).exists()) {
                    System.out.println("Attempting to rename to a file that already exists");
                    System.out.println(format("%s -> %s", file.getAbsolutePath(), finalPath));
                    group.ignore();
                }
            });
        });

        int groupCount = fileGroups.size();
        int fileCount = fileGroups.values().stream().map((group) -> group.getFinalPaths().size()).reduce(0, Integer::sum);

        File mappingFile = new File("mapping.txt");
        try {
            if (!mappingFile.exists()) mappingFile.createNewFile();
            PrintWriter writer = new PrintWriter(new FileOutputStream(mappingFile));

            fileGroups.values().stream()
                    .filter((group) -> !group.isIgnored())
                    .forEach((group) -> group.getFiles().forEach((file) -> {
                        writer.write(format("%s -> %s%n", file.getAbsolutePath(), group.getFinalPaths().get(file)));
                    }));
            writer.close();
            System.out.println(format("Mapping file written to %s", mappingFile.getAbsolutePath()));
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to write mapping file");
        }

        System.out.println(format("Will rename %d files (%d) groups",
                fileCount, groupCount));

        System.out.print("Would you like to continue? [Y/N] ");
        if (!input.nextLine().equalsIgnoreCase("Y")) return;

        fileGroups.values().stream()
                .filter((group) -> !group.isIgnored())
                .forEach((group) -> group.getFiles().forEach((file) -> {
                    file.renameTo(new File(group.getFinalPaths().get(file)));
        }));

        System.out.println("Finished!");

    }

    public static String removeFileExtension(String file) {
        return file.substring(0, file.lastIndexOf("."));
    }


    public static List<File> getFilesByExtensionType(String dirpath, String ext) {
        try {
            return Files.walk(Paths.get(dirpath))
                    .filter(Files::isRegularFile)
                    .filter((path) ->
                            path.toFile().getName().toUpperCase().endsWith(ext.toUpperCase()))
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("Error scanning directory!");
            e.printStackTrace();
            return new ArrayList<>(); // Return an empty list.
        }
    }
}
