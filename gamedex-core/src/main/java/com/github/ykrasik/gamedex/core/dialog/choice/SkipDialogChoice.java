package com.github.ykrasik.gamedex.core.dialog.choice;

import com.github.ykrasik.gamedex.datamodel.info.GameInfo;

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
