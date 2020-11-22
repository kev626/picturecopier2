package org.homenet.raneri.comparator;

import org.homenet.raneri.FileGroup;

import java.util.Comparator;

public class ForwardComparator implements Comparator<FileGroup> {
    @Override
    public int compare(FileGroup o1, FileGroup o2) {
        return o1.getPrefixName().compareTo(o2.getPrefixName());
    }
}
