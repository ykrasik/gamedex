package com.github.ykrasik.gamedex.core.config;

import com.github.ykrasik.gamedex.common.service.AbstractService;
import com.github.ykrasik.gamedex.core.config.type.GameSort;
import com.github.ykrasik.gamedex.core.config.type.GameWallImageDisplay;
import com.github.ykrasik.opt.Opt;
import com.thoughtworks.xstream.XStream;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Accessors;
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
@Accessors(fluent = true)
public class ConfigServiceImpl extends AbstractService implements ConfigService {
    private static final String NAME = "config.xml";
    private static final XStream XSTREAM = new XStream();

    private Path file;

    @Getter private final ObjectProperty<Opt<Path>> prevDirectoryProperty = new SimpleObjectProperty<>(Opt.absent());
    @Getter private final BooleanProperty autoSkipProperty = new SimpleBooleanProperty(false);
    @Getter private final BooleanProperty showLogProperty = new SimpleBooleanProperty(true);
    @Getter private final ObjectProperty<GameWallImageDisplay> gameWallImageDisplayProperty = new SimpleObjectProperty<>(GameWallImageDisplay.FIT);
    @Getter private final ObjectProperty<GameSort> gameSortProperty = new SimpleObjectProperty<>(GameSort.NAME_ASC);

    @Override
    protected void doStart() throws Exception {
        this.file = getFile();
        final String fileContent = IO.read(file);
        if (!fileContent.isEmpty()) {
            final Config config = (Config) XSTREAM.fromXML(fileContent);
            loadProperties(config);
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
    protected void doStop() throws Exception {
        flushConfig();
    }

    private void loadProperties(Config config) {
        prevDirectoryProperty.set(config.prevDirectory);
        autoSkipProperty.set(config.autoSkip);
        showLogProperty.set(config.showLog);
        gameWallImageDisplayProperty.set(config.gameWallImageDisplay);
        gameSortProperty.set(config.gameSort);
    }

    private void flushConfig() {
        final Config config = Config.builder()
            .prevDirectory(prevDirectoryProperty.get())
            .showLog(showLogProperty.get())
            .gameWallImageDisplay(gameWallImageDisplayProperty.get())
            .gameSort(gameSortProperty.get())
            .build();
        IO.write(file, XSTREAM.toXML(config));
    }

    @Value
    @Builder
    private static class Config {
        @NonNull private final Opt<Path> prevDirectory;
        @NonNull private final boolean autoSkip;
        @NonNull private final boolean showLog;
        @NonNull private final GameWallImageDisplay gameWallImageDisplay;
        @NonNull private final GameSort gameSort;
    }
}
