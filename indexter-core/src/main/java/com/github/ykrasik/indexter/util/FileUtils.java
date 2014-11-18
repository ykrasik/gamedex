package com.github.ykrasik.indexter.util;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Yevgeny Krasik
 */
public final class FileUtils {
    private static final Filter<Path> DIRECTORY_FILTER = Files::isDirectory;

    private FileUtils() {
    }

    public static List<Path> listChildDirectories(Path root) throws IOException {
        final List<Path> childDirectories = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root, DIRECTORY_FILTER)) {
            stream.forEach(childDirectories::add);
        }
        return childDirectories;
    }

    public static boolean hasChildDirectories(Path root) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root, DIRECTORY_FILTER)) {
            return stream.iterator().hasNext();
        }
    }
}
