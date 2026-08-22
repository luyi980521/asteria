package io.asteria.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.List;

/**
 * JSON conversion utilities based on Jackson.
 *
 * <p>The mapper is immutable after construction and can safely be shared
 * between threads.</p>
 */
public final class JsonUtils {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .findAndAddModules()
            .build();

    private JsonUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /** Returns the shared mapper for advanced Jackson use cases. */
    public static ObjectMapper mapper() {
        return OBJECT_MAPPER;
    }

    /** Serializes an object to compact JSON. */
    public static String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to serialize value as JSON", exception);
        }
    }

    /** Serializes an object to formatted JSON. */
    public static String toPrettyJson(Object value) {
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to serialize value as JSON", exception);
        }
    }

    /** Serializes an object to UTF-8 JSON bytes. */
    public static byte[] toJsonBytes(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsBytes(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to serialize value as JSON", exception);
        }
    }

    /** Deserializes JSON into a concrete class. */
    public static <T> T fromJson(String json, Class<T> targetType) {
        requireJson(json);
        requireTargetType(targetType);
        try {
            return OBJECT_MAPPER.readValue(json, targetType);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to deserialize JSON", exception);
        }
    }

    /** Deserializes JSON into a generic type, such as List<Foo>. */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        requireJson(json);
        if (typeReference == null) {
            throw new IllegalArgumentException("Target type must not be null");
        }
        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to deserialize JSON", exception);
        }
    }

    /** Deserializes JSON using a Jackson JavaType. */
    public static <T> T fromJson(String json, JavaType targetType) {
        requireJson(json);
        if (targetType == null) {
            throw new IllegalArgumentException("Target type must not be null");
        }
        try {
            return OBJECT_MAPPER.readValue(json, targetType);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to deserialize JSON", exception);
        }
    }

    /** Deserializes a JSON array into a typed list, similar to FastJSON's parseArray. */
    public static <T> List<T> parseArray(String json, Class<T> elementType) {
        requireJson(json);
        requireTargetType(elementType);
        JavaType listType = OBJECT_MAPPER.getTypeFactory()
                .constructCollectionType(List.class, elementType);
        try {
            return OBJECT_MAPPER.readValue(json, listType);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to deserialize JSON array", exception);
        }
    }

    /** Deserializes UTF-8 JSON bytes into a concrete class. */
    public static <T> T fromJson(byte[] json, Class<T> targetType) {
        if (json == null || json.length == 0) {
            throw new IllegalArgumentException("JSON bytes must not be null or empty");
        }
        requireTargetType(targetType);
        try {
            return OBJECT_MAPPER.readValue(json, targetType);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to deserialize JSON", exception);
        }
    }

    /** Parses JSON into a tree. */
    public static JsonNode readTree(String json) {
        requireJson(json);
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to parse JSON", exception);
        }
    }

    /** Converts an object to another type through Jackson's data binding. */
    public static <T> T convert(Object value, Class<T> targetType) {
        requireTargetType(targetType);
        try {
            return OBJECT_MAPPER.convertValue(value, targetType);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Failed to convert value", exception);
        }
    }

    /** Converts an object to a generic type, such as Map<String, Object>. */
    public static <T> T convert(Object value, TypeReference<T> typeReference) {
        if (typeReference == null) {
            throw new IllegalArgumentException("Target type must not be null");
        }
        try {
            return OBJECT_MAPPER.convertValue(value, typeReference);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Failed to convert value", exception);
        }
    }

    private static void requireJson(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON must not be null or blank");
        }
    }

    private static void requireTargetType(Class<?> targetType) {
        if (targetType == null) {
            throw new IllegalArgumentException("Target type must not be null");
        }
    }
}
