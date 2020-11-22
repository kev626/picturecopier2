package org.homenet.raneri.comparator;

import org.homenet.raneri.FileGroup;

import java.util.Comparator;

public class ReverseComparator implements Comparator<FileGroup> {
    @Override
    public int compare(FileGroup o1, FileGroup o2) {
        int cmp = o1.getTimestamp().compareTo(o2.getTimestamp());
        if (cmp == 0) {
            cmp = Long.compare(o1.getSequenceNumber(), o2.getSequenceNumber());
        }
        return cmp;
    }
}
