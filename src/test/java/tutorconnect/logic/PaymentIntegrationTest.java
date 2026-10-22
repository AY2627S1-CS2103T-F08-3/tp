package tutorconnect.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.node.TextNode;

import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.model.ModelManager;
import tutorconnect.model.student.PaymentHistory;
import tutorconnect.model.student.PaymentStatus;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.StudentFields;
import tutorconnect.storage.AtomicJsonFile;
import tutorconnect.storage.JsonStudentDirectoryStorage;
import tutorconnect.storage.JsonUserPrefsStorage;
import tutorconnect.storage.StorageManager;
import tutorconnect.testutil.StudentBuilder;

public class PaymentIntegrationTest {
    @TempDir
    private Path directory;

    @Test
    public void setReplaceNoChangeAndEdit_preserveIdentityFieldsAndPersist() throws Exception {
        ModelManager model = model();
        Student first = model.getStudentDirectory().getStudentList().getFirst();
        Student target = model.getStudentDirectory().getStudentList().getLast();
        model.selectStudent(first.getId());
        model.updateFilteredStudentList(student -> student.getId().equals(target.getId()));
        AtomicInteger writes = new AtomicInteger();
        AtomicJsonFile writer = new AtomicJsonFile() {
            @Override
            public void write(Path destination, String json) throws IOException {
                writes.incrementAndGet();
                super.write(destination, json);
            }
        };
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(
                directory.resolve("students.json"), writer);
        Logic logic = logic(model, storage);

        assertEquals("Payment status set for Casey Lim (+6581234567) (2026-10): pending.",
                logic.execute("payment 1 s/pending m/2026-10").getFeedbackToUser());
        assertEquals(target.getId(), model.getSelectedStudentId());
        assertSame(first, model.getStudentDirectory().getStudentList().getFirst());
        assertEquals("Payment status updated for Casey Lim (+6581234567) (2026-10): pending -> completed.",
                logic.execute("payment 1 m/2026-10 s/completed").getFeedbackToUser());
        assertEquals("Payment status set for Casey Lim (+6581234567) (2026-11): pending.",
                logic.execute("payment 1 m/2026-11 s/pending").getFeedbackToUser());
        assertEquals(3, writes.get());

        assertEquals("Payment status for Casey Lim (+6581234567) (2026-10) is already completed.",
                logic.execute("payment 1 m/2026-10 s/completed").getFeedbackToUser());
        assertEquals(3, writes.get());
        assertEquals(target.getId(), model.getSelectedStudentId());

        logic.execute("edit 1 n/Casey Renamed");
        Student updated = model.getStudentDirectory().getStudentList().getLast();
        assertEquals(target.getId(), updated.getId());
        assertEquals("preserve subject", updated.getStudentFields().display("subject"));
        assertEquals(Map.of(YearMonth.of(2026, 10), PaymentStatus.COMPLETED,
                        YearMonth.of(2026, 11), PaymentStatus.PENDING),
                updated.getStudentFields().get(PaymentHistory.FIELD).orElseThrow().asMap());

        Student reloaded = storage.readStudentDirectory().orElseThrow().getStudentList().getLast();
        assertEquals(updated, reloaded);
        assertEquals(target.getId(), reloaded.getId());
        assertEquals("completed", reloaded.getStudentFields().get(PaymentHistory.FIELD).orElseThrow()
                .getStatus(YearMonth.of(2026, 10)).orElseThrow().toString());
    }

    @Test
    public void failedSave_preservesStorageModelFilterAndSelection() throws Exception {
        ModelManager model = model();
        Student target = model.getStudentDirectory().getStudentList().getLast();
        model.updateFilteredStudentList(student -> student.getId().equals(target.getId()));
        model.selectStudent(target.getId());
        var predicate = model.getStudentPredicate();
        List<Student> before = List.copyOf(model.getStudentDirectory().getStudentList());
        Path file = directory.resolve("failure.json");
        JsonStudentDirectoryStorage initialStorage = new JsonStudentDirectoryStorage(file);
        initialStorage.saveStudentDirectory(model.getStudentDirectory());
        String saved = Files.readString(file);
        AtomicJsonFile writer = new AtomicJsonFile() {
            @Override
            protected void replace(Path temporary, Path destination) throws IOException {
                throw new IOException("Injected replacement failure");
            }
        };
        Logic logic = logic(model, new JsonStudentDirectoryStorage(file, writer));

        CommandException failure = assertThrows(CommandException.class, ()
                -> logic.execute("payment 1 m/2026-10 s/pending"));

        assertEquals(LogicManager.MESSAGE_SAVE_FAILURE, failure.getMessage());
        assertEquals(saved, Files.readString(file));
        assertEquals(before, model.getStudentDirectory().getStudentList());
        assertSame(target, model.getFilteredStudentList().getFirst());
        assertSame(predicate, model.getStudentPredicate());
        assertEquals(target.getId(), model.getSelectedStudentId());
    }

    @Test
    public void delete_removesAttachedPaymentHistory() throws Exception {
        ModelManager model = model();
        Student target = model.getStudentDirectory().getStudentList().getFirst();
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(directory.resolve("delete.json"));
        Logic logic = logic(model, storage);
        logic.execute("payment 1 m/2026-10 s/pending");

        logic.execute("delete 1");

        assertTrue(storage.readStudentDirectory().orElseThrow().getStudentList().stream()
                .noneMatch(student -> student.getId().equals(target.getId())));
    }

    @Test
    public void malformedStoredHistory_isReportedWithoutSavingOrChangingSelection() throws Exception {
        ModelManager model = new ModelManager();
        Student student = new StudentBuilder().withName("Alex Tan").withPhone("91234567").build()
                .withStudentFields(new StudentFields(Map.of("payments", TextNode.valueOf("bad data"))));
        model.addStudent(student);
        model.selectStudent(student.getId());
        Path file = directory.resolve("malformed-payments.json");
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(file);
        storage.saveStudentDirectory(model.getStudentDirectory());
        String saved = Files.readString(file);
        Logic logic = logic(model, storage);
        String expected = "Error: The stored payment history for Alex Tan could not be read. "
                + "Correct or remove the \"payments\" entry in the data file.";

        assertEquals(expected, assertThrows(CommandException.class, ()
                -> logic.execute("payment 1 m/2026-10 s/pending")).getMessage());
        assertEquals(expected, assertThrows(CommandException.class, ()
                -> logic.execute("payments m/2026-10")).getMessage());
        assertEquals(saved, Files.readString(file));
        assertEquals(student, model.getStudentDirectory().getStudentList().getFirst());
        assertEquals(student.getId(), model.getSelectedStudentId());
    }

    private ModelManager model() {
        StudentFields fields = new StudentFields(Map.of("subject", TextNode.valueOf("preserve subject")));
        ModelManager model = new ModelManager();
        model.addStudent(new StudentBuilder().withName("Alex Tan").withPhone("91234567").build());
        model.addStudent(new StudentBuilder().withName("Casey Lim").withPhone("81234567").build()
                .withStudentFields(fields));
        return model;
    }

    private Logic logic(ModelManager model, JsonStudentDirectoryStorage storage) {
        return new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(directory.resolve("prefs.json"))));
    }
}
