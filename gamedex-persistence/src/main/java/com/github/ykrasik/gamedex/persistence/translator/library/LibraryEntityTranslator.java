package com.github.ykrasik.gamedex.persistence.translator.library;

import com.github.ykrasik.gamedex.datamodel.persistence.Library;
import com.github.ykrasik.gamedex.persistence.entity.LibraryEntity;

/**
 * @author Yevgeny Krasik
 */
public interface LibraryEntityTranslator {
    Library translate(LibraryEntity entity);

    LibraryEntity translate(Library library);
}
