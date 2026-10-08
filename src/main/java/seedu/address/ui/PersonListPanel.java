package seedu.address.ui;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Region;
import seedu.address.commons.core.LogsCenter;
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
        this(personList, unused -> {});
    }

    /** Passes row selection to the shared model/details view without interpreting displayed indices. */
    public PersonListPanel(ObservableList<Person> personList, Consumer<Person> selectionHandler) {
        super(FXML);
        personListView.setItems(personList);
        personListView.setPlaceholder(new Label("No students found."));
        personListView.setCellFactory(listView -> new PersonListViewCell());
        personListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
                selectionHandler.accept(newValue));
    }

    /** Selects and scrolls to a stable student ID after a successful command publication. */
    public void selectPerson(UUID id) {
        personListView.refresh();
        if (id == null) {
            personListView.getSelectionModel().clearSelection();
            return;
        }
        for (int i = 0; i < personListView.getItems().size(); i++) {
            if (personListView.getItems().get(i).getId().equals(id)) {
                if (personListView.getSelectionModel().getSelectedItem() == personListView.getItems().get(i)) {
                    return;
                }
                personListView.getSelectionModel().select(i);
                personListView.scrollTo(i);
                return;
            }
        }
    }

    /** Selects and scrolls to a zero-based row in the currently displayed list. */
    public void selectIndex(int zeroBasedIndex) {
        personListView.getSelectionModel().select(zeroBasedIndex);
        personListView.scrollTo(zeroBasedIndex);
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
