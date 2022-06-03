package tp1.impl.servers.common.kafka.operations;

import org.apache.kafka.clients.producer.ProducerRecord;
import tp1.api.service.java.Directory;

import java.util.function.Consumer;

public enum DirectoryAnnouncements implements AnnouncementHandlerGenerator, AnnouncementGenerator {

    FILE_SHARED,
    FILE_UNSHARED,
    FILE_REMOVED,
    FILE_FOUND;

    private static final String NAMESPACE = Directory.SERVICE_NAME;

    @Override
    public OperationHandler generateOperationHandler(Consumer<String> handler) {
        return new OperationHandler(NAMESPACE, this.name(), handler);
    }

    @Override
    public ProducerRecord<String, String> generateOperation(String args) {
        return new ProducerRecord<>(NAMESPACE, this.name(), args);
    }

}
