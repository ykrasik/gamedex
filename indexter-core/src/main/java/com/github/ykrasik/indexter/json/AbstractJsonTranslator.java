package com.github.ykrasik.indexter.json;

import com.github.ykrasik.indexter.exception.IndexterException;
import org.codehaus.jackson.JsonNode;

import java.util.*;
import java.util.function.Function;

/**
 * @author Yevgeny Krasik
 */
public abstract class AbstractJsonTranslator {
    protected boolean exists(JsonNode node) {
        return node != null && !node.isNull();
    }

    protected Optional<JsonNode> getField(JsonNode node, String fieldName) {
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

    protected JsonNode getMandatoryField(JsonNode node, String fieldName) {
        return getField(node, fieldName).orElseThrow(
            () -> new IndexterException("Json has no %s!", fieldName)
        );
    }

    protected <T> Optional<T> mapField(JsonNode node, String fieldName, Function<JsonNode, T> f) {
        return getField(node, fieldName).map(f);
    }

    protected Optional<String> extractString(JsonNode node, String fieldName) {
        return mapField(node, fieldName, JsonNode::asText);
    }

    protected Optional<Integer> extractInt(JsonNode node, String fieldName) {
        return mapField(node, fieldName, JsonNode::asInt);
    }

    protected Optional<Double> extractDouble(JsonNode node, String fieldName) {
        return mapField(node, fieldName, JsonNode::asDouble);
    }

    protected <T> List<T> mapList(JsonNode root, Function<JsonNode, T> f) {
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

    protected <T> List<T> flatMapList(JsonNode root, Function<JsonNode, Optional<T>> f) {
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
