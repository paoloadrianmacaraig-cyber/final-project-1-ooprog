import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public final class DataStore {

    public static final String FILE_NAME = "airline-data.ser";

    private DataStore() {
    }

    public static SystemData load() {
        Path file = Paths.get(FILE_NAME);
        if (!Files.exists(file)) {
            return null;
        }
        try (InputStream in = Files.newInputStream(file);
                ObjectInputStream objects = new ObjectInputStream(in)) {
            return (SystemData) objects.readObject();
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            System.out.println(ConsoleStyle.error("Saved data couldn't be read (" + e.getClass().getSimpleName()
                    + "). Starting with fresh demo data."));
            return null;
        }
    }

    public static void save(SystemData data) {
        Path file = Paths.get(FILE_NAME);
        Path temp = Paths.get(FILE_NAME + ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(temp);
                    ObjectOutputStream objects = new ObjectOutputStream(out)) {
                objects.writeObject(data);
            }
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.out.println(ConsoleStyle.error("Data couldn't be saved (" + e.getMessage() + ")."));
        } finally {

            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignored) {

            }
        }
    }
}
