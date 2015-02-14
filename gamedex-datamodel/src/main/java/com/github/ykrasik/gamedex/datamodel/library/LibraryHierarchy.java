package com.github.ykrasik.gamedex.datamodel.library;

import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.github.ykrasik.gamedex.datamodel.persistence.Library;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * @author Yevgeny Krasik
 */
public class LibraryHierarchy {
    private final Deque<Library> libraries = new ArrayDeque<>();

    public LibraryHierarchy(Library library) {
        libraries.push(library);
    }

    public void pushLibrary(Library library) {
        libraries.addLast(library);
    }

    public void popLibrary() {
        if (libraries.size() == 1) {
            throw new IllegalStateException("Cannot pop the last library in the hierarchy!");
        }
        libraries.pollLast();
    }

    public Library getCurrentLibrary() {
        return Objects.requireNonNull(libraries.peekLast(), "Empty library hierarchy?!");
    }

    public Iterable<Library> getLibraries() {
        return libraries;
    }

    public GamePlatform getPlatform() {
        return getCurrentLibrary().getPlatform();
    }
}
