package tp1.impl.servers.common.kafka.operations;

import java.util.function.Consumer;

/**
 * Specifies an operation
 * @param namespace the context of the operation name (used for disambiguation)
 * @param name the name of the operation
 * @param handler the method that handles this operation
 */
public record OperationHandler(String namespace, String name, Consumer<String> handler) {}
