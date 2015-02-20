package com.github.ykrasik.gamedex.core.config;

import com.github.ykrasik.opt.Opt;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.boon.IO;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Yevgeny Krasik
 */
// FIXME: This class should probably disappear.
public class GameCollectionConfigImpl implements GameCollectionConfig {
    private static final String NAME = "config.xml";

    private final Path file;

    private volatile Config config;

    public GameCollectionConfigImpl() throws IOException {
        this.file = getFile();
        final String fileContent = IO.read(file);
        if (!fileContent.isEmpty()) {
            this.config = new Config(Opt.of(Paths.get(fileContent)));
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
    public Opt<Path> getPrevDirectory() {
        return config.prevDirectory;
    }

    @Override
    public void setPrevDirectory(Path prevDirectory) {
        config = config.withPrevDirectory(prevDirectory);
        onConfigUpdated();
    }

    private void onConfigUpdated() {
        final Config config = this.config;
        IO.write(file, config.prevDirectory.get().toString());
    }

    @RequiredArgsConstructor
    private static class Config {
        @NonNull private final Opt<Path> prevDirectory;

        public Config withPrevDirectory(Path prevDirectory) {
            return new Config(Opt.of(prevDirectory));
        }

        public static Config empty() {
            return new Config(Opt.absent());
        }
    }
}
