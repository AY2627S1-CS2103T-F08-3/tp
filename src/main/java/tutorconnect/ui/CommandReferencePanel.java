package tutorconnect.ui;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Read-only list of every command format, grouped as in {@link CommandGuide}. It replaces a clickable navigation
 * sidebar: TutorConnect is driven by typing, so the rail only shows what to type.
 */
public class CommandReferencePanel extends VBox {
    private static final String[][] SHORTCUTS = {
        {"Enter", "run the command"},
        {"Tab", "complete a command word"},
        {"↑ / ↓", "earlier commands"},
        {"Esc", "clear the command bar"},
        {"Ctrl/⌘ ↑↓", "select another student"},
        {"Ctrl/⌘ L", "jump to the command bar"},
        {"PgUp / PgDn", "scroll the profile"},
        {"F1", "open help"},
    };

    /** Creates the reference from the command guide. */
    public CommandReferencePanel() {
        setId("commandReference");
        getStyleClass().add("command-reference");
        setSpacing(4);
        setFocusTraversable(false);
        setMouseTransparent(true);
        for (String group : CommandGuide.GROUPS) {
            getChildren().add(styled(new Label(group.toUpperCase()), "rail-overline"));
            CommandGuide.entries().stream()
                    .filter(entry -> entry.group().equals(group))
                    .forEach(entry -> getChildren().add(commandRow(entry)));
        }
        getChildren().add(styled(new Label("KEYS"), "rail-overline"));
        for (String[] shortcut : SHORTCUTS) {
            Label row = styled(new Label(shortcut[1]), "rail-shortcut");
            row.setGraphic(styled(new Label(shortcut[0]), "keycap"));
            getChildren().add(row);
        }
    }

    private static VBox commandRow(CommandGuide.Entry entry) {
        Label word = styled(new Label(entry.word()), "rail-command");
        String parameters = entry.format().substring(entry.word().length()).strip();
        VBox row = new VBox(word);
        row.getStyleClass().add("rail-entry");
        if (!parameters.isEmpty()) {
            Label format = styled(new Label(parameters), "rail-format");
            format.setWrapText(true);
            row.getChildren().add(format);
        }
        return row;
    }

    private static Label styled(Label label, String styleClass) {
        label.getStyleClass().add(styleClass);
        return label;
    }
}
