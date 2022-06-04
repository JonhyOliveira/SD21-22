package tp1.impl.servers.common.kafka.operations;

import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.function.Consumer;

public enum GeneralAnnouncements implements AnnouncementGenerator, AnnouncementHandlerGenerator {
    AM_UP;

    private static final String NAMESPACE = "General";

    @Override
    public OperationHandler generateOperationHandler(Consumer<String> handler) {
        return new OperationHandler(NAMESPACE, this.name(), handler);
    }

    @Override
    public ProducerRecord<String, String> generateRecord(String args) {
        return new ProducerRecord<>(NAMESPACE, this.name(), args);
    }
}
