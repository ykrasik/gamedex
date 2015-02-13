package com.github.ykrasik.gamedex.core.dialog.choice;

import com.github.ykrasik.gamedex.datamodel.provider.GameInfo;
import com.github.ykrasik.opt.Opt;

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
    public Opt<GameInfo> resolve(DialogChoiceResolver resolver) {
        return resolver.proceedAnyway();
    }
}
