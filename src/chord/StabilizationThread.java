package chord;

/**
 * Thread that runs periodically to ensure that all the successors in the finger table
 * are up to date.
 */
public class StabilizationThread extends Thread {
    private final ChordNode node;

    public StabilizationThread(ChordNode node) {
        this.node = node;
    }

    @Override
    public void run() {

    }
}
