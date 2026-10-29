package tutorconnect.storage;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Keeps a copy of a data file that could not be loaded, so the next save cannot destroy the only copy.
 */
public final class UnreadableDataBackup {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private UnreadableDataBackup() {}

    /**
     * Copies {@code dataFile} beside itself as {@code NAME.unreadable-yyyyMMdd-HHmmss.bak} and returns the copy.
     * An existing backup is never overwritten; a numbered name is chosen instead.
     *
     * @throws IOException if the copy cannot be created.
     */
    public static Path copyAside(Path dataFile, Clock clock) throws IOException {
        requireNonNull(dataFile);
        requireNonNull(clock);
        Path folder = dataFile.toAbsolutePath().getParent();
        String base = dataFile.getFileName() + ".unreadable-" + LocalDateTime.now(clock).format(STAMP);
        Path backup = folder.resolve(base + ".bak");
        for (int copy = 2; Files.exists(backup); copy++) {
            backup = folder.resolve(base + "-" + copy + ".bak");
        }
        return Files.copy(dataFile, backup);
    }
}
