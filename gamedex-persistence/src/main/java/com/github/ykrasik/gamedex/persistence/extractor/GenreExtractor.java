package com.github.ykrasik.gamedex.persistence.extractor;

import com.github.ykrasik.gamedex.datamodel.persistence.Genre;
import com.github.ykrasik.gamedex.datamodel.persistence.Id;
import com.github.ykrasik.gamedex.persistence.entity.GenreGameLinkEntity;
import com.gs.collections.api.map.ImmutableMap;
import com.gs.collections.api.multimap.ImmutableMultimap;

/**
 * @author Yevgeny Krasik
 */
public class GenreExtractor extends AbstractLinkExtractor<Genre, GenreGameLinkEntity> {
    public GenreExtractor(ImmutableMap<Id<Genre>, Genre> genreMap, ImmutableMultimap<Integer, GenreGameLinkEntity> linkMap) {
        super(genreMap, linkMap);
    }

    @Override
    protected Id<Genre> getLinkDataModelId(GenreGameLinkEntity linkEntity) {
        return new Id<>(linkEntity.getGenre().getId());
    }
}
