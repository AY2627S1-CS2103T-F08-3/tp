package tutorconnect.ui;

import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tutorconnect.commons.core.GuiSettings;
import tutorconnect.commons.core.LogsCenter;
import tutorconnect.logic.Logic;
import tutorconnect.logic.commands.CommandResult;
import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.logic.parser.exceptions.ParseException;
import tutorconnect.model.student.WeeklySlotField;

/**
 * The main window. Provides the basic application layout containing
 * a menu bar and space where other JavaFX elements can be placed.
 */
public class MainWindow extends UiPart<Stage> {

    /** Style class that outlines the panel holding keyboard focus. */
    static final String FOCUSED_PANEL_STYLE_CLASS = "panel-focused";

    private static final String FXML = "MainWindow.fxml";
    /** Below this window width the command rail is hidden so the list and profile keep their space. */
    private static final double RAIL_MIN_WINDOW_WIDTH = 1100;
    /** Below this window width the summary counts are hidden; the list header still shows the count. */
    private static final double STATS_MIN_WINDOW_WIDTH = 900;
    private static final double WIDE_SIDE_WIDTH = 330;
    private static final double NARROW_SIDE_WIDTH = 270;
    /** Fraction of a scroll panel's range moved by one Page Up/Down. */
    private static final double PAGE_SCROLL_STEP = 0.25;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH);
    private static final KeyCombination FOCUS_COMMAND = new KeyCodeCombination(KeyCode.L, KeyCombination.SHORTCUT_DOWN);
    private static final KeyCombination SELECT_NEXT = new KeyCodeCombination(KeyCode.DOWN,
            KeyCombination.SHORTCUT_DOWN);
    private static final KeyCombination SELECT_PREVIOUS = new KeyCodeCombination(KeyCode.UP,
            KeyCombination.SHORTCUT_DOWN);
    private static final KeyCombination NEXT_PANEL = new KeyCodeCombination(KeyCode.TAB, KeyCombination.SHIFT_DOWN);
    private static final KeyCombination COMPLETE_OR_RETURN = new KeyCodeCombination(KeyCode.TAB);
    /** Pixels scrolled by one arrow key in a focused scroll panel. */
    private static final double ARROW_SCROLL_PIXELS = 48;

    private final Logger logger = LogsCenter.getLogger(getClass());

    private Stage primaryStage;
    private Logic logic;
    private Path dataFilePath;

    // Independent Ui parts residing in this Ui container
    private StudentListPanel studentListPanel;
    private ResultDisplay resultDisplay;
    private HelpWindow helpWindow;
    private StudentDetailsPanel studentDetailsPanel;
    private WeeklySchedulePanel weeklySchedulePanel;
    private CommandBox commandBox;
    private boolean executingCommand;

    @FXML
    private ScrollPane commandRail;

    @FXML
    private StackPane commandReferencePlaceholder;

    @FXML
    private Label todayLabel;

    @FXML
    private Label shownCount;

    @FXML
    private Label lessonCount;

    @FXML
    private Label todayLessonCount;

    @FXML
    private Label todayLessonNote;

    @FXML
    private Label listCountLabel;

    @FXML
    private HBox statsRow;

    @FXML
    private VBox sideArea;

    @FXML
    private ScrollPane profileScroll;

    @FXML
    private VBox profileCard;

    @FXML
    private VBox studentList;

    @FXML
    private StackPane commandBoxPlaceholder;

    @FXML
    private MenuItem helpMenuItem;

    @FXML
    private StackPane studentListPanelPlaceholder;

    @FXML
    private StackPane studentDetailsPlaceholder;

    @FXML
    private StackPane weeklySchedulePlaceholder;

    @FXML
    private StackPane resultDisplayPlaceholder;

    @FXML
    private StackPane statusbarPlaceholder;

    /**
     * Creates a {@code MainWindow} with the given {@code Stage}, {@code Logic},
     * and the data file path to show in the status bar.
     */
    public MainWindow(Stage primaryStage, Logic logic, Path dataFilePath) {
        super(FXML, primaryStage);

        // Set dependencies
        this.primaryStage = primaryStage;
        this.logic = logic;
        this.dataFilePath = dataFilePath;

        // Configure the UI
        setWindowDefaultSize(logic.getGuiSettings());

        setAccelerators();

        helpWindow = new HelpWindow();
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    private void setAccelerators() {
        setAccelerator(helpMenuItem, KeyCombination.valueOf("F1"));
    }

    /**
     * Sets the accelerator of a MenuItem.
     * @param keyCombination the KeyCombination value of the accelerator
     */
    private void setAccelerator(MenuItem menuItem, KeyCombination keyCombination) {
        menuItem.setAccelerator(keyCombination);

        /*
         * TODO: the code below can be removed once the bug reported here
         * https://bugs.openjdk.java.net/browse/JDK-8131666
         * is fixed in a later version of the SDK.
         *
         * According to the bug report, TextInputControl (TextField, TextArea) will
         * consume function-key events. Because CommandBox contains a TextField and
         * ResultDisplay contains a TextArea, some accelerators (e.g., F1) will
         * not work when the focus is in them because the key event is consumed by
         * the TextInputControl(s).
         *
         * For now, we add the following event filter to capture such key events and open
         * the help window purposely so as to support accelerators even when focus is
         * in CommandBox or ResultDisplay.
         */
        getRoot().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getTarget() instanceof TextInputControl && keyCombination.match(event)) {
                menuItem.getOnAction().handle(new ActionEvent());
                event.consume();
            }
        });
    }

    /**
     * Fills up all the placeholders of this window.
     */
    void fillInnerParts() {
        studentDetailsPanel = new StudentDetailsPanel();
        studentDetailsPlaceholder.getChildren().add(studentDetailsPanel);
        studentListPanel = new StudentListPanel(logic.getFilteredStudentList(), student -> {
            if (!executingCommand) {
                logic.selectStudent(student == null ? null : student.getId());
                studentDetailsPanel.showStudent(student);
            }
        });
        studentListPanelPlaceholder.getChildren().add(studentListPanel.getRoot());
        weeklySchedulePanel = new WeeklySchedulePanel(logic.getWeeklySchedule());
        weeklySchedulePlaceholder.getChildren().add(weeklySchedulePanel);

        resultDisplay = new ResultDisplay();
        resultDisplayPlaceholder.getChildren().add(resultDisplay.getRoot());

        StatusBarFooter statusBarFooter = new StatusBarFooter(dataFilePath);
        statusbarPlaceholder.getChildren().add(statusBarFooter.getRoot());

        commandBox = new CommandBox(this::executeCommand);
        commandBoxPlaceholder.getChildren().add(commandBox.getRoot());

        commandReferencePlaceholder.getChildren().add(new CommandReferencePanel());
        showOverview(LocalDate.now());
        adaptToWindowWidth();
        addKeyboardNavigation();
        outlineFocusedPanel();
        commandBox.focus();
    }

    /** Shows the date and live counts that sit above the student list; they follow the observed lists. */
    private void showOverview(LocalDate today) {
        todayLabel.setText(today.format(DATE_FORMAT));
        DayOfWeek day = today.getDayOfWeek();
        var students = logic.getFilteredStudentList();
        var lessons = logic.getWeeklySchedule();
        shownCount.textProperty().bind(Bindings.size(students).asString("%02d"));
        listCountLabel.textProperty().bind(Bindings.size(students).asString("(%d)"));
        lessonCount.textProperty().bind(Bindings.size(lessons).asString("%02d"));
        todayLessonCount.textProperty().bind(Bindings.createStringBinding(() -> String.format("%02d",
                lessons.stream().filter(student -> WeeklySlotField.get(student).orElseThrow().getDay() == day)
                        .count()), lessons));
        todayLessonNote.setText("on " + day.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                + "  ·  timetable d/" + day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
    }

    /**
     * Gives narrow windows to the list and profile: the read-only command rail and the summary counts are hidden and
     * the profile column narrows. Every command still works from the command bar.
     */
    private void adaptToWindowWidth() {
        BorderPane root = (BorderPane) primaryStage.getScene().getRoot();
        Runnable update = () -> {
            double width = primaryStage.getWidth();
            root.setLeft(width >= RAIL_MIN_WINDOW_WIDTH ? commandRail : null);
            statsRow.setVisible(width >= STATS_MIN_WINDOW_WIDTH);
            statsRow.setManaged(statsRow.isVisible());
            sideArea.setPrefWidth(width >= RAIL_MIN_WINDOW_WIDTH ? WIDE_SIDE_WIDTH : NARROW_SIDE_WIDTH);
        };
        primaryStage.widthProperty().addListener((unused1, unused2, unused3) -> update.run());
        update.run();
    }

    /** A panel that keyboard focus can move to: the node that takes focus and the card that is outlined. */
    private record Panel(Node focusTarget, Node outline) {}

    /** Returns the panels in Shift+Tab order, skipping the command rail while it is hidden. */
    private List<Panel> panels() {
        List<Panel> panels = new ArrayList<>();
        panels.add(new Panel(commandBox.getField(), commandBoxPlaceholder));
        if (commandRail.getScene() != null) {
            panels.add(new Panel(commandRail, commandRail));
        }
        panels.add(new Panel(studentListPanel.getListView(), studentList));
        panels.add(new Panel(profileScroll, profileCard));
        panels.add(new Panel(weeklySchedulePanel.getRowsView(), weeklySchedulePlaceholder));
        return panels;
    }

    /** Moves keyboard focus to the panel after the one that has it, wrapping back to the command bar. */
    void focusNextPanel() {
        List<Panel> panels = panels();
        int current = indexOfFocusedPanel(panels);
        panels.get((current + 1) % panels.size()).focusTarget().requestFocus();
    }

    private int indexOfFocusedPanel(List<Panel> panels) {
        Node owner = primaryStage.getScene().getFocusOwner();
        for (int i = 0; i < panels.size(); i++) {
            if (isWithin(owner, panels.get(i).outline())) {
                return i;
            }
        }
        return 0;
    }

    private static boolean isWithin(Node node, Node ancestor) {
        for (Node current = node; current != null; current = current.getParent()) {
            if (current == ancestor) {
                return true;
            }
        }
        return false;
    }

    /** Outlines the panel that holds keyboard focus, so it is always clear where keys will go. */
    private void outlineFocusedPanel() {
        Runnable update = () -> {
            // The rail is left out of panels() while hidden, so clear it explicitly.
            commandRail.getStyleClass().remove(FOCUSED_PANEL_STYLE_CLASS);
            List<Panel> panels = panels();
            int focused = indexOfFocusedPanel(panels);
            for (int i = 0; i < panels.size(); i++) {
                ObservableList<String> styles = panels.get(i).outline().getStyleClass();
                styles.remove(FOCUSED_PANEL_STYLE_CLASS);
                if (i == focused) {
                    styles.add(FOCUSED_PANEL_STYLE_CLASS);
                }
            }
        };
        primaryStage.getScene().focusOwnerProperty().addListener((unused1, unused2, unused3) -> update.run());
        update.run();
    }

    /**
     * Keeps the window type-first. Tab completes command words in the command bar and returns to it from anywhere
     * else; Shift+Tab moves to the next panel; arrow keys and Page Up/Down scroll or move inside the focused panel;
     * Esc returns to the command bar; Ctrl/Cmd+L focuses it; Ctrl/Cmd+Up/Down move the selected student; and
     * printable keys typed outside the command bar go to it.
     */
    private void addKeyboardNavigation() {
        getRoot().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            Node owner = primaryStage.getScene().getFocusOwner();
            boolean isInCommandBar = owner == commandBox.getField();
            if (FOCUS_COMMAND.match(event)) {
                commandBox.focus();
            } else if (NEXT_PANEL.match(event)) {
                focusNextPanel();
            } else if (!isInCommandBar && (COMPLETE_OR_RETURN.match(event) || event.getCode() == KeyCode.ESCAPE)) {
                commandBox.focus();
            } else if (SELECT_NEXT.match(event)) {
                studentListPanel.moveSelection(1);
            } else if (SELECT_PREVIOUS.match(event)) {
                studentListPanel.moveSelection(-1);
            } else if (!scrollFocusedPanel(owner, event.getCode(), isInCommandBar)) {
                return;
            }
            event.consume();
        });
        getRoot().addEventFilter(KeyEvent.KEY_TYPED, event -> {
            boolean isPrintable = !event.getCharacter().isEmpty() && !Character.isISOControl(event.getCharacter()
                    .charAt(0));
            boolean isInTextField = event.getTarget() instanceof TextInputControl input && input.isEditable();
            if (isPrintable && !isInTextField && !event.isShortcutDown() && !event.isAltDown()) {
                commandBox.focusAndType(event.getCharacter());
                event.consume();
            }
        });
    }

    /**
     * Scrolls the focused panel for arrow and Page keys and returns whether the key was used. Page Up/Down from the
     * command bar scroll the profile; the student list keeps its own arrow-key selection.
     */
    private boolean scrollFocusedPanel(Node owner, KeyCode code, boolean isInCommandBar) {
        boolean isPage = code == KeyCode.PAGE_UP || code == KeyCode.PAGE_DOWN;
        boolean isArrow = code == KeyCode.UP || code == KeyCode.DOWN;
        if (!isPage && !isArrow) {
            return false;
        }
        int direction = code == KeyCode.UP || code == KeyCode.PAGE_UP ? -1 : 1;
        if (owner == weeklySchedulePanel.getRowsView()) {
            weeklySchedulePanel.scrollBy(direction * (isPage ? 4 : 1));
        } else if (owner == commandRail || owner == profileScroll) {
            scroll((ScrollPane) owner, direction, isPage);
        } else if (isInCommandBar && isPage) {
            scroll(profileScroll, direction, true);
        } else {
            return false;
        }
        return true;
    }

    private static void scroll(ScrollPane pane, int direction, boolean isPage) {
        double hidden = pane.getContent().getLayoutBounds().getHeight() - pane.getViewportBounds().getHeight();
        if (hidden <= 0) {
            return;
        }
        double step = isPage ? PAGE_SCROLL_STEP : ARROW_SCROLL_PIXELS / hidden;
        pane.setVvalue(Math.max(0, Math.min(1, pane.getVvalue() + direction * step)));
    }

    /**
     * Sets the default size based on {@code guiSettings}.
     */
    private void setWindowDefaultSize(GuiSettings guiSettings) {
        primaryStage.setHeight(guiSettings.getWindowHeight());
        primaryStage.setWidth(guiSettings.getWindowWidth());
        if (guiSettings.getWindowCoordinates() != null) {
            primaryStage.setX(guiSettings.getWindowCoordinates().getX());
            primaryStage.setY(guiSettings.getWindowCoordinates().getY());
        }
    }

    /**
     * Opens the help window or focuses on it if it's already opened.
     */
    @FXML
    public void handleHelp() {
        if (!helpWindow.isShowing()) {
            helpWindow.show();
        } else {
            helpWindow.focus();
        }
    }

    void show() {
        primaryStage.show();
    }

    /**
     * Closes the application.
     */
    @FXML
    private void handleExit() {
        GuiSettings guiSettings = new GuiSettings(primaryStage.getWidth(), primaryStage.getHeight(),
                (int) primaryStage.getX(), (int) primaryStage.getY());
        logic.setGuiSettings(guiSettings);
        helpWindow.hide();
        primaryStage.hide();
    }

    public StudentListPanel getStudentListPanel() {
        return studentListPanel;
    }

    /** Shows a message in the result box without running a command, e.g. a startup warning. */
    void showFeedback(String feedback) {
        resultDisplay.setFeedbackToUser(feedback);
    }

    /**
     * Executes the command and returns the result.
     *
     * @see tutorconnect.logic.Logic#execute(String)
     */
    private CommandResult executeCommand(String commandText) throws CommandException, ParseException {
        executingCommand = true;
        try {
            CommandResult commandResult = logic.execute(commandText);
            commandResult.getTimetableView().ifPresent(view -> weeklySchedulePanel.showDay(view.day()));
            studentListPanel.selectStudent(logic.getSelectedStudentId());
            studentDetailsPanel.showStudent(logic.getFilteredStudentList().stream()
                    .filter(student -> student.getId().equals(logic.getSelectedStudentId())).findFirst().orElse(null));
            logger.fine("Command completed; updating UI");
            resultDisplay.setFeedbackToUser(commandResult.getFeedbackToUser());
            commandResult.getSelectedStudentIndex().ifPresent(index -> studentListPanel.selectIndex(index - 1));

            if (commandResult.isShowHelp()) {
                handleHelp();
            }

            if (commandResult.isExit()) {
                handleExit();
            }

            return commandResult;
        } catch (CommandException | ParseException e) {
            logger.fine("Command rejected; displaying error");
            resultDisplay.setFeedbackToUser(e.getMessage());
            throw e;
        } finally {
            executingCommand = false;
        }
    }
}
