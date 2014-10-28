package com.github.ykrasik.indexter.games.config;

import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.Library;
import com.github.ykrasik.indexter.util.ListUtils;
import com.google.common.annotations.VisibleForTesting;
import org.boon.IO;
import org.codehaus.jackson.map.ObjectMapper;
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
    private static final String NAME = "config.json";

    private final ObjectMapper mapper;
    private final Path file;

    private final AtomicInteger modificationCounter;
    private volatile Config config;

    public GameCollectionConfigImpl(ObjectMapper mapper) throws IOException {
        this.mapper = Objects.requireNonNull(mapper);
        this.modificationCounter = new AtomicInteger(0);

        this.file = getFile();
        final String fileContent = IO.read(file);
        if (!fileContent.isEmpty()) {
            final SerializedConfig serializedConfig = mapper.readValue(fileContent, SerializedConfig.class);
            this.config = serializedConfig.toConfig();
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
        try {
            final Config config = this.config;
            final SerializedConfig serializedConfig = SerializedConfig.from(config);
            mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), serializedConfig);
        } catch (IOException e) {
            LOG.warn("Error", e);
        }
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

    private static class SerializedConfig {
        public final String prevDirectory;
        public final List<SerializedLibrary> libraries;
        public final List<SerializedLibrary> subLibraries;

        private SerializedConfig(String prevDirectory, List<SerializedLibrary> libraries, List<SerializedLibrary> subLibraries) {
            this.prevDirectory = Objects.requireNonNull(prevDirectory);
            this.libraries = Objects.requireNonNull(libraries);
            this.subLibraries = Objects.requireNonNull(subLibraries);
        }

        public Config toConfig() {
            final Map<Path, Library> libraryMap = ListUtils.toMap(ListUtils.map(this.libraries, SerializedLibrary::toLibrary), Library::getPath);
            final Map<Path, Library> subLibraryMap = ListUtils.toMap(ListUtils.map(this.subLibraries, SerializedLibrary::toLibrary), Library::getPath);
            return new Config(
                Optional.ofNullable(prevDirectory).map(File::new),
                libraryMap,
                subLibraryMap
            );
        }

        public static SerializedConfig from(Config config) {
            final List<SerializedLibrary> libraries = ListUtils.map(config.libraries.values(), SerializedLibrary::from);
            final List<SerializedLibrary> subLibraries = ListUtils.map(config.subLibraries.values(), SerializedLibrary::from);
            return new SerializedConfig(config.prevDirectory.map(File::getAbsolutePath).orElse(null), libraries, subLibraries);
        }
    }

    private static class SerializedLibrary {
        public final String name;
        public final String path;
        public final GamePlatform platform;

        private SerializedLibrary(String name, String path, GamePlatform platform) {
            this.name = Objects.requireNonNull(name);
            this.path = Objects.requireNonNull(path);
            this.platform = Objects.requireNonNull(platform);
        }

        public Library toLibrary() {
            return new Library(name, Paths.get(path), platform);
        }

        public static SerializedLibrary from(Library library) {
            return new SerializedLibrary(library.getName(), library.getPath().toString(), library.getPlatform());
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
