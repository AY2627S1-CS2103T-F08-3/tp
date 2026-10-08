package seedu.address.storage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/** Saves complete JSON through a sibling temporary file and atomic replacement, with no unsafe fallback. */
public class AtomicJsonFile {
    /** An unsupported atomic move fails without changing the old file. */
    public void write(Path destination, String json) throws IOException {
        Path target = destination.toAbsolutePath();
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), ".student-save-", ".tmp");
        try {
            writeTemporary(temporary, json);
            replace(temporary, target);
        } finally {
            // Do not report a failed transaction after a successful replacement due to cleanup errors.
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException ignored) {
                // A leftover temporary file is not a published student profile.
            }
        }
    }

    protected void writeTemporary(Path temporary, String json) throws IOException {
        try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
            ByteBuffer bytes = StandardCharsets.UTF_8.encode(json);
            while (bytes.hasRemaining()) {
                channel.write(bytes);
            }
            channel.force(true);
        }
    }

    protected void replace(Path temporary, Path target) throws IOException {
        Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
}
