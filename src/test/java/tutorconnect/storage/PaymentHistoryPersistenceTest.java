package tutorconnect.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tutorconnect.testutil.Assert.assertThrows;
import static tutorconnect.testutil.TypicalStudents.ALICE;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import tutorconnect.commons.util.JsonUtil;
import tutorconnect.model.StudentDirectory;
import tutorconnect.model.student.PaymentHistory;
import tutorconnect.model.student.PaymentStatus;
import tutorconnect.model.student.Student;

public class PaymentHistoryPersistenceTest {
    @TempDir
    public Path directory;

    @Test
    public void saveAndReload_preservesHistoryAndStableId() throws Exception {
        Path file = directory.resolve("students.json");
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(file);
        PaymentHistory history = new PaymentHistory(Map.of(
                YearMonth.of(2026, 10), PaymentStatus.PENDING,
                YearMonth.of(2026, 11), PaymentStatus.COMPLETED));
        Student student = ALICE.withStudentFields(ALICE.getStudentFields()
                .with(PaymentHistory.FIELD, history)
                .with(tutorconnect.model.student.Subject.FIELD, new tutorconnect.model.student.Subject("Math")));
        StudentDirectory directoryData = new StudentDirectory();
        directoryData.addStudent(student);

        storage.saveStudentDirectory(directoryData);
        Student reloaded = storage.readStudentDirectory().orElseThrow().getStudentList().getFirst();

        assertEquals(student.getId(), reloaded.getId());
        assertEquals(history, reloaded.getStudentFields().get(PaymentHistory.FIELD).orElseThrow());
        assertEquals("Math", reloaded.getStudentFields().display("subject"));
        var json = JsonUtil.fromJsonString(Files.readString(file), com.fasterxml.jackson.databind.JsonNode.class);
        assertEquals("pending", json.get("persons").get(0).get("payments").get("2026-10").asText());
    }

    @Test
    public void read_legacyRecordWithoutPayments_remainsUnset() throws Exception {
        Path file = directory.resolve("legacy.json");
        ObjectNode record = JsonUtil.fromJsonString(JsonUtil.toJsonString(new JsonAdaptedStudent(ALICE)),
                ObjectNode.class);
        record.remove("payments");
        String content = JsonNodeFactory.instance.objectNode()
                .set("persons", JsonNodeFactory.instance.arrayNode().add(record)).toString();
        Files.writeString(file, content);

        Student loaded = new JsonStudentDirectoryStorage(file).readStudentDirectory().orElseThrow()
                .getStudentList().getFirst();

        assertTrue(loaded.getStudentFields().get(PaymentHistory.FIELD).isEmpty());
        assertEquals("—", loaded.getStudentFields().display("payments"));
    }

    @Test
    public void readMalformedPaymentHistory_preservesRecordAndFileForSafeErrorHandling() throws Exception {
        Path file = directory.resolve("malformed.json");
        ObjectNode record = JsonUtil.fromJsonString(JsonUtil.toJsonString(new JsonAdaptedStudent(ALICE)),
                ObjectNode.class);
        record.set("payments", JsonNodeFactory.instance.objectNode().put("2026-10", "paid"));
        String content = JsonNodeFactory.instance.objectNode()
                .set("persons", JsonNodeFactory.instance.arrayNode().add(record)).toString();
        Files.writeString(file, content);

        Student loaded = new JsonStudentDirectoryStorage(file).readStudentDirectory().orElseThrow()
                .getStudentList().getFirst();
        assertThrows(IllegalArgumentException.class, () -> loaded.getStudentFields().get(PaymentHistory.FIELD));
        assertEquals(content, Files.readString(file));
    }
}
