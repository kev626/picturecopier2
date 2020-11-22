package org.homenet.raneri.name;

import org.homenet.raneri.FileGroup;

public interface NameGenerator {

    String generateFileName(int index, FileGroup group);

}
