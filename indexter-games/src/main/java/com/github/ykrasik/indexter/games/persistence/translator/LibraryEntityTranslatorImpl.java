package com.github.ykrasik.indexter.games.persistence.translator;

import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.persistence.Library;
import com.github.ykrasik.indexter.games.persistence.entity.LibraryEntity;
import com.github.ykrasik.indexter.id.Id;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Yevgeny Krasik
 */
public class LibraryEntityTranslatorImpl extends AbstractEntityTranslator implements LibraryEntityTranslator {
    @Override
    public Library translate(LibraryEntity entity) {
        final Id<Library> id = translateId(entity.getId());
        final String name = entity.getName();
        final Path path = Paths.get(entity.getPath());
        final GamePlatform platform = entity.getPlatform();
        return new Library(id, name, path, platform);
    }

    @Override
    public LibraryEntity translate(Library library) {
        final LibraryEntity entity = new LibraryEntity();
        entity.setId(library.getId().getId());
        entity.setName(library.getName());
        entity.setPath(library.getPath().toString());
        entity.setPlatform(library.getPlatform());
        return entity;
    }
}
