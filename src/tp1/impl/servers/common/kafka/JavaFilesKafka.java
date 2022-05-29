package tp1.impl.servers.common.kafka;

import tp1.api.service.java.Result;
import tp1.api.service.java.Users;
import tp1.impl.servers.common.JavaFiles;
import tp1.impl.servers.common.kafka.operations.OperationProcessor;
import tp1.impl.servers.common.kafka.operations.UsersOperations;
import util.kafka.KafkaSubscriber;


import java.util.List;

public class JavaFilesKafka extends JavaFiles {

    private final OperationProcessor operationProcessor = new OperationProcessor();

    public JavaFilesKafka() {
        super();
        operationProcessor.registerOperationHandler(UsersOperations.DELETE
                .generateOperationHandler(userId -> super.deleteUserFiles(userId, "")));

        // TODO estas constantes deviam de estar num sitio melhor
        KafkaSubscriber.createSubscriber("kafka:9092", List.of(Users.SERVICE_NAME), "earliest")
                .start(false, operationProcessor);
    }

    @Override
    public Result<Void> deleteUserFiles(String userId, String token) {
        return Result.error(Result.ErrorCode.FORBIDDEN); // disabled, done through kafka
    }
}

