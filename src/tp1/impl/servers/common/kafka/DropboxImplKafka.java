package tp1.impl.servers.common.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import tp1.api.service.java.Result;
import tp1.api.service.java.Users;
import tp1.impl.servers.common.DropboxImpl;
import tp1.impl.servers.common.replication.DirectoryOperation;
import util.Token;
import util.kafka.KafkaSubscriber;
import util.kafka.RecordProcessor;

import java.util.List;
import java.util.logging.Logger;

public class DropboxImplKafka extends DropboxImpl implements RecordProcessor {

    final static Logger Log = Logger.getLogger(JavaFilesKafka.class.getName());

    public DropboxImplKafka() {
        super();
        KafkaSubscriber.createSubscriber("kafka:9092", List.of(Users.SERVICE_NAME), "earliest")
                .start(false, this);
    }

    @Override
    public Result<Void> deleteUserFiles(String userId, String token) {
        return Result.error(Result.ErrorCode.FORBIDDEN, "Disabled"); // disabled, done through kafka
    }

    @Override
    public void onReceive(ConsumerRecord<String, String> r) {
        Log.info("GCFN %s".formatted(r));
        var op = DirectoryOperation.Operation.fromRecord(r);

        if (op.operationType().equals(DirectoryOperation.OperationType.DELETE_USER)) {
            Log.info(String.format("User %s was deleted. Clearing files...", op.data()));
            super.deleteUserFiles(op.data(), Token.get());
        }

    }
}
