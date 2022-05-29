package tp1.impl.servers.common.kafka.operations;

import org.apache.kafka.clients.producer.ProducerRecord;

public interface OperationGenerator {

    ProducerRecord<String, String> generateOperation(String value);

}
