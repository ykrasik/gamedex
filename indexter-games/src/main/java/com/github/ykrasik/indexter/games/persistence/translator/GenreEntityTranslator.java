package com.github.ykrasik.indexter.games.persistence.translator;

import com.github.ykrasik.indexter.games.datamodel.persistence.Genre;
import com.github.ykrasik.indexter.games.persistence.entity.GenreEntity;

/**
 * @author Yevgeny Krasik
 */
public interface GenreEntityTranslator {
    Genre translate(GenreEntity entity);

    GenreEntity translate(Genre genre);
}
