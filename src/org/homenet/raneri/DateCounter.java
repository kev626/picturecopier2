package org.homenet.raneri;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DateCounter {
    private Map<Date, Integer> dates;

    public DateCounter() {
        dates = new HashMap<>();
    }

    public int getCountForDate(Date date) {
        if (dates.containsKey(date)) {
            dates.replace(date, dates.get(date) + 1);
            return dates.get(date);
        } else {
            dates.put(date, 0);
            return 0;
        }
    }
}
