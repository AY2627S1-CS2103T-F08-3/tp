package tutorconnect.ui;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Commands entered in this session, browsed like a shell history with Up and Down.
 * The text being typed before browsing is kept and restored when the user moves past the newest entry.
 */
public class CommandHistory {
    static final int MAX_ENTRIES = 100;

    private final List<String> entries = new ArrayList<>();
    private int position;
    private String draft = "";

    /** Records a submitted command, skipping blanks and an immediate repeat, and resets browsing. */
    public void add(String command) {
        requireNonNull(command);
        if (!command.isBlank() && (entries.isEmpty() || !entries.get(entries.size() - 1).equals(command))) {
            entries.add(command);
            if (entries.size() > MAX_ENTRIES) {
                entries.remove(0);
            }
        }
        position = entries.size();
        draft = "";
    }

    /** Returns the previous (older) command, keeping {@code currentText} as the draft when browsing starts. */
    public Optional<String> previous(String currentText) {
        requireNonNull(currentText);
        if (position == 0) {
            return Optional.empty();
        }
        if (position == entries.size()) {
            draft = currentText;
        }
        position--;
        return Optional.of(entries.get(position));
    }

    /** Returns the next (newer) command, or the saved draft after the newest command. */
    public Optional<String> next() {
        if (position >= entries.size()) {
            return Optional.empty();
        }
        position++;
        return Optional.of(position == entries.size() ? draft : entries.get(position));
    }

    /** Stops browsing, e.g. after the text is cleared, without forgetting any command. */
    public void resetPosition() {
        position = entries.size();
        draft = "";
    }
}
