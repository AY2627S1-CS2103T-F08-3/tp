package tutorconnect.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static tutorconnect.testutil.TypicalStudents.ALICE;
import static tutorconnect.testutil.TypicalStudents.BENSON;
import static tutorconnect.testutil.TypicalStudents.CARL;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import tutorconnect.model.student.Student;

class StudentListPanelTest {
    @BeforeAll
    static void startToolkit() throws Exception {
        CompletableFuture<Void> started = new CompletableFuture<>();
        try {
            Platform.startup(() -> {
                Platform.setImplicitExit(false);
                started.complete(null);
            });
        } catch (IllegalStateException alreadyStarted) {
            Platform.runLater(() -> started.complete(null));
        }
        started.get(10, TimeUnit.SECONDS);
    }

    @Test
    void selection_survivesRowShiftClearsForDeletedTargetAndList() throws Exception {
        CompletableFuture<Void> finished = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                var people = FXCollections.observableArrayList(ALICE, BENSON, CARL);
                StudentListPanel panel = new StudentListPanel(people);
                @SuppressWarnings("unchecked")
                ListView<Student> view = (ListView<Student>) panel.getRoot().lookup("#studentListView");
                view.getSelectionModel().select(1);
                Student selected = view.getSelectionModel().getSelectedItem();
                people.set(1, BENSON.withDetails(BENSON.getName(), BENSON.getPhone(),
                        BENSON.getEmail(), BENSON.getAddress(), BENSON.getTags()));
                people.remove(0);
                panel.selectStudent(selected.getId());
                assertEquals(BENSON.getId(), view.getSelectionModel().getSelectedItem().getId());
                assertEquals(0, view.getSelectionModel().getSelectedIndex());
                people.remove(BENSON);
                panel.selectStudent(null);
                assertNull(view.getSelectionModel().getSelectedItem());
                view.getSelectionModel().select(0);
                assertSame(CARL, view.getSelectionModel().getSelectedItem());
                panel.selectStudent(null);
                assertNull(view.getSelectionModel().getSelectedItem());
                people.clear();
                assertEquals("No students found.", ((Label) view.getPlaceholder()).getText());
                finished.complete(null);
            } catch (Throwable failure) {
                finished.completeExceptionally(failure);
            }
        });
        finished.get(10, TimeUnit.SECONDS);
    }
}
