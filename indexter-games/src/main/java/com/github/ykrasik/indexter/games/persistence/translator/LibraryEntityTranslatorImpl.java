package com.github.ykrasik.indexter.games.persistence.translator;

import com.github.ykrasik.indexter.games.persistence.entity.LibraryEntity;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.Library;
import com.github.ykrasik.indexter.games.datamodel.LocalLibrary;
import com.github.ykrasik.indexter.id.Id;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Yevgeny Krasik
 */
public class LibraryEntityTranslatorImpl extends AbstractEntityTranslator implements LibraryEntityTranslator {
    @Override
    public LocalLibrary translate(LibraryEntity entity) {
        final Id<LocalLibrary> id = translateId(entity.getId());
        final String name = entity.getName();
        final Path path = Paths.get(entity.getPath());
        final GamePlatform platform = entity.getPlatform();
        final Library library = new Library(name, path, platform);
        return new LocalLibrary(id, library);
    }

    @Override
    public LibraryEntity translate(Library library) {
        final LibraryEntity entity = new LibraryEntity();
        entity.setName(library.getName());
        entity.setPath(library.getPath().toString());
        entity.setPlatform(library.getPlatform());
        return entity;
    }

    @Override
    public LibraryEntity translate(LocalLibrary library) {
        final LibraryEntity entity = translate(library.getLibrary());
        entity.setId(library.getId().getId());
        return entity;
    }
}
