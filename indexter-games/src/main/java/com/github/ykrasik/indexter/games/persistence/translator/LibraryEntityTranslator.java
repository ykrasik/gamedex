package com.github.ykrasik.indexter.games.persistence.translator;

import com.github.ykrasik.indexter.games.persistence.entity.LibraryEntity;
import com.github.ykrasik.indexter.games.datamodel.Library;
import com.github.ykrasik.indexter.games.datamodel.LocalLibrary;

/**
 * @author Yevgeny Krasik
 */
public interface LibraryEntityTranslator {
    LocalLibrary translate(LibraryEntity entity);

    LibraryEntity translate(Library library);
    LibraryEntity translate(LocalLibrary library);
}
