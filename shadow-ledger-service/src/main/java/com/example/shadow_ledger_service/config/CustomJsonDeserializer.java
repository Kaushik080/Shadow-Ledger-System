package com.example.shadow_ledger_service.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Custom JSON deserializer to replace the deprecated Spring Kafka JsonDeserializer
 */
public class CustomJsonDeserializer<T> implements Deserializer<T> {

    private static final Logger log = LoggerFactory.getLogger(CustomJsonDeserializer.class);

    private final ObjectMapper objectMapper;
    private Class<T> targetType;

    public CustomJsonDeserializer() {
        this.objectMapper = createObjectMapper();
    }

    public CustomJsonDeserializer(Class<T> targetType) {
        this.targetType = targetType;
        this.objectMapper = createObjectMapper();
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.findAndRegisterModules(); // This will auto-register JavaTimeModule if available
        return mapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void configure(Map<String, ?> configs, boolean isKey) {
        if (configs.containsKey("value.deserializer.type")) {
            try {
                this.targetType = (Class<T>) Class.forName((String) configs.get("value.deserializer.type"));
            } catch (ClassNotFoundException e) {
                throw new SerializationException("Failed to configure deserializer", e);
            }
        }
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null || data.length == 0) {
            log.debug("Received null or empty data for topic: {}", topic);
            return null;
        }
        try {
            String jsonString = new String(data, StandardCharsets.UTF_8);
            log.debug("Deserializing from topic {}: {}", topic, jsonString);
            T result = objectMapper.readValue(data, targetType);
            log.debug("Successfully deserialized object of type: {}", targetType.getName());
            return result;
        } catch (Exception e) {
            log.error("Failed to deserialize JSON from topic {}: {}", topic, new String(data, StandardCharsets.UTF_8), e);
            throw new SerializationException("Failed to deserialize JSON", e);
        }
    }

    @Override
    public T deserialize(String topic, Headers headers, byte[] data) {
        return deserialize(topic, data);
    }

    @Override
    public void close() {
        // No resources to close
    }
}

