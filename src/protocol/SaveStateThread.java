package protocol;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

public class SaveStateThread extends Thread {
    @Override
    public void run() {
        File stateFile = new File("peer" + Peer.id + File.separator + "state.ser");

        try {
            if (!stateFile.getParentFile().mkdirs()) {
                return;
            }

            if (stateFile.exists() && !stateFile.delete()) {
                return;
            }

            if (!stateFile.createNewFile()) {
                return;
            }

            ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(stateFile));

            outputStream.writeObject(Peer.state);
            outputStream.close();
        }
        catch (Exception ex) {
            System.err.println("Exception in SaveStateThread: " + ex.getMessage());
        }
    }
}
