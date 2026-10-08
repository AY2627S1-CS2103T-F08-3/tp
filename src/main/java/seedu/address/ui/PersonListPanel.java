package seedu.address.ui;

import java.util.logging.Logger;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Region;
import seedu.address.commons.core.LogsCenter;
import seedu.address.logic.commands.CommandResult.SelectionAction;
import seedu.address.model.person.Person;

/**
 * Panel containing the list of persons.
 */
public class PersonListPanel extends UiPart<Region> {
    private static final String FXML = "PersonListPanel.fxml";
    private final Logger logger = LogsCenter.getLogger(PersonListPanel.class);

    @FXML
    private ListView<Person> personListView;

    /**
     * Creates a {@code PersonListPanel} with the given {@code ObservableList}.
     */
    public PersonListPanel(ObservableList<Person> personList) {
        super(FXML);
        personListView.setItems(personList);
        personListView.setCellFactory(listView -> new PersonListViewCell());
        personListView.setPlaceholder(new Label("No students found."));
    }

    public Person getSelectedPerson() {
        return personListView.getSelectionModel().getSelectedItem();
    }

    /** Restores the same surviving record, never the record now occupying its former row. */
    public void applySelection(SelectionAction action, Person previousSelection) {
        if (action == SelectionAction.UNCHANGED) {
            return;
        }
        personListView.getSelectionModel().clearSelection();
        if (action == SelectionAction.PRESERVE && previousSelection != null) {
            for (int i = 0; i < personListView.getItems().size(); i++) {
                if (personListView.getItems().get(i).getId().equals(previousSelection.getId())) {
                    personListView.getSelectionModel().select(i);
                    break;
                }
            }
        }
        personListView.refresh();
    }

    /**
     * Custom {@code ListCell} that displays the graphics of a {@code Person} using a {@code PersonCard}.
     */
    class PersonListViewCell extends ListCell<Person> {
        @Override
        protected void updateItem(Person person, boolean empty) {
            super.updateItem(person, empty);

            if (empty || person == null) {
                setGraphic(null);
                setText(null);
            } else {
                setGraphic(new PersonCard(person, getIndex() + 1).getRoot());
            }
        }
    }

}
