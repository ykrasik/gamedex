package com.github.ykrasik.gamedex.persistence.translator.library;

import com.github.ykrasik.gamedex.datamodel.persistence.Library;
import com.github.ykrasik.gamedex.persistence.entity.LibraryEntity;
import com.github.ykrasik.gamedex.persistence.translator.AbstractEntityTranslator;

import java.nio.file.Paths;

/**
 * @author Yevgeny Krasik
 */
public class LibraryEntityTranslatorImpl extends AbstractEntityTranslator implements LibraryEntityTranslator {
    @Override
    public Library translate(LibraryEntity entity) {
        return Library.builder()
            .id(translateId(entity.id()))
            .name(entity.name())
            .path(Paths.get(entity.path()))
            .platform(entity.platform())
            .build();
    }

    @Override
    public LibraryEntity translate(Library library) {
        return new LibraryEntity()
            .id(library.getId().getId())
            .name(library.getName())
            .path(library.getPath().toString())
            .platform(library.getPlatform());
    }
}
