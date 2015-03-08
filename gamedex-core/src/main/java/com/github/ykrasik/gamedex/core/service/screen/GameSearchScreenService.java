package com.github.ykrasik.gamedex.core.service.screen;

import com.github.ykrasik.gamedex.core.javafx.JavaFxUtils;
import com.github.ykrasik.gamedex.core.manager.info.SearchContext;
import com.github.ykrasik.gamedex.core.service.task.TaskService;
import com.github.ykrasik.gamedex.core.ui.search.GameSearchChoice;
import com.github.ykrasik.gamedex.core.ui.search.GameSearchScreen;
import com.github.ykrasik.gamedex.datamodel.provider.SearchResult;
import com.github.ykrasik.gamedex.provider.GameInfoProvider;
import com.gs.collections.api.list.ImmutableList;

import java.util.concurrent.Callable;

/**
 * @author Yevgeny Krasik
 */
public class GameSearchScreenService {
    private final ScreenService screenService;
    private final GameSearchScreen gameSearchScreen;

    public GameSearchScreenService(ScreenService screenService, TaskService taskService) {
        this.screenService = screenService;
        gameSearchScreen = JavaFxUtils.callLaterIfNecessary(() -> new GameSearchScreen(taskService));

    }

    public GameSearchChoice showGameSearchScreen(GameInfoProvider gameInfoProvider,
                                          String searchedName,
                                          SearchContext searchContext,
                                          ImmutableList<SearchResult> searchResults) {
        final Callable<GameSearchChoice> showCallable = () -> gameSearchScreen.show(gameInfoProvider, searchedName, searchContext, searchResults);
        return JavaFxUtils.callLaterIfNecessary(() -> screenService.callWithBlur(showCallable));
    }
}
