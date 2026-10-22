package tutorconnect.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.IntNode;

import javafx.application.Platform;
import javafx.scene.control.Label;
import tutorconnect.model.student.PaymentHistory;
import tutorconnect.model.student.PaymentStatus;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.StudentFields;
import tutorconnect.testutil.JavaFxTestUtil;
import tutorconnect.testutil.StudentBuilder;

/** Verifies monthly payment history in the selected-student details panel. */
public class PaymentDisplayUiTest {
    @BeforeAll
    public static void startToolkit() {
        JavaFxTestUtil.startToolkit();
    }

    @Test
    public void history_isChronologicalAndUnsetOrMalformedDataShowsEmDashOrSafeMessage() throws Exception {
        PaymentHistory history = new PaymentHistory(Map.of(
                YearMonth.of(2026, 12), PaymentStatus.COMPLETED,
                YearMonth.of(2026, 2), PaymentStatus.PENDING));
        Student recorded = new StudentBuilder().withName("Alex Tan").build()
                .withStudentFields(new StudentFields(Map.of()).with(PaymentHistory.FIELD, history));

        List<String> recordedDetails = detailText(recorded);
        int header = recordedDetails.indexOf("Payment history:");
        assertTrue(header >= 0, recordedDetails.toString());
        assertEquals(List.of("2026-02: pending", "2026-12: completed"),
                recordedDetails.subList(header + 1, header + 3));
        assertTrue(detailText(new StudentBuilder().build()).contains("Payment history: —"));

        Student malformed = new StudentBuilder().build().withStudentFields(
                new StudentFields(Map.of("payments", IntNode.valueOf(7))));
        assertTrue(detailText(malformed).contains("Payment history: stored data could not be read"));
    }

    private List<String> detailText(Student student) throws Exception {
        FutureTask<List<String>> task = new FutureTask<>(() -> {
            StudentDetailsPanel panel = new StudentDetailsPanel();
            panel.showStudent(student);
            return panel.getChildren().stream().map(node -> ((Label) node).getText()).toList();
        });
        Platform.runLater(task);
        return task.get(15, TimeUnit.SECONDS);
    }
}
