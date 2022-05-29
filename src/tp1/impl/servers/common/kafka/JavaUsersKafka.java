package tp1.impl.servers.common.kafka;

import tp1.api.User;
import tp1.api.service.java.Result;
import tp1.impl.servers.common.JavaUsers;

public class JavaUsersKafka extends JavaUsers {

    public JavaUsersKafka() {
        super();
    }

    @Override
    public Result<User> deleteUser(String userId, String password) {
        return super.deleteUser(userId, password);
    }
}
