package tutorconnect.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tutorconnect.commons.exceptions.DataLoadingException;

public class HourlyRatePersistenceTest {
    @TempDir
    private Path directory;

    @Test
    public void legacyTextAndNumericRates_reloadAsExactTwoDecimalStrings() throws Exception {
        for (String amount : List.of("1", "1.01", "19.99", "999.99", "1000")) {
            for (String jsonValue : List.of(amount, "\"" + amount + "\"")) {
                Path file = directory.resolve("students.json");
                Files.writeString(file, register(",\"hourlyRate\":" + jsonValue));
                JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(file);
                var loaded = storage.readStudentDirectory().orElseThrow();
                var student = loaded.getStudentList().getFirst();
                assertEquals(new BigDecimal(amount).setScale(2), student.getHourlyRate().orElseThrow().value);
                storage.saveStudentDirectory(loaded);
                String saved = Files.readString(file);
                assertEquals(1, saved.split("\"hourlyRate\"", -1).length - 1);
                assertTrue(saved.contains("\"hourlyRate\" : \"" + new BigDecimal(amount).setScale(2) + "\""));
                assertTrue(saved.contains("\"persons\""));
                assertFalse(saved.contains("\"students\""));
                var reloaded = storage.readStudentDirectory().orElseThrow().getStudentList().getFirst();
                assertEquals(student, reloaded);
                assertEquals(student.getId(), reloaded.getId());
                assertEquals("keep", reloaded.getStudentFields().display("futureField"));
            }
        }
    }

    @Test
    public void missingAndNullRates_remainUnsetAcrossRestart() throws Exception {
        for (String field : List.of("", ",\"hourlyRate\":null")) {
            Path file = directory.resolve("students.json");
            Files.writeString(file, register(field));
            JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(file);
            var loaded = storage.readStudentDirectory().orElseThrow();
            assertTrue(loaded.getStudentList().getFirst().getHourlyRate().isEmpty());
            storage.saveStudentDirectory(loaded);
            assertFalse(Files.readString(file).contains("hourlyRate"));
            assertTrue(storage.readStudentDirectory().orElseThrow().getStudentList()
                    .getFirst().getHourlyRate().isEmpty());
        }
    }

    @Test
    public void invalidRates_produceControlledLoadErrorsWithoutChangingFile() throws Exception {
        for (String value : List.of("-1", "0", "0.99", "1000.01", "40.001", "\"nonnumeric\"",
                "true", "{}", "[]", "\"\"", "\"19.99000000000000001\"")) {
            Path file = directory.resolve("invalid.json");
            Files.writeString(file, register(",\"hourlyRate\":" + value));
            byte[] before = Files.readAllBytes(file);
            assertThrows(DataLoadingException.class, ()
                -> new JsonStudentDirectoryStorage(file).readStudentDirectory(), value);
            assertArrayEquals(before, Files.readAllBytes(file));
        }
    }

    private String register(String rateField) {
        return "{\"persons\":[{\"name\":\"Alex Tan\",\"phone\":\"81234567\",\"address\":\"21 Street\","
                + "\"futureField\":\"keep\"" + rateField + "}]}";
    }
}
