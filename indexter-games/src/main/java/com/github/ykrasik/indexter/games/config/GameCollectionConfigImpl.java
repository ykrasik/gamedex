package com.github.ykrasik.indexter.games.config;

import com.thoughtworks.xstream.XStream;
import org.boon.IO;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

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

    private void onConfigUpdated() {
        final Config config = this.config;
        final String xml = XSTREAM.toXML(config);
        IO.write(file, xml);
    }

    private static class Config {
        private final Optional<File> prevDirectory;

        private Config(Optional<File> prevDirectory) {
            this.prevDirectory = Objects.requireNonNull(prevDirectory);
        }

        public Config withPrevDirectory(File prevDirectory) {
            return new Config(Optional.of(prevDirectory));
        }

        public static Config empty() {
            return new Config(Optional.empty());
        }
    }
}
