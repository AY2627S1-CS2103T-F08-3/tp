package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BENSON;
import static seedu.address.testutil.TypicalPersons.CARL;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import seedu.address.logic.commands.CommandResult.SelectionAction;
import seedu.address.model.person.Person;

class PersonListPanelTest {
    @BeforeAll
    static void startToolkit() throws Exception {
        CompletableFuture<Void> started = new CompletableFuture<>();
        Platform.startup(() -> started.complete(null));
        started.get(10, TimeUnit.SECONDS);
    }

    @Test
    void selection_survivesRowShiftClearsForDeletedTargetAndList() throws Exception {
        CompletableFuture<Void> finished = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                var people = FXCollections.observableArrayList(ALICE, BENSON, CARL);
                PersonListPanel panel = new PersonListPanel(people);
                @SuppressWarnings("unchecked")
                ListView<Person> view = (ListView<Person>) panel.getRoot().lookup("#personListView");
                view.getSelectionModel().select(1);
                Person selected = panel.getSelectedPerson();
                people.set(1, BENSON.withDetails(BENSON.getName(), BENSON.getPhone(),
                        BENSON.getEmail(), BENSON.getAddress(), BENSON.getTags()));
                people.remove(0);
                panel.applySelection(SelectionAction.PRESERVE, selected);
                assertEquals(BENSON.getId(), panel.getSelectedPerson().getId());
                assertEquals(0, view.getSelectionModel().getSelectedIndex());
                people.remove(BENSON);
                panel.applySelection(SelectionAction.PRESERVE, selected);
                assertNull(panel.getSelectedPerson());
                view.getSelectionModel().select(0);
                panel.applySelection(SelectionAction.UNCHANGED, CARL);
                assertSame(CARL, panel.getSelectedPerson());
                panel.applySelection(SelectionAction.CLEAR, CARL);
                assertNull(panel.getSelectedPerson());
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
