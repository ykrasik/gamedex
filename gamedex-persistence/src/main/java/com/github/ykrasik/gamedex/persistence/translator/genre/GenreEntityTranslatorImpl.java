package com.github.ykrasik.gamedex.persistence.translator.genre;

import com.github.ykrasik.gamedex.datamodel.persistence.Genre;
import com.github.ykrasik.gamedex.persistence.entity.GenreEntity;
import com.github.ykrasik.gamedex.datamodel.persistence.Id;

/**
 * @author Yevgeny Krasik
 */
public class GenreEntityTranslatorImpl implements GenreEntityTranslator {
    @Override
    public Genre translate(GenreEntity entity) {
        return new Genre(new Id<>(entity.getId()), entity.getName());
    }

    @Override
    public GenreEntity translate(Genre genre) {
        final GenreEntity entity = new GenreEntity();
        entity.setId(genre.getId().getId());
        entity.setName(genre.getName());
        return entity;
    }
}
