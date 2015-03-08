package com.github.ykrasik.gamedex.core.service.screen;

import com.github.ykrasik.gamedex.common.exception.RunnableThrows;
import com.github.ykrasik.gamedex.datamodel.persistence.Game;
import com.github.ykrasik.opt.Opt;

import java.util.concurrent.Callable;

/**
 * @author Yevgeny Krasik
 */
public interface ScreenService {
    void runWithBlur(RunnableThrows runnable);
    <T> T callWithBlur(Callable<T> callable);

    Opt<Game> showGameDetails(Game game);

    void showSettingsScreen();
}
