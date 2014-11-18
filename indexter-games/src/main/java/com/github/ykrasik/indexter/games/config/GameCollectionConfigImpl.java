package com.github.ykrasik.indexter.games.config;

import com.github.ykrasik.indexter.games.datamodel.Library;
import com.google.common.annotations.VisibleForTesting;
import com.thoughtworks.xstream.XStream;
import org.boon.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Yevgeny Krasik
 */
public class GameCollectionConfigImpl implements GameCollectionConfig {
    private static final Logger LOG = LoggerFactory.getLogger(GameCollectionConfigImpl.class);
    private static final String NAME = "config.xml";
    private static final XStream XSTREAM = new XStream();

    private final Path file;

    private final AtomicInteger modificationCounter;
    private volatile Config config;

    public GameCollectionConfigImpl() throws IOException {
        this.modificationCounter = new AtomicInteger(0);

        this.file = getFile();
        final String fileContent = IO.read(file);
        if (!fileContent.isEmpty()) {
            this.config = (Config) XSTREAM.fromXML(fileContent);
        } else {
            this.config = new Config(Optional.empty(), Collections.emptyMap(), Collections.emptyMap());
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
    public Map<Path, Library> getLibraries() {
        return Collections.unmodifiableMap(config.libraries);
    }

    @Override
    public void addLibrary(Library library) {
        final Config config = this.config;
        final Map<Path, Library> newLibraries = new HashMap<>(config.libraries);
        newLibraries.put(library.getPath(), library);
        this.config = config.withLibraries(newLibraries);
        onConfigUpdated();
    }

    @Override
    public Map<Path, Library> getSubLibraries() {
        return Collections.unmodifiableMap(config.subLibraries);
    }

    @Override
    public void addSubLibraries(List<Library> subLibraries) {
        final Config config = this.config;
        final Map<Path, Library> newSubLibraries = new HashMap<>(config.subLibraries);
        for (Library subLibrary : subLibraries) {
            newSubLibraries.put(subLibrary.getPath(), subLibrary);
        }
        this.config = config.withSubLibraries(newSubLibraries);
        onConfigUpdated();
    }

    @VisibleForTesting
    public void clearLibraries() {
        config = config.clear();
        onConfigUpdated();
    }

    private void onConfigUpdated() {
        final Config config = this.config;
        final String xml = XSTREAM.toXML(config);
        IO.write(file, xml);
    }

    private static class Config {
        private final Optional<File> prevDirectory;
        private final Map<Path, Library> libraries;
        private final Map<Path, Library> subLibraries;

        private Config(Optional<File> prevDirectory, Map<Path, Library> libraries, Map<Path, Library> subLibraries) {
            this.prevDirectory = Objects.requireNonNull(prevDirectory);
            this.libraries = Objects.requireNonNull(libraries);
            this.subLibraries = Objects.requireNonNull(subLibraries);
        }

        public Config withPrevDirectory(File prevDirectory) {
            return new Config(Optional.of(prevDirectory), this.libraries, this.subLibraries);
        }

        public Config withLibraries(Map<Path, Library> libraries) {
            return new Config(prevDirectory, libraries, subLibraries);
        }

        public Config withSubLibraries(Map<Path, Library> subLibraries) {
            return new Config(prevDirectory, libraries, subLibraries);
        }

        public Config clear() {
            return new Config(prevDirectory, Collections.emptyMap(), Collections.emptyMap());
        }
    }

//    private class ConfigUpdaterThread extends Thread {
//        @Override
//        public void run() {
//            whil
//            try {
//                synchronized (GameCollectionConfigImpl.this) {
//                    wait();
//                }
//            } catch (Exception e) {
//                LOG.warn("Error", e);
//            }
//        }
//    }
}
