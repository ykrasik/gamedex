package com.github.ykrasik.gamedex.core.dialog.choice;

import com.github.ykrasik.gamedex.datamodel.info.GameInfo;

import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public interface DialogChoice {
    Optional<GameInfo> resolve(DialogChoiceResolver resolver) throws Exception;
}
