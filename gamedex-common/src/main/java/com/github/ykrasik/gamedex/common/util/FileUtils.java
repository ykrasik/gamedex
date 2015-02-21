package com.github.ykrasik.gamedex.common.util;

import com.gs.collections.api.list.ImmutableList;
import com.gs.collections.api.list.MutableList;
import com.gs.collections.impl.factory.Lists;
import com.gs.collections.impl.list.mutable.FastList;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

/**
 * @author Yevgeny Krasik
 */
public final class FileUtils {
    private static final Filter<Path> DIRECTORY_FILTER = Files::isDirectory;
    private static final Filter<Path> FILE_FILTER = Files::isRegularFile;

    private FileUtils() {
    }

    public static ImmutableList<Path> listChildDirectories(Path root) throws IOException {
        final MutableList<Path> childDirectories = Lists.mutable.of();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root, DIRECTORY_FILTER)) {
            stream.forEach(childDirectories::add);
        }
        return childDirectories.toImmutable();
    }

    public static ImmutableList<Path> listFirstChildDirectories(Path root, int amount) throws IOException {
        final MutableList<Path> childDirectories = FastList.newList(amount);
        int current = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root, DIRECTORY_FILTER)) {
            final Iterator<Path> iterator = stream.iterator();
            while (iterator.hasNext() && current < amount) {
                childDirectories.add(iterator.next());
                current++;
            }
        }
        return childDirectories.toImmutable();
    }

    public static boolean hasChildDirectories(Path root) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root, DIRECTORY_FILTER)) {
            return stream.iterator().hasNext();
        }
    }

    public static boolean hasChildFiles(Path root) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root, FILE_FILTER)) {
            return stream.iterator().hasNext();
        }
    }
}

