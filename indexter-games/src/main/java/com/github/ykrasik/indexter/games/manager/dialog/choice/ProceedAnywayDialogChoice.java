package com.github.ykrasik.indexter.games.manager.dialog.choice;

import com.github.ykrasik.indexter.games.datamodel.info.GameInfo;

import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public class ProceedAnywayDialogChoice implements DialogChoice {
    private static final ProceedAnywayDialogChoice INSTANCE = new ProceedAnywayDialogChoice();

    public static ProceedAnywayDialogChoice instance() {
        return INSTANCE;
    }

    private ProceedAnywayDialogChoice() { }

    @Override
    public Optional<GameInfo> resolve(DialogChoiceResolver resolver) {
        return resolver.proceedAnyway();
    }
}
