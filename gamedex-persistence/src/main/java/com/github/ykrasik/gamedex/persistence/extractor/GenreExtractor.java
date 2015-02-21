package com.github.ykrasik.gamedex.persistence.extractor;

import com.github.ykrasik.gamedex.datamodel.persistence.Genre;
import com.github.ykrasik.gamedex.datamodel.persistence.Id;
import com.github.ykrasik.gamedex.persistence.entity.GenreGameLinkEntity;
import com.gs.collections.api.list.ImmutableList;
import com.gs.collections.api.map.ImmutableMap;
import com.gs.collections.api.multimap.ImmutableMultimap;
import com.gs.collections.api.multimap.list.ImmutableListMultimap;

/**
 * @author Yevgeny Krasik
 */
public class GenreExtractor extends AbstractLinkExtractor<Genre, GenreGameLinkEntity> {
    public GenreExtractor(ImmutableMap<Id<Genre>, Genre> genreMap, ImmutableMultimap<Integer, GenreGameLinkEntity> linkMap) {
        super(genreMap, linkMap);
    }

    @Override
    protected Id<Genre> getLinkDataModelId(GenreGameLinkEntity linkEntity) {
        return new Id<>(linkEntity.genre().id());
    }

    public static GenreExtractor from(ImmutableList<Genre> genres, ImmutableList<GenreGameLinkEntity> genreGames) {
        final ImmutableMap<Id<Genre>, Genre> genreMap = genres.groupByUniqueKey(Genre::getId);
        final ImmutableListMultimap<Integer, GenreGameLinkEntity> genreGameMap = genreGames.groupBy(entity -> entity.game().id());
        return new GenreExtractor(genreMap, genreGameMap);
    }
}
