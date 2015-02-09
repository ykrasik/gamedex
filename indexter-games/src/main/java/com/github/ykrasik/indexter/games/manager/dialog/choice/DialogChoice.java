package com.github.ykrasik.indexter.games.manager.dialog.choice;

import com.github.ykrasik.indexter.games.datamodel.info.GameInfo;

import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public interface DialogChoice {
    Optional<GameInfo> resolve(DialogChoiceResolver resolver) throws Exception;
}
