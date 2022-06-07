package tp1.impl.servers.common.replication;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public interface IOperation extends Comparable<IOperation> {

    String VERSION_HEADER = "Version";
    String REPLICA_HEADER = "ReplicaID";
    Charset HEADER_CHARSET = StandardCharsets.UTF_8;

    Version version();

    String key();

    String data();

    default ProducerRecord<String, String> toRecord(String topic) {
        var record = new ProducerRecord<>(topic, key(), data());

        record.headers().add(new RecordHeader(VERSION_HEADER, version() != null ? new byte[]{version().getVersion().byteValue()} : new byte[] { Long.valueOf(-1L).byteValue() }));
        record.headers().add(new RecordHeader(REPLICA_HEADER, version() != null ? version().getReplicaID().getBytes(HEADER_CHARSET) : "".getBytes(HEADER_CHARSET)));

        return record;
    }

    @Override
    default int compareTo(IOperation o) {
        return this.version().compareTo(o.version());
    }
}
