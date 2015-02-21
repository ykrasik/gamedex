package com.github.ykrasik.gamedex.persistence.extractor;

import com.github.ykrasik.gamedex.datamodel.persistence.Id;
import com.github.ykrasik.gamedex.datamodel.persistence.Library;
import com.github.ykrasik.gamedex.persistence.entity.LibraryGameLinkEntity;
import com.gs.collections.api.map.ImmutableMap;
import com.gs.collections.api.multimap.ImmutableMultimap;

/**
 * @author Yevgeny Krasik
 */
public class LibraryExtractor extends AbstractLinkExtractor<Library, LibraryGameLinkEntity> {
    public LibraryExtractor(ImmutableMap<Id<Library>, Library> libraryMap, ImmutableMultimap<Integer, LibraryGameLinkEntity> linkMap) {
        super(libraryMap, linkMap);
    }

    @Override
    protected Id<Library> getLinkDataModelId(LibraryGameLinkEntity linkEntity) {
        return new Id<>(linkEntity.getLibrary().getId());
    }
}
