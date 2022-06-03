package tp1.impl.servers.common.kafka.operations;

import java.util.function.Consumer;

public interface AnnouncementHandlerGenerator {

    OperationHandler generateOperationHandler(Consumer<String> handler);

}
