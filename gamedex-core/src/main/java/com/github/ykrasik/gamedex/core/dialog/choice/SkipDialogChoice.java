package com.github.ykrasik.gamedex.core.dialog.choice;

import com.github.ykrasik.gamedex.datamodel.provider.GameInfo;
import com.github.ykrasik.opt.Opt;

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
    public Opt<GameInfo> resolve(DialogChoiceResolver resolver) {
        return resolver.skip();
    }
}
