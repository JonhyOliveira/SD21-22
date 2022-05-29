package tp1.impl.servers.common.kafka.operations;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import util.kafka.RecordProcessor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class OperationProcessor implements RecordProcessor {

    private static Logger Log = Logger.getLogger(OperationProcessor.class.getName());

    private final Map<String, Map<String, Consumer<String>>> operations = new ConcurrentHashMap<>();

    public <T> void registerOperationHandler(OperationHandler operation) {
        operations.putIfAbsent(operation.namespace(), new ConcurrentHashMap<>());
        var operationsNamespace = operations.get(operation.namespace());

        if (operationsNamespace.putIfAbsent(operation.name(), operation.handler()) != null) // there was already a consumer there
            throw new OperationAlreadyExistsException();
    }

    @Override
    public void onReceive(ConsumerRecord<String, String> r) {
        var operationNamespace = operations.get(r.topic());
        Consumer<String> operation = operationNamespace.get(r.key());
        operation.accept(r.value());

    }

}
