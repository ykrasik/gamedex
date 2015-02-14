package com.github.ykrasik.gamedex.persistence.extractor;

import com.github.ykrasik.gamedex.datamodel.persistence.Genre;
import com.github.ykrasik.gamedex.datamodel.persistence.Id;
import com.github.ykrasik.gamedex.persistence.entity.GenreGameLinkEntity;

import java.util.List;
import java.util.Map;

/**
 * @author Yevgeny Krasik
 */
public class GenreExtractor extends AbstractLinkExtractor<Genre, GenreGameLinkEntity> {
    public GenreExtractor(Map<Id<Genre>, Genre> genreMap, Map<Integer, List<GenreGameLinkEntity>> linkMap) {
        super(genreMap, linkMap);
    }

    @Override
    protected Id<Genre> getLinkDataModelId(GenreGameLinkEntity linkEntity) {
        return new Id<>(linkEntity.getGenre().getId());
    }
}
