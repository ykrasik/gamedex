package com.github.ykrasik.indexter.games.manager.dialog.choice;

import com.github.ykrasik.indexter.games.datamodel.info.GameInfo;
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
