package com.github.ykrasik.gamedex.core.dialog.choice;

import com.github.ykrasik.gamedex.datamodel.info.GameInfo;

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
