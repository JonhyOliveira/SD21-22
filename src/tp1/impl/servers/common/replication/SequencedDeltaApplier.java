package tp1.impl.servers.common.replication;

import tp1.impl.servers.common.JavaDirectorySynchronizer;

import java.util.PriorityQueue;
import java.util.logging.Logger;

public abstract class SequencedDeltaApplier implements Runnable {

    protected static final Logger Log = Logger.getLogger(SequencedDeltaApplier.class.getName());

    protected static final int WAIT_PERIOD = 100;

    protected final PriorityQueue<SequencedFileDelta> toExecute = new PriorityQueue<>();

    private final Thread thread;

    protected SequencedDeltaApplier() {
        thread = new Thread(this);
    }

    @Override
    public void run() {
        for (;;) {
            SequencedFileDelta deltaToExecute = toExecute.poll();
            if (deltaToExecute == null) {
                try {
                    Thread.sleep(WAIT_PERIOD);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return; // stop execution
                }

                continue;
            }


            if (!apply(deltaToExecute))
                return;

            Log.fine("Done. Next delta: %s".formatted(toExecute.peek()));
        }
    }

    public void start() {
        thread.start();
    }

    public void interrupt() {
        thread.interrupt();
    }

    /**
     * Submit a delta to be applied
     * @param deltaToApply the delta to be applied
     */
    public synchronized void submit(SequencedFileDelta deltaToApply) {
        this.toExecute.add(deltaToApply);
        this.notify();
    }

    /**
     * Applies the delta
     * @param delta delta to apply
     * @return whether we should continue applying deltas, if not the passed delta will be put back into the queue
     * and the Thread will wait until it is asked to proceed
     */
    protected abstract boolean apply(SequencedFileDelta delta);

}
