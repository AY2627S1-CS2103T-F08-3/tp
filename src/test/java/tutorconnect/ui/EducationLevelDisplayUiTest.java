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

import com.fasterxml.jackson.databind.node.TextNode;

import javafx.application.Platform;
import javafx.scene.control.Label;
import tutorconnect.model.student.EducationLevel;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.StudentFields;
import tutorconnect.testutil.JavaFxTestUtil;
import tutorconnect.testutil.StudentBuilder;

/** F05: the list card and the details panel show the canonical education level, or an em dash when unset. */
public class EducationLevelDisplayUiTest {
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

    private static Student studentWithStoredLevel(String name, String storedLevel) {
        Student student = new StudentBuilder().withName(name).build();
        return storedLevel == null ? student
                : student.withStudentFields(new StudentFields(Map.of("educationLevel", TextNode.valueOf(storedLevel))));
    }

    private static List<String> cardLabels(Student student) throws Exception {
        return onFxThread(() -> new StudentCard(student, 1).getRoot().lookupAll(".label").stream()
                .map(Label.class::cast).map(Label::getText).toList());
    }

    private static List<String> detailLabels(Student student) throws Exception {
        return onFxThread(() -> {
            StudentDetailsPanel panel = new StudentDetailsPanel();
            panel.showStudent(student);
            return panel.getChildren().stream().map(node -> ((Label) node).getText()).toList();
        });
    }

    private void assertShown(String storedLevel, String expectedLevel) throws Exception {
        Student student = studentWithStoredLevel("Amy Bee", storedLevel);
        assertTrue(cardLabels(student).contains("Level: " + expectedLevel),
                "card for stored " + storedLevel + ": " + cardLabels(student));
        assertTrue(detailLabels(student).contains("Education level: " + expectedLevel),
                "details for stored " + storedLevel + ": " + detailLabels(student));
    }

    @Test
    public void unsetLevel_showsAnEmDashOnTheCardAndTheDetailsPanel() throws Exception {
        assertShown(null, EM_DASH);
    }

    @Test
    public void canonicalLevels_areShown() throws Exception {
        assertShown("Secondary 4", "Secondary 4");
        assertShown("Primary 6", "Primary 6");
        assertShown("JC 2", "JC 2");
    }

    @Test
    public void storedAliasOrDifferentCase_isShownCanonically() throws Exception {
        assertShown("sec4", "Secondary 4");
        assertShown("S 4", "Secondary 4");
        assertShown("SECONDARY 4", "Secondary 4");
        assertShown("p1", "Primary 1");
        assertShown("jc2", "JC 2");
    }

    @Test
    public void unreadableStoredLevel_showsAnEmDash() throws Exception {
        assertShown("SECONDARY_1", EM_DASH);
        assertShown("Sec 6", EM_DASH);
    }

    @Test
    public void replacedLevel_showsTheNewLevel() throws Exception {
        Student student = studentWithStoredLevel("Amy Bee", "Secondary 4");
        Student replaced = student.withStudentFields(student.getStudentFields().with(EducationLevel.FIELD,
                new EducationLevel("JC1")));
        assertTrue(cardLabels(replaced).contains("Level: JC 1"));
        assertTrue(detailLabels(replaced).contains("Education level: JC 1"));
        assertEquals(List.of(), cardLabels(replaced).stream().filter(text -> text.contains("Secondary 4")).toList());
    }
}
