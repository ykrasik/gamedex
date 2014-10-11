package com.github.ykrasik.indexter.games.info;

import java.util.List;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public interface GameInfoService {
    List<GameBriefInfo> search(String name, Platform platform) throws Exception;

    // FIXME: Add type-safety, for metacritic this is name, for giantbomb this is a url.
    Optional<GameDetailedInfo> get(String moreDetailsId, Platform platform) throws Exception;
}

