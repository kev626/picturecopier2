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
import java.util.concurrent.atomic.AtomicInteger;
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

        System.out.println("Scanning for files in " + path);

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

        List<File> otherFiles;
        otherFiles = getFilesByExtensionTypeNot(path, ".JPG", ".ARW");
        if (otherFiles.size() > 0) {
            System.out.println("Found " + otherFiles.size() + " other files");
        }

        //Find pictures which have matches
        System.out.println("Searching for file name matches...");

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

        System.out.println("Calculating file pairings...");
        otherFiles.forEach((file) -> {
            String fileName = file.getName().substring(0, file.getName().length() - 1);
            char lastChar = file.getName().charAt(file.getName().length() - 1);

            while (fileName.length() >= 1) {
                if (lastChar == '.' &&
                        fileGroups.containsKey(fileName)) {
                    fileGroups.get(fileName).getFiles().add(file);
                    System.out.println("Pairing " + file.getName() + " with " + fileName);
                    return;
                }
                lastChar = fileName.charAt(fileName.length() - 1);
                fileName = fileName.substring(0, fileName.length() - 1);
            }

            System.out.println("File " + file.getName() + " didn't match any groups.");
        });

        System.out.println("Reading exif data...");
        AtomicInteger finishedGroups = new AtomicInteger(0);
        fileGroups.values().forEach((group) -> {
            List<File> jpegs = group.getFiles().stream()
                    .filter((file) -> file.getName().toUpperCase().endsWith(".JPG"))
                    .collect(Collectors.toList());

            printProgressBar(finishedGroups.getAndIncrement(), fileGroups.size(), 48);

            if (jpegs.size() == 0) {
                group.ignore();
                System.out.print("                                                            \r");
                System.out.println(format("Group %s has no JPEG files. Ignoring", group.getPrefixName()));
            } else {
                jpegs.forEach((jpegFile) -> {
                    try {
                        Metadata meta = ImageMetadataReader.readMetadata(jpegFile);

                        ExifSubIFDDirectory directory = meta.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
                        Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);

                        String camera = meta.getFirstDirectoryOfType(ExifIFD0Directory.class).getString(ExifIFD0Directory.TAG_MODEL);
                        camera = camera.replace("ILCE-", "A");
                        camera = camera.trim();

                        if (group.getTimestamp() != null && date != null && !group.getTimestamp().equals(date)) {
                            System.out.print("                                                            \r");
                            System.out.println(format("Group %s has a date mismatch!", group.getPrefixName()));
                            group.ignore();
                            return;
                        }

                        group.setTimestamp(date);
                        group.setCamera(camera);

                    } catch (ImageProcessingException | IOException e) {
                        System.out.print("                                                            \r");
                        System.out.println(format("Unable to read EXIF data for group %s", group.getPrefixName()));
                    }
                });

                if (group.getTimestamp() == null) {
                    System.out.print("                                                            \r");
                    System.out.println(format("Group %s had no date in any JPEG file."));
                    group.ignore();
                    return;
                }
            }
        });
        System.out.print("                                                            \r");

        DateCounter dateCounter = new DateCounter();
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
        dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));

        System.out.println("Calculating new file names...");
        fileGroups.values().stream().filter((group) -> !group.isIgnored())
                .sorted()
                .forEach((group) -> {
            int pictureNumber = dateCounter.getCountForDate(group.getTimestamp());
            group.setFinalName(format("%s-%02d %s",
                    dateFormatter.format(group.getTimestamp()),
                    pictureNumber,
                    group.getCamera().toUpperCase()));
        });

        fileGroups.values().stream().filter((group) -> !group.isIgnored()).forEach((group) -> {
            group.getFiles().forEach((file) -> {
                group.getPrefixName();
                file.getName();
                String finalPath = removeFileExtension(file.getAbsolutePath());
                String[] pieces = finalPath.split("\\\\");
                pieces[pieces.length - 1] = group.getFinalName();
                pieces[pieces.length - 1] += file.getName().substring(group.getPrefixName().length()).toLowerCase();
                finalPath = String.join("\\", pieces);

                group.getFinalPaths().put(file, finalPath);

                if (new File(finalPath).exists()) {
                    if (!finalPath.equalsIgnoreCase(file.getAbsolutePath())) {
                        // Only print the warning message if the file isn't being renamed to itself.
                        System.out.println("Attempting to rename to a file that already exists");
                        System.out.println(format("%s -> %s", file.getAbsolutePath(), finalPath));
                    }
                    group.ignore();
                }
            });
        });

        long groupCount = fileGroups.values().stream().filter((group) -> !group.isIgnored()).count();
        int fileCount = fileGroups.values().stream()
                .filter((group) -> !group.isIgnored())
                .map((group) -> group.getFinalPaths().size()).reduce(0, Integer::sum);

        if (fileCount == 0) {
            System.out.println("No files to rename. Nothing to do.");
            return;
        }

        Map<String, Integer> summary = new HashMap<>();

        System.out.println("File names calculated. Writing mapping file.");
        File mappingFile = new File(Paths.get(path).toAbsolutePath() + "/mapping.txt");
        try {
            if (!mappingFile.exists()) mappingFile.createNewFile();
            PrintWriter writer = new PrintWriter(new FileOutputStream(mappingFile));

            fileGroups.values().stream()
                    .filter((group) -> !group.isIgnored())
                    .forEach((group) -> group.getFiles().forEach((file) -> {
                        writer.write(format("move \"%s\" \"%s\"%n", file.getAbsolutePath().substring(path.length() + 1), group.getFinalPaths().get(file).substring(path.length() + 1)));

                        String[] extSplit = file.getAbsolutePath().split("\\.");
                        String ext = extSplit[extSplit.length - 1].toUpperCase();
                        if (summary.containsKey(ext)) {
                            summary.put(ext, summary.get(ext) + 1);
                        } else {
                            summary.put(ext, 1);
                        }
                    }));
            writer.close();
            System.out.println(format("Mapping file written to %s", mappingFile.getAbsolutePath()));
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to write mapping file");
        }

        System.out.println(format("Will rename %d files (%d) groups",
                fileCount, groupCount));

        System.out.println("---- SUMMARY ----");
        summary.keySet().stream().sorted().forEach((key) ->
            System.out.println(format("Will rename %d %s files.",
                    summary.get(key), key.toUpperCase())));

        long skippedGroups = fileGroups.values().stream()
                .filter(FileGroup::isIgnored)
                .count();

        System.out.println(format("Skipping %d groups, including:", skippedGroups));
        fileGroups.values().stream()
                .filter(FileGroup::isIgnored)
                .limit(10)
                .forEach((group) ->
                    System.out.println(format("    %s (%d files)", group.getPrefixName(), group.getFiles().size())));


        System.out.print("\n");

        System.out.print("Would you like to continue? [Y/N] ");
        if (!input.nextLine().equalsIgnoreCase("Y")) return;

        System.out.println("Renaming files. Do not exit.");

        List<FileGroup> finalGroupList = fileGroups.values().stream()
                .filter((group) -> !group.isIgnored()).collect(Collectors.toList());

        for (int i = 0; i < finalGroupList.size(); i++) {
            FileGroup group = finalGroupList.get(i);
            printProgressBar(i, finalGroupList.size(), 48);
            group.getFiles().forEach((file) -> {
                file.renameTo(new File(group.getFinalPaths().get(file)));
            });
        }

        System.out.print("                                                            \r");
        System.out.println("Finished!");

    }

    public static String removeFileExtension(String file) {
        return file.substring(0, file.lastIndexOf("."));
    }


    public static List<File> getFilesByExtensionType(String dirpath, String ext) {
        try {
            AtomicInteger i = new AtomicInteger(0);
            return Files.walk(Paths.get(dirpath))
                    .filter(Files::isRegularFile)
                    .filter((path) ->
                            path.toFile().getName().toUpperCase().endsWith(ext.toUpperCase()))
                    .map(Path::toFile)
                    .filter(path -> {
                        System.out.println("Found " + i.incrementAndGet() + " files");
                        return true;
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("Error scanning directory!");
            e.printStackTrace();
            return new ArrayList<>(); // Return an empty list.
        }
    }

    public static List<File> getFilesByExtensionTypeNot(String dirpath, String... ext) {
        try {
            return Files.walk(Paths.get(dirpath))
                    .filter(Files::isRegularFile)
                    .filter((path) -> {
                        for (String s : ext) {
                            if (path.toFile().getName().toUpperCase().endsWith(s.toUpperCase()))
                                return false;

                        }
                        return true;
                    })
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("Error scanning directory!");
            e.printStackTrace();
            return new ArrayList<>(); // Return an empty list.
        }
    }

    public static void printProgressBar(int completed, int total, int length) {
        double progress = (double) completed / total;
        System.out.print("[");
        for (int i = 0; i < length; i++)
            System.out.print(progress*length < i ? " " : "=");
        System.out.print("] " + Math.round(progress * 1000)/10d + "%\r");
    }

}
