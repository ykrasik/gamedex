package com.github.ykrasik.indexter.games.manager.flow.dialog.choice;

import com.github.ykrasik.indexter.games.datamodel.info.GameInfo;

import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public class SkipDialogChoice implements DialogChoice {
    private static final SkipDialogChoice INSTANCE = new SkipDialogChoice();

    public static SkipDialogChoice instance() {
        return INSTANCE;
    }

    private SkipDialogChoice() { }

    @Override
    public Optional<GameInfo> resolve(DialogChoiceResolver resolver) {
        return resolver.skip();
    }
}
