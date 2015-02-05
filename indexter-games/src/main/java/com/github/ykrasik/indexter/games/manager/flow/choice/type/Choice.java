package com.github.ykrasik.indexter.games.manager.flow.choice.type;

import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * @author Yevgeny Krasik
 */
public class Choice {
    public static final Choice SKIP = new Choice(ChoiceType.SKIP);
    public static final Choice EXCLUDE = new Choice(ChoiceType.EXCLUDE);
    public static final Choice PROCEED_ANYWAY = new Choice(ChoiceType.PROCEED_ANYWAY);

    private final ChoiceType type;

    public Choice(ChoiceType type) {
        this.type = Objects.requireNonNull(type);
    }

    public ChoiceType getType() {
        return type;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("type", type)
            .toString();
    }

}
