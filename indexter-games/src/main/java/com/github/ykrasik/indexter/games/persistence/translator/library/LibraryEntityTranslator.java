package com.github.ykrasik.indexter.games.persistence.translator.library;

import com.github.ykrasik.indexter.games.datamodel.persistence.Library;
import com.github.ykrasik.indexter.games.persistence.entity.LibraryEntity;

/**
 * @author Yevgeny Krasik
 */
public interface LibraryEntityTranslator {
    Library translate(LibraryEntity entity);

    LibraryEntity translate(Library library);
}
