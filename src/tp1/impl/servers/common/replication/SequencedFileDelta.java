package tp1.impl.servers.common.replication;

import tp1.api.FileDelta;

import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

public record SequencedFileDelta(Version sequencingVersion, CountDownLatch done, FileDelta deltaToExecute,
                                 byte[] data, Supplier<FileDelta> effect) implements Comparable<SequencedFileDelta> {

    @Override
    public int compareTo(SequencedFileDelta o) {
        return sequencingVersion.compareTo(o.sequencingVersion);
    }

}
