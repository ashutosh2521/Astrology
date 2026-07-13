package codes.ashutoshkumar.kundli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KundliApplication {

    public static void main(String[] args) throws IOException {
        ensureDbDirectory();
        SpringApplication.run(KundliApplication.class, args);
    }

    /**
     * SQLite creates the database file on first connect, but not its parent directory.
     * Must happen before the datasource initializes, hence before run().
     */
    private static void ensureDbDirectory() throws IOException {
        String dbPath = System.getenv().getOrDefault("KUNDLI_DB_PATH", "./data/kundli.db");
        Path parent = Path.of(dbPath).toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}
