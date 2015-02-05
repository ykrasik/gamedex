package com.github.ykrasik.indexter.games.manager.flow.choice.type;

import java.util.Objects;

/**
 * @author Yevgeny Krasik
 */
public class ChoiceData extends Choice {
    private final Object choice;

    public ChoiceData(ChoiceType type, Object choice) {
        super(type);
        this.choice = Objects.requireNonNull(choice);
    }

    public Object getChoice() {
        return choice;
    }

    @Override
    public String toString() {
        return choice.toString();
    }
}
