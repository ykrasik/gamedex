package com.github.ykrasik.gamedex.persistence.translator.genre;

import com.github.ykrasik.gamedex.datamodel.persistence.Genre;
import com.github.ykrasik.gamedex.persistence.entity.GenreEntity;

/**
 * @author Yevgeny Krasik
 */
public interface GenreEntityTranslator {
    Genre translate(GenreEntity entity);

    GenreEntity translate(Genre genre);
}
