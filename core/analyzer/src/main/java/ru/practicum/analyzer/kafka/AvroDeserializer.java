package ru.practicum.analyzer.kafka;

import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.serialization.Deserializer;

import java.lang.reflect.Constructor;
import java.util.Map;

public class AvroDeserializer<T extends SpecificRecordBase> implements Deserializer<T> {

    private final Class<T> targetClass;

    public AvroDeserializer(Class<T> targetClass) {
        this.targetClass = targetClass;
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null || data.length == 0) return null;

        try {
            Constructor<T> c = targetClass.getDeclaredConstructor();
            c.setAccessible(true);
            T empty = c.newInstance();

            BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(data, null);
            SpecificDatumReader<T> reader = new SpecificDatumReader<>(empty.getSchema());
            return reader.read(null, decoder);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize Avro message for " + targetClass.getName(), e);
        }
    }

    @Override
    public void close() {
    }
}
