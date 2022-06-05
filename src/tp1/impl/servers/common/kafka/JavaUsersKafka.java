package tp1.impl.servers.common.kafka;

import tp1.api.User;
import tp1.api.service.java.Result;
import tp1.impl.servers.common.JavaUsers;
import tp1.impl.servers.common.replication.DirectoryOperation;
import util.kafka.KafkaPublisher;

import java.util.logging.Logger;

public class JavaUsersKafka extends JavaUsers {

    final static Logger Log = Logger.getLogger(JavaUsersKafka.class.getName());

    protected final KafkaPublisher publisher = KafkaPublisher.createPublisher("kafka:9092");

    public JavaUsersKafka() {
        super();
    }

    @Override
    public Result<User> deleteUser(String userId, String password) {
        var r = super.deleteUser(userId, password);

        if (r.isOK())
            publisher.publish(DirectoryOperation.OperationType.DELETE_USER
                    .toOperation(null, userId).toRecord());

        return r;
    }

}
