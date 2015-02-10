package com.github.ykrasik.gamedex.core.dialog.choice;

import com.github.ykrasik.gamedex.datamodel.info.GameInfo;
import lombok.NonNull;
import lombok.Value;

import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
@Value
public class NewNameDialogChoice implements DialogChoice {
    @NonNull private final String name;

    @Override
    public Optional<GameInfo> resolve(DialogChoiceResolver resolver) throws Exception {
        return resolver.newName(name);
    }
}
