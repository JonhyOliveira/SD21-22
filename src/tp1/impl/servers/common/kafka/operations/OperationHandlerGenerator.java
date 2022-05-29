package tp1.impl.servers.common.kafka.operations;

import java.util.function.Consumer;

public interface OperationHandlerGenerator {

    OperationHandler generateOperationHandler(Consumer<String> handler);

}
