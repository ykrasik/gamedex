//package com.github.ykrasik.gamedex.provider.util;
//
//import com.github.ykrasik.gamedex.common.exception.GameDexException;
//import com.github.ykrasik.yava.option.Opt;
//import com.gs.collections.api.list.ImmutableList;
//import com.gs.collections.api.list.MutableList;
//import com.gs.collections.impl.factory.Lists;
//import org.codehaus.jackson.JsonNode;
//
//import java.util.Iterator;
//import java.util.function.Function;
//
///**
// * @author Yevgeny Krasik
// */
//public final class JsonUtils {
//    private JsonUtils() {
//    }
//
//    public static boolean exists(JsonNode node) {
//        return node != null && !node.isNull();
//    }
//
//    public static Opt<JsonNode> getField(JsonNode node, String fieldName) {
//        if (!exists(node)) {
//            return Opt.none();
//        }
//
//        final JsonNode field = node.get(fieldName);
//        if (exists(field)) {
//            return Opt.some(field);
//        } else {
//            return Opt.none();
//        }
//    }
//
//    public static JsonNode getMandatoryField(JsonNode node, String fieldName) {
//        return getField(node, fieldName).getOrElseThrow(
//            () -> new GameDexException("Json has no '%s' field!", fieldName)
//        );
//    }
//
//    public static int getMandatoryInt(JsonNode node, String fieldName) {
//        return getMandatoryField(node, fieldName).asInt();
//    }
//
//    public static String getMandatoryString(JsonNode node, String fieldName) {
//        return getMandatoryField(node, fieldName).asText();
//    }
//
//    public static Opt<String> getString(JsonNode node, String fieldName) {
//        return mapField(node, fieldName, JsonNode::asText);
//    }
//
//    public static Opt<Integer> getInt(JsonNode node, String fieldName) {
//        return mapField(node, fieldName, JsonNode::asInt);
//    }
//
//    public static Opt<Double> getDouble(JsonNode node, String fieldName) {
//        return mapField(node, fieldName, JsonNode::asDouble);
//    }
//
//    private static <T> Opt<T> mapField(JsonNode node, String fieldName, Function<JsonNode, T> function) {
//        return getField(node, fieldName).map(function);
//    }
//
//    public static <T> ImmutableList<T> mapList(JsonNode root, Function<JsonNode, T> f) {
//        if (!exists(root)) {
//            return Lists.immutable.of();
//        }
//
//        final MutableList<T> list = Lists.mutable.of();
//        final Iterator<JsonNode> iterator = root.getElements();
//        while (iterator.hasNext()) {
//            final JsonNode node = iterator.next();
//            if (exists(node)) {
//                list.add(f.apply(node));
//            }
//        }
//        return list.toImmutable();
//    }
//
//    public static <T> ImmutableList<T> flatMapList(JsonNode root, Function<JsonNode, Opt<T>> f) {
//        if (!exists(root)) {
//            return Lists.immutable.of();
//        }
//
//        final MutableList<T> list = Lists.mutable.of();
//        final Iterator<JsonNode> iterator = root.getElements();
//        while (iterator.hasNext()) {
//            final JsonNode node = iterator.next();
//            if (exists(node)) {
//                final Opt<T> result = f.apply(node);
//                result.ifDefined(list::add);
//            }
//        }
//        return list.toImmutable();
//    }
//}
