package com.github.ykrasik.gamedex.persistence.extractor;

import com.github.ykrasik.gamedex.datamodel.persistence.Id;
import com.github.ykrasik.gamedex.datamodel.persistence.Library;
import com.github.ykrasik.gamedex.persistence.entity.LibraryGameLinkEntity;

import java.util.List;
import java.util.Map;

/**
 * @author Yevgeny Krasik
 */
public class LibraryExtractor extends AbstractLinkExtractor<Library, LibraryGameLinkEntity> {
    public LibraryExtractor(Map<Id<Library>, Library> libraryMap, Map<Integer, List<LibraryGameLinkEntity>> linkMap) {
        super(libraryMap, linkMap);
    }

    @Override
    protected Id<Library> getLinkDataModelId(LibraryGameLinkEntity linkEntity) {
        return new Id<>(linkEntity.getLibrary().getId());
    }
}
