package tp1.impl.servers.common.kafka.operations;

import org.apache.kafka.clients.producer.ProducerRecord;
import tp1.api.service.java.Files;

import java.util.function.Consumer;

public enum FilesOperations implements AnnouncementGenerator, AnnouncementHandlerGenerator {

    FILE_DELETED,
    FILE_UPDATED;

    private static final String NAMESPACE = Files.SERVICE_NAME;

    @Override
    public OperationHandler generateOperationHandler(Consumer<String> handler) {
        return new OperationHandler(NAMESPACE, this.name(), handler);
    }

    @Override
    public ProducerRecord<String, String> generateRecord(String args) {
        return new ProducerRecord<>(NAMESPACE, this.name(), args);
    }
}
