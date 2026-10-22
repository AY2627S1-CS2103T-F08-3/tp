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
import tutorconnect.model.student.AcademicNote;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.StudentFields;
import tutorconnect.testutil.JavaFxTestUtil;
import tutorconnect.testutil.StudentBuilder;

/** The selected-student details show the note, an em dash when unset, and wrap a long note. */
public class NoteDisplayUiTest {
    private static final String EM_DASH = "—";

    @BeforeAll
    public static void startToolkit() {
        JavaFxTestUtil.startToolkit();
    }

    private static <T> T onFxThread(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(15, TimeUnit.SECONDS);
    }

    private static Student studentWithNote(String note) {
        Student student = new StudentBuilder().withName("Amy Bee").build();
        return note == null ? student : student.withStudentFields(student.getStudentFields()
                .with(AcademicNote.FIELD, new AcademicNote(note)));
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
    public void unsetNote_showsAnEmDash() throws Exception {
        assertTrue(detailText(studentWithNote(null)).contains("Note: " + EM_DASH));
    }

    @Test
    public void note_isShownWithItsPunctuation() throws Exception {
        String note = "Weak in calculus; he/she works w/ parent (50%)!";
        assertTrue(detailText(studentWithNote(note)).contains("Note: " + note));
    }

    @Test
    public void noteSitsBetweenTheSubjectAndTheHourlyRate() throws Exception {
        List<String> details = detailText(studentWithNote("Weak in calculus"));
        int subject = details.indexOf("Subject: " + EM_DASH);
        assertTrue(subject >= 0, details.toString());
        assertEquals("Note: Weak in calculus", details.get(subject + 1));
    }

    @Test
    public void longNote_isShownInFullAndTheLabelWrapsSoThePanelCanScroll() throws Exception {
        String longest = "word ".repeat(99) + "word";
        assertEquals(499, longest.length());
        List<Label> labels = detailLabels(studentWithNote(longest));
        Label note = labels.stream().filter(label -> label.getText().startsWith("Note: ")).findFirst()
                .orElseThrow();
        assertEquals("Note: " + longest, note.getText());
        assertTrue(note.isWrapText());
    }

    @Test
    public void unreadableStoredNote_showsAnEmDashInsteadOfFailing() throws Exception {
        Student student = new StudentBuilder().withName("Amy Bee").build().withStudentFields(
                new StudentFields(Map.of("academicNote", IntNode.valueOf(5))));
        assertTrue(detailText(student).contains("Note: " + EM_DASH));
    }
}
