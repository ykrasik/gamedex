package com.github.ykrasik.gamedex.core.service.dialog.choice;

import com.github.ykrasik.gamedex.datamodel.provider.GameInfo;
import com.github.ykrasik.opt.Opt;

/**
 * @author Yevgeny Krasik
 */
public interface DialogChoice {
    Opt<GameInfo> resolve(DialogChoiceResolver resolver) throws Exception;
}
