package com.github.ykrasik.gamedex.core.config;

import com.github.ykrasik.opt.Opt;
import com.thoughtworks.xstream.XStream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Wither;
import lombok.extern.slf4j.Slf4j;
import org.boon.IO;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Yevgeny Krasik
 */
@Slf4j
public class ConfigManagerImpl implements ConfigManager {
    private static final String NAME = "config.xml";
    private static final XStream XSTREAM = new XStream();

    private final Path file;

    private Config config;

    public ConfigManagerImpl() throws IOException {
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
    public Opt<Path> getPrevDirectory() {
        return config.prevDirectory;
    }

    @Override
    public void setPrevDirectory(Path prevDirectory) {
        config = config.withPrevDirectory(Opt.of(prevDirectory));
        updateConfig();
    }

    @Override
    public boolean isShowLog() {
        return config.showLog;
    }

    @Override
    public void setShowLog(boolean show) {
        config = config.withShowLog(show);
        updateConfig();
    }

    private void updateConfig() {
        IO.write(file, XSTREAM.toXML(config));
    }

    @RequiredArgsConstructor
    private static class Config {
        @Wither @NonNull private final Opt<Path> prevDirectory;
        @Wither private final boolean showLog;

        public static Config empty() {
            return new Config(Opt.absent(), true);
        }
    }
}
