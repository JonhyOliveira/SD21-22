package tp1.impl.servers.common.replication;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public interface DirectoryOperation extends IOperation {

    String NAMESPACE = "DIRECTORY_OPS";

    default ProducerRecord<String, String> toRecord() {
        return IOperation.super.toRecord(NAMESPACE);
    }

    enum OperationType {
        UPDATE_FILE("uf"),
        DELETE_USER("du");

        private final String alias;

        OperationType(String alias) {
            this.alias = alias;
        }

        public Operation toOperation(Version version, String data) {
            return new Operation(this, version, data);
        }

        public String alias() {
            return alias;
        }

        private static final Map<String, OperationType> aliasMapping = Arrays.stream(values())
                .collect(Collectors.toUnmodifiableMap(OperationType::alias, operation -> operation));

        public static OperationType fromAlias(String alias) {
            if (aliasMapping.containsKey(alias))
                return aliasMapping.get(alias);
            else
                throw new IllegalArgumentException("Alias not found");
        }

    }

    record Operation(OperationType operationType, Version version, String data) implements DirectoryOperation {

        public static Operation fromRecord(ConsumerRecord<String, String> r) {
            if (r.topic().equals(NAMESPACE)) {
                Long recordVersion = Long.getLong(new String(r.headers().lastHeader(VERSION_HEADER).value(), IOperation.HEADER_CHARSET));
                String recordReplicaID = new String(r.headers().lastHeader(REPLICA_HEADER).value(), IOperation.HEADER_CHARSET);
                String recordData = r.value();

                return new Operation(OperationType.fromAlias(r.key()), new Version(recordVersion != null ? recordVersion : -1L, recordReplicaID), recordData);
            }

            return null;
        }


        @Override
        public String key() {
            return operationType.alias();
        }
    }

}


