package tutorconnect.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tutorconnect.logic.commands.PaymentsCommand;
import tutorconnect.logic.parser.exceptions.ParseException;
import tutorconnect.model.ModelManager;
import tutorconnect.model.student.PaymentHistory;
import tutorconnect.model.student.PaymentStatus;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.StudentFields;
import tutorconnect.storage.JsonStudentDirectoryStorage;
import tutorconnect.storage.JsonUserPrefsStorage;
import tutorconnect.storage.StorageManager;
import tutorconnect.testutil.StudentBuilder;

public class PaymentsIntegrationTest {
    private static final YearMonth OCTOBER = YearMonth.of(2026, 10);

    @TempDir
    private Path directory;

    @Test
    public void list_usesFullRegisterInsertionOrderAndOptionalStatusFilter() throws Exception {
        ModelManager model = model();
        List<Student> students = List.copyOf(model.getStudentDirectory().getStudentList());
        model.updateFilteredStudentList(student -> student.getName().toString().startsWith("Alex"));
        model.selectStudent(students.getFirst().getId());
        Path file = directory.resolve("students.json");
        Logic logic = logic(model, file);

        assertEquals("Alex Tan (+6591234567) — 2026-10: pending\n"
                        + "Casey Lim (+6581234567) — 2026-10: completed",
                logic.execute("payments m/2026-10").getFeedbackToUser());
        assertEquals(students.getFirst().getId(), model.getSelectedStudentId());
        assertEquals("Casey Lim (+6581234567) — 2026-10: completed",
                logic.execute("payments s/completed m/2026-10").getFeedbackToUser());
        assertTrue(Files.notExists(file));
    }

    @Test
    public void list_excludesUnrecordedStudentsAndUsesConsistentEmptyState() throws Exception {
        ModelManager model = new ModelManager();
        model.addStudent(new StudentBuilder().withName("No Payment").withPhone("61234567").build());
        Logic logic = logic(model, directory.resolve("students.json"));

        assertEquals(String.format(PaymentsCommand.MESSAGE_EMPTY, OCTOBER),
                logic.execute("payments m/2026-10 s/pending").getFeedbackToUser());
        assertEquals(String.format(PaymentsCommand.MESSAGE_EMPTY, OCTOBER),
                logic.execute("payments m/2026-10").getFeedbackToUser());
    }

    @Test
    public void parser_validatesStructureThenMonthThenStatus() throws Exception {
        Logic logic = logic(model(), directory.resolve("students.json"));

        assertParseFailure(logic, "payments s/pending", "Error: Missing required parameter: m/MONTH.");
        assertParseFailure(logic, "payments m/2026-13 s/invalid",
                PaymentHistory.MESSAGE_INVALID_MONTH);
        assertParseFailure(logic, "payments m/2026-10 s/invalid", PaymentStatus.MESSAGE_INVALID);
        assertParseFailure(logic, "payments m/2026-10 m/2026-11", "Error: Parameter m/ may be specified only once.");
        assertParseFailure(logic, "payments m/2026-10 x/test", "Error: Unknown parameter: x/.");
        assertParseFailure(logic, "payments\nm/2026-10", "Error: Unexpected text after command.");
    }

    private ModelManager model() {
        ModelManager model = new ModelManager();
        PaymentHistory firstHistory = new PaymentHistory(Map.of(OCTOBER, PaymentStatus.PENDING));
        PaymentHistory secondHistory = new PaymentHistory(Map.of(OCTOBER, PaymentStatus.COMPLETED));
        Student first = new StudentBuilder().withName("Alex Tan").withPhone("91234567").build()
                .withStudentFields(new StudentFields(Map.of())
                        .with(PaymentHistory.FIELD, firstHistory));
        Student second = new StudentBuilder().withName("Casey Lim").withPhone("81234567").build()
                .withStudentFields(new StudentFields(Map.of())
                        .with(PaymentHistory.FIELD, secondHistory));
        model.addStudent(first);
        model.addStudent(second);
        model.addStudent(new StudentBuilder().withName("Other Month").withPhone("81234568").build()
                .withStudentFields(new StudentFields(Map.of())
                        .with(PaymentHistory.FIELD, new PaymentHistory(Map.of(
                                YearMonth.of(2026, 11), PaymentStatus.PENDING)))));
        return model;
    }

    private Logic logic(ModelManager model, Path file) {
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(file);
        return new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(directory.resolve("prefs.json"))));
    }

    private void assertParseFailure(Logic logic, String command, String expected) {
        ParseException failure = org.junit.jupiter.api.Assertions.assertThrows(ParseException.class, ()
                -> logic.execute(command));
        assertEquals(expected, failure.getMessage());
    }
}
