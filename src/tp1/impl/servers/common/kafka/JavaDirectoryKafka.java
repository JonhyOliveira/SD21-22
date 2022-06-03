package tp1.impl.servers.common.kafka;

import tp1.api.service.java.Result;
import tp1.impl.servers.common.JavaDirectory;
import tp1.impl.servers.common.kafka.operations.OperationProcessor;
import tp1.impl.servers.common.kafka.operations.UsersAnnouncement;
import util.kafka.KafkaSubscriber;

import java.util.List;
import java.util.logging.Logger;

public class JavaDirectoryKafka extends JavaDirectory {

    final static Logger Log = Logger.getLogger(JavaDirectoryKafka.class.getName());

    protected final OperationProcessor operationProcessor = new OperationProcessor();

    public JavaDirectoryKafka() {
        super();




            operationProcessor.registerOperationHandler(UsersAnnouncement.USER_DELETED.generateOperationHandler(userId -> {
                Log.fine(String.format("User %s deleted, updating cache..", userId));
                super.deleteUserFiles(userId, "", "");
            }));

            KafkaSubscriber.createSubscriber("kafka:9092", List.of(UsersAnnouncement.NAMESPACE), "earliest")
                    .start(false, operationProcessor);

    }



    @Override
    public Result<Void> deleteUserFiles(String userId, String password, String token) {
        return Result.error(Result.ErrorCode.FORBIDDEN, "Disabled"); // disabled, done through kafka
    }
}
