package com.github.ykrasik.gamedex.persistence.extractor;

import com.github.ykrasik.gamedex.datamodel.persistence.Id;
import com.github.ykrasik.gamedex.datamodel.persistence.Library;
import com.github.ykrasik.gamedex.persistence.entity.LibraryGameLinkEntity;
import com.gs.collections.api.list.ImmutableList;
import com.gs.collections.api.map.ImmutableMap;
import com.gs.collections.api.multimap.ImmutableMultimap;
import com.gs.collections.api.multimap.list.ImmutableListMultimap;

/**
 * @author Yevgeny Krasik
 */
public class LibraryExtractor extends AbstractLinkExtractor<Library, LibraryGameLinkEntity> {
    public LibraryExtractor(ImmutableMap<Id<Library>, Library> libraryMap, ImmutableMultimap<Integer, LibraryGameLinkEntity> linkMap) {
        super(libraryMap, linkMap);
    }

    @Override
    protected Id<Library> getLinkDataModelId(LibraryGameLinkEntity linkEntity) {
        return new Id<>(linkEntity.library().id());
    }

    public static LibraryExtractor from(ImmutableList<Library> libraries, ImmutableList<LibraryGameLinkEntity> libraryGames) {
        final ImmutableMap<Id<Library>, Library> libraryMap = libraries.groupByUniqueKey(Library::getId);
        final ImmutableListMultimap<Integer, LibraryGameLinkEntity> libraryGameMap = libraryGames.groupBy(entity -> entity.game().id());
        return new LibraryExtractor(libraryMap, libraryGameMap);
    }
}
