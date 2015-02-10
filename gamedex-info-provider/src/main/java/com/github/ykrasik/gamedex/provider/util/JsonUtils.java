package com.github.ykrasik.gamedex.provider.util;

import com.github.ykrasik.gamedex.common.exception.GameDexException;
import org.codehaus.jackson.JsonNode;

import java.util.*;
import java.util.function.Function;

/**
 * @author Yevgeny Krasik
 */
public final class JsonUtils {
    private JsonUtils() {
    }

    public static boolean exists(JsonNode node) {
        return node != null && !node.isNull();
    }

    public static Optional<JsonNode> getField(JsonNode node, String fieldName) {
        if (!exists(node)) {
            return Optional.empty();
        }

        final JsonNode field = node.get(fieldName);
        if (exists(field)) {
            return Optional.of(field);
        } else {
            return Optional.empty();
        }
    }

    public static JsonNode getMandatoryField(JsonNode node, String fieldName) {
        return getField(node, fieldName).orElseThrow(
            () -> new GameDexException("Json has no '%s' field!", fieldName)
        );
    }

    public static int getMandatoryInt(JsonNode node, String fieldName) {
        return getMandatoryField(node, fieldName).asInt();
    }

    public static String getMandatoryString(JsonNode node, String fieldName) {
        return getMandatoryField(node, fieldName).asText();
    }

    public static Optional<String> getString(JsonNode node, String fieldName) {
        return mapField(node, fieldName, JsonNode::asText);
    }

    public static Optional<Integer> getInt(JsonNode node, String fieldName) {
        return mapField(node, fieldName, JsonNode::asInt);
    }

    public static Optional<Double> getDouble(JsonNode node, String fieldName) {
        return mapField(node, fieldName, JsonNode::asDouble);
    }

    private static <T> Optional<T> mapField(JsonNode node, String fieldName, Function<JsonNode, T> function) {
        return getField(node, fieldName).map(function);
    }

    public static <T> List<T> mapList(JsonNode root, Function<JsonNode, T> f) {
        if (!exists(root)) {
            return Collections.emptyList();
        }

        final List<T> list = new ArrayList<>();
        final Iterator<JsonNode> iterator = root.getElements();
        while (iterator.hasNext()) {
            final JsonNode node = iterator.next();
            if (exists(node)) {
                list.add(f.apply(node));
            }
        }
        return list;
    }

    public static <T> List<T> flatMapList(JsonNode root, Function<JsonNode, Optional<T>> f) {
        if (!exists(root)) {
            return Collections.emptyList();
        }

        final List<T> list = new ArrayList<>();
        final Iterator<JsonNode> iterator = root.getElements();
        while (iterator.hasNext()) {
            final JsonNode node = iterator.next();
            if (exists(node)) {
                final Optional<T> result = f.apply(node);
                result.ifPresent(list::add);
            }
        }
        return list;
    }
}
