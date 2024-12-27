package com.tcmp.fcupload.utils;

import java.util.List;
import java.util.Map;

public class FileUtils {

    public boolean haveLines(String fileName, Map<String, List<String>> linesFile) {
        List<String> lines = linesFile.get(fileName);
        return lines != null && !lines.isEmpty();
    }
}
