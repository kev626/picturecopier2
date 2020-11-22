package org.homenet.raneri.name;

import org.homenet.raneri.FileGroup;

public class NumericNameGenerator implements NameGenerator {

    @Override
    public String generateFileName(int index, FileGroup group) {
        return String.format("%s%04d",
                group.getCamera().isEmpty() ? "XXXX" : group.getCamera().toUpperCase(),
                index);
    }

}
