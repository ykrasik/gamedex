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
        return new Genre(new Id<>(entity.id()), entity.name());
    }

    @Override
    public GenreEntity translate(Genre genre) {
        return new GenreEntity()
            .id(genre.getId().getId())
            .name(genre.getName());
    }
}
