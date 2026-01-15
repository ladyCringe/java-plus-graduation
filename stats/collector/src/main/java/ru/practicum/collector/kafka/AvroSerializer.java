package ru.practicum.collector.kafka;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.util.Map;

public class AvroSerializer<T extends SpecificRecordBase> implements Serializer<T> {

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public byte[] serialize(String topic, T data) {
        if (data == null) return null;

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            SpecificDatumWriter<T> writer = new SpecificDatumWriter<>(data.getSchema());
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
            writer.write(data, encoder);
            encoder.flush();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize Avro message", e);
        }
    }

    @Override
    public void close() {
    }
}
