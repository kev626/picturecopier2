package org.homenet.raneri.name;

import org.homenet.raneri.DateCounter;
import org.homenet.raneri.FileGroup;

import java.text.SimpleDateFormat;

import static java.lang.String.format;

public class DateNameGenerator implements NameGenerator {

    private DateCounter dateCounter;
    private SimpleDateFormat dateFormatter;

    public DateNameGenerator(DateCounter dateCounter, SimpleDateFormat dateFormatter) {
        this.dateCounter = dateCounter;
        this.dateFormatter = dateFormatter;
    }

    @Override
    public String generateFileName(int index, FileGroup group) {
        int pictureNumber = dateCounter.getCountForDate(group.getTimestamp());
        if (group.getCamera().isEmpty()) {
            return format("%s-%02d",
                    dateFormatter.format(group.getTimestamp()),
                    pictureNumber);
        } else {
            return format("%s-%02d %s",
                    dateFormatter.format(group.getTimestamp()),
                    pictureNumber,
                    group.getCamera().toUpperCase());
        }
    }

}
