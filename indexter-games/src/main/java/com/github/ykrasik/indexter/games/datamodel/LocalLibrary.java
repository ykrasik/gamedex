package com.github.ykrasik.indexter.games.datamodel;

import com.github.ykrasik.indexter.id.Id;
import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * @author Yevgeny Krasik
 */
public class LocalLibrary {
    private final Id<LocalLibrary> id;
    private final Library library;

    public LocalLibrary(Id<LocalLibrary> id, Library library) {
        this.id = Objects.requireNonNull(id);
        this.library = Objects.requireNonNull(library);
    }

    public Id<LocalLibrary> getId() {
        return id;
    }

    public Library getLibrary() {
        return library;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final LocalLibrary that = (LocalLibrary) o;

        if (!id.equals(that.id)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("id", id)
            .add("library", library)
            .toString();
    }
}
