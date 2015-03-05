package com.github.ykrasik.gamedex.core.javafx;

import javafx.beans.binding.Binding;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;

import java.util.function.Function;

/**
 * @author Yevgeny Krasik
 */
public class MoreBindings {
    public static <A, B> Binding<B> transformBinding(ObservableValue<A> observable, Function<A, B> function) {
        return new ObjectBinding<B>() {
            {
                super.bind(observable);
            }

            @Override
            protected B computeValue() {
                return function.apply(observable.getValue());
            }
        };
    }

    public static <T> BooleanBinding isNotEmpty(ObservableValue<ObservableList<T>> list) {
        return new BooleanBinding() {
            {
                super.bind(list);
            }

            @Override
            public void dispose() {
                super.unbind(list);
            }

            @Override
            protected boolean computeValue() {
                return !list.getValue().isEmpty();
            }
        };
    }
}
