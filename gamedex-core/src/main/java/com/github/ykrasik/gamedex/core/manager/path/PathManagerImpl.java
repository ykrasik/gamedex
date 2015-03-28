package com.github.ykrasik.gamedex.core.manager.path;

import com.github.ykrasik.gamedex.common.util.FileUtils;
import com.github.ykrasik.gamedex.core.manager.exclude.ExcludedPathManager;
import com.github.ykrasik.gamedex.core.manager.game.GameManager;
import com.github.ykrasik.gamedex.core.manager.library.LibraryManager;
import com.github.ykrasik.gamedex.core.manager.path.ProcessPathReturnValue.Type;
import com.github.ykrasik.gamedex.core.manager.provider.GameInfoProviderManager;
import com.github.ykrasik.gamedex.core.manager.provider.SearchContext;
import com.github.ykrasik.gamedex.core.service.action.ExcludeException;
import com.github.ykrasik.gamedex.core.service.action.SkipException;
import com.github.ykrasik.gamedex.core.service.config.ConfigService;
import com.github.ykrasik.gamedex.core.service.dialog.DialogService;
import com.github.ykrasik.gamedex.core.ui.library.LibraryDef;
import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.github.ykrasik.gamedex.datamodel.flow.LibraryHierarchy;
import com.github.ykrasik.gamedex.datamodel.persistence.Game;
import com.github.ykrasik.gamedex.datamodel.persistence.Library;
import com.github.ykrasik.gamedex.datamodel.provider.GameInfo;
import com.github.ykrasik.gamedex.datamodel.provider.UnifiedGameInfo;
import com.github.ykrasik.opt.Opt;
import com.gs.collections.api.list.ImmutableList;
import javafx.beans.property.*;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * @author Yevgeny Krasik
 */
@Slf4j
@Accessors(fluent = true)
@RequiredArgsConstructor
public class PathManagerImpl implements PathManager {
    private static final Pattern META_DATA_PATTERN = Pattern.compile("(\\[.*?\\])|(-)");
    private static final Pattern SPACE_PATTERN = Pattern.compile("\\s+");

    private static final ProcessPathReturnValue OK = new ProcessPathReturnValue(Type.OK, Opt.absent());
    private static final ProcessPathReturnValue NOT_OK = new ProcessPathReturnValue(Type.NOT_OK, Opt.absent());
    private static final ProcessPathReturnValue SKIP = new ProcessPathReturnValue(Type.SKIP, Opt.absent());
    private static final ProcessPathReturnValue EXCLUDE = new ProcessPathReturnValue(Type.EXCLUDE, Opt.absent());

    @Getter private final StringProperty messageProperty = new SimpleStringProperty();
    private final BooleanProperty fetchingProperty = new SimpleBooleanProperty();

    @NonNull private final ConfigService configService;
    @NonNull private final DialogService dialogService;

    @NonNull private final GameManager gameManager;
    @NonNull private final LibraryManager libraryManager;
    @NonNull private final ExcludedPathManager excludedPathManager;

    @NonNull private final GameInfoProviderManager metacriticManager;
    @NonNull private final GameInfoProviderManager giantBombManager;

    @Override
    public ReadOnlyBooleanProperty fetchingProperty() {
        return fetchingProperty;
    }

    @Override
    public ProcessPathReturnValue processPath(LibraryHierarchy libraryHierarchy, Path path) throws Exception {
        if (gameManager.isGame(path)) {
            log.info("{} is already mapped, skipping...", path);
            return NOT_OK;
        }

        if (libraryManager.isLibrary(path)) {
            log.info("{} is a library, skipping...", path);
            return NOT_OK;
        }

        if (excludedPathManager.isExcluded(path)) {
            log.info("{} is excluded, skipping...", path);
            return NOT_OK;
        }

        if (!configService.isAutoSkip()) {
            final Opt<LibraryDef> libraryDef = tryCreateLibrary(path, libraryHierarchy);
            if (libraryDef.isPresent()) {
                final Library library = libraryManager.createLibrary(libraryDef.get());
                message("New library created: '%s'", library.getName());
                return new ProcessPathReturnValue(Type.NEW_LIBRARY, Opt.of(library));
            }
        }

        message("Processing: %s...", path);
        final String name = normalizeName(path);
        try {
            return doProcessPath(libraryHierarchy, path, name);
        } catch (SkipException e) {
            message("Skipping...");
            return SKIP;
        } catch (ExcludeException e) {
            message("Excluding...");
            excludedPathManager.addExcludedPath(path);
            return EXCLUDE;
        } finally {
            message("Finished processing %s.\n", path);
        }
    }

    private Opt<LibraryDef> tryCreateLibrary(Path path, LibraryHierarchy libraryHierarchy) throws Exception {
        assertNotStopped();

        if (!FileUtils.hasChildDirectories(path) || FileUtils.hasChildFiles(path)) {
            // Only directories that have sub-directories and no files can be libraries.
            return Opt.absent();
        }

        final ImmutableList<Path> children = FileUtils.listChildDirectories(path);
        return dialogService.createLibraryDialog(path, children, libraryHierarchy.getPlatform());
    }

    private String normalizeName(Path path) {
        // Remove all metaData enclosed with '[]' from the game name.
        final String rawName = path.getFileName().toString();
        final String nameWithoutMetadata = META_DATA_PATTERN.matcher(rawName).replaceAll("");
        return SPACE_PATTERN.matcher(nameWithoutMetadata).replaceAll(" ");
    }

    private ProcessPathReturnValue doProcessPath(LibraryHierarchy libraryHierarchy, Path path, String name) throws Exception {
        if (name.isEmpty()) {
            message("Empty name provided.");
            throw new SkipException();
        }

        final GamePlatform platform = libraryHierarchy.getPlatform();

        final SearchContext searchContext = new SearchContext(path, platform);
        final Opt<GameInfo> metacriticGameOpt = fetchGameInfo(metacriticManager, name, searchContext);
        if (metacriticGameOpt.isEmpty()) {
            message("Game not found on Metacritic.");
            throw new SkipException();
        }

        final GameInfo metacriticGame = metacriticGameOpt.get();
        log.debug("Metacritic gameInfo: {}", metacriticGame);

        final String metacriticName = metacriticGame.getName();
        final Opt<GameInfo> giantBombGameOpt = fetchGameInfo(giantBombManager, metacriticName, searchContext);
        if (!giantBombGameOpt.isPresent()) {
            message("Game not found on GiantBomb.");
        }

        final UnifiedGameInfo gameInfo = UnifiedGameInfo.from(metacriticGame, giantBombGameOpt);
        final Game game = gameManager.addGame(gameInfo, path, platform);
        libraryManager.addGameToLibraryHierarchy(game, libraryHierarchy);
        return OK;
    }

    private Opt<GameInfo> fetchGameInfo(GameInfoProviderManager manager, String name, SearchContext context) throws Exception {
        assertNotStopped();

        messageProperty.bind(manager.messageProperty());
        fetchingProperty.bind(manager.fetchingProperty());
        try {
            return manager.fetchGameInfo(name, context);
        } finally {
            messageProperty.unbind();
            fetchingProperty.unbind();
        }
    }
}
