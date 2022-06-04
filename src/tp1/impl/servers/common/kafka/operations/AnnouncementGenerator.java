package tp1.impl.servers.common.kafka.operations;

import org.apache.kafka.clients.producer.ProducerRecord;

public interface AnnouncementGenerator {

    ProducerRecord<String, String> generateRecord(String value);

}
