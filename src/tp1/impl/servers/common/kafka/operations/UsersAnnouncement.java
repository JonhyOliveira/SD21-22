package tp1.impl.servers.common.kafka.operations;

import org.apache.kafka.clients.producer.ProducerRecord;
import tp1.api.service.java.Users;

import java.util.function.Consumer;

public enum UsersAnnouncement implements AnnouncementHandlerGenerator, AnnouncementGenerator {

    USER_DELETED;

    public static final String NAMESPACE = Users.SERVICE_NAME;

    @Override
    public OperationHandler generateOperationHandler(Consumer<String> handler) {
        return new OperationHandler(NAMESPACE, this.name(), handler);
    }

    @Override
    public ProducerRecord<String, String> generateOperation(String args) {
        return new ProducerRecord<>(NAMESPACE, this.name(), args);
    }
}
