package com.github.ykrasik.gamedex.core.service.screen.search;

import com.github.ykrasik.gamedex.datamodel.provider.SearchResult;
import com.github.ykrasik.opt.Opt;
import lombok.*;
import lombok.experimental.Accessors;

/**
 * @author Yevgeny Krasik
 */
@Value
@Accessors(fluent = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class GameSearchChoice {
    @NonNull private final GameSearchChoiceType type;
    @NonNull private final Opt<SearchResult> searchResult;
    @NonNull private final Opt<String> newName;

    @Getter private static final GameSearchChoice skip = noReturnChoice(GameSearchChoiceType.SKIP);
    @Getter private static final GameSearchChoice exclude = noReturnChoice(GameSearchChoiceType.EXCLUDE);
    @Getter private static final GameSearchChoice proceedAnyway = noReturnChoice(GameSearchChoiceType.PROCEED_ANYWAY);

    private static GameSearchChoice noReturnChoice(GameSearchChoiceType type) {
        return new GameSearchChoice(type, Opt.absent(), Opt.absent());
    }

    public static GameSearchChoice select(SearchResult searchResult) {
        return new GameSearchChoice(GameSearchChoiceType.SELECT_RESULT, Opt.of(searchResult), Opt.absent());
    }

    public static GameSearchChoice newName(String newName) {
        return new GameSearchChoice(GameSearchChoiceType.NEW_NAME, Opt.absent(), Opt.of(newName));
    }
}
