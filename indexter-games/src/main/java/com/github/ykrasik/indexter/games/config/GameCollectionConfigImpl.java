package com.github.ykrasik.indexter.games.config;

import com.google.common.annotations.VisibleForTesting;
import com.thoughtworks.xstream.XStream;
import org.boon.IO;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author Yevgeny Krasik
 */
// FIXME: This class should probably disappear.
public class GameCollectionConfigImpl implements GameCollectionConfig {
    private static final String NAME = "config.xml";
    private static final XStream XSTREAM = new XStream();

    private final Path file;

    private volatile Config config;

    public GameCollectionConfigImpl() throws IOException {
        this.file = getFile();
        final String fileContent = IO.read(file);
        if (!fileContent.isEmpty()) {
            this.config = (Config) XSTREAM.fromXML(fileContent);
        } else {
            this.config = Config.empty();
        }
    }

    private Path getFile() throws IOException {
        Path path = Paths.get(NAME);
        if (!Files.exists(path)) {
            path = Files.createFile(path);
        }
        return path;
    }

    @Override
    public Optional<File> getPrevDirectory() {
        return config.prevDirectory;
    }

    @Override
    public void setPrevDirectory(File prevDirectory) {
        config = config.withPrevDirectory(prevDirectory);
        onConfigUpdated();
    }

    @Override
    public Set<Path> getExcludedPaths() {
        return Collections.unmodifiableSet(config.excludedPaths);
    }

    @Override
    public void addExcludedPath(Path path) {
        final Config config = this.config;
        final Set<Path> newExcludedPaths = new HashSet<>(config.excludedPaths);
        newExcludedPaths.add(path);
        this.config = config.withExcludedPaths(newExcludedPaths);
        onConfigUpdated();
    }

    @VisibleForTesting
    public void clearLibraries() {
        config = Config.empty();
        onConfigUpdated();
    }

    private void onConfigUpdated() {
        final Config config = this.config;
        final String xml = XSTREAM.toXML(config);
        IO.write(file, xml);
    }

    private static class Config {
        private final Optional<File> prevDirectory;
        private final Set<Path> excludedPaths;

        private Config(Optional<File> prevDirectory,
                       Set<Path> excludedPaths) {
            this.prevDirectory = Objects.requireNonNull(prevDirectory);
            this.excludedPaths = Objects.requireNonNull(excludedPaths);
        }

        public Config withPrevDirectory(File prevDirectory) {
            return new Config(Optional.of(prevDirectory), excludedPaths);
        }

        public Config withExcludedPaths(Set<Path> excludedPaths) {
            return new Config(prevDirectory, excludedPaths);
        }

        public static Config empty() {
            return new Config(Optional.empty(), Collections.emptySet());
        }
    }
}
