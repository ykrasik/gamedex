package com.github.ykrasik.gamedex.core.service.dialog.choice;

import com.github.ykrasik.gamedex.datamodel.provider.GameInfo;
import com.github.ykrasik.opt.Opt;
import lombok.NonNull;
import lombok.Value;

/**
 * @author Yevgeny Krasik
 */
@Value
public class NewNameDialogChoice implements DialogChoice {
    @NonNull private final String name;

    @Override
    public Opt<GameInfo> resolve(DialogChoiceResolver resolver) throws Exception {
        return resolver.newName(name);
    }
}
