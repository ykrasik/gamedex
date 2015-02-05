package com.github.ykrasik.indexter.games.persistence.translator;

import com.github.ykrasik.indexter.games.datamodel.persistence.Genre;
import com.github.ykrasik.indexter.games.persistence.entity.GenreEntity;
import com.github.ykrasik.indexter.id.Id;

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
