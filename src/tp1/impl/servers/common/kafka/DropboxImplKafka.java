package tp1.impl.servers.common.kafka;

import tp1.api.service.java.Result;
import tp1.api.service.java.Users;
import tp1.impl.servers.common.DropboxImpl;
import tp1.impl.servers.common.kafka.operations.OperationProcessor;
import tp1.impl.servers.common.kafka.operations.UsersAnnouncement;
import util.kafka.KafkaSubscriber;

import java.util.List;
import java.util.logging.Logger;

public class DropboxImplKafka extends DropboxImpl {

    final static Logger Log = Logger.getLogger(JavaFilesKafka.class.getName());

    private final OperationProcessor operationProcessor = new OperationProcessor();

    public DropboxImplKafka() {
        super();
        operationProcessor.registerOperationHandler(UsersAnnouncement.USER_DELETED
                .generateOperationHandler(userId -> {
                    Log.info(String.format("User %s was deleted, files cleared.", userId));
                    super.deleteUserFiles(userId, "");
                }));

        KafkaSubscriber.createSubscriber("kafka:9092", List.of(Users.SERVICE_NAME), "earliest")
                .start(false, operationProcessor);
    }

    @Override
    public Result<Void> deleteUserFiles(String userId, String token) {
        return Result.error(Result.ErrorCode.FORBIDDEN, "Disabled"); // disabled, done through kafka
    }

}
