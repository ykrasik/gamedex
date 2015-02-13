package com.github.ykrasik.gamedex.core.dialog.choice;

import com.github.ykrasik.gamedex.datamodel.provider.GameInfo;
import com.github.ykrasik.opt.Opt;

/**
 * @author Yevgeny Krasik
 */
public class ExcludeDialogChoice implements DialogChoice {
    private static final ExcludeDialogChoice INSTANCE = new ExcludeDialogChoice();

    public static ExcludeDialogChoice instance() {
        return INSTANCE;
    }

    private ExcludeDialogChoice() { }

    @Override
    public Opt<GameInfo> resolve(DialogChoiceResolver resolver) {
        return resolver.exclude();
    }
}
