package me.chenzz.mweb.format.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileUtils {

    public static String readFileToString(String path, String encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    public static void writeStringToFile(String path, String content, String encoding) throws IOException {
        Files.write(Paths.get(path), content.getBytes(encoding));
    }
}
