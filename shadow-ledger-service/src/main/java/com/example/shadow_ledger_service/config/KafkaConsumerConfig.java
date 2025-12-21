package com.example.shadow_ledger_service.config;

import com.example.shadow_ledger_service.model.LedgerEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, LedgerEvent> consumerFactory() {
        // Create custom JSON deserializer
        CustomJsonDeserializer<LedgerEvent> jsonDeserializer = new CustomJsonDeserializer<>(LedgerEvent.class);

        // Wrap with ErrorHandlingDeserializer for resilience
        ErrorHandlingDeserializer<LedgerEvent> errorHandlingDeserializer =
            new ErrorHandlingDeserializer<>(jsonDeserializer);

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "shadow-ledger-consumer-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);

        return new DefaultKafkaConsumerFactory<>(
            props,
            new StringDeserializer(),
            errorHandlingDeserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, LedgerEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, LedgerEvent> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setAutoStartup(true);
        return factory;
    }
}

