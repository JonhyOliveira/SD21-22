package tp1.impl.servers.common.kafka;

import tp1.api.service.java.Result;
import tp1.impl.servers.common.JavaDirectory;

public class JavaDirectoryKafka extends JavaDirectory {

    public JavaDirectoryKafka() {
        super();
    }

    @Override
    public Result<Void> deleteUserFiles(String userId, String password, String token) {
        return Result.error(Result.ErrorCode.FORBIDDEN); // disabled, done through kafka
    }
}
