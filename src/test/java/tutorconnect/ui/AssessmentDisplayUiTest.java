package tutorconnect.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.IntNode;

import javafx.application.Platform;
import javafx.scene.control.Label;
import tutorconnect.model.student.AssessmentHistory;
import tutorconnect.model.student.AssessmentRecord;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.StudentFields;
import tutorconnect.testutil.JavaFxTestUtil;
import tutorconnect.testutil.StudentBuilder;

/** The selected-student details show the assessment history, its empty and unreadable states, and many records. */
public class AssessmentDisplayUiTest {

    @BeforeAll
    public static void startToolkit() {
        JavaFxTestUtil.startToolkit();
    }

    private static <T> T onFxThread(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(15, TimeUnit.SECONDS);
    }

    private static Student studentWith(AssessmentRecord... records) {
        Student student = new StudentBuilder().withName("Amy Bee").build();
        if (records.length == 0) {
            return student;
        }
        AssessmentHistory history = new AssessmentHistory();
        for (AssessmentRecord record : records) {
            history = history.with(record);
        }
        return student.withStudentFields(student.getStudentFields().with(AssessmentHistory.FIELD, history));
    }

    private static List<Label> detailLabels(Student student) throws Exception {
        return onFxThread(() -> {
            StudentDetailsPanel panel = new StudentDetailsPanel();
            panel.showStudent(student);
            return panel.getChildren().stream().map(node -> (Label) node).toList();
        });
    }

    private static List<String> detailText(Student student) throws Exception {
        return detailLabels(student).stream().map(Label::getText).toList();
    }

    @Test
    public void noRecords_showAnEmptyState() throws Exception {
        assertTrue(detailText(studentWith()).contains("Assessments: none recorded"));
    }

    @Test
    public void records_areShownNumberedInDateOrderAfterTheWeeklySlot() throws Exception {
        List<String> details = detailText(studentWith(AssessmentRecord.of("2026-10-05", "Later test", "B+"),
                AssessmentRecord.of("2026-09-01", "Earlier test", "85/100")));

        int slot = details.indexOf("Weekly slot: —");
        int paymentHistory = details.indexOf("Payment history: —");
        assertTrue(slot >= 0, details.toString());
        assertTrue(paymentHistory > slot, details.toString());
        assertEquals(List.of("Assessments (2):", "1. 2026-09-01 Earlier test: 85/100",
                "2. 2026-10-05 Later test: B+"), details.subList(slot + 1, paymentHistory));
    }

    @Test
    public void aFullHistory_isShownInFullSoThePanelCanScroll() throws Exception {
        AssessmentRecord[] records = new AssessmentRecord[100];
        for (int i = 0; i < records.length; i++) {
            records[i] = AssessmentRecord.of("2026-01-01", "Test " + (i + 1), "A");
        }
        List<Label> labels = detailLabels(studentWith(records));

        List<String> text = labels.stream().map(Label::getText).toList();
        assertTrue(text.contains("Assessments (100):"));
        assertTrue(text.contains("100. 2026-01-01 Test 100: A"));
        assertTrue(labels.stream().allMatch(Label::isWrapText));
    }

    @Test
    public void aLongTitleWraps() throws Exception {
        String title = "word ".repeat(11) + "word";
        List<Label> labels = detailLabels(studentWith(AssessmentRecord.of("2026-10-01", title, "A1")));

        Label line = labels.stream().filter(label -> label.getText().startsWith("1. ")).findFirst().orElseThrow();
        assertEquals("1. 2026-10-01 " + title + ": A1", line.getText());
        assertTrue(line.isWrapText());
    }

    @Test
    public void unreadableStoredHistory_isReportedInsteadOfFailing() throws Exception {
        Student student = new StudentBuilder().withName("Amy Bee").build().withStudentFields(
                new StudentFields(Map.of("assessments", IntNode.valueOf(5))));
        assertTrue(detailText(student).contains("Assessments: stored data could not be read"));
    }
}
