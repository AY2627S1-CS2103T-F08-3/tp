package tutorconnect.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

public class CommandHistoryTest {
    @Test
    public void browsing_movesThroughCommandsAndRestoresDraft() {
        CommandHistory history = new CommandHistory();
        history.add("list");
        history.add("find amy");
        assertEquals(Optional.of("find amy"), history.previous("sch"));
        assertEquals(Optional.of("list"), history.previous("ignored"));
        assertEquals(Optional.empty(), history.previous("ignored"));
        assertEquals(Optional.of("find amy"), history.next());
        assertEquals(Optional.of("sch"), history.next());
        assertEquals(Optional.empty(), history.next());
    }

    @Test
    public void add_skipsBlanksAndImmediateRepeats() {
        CommandHistory history = new CommandHistory();
        history.add("list");
        history.add("list");
        history.add("   ");
        assertEquals(Optional.of("list"), history.previous(""));
        assertEquals(Optional.empty(), history.previous(""));
    }

    @Test
    public void add_keepsOnlyNewestEntries() {
        CommandHistory history = new CommandHistory();
        for (int i = 0; i <= CommandHistory.MAX_ENTRIES; i++) {
            history.add("delete " + i);
        }
        String oldest = "";
        for (Optional<String> entry = history.previous(""); entry.isPresent(); entry = history.previous("")) {
            oldest = entry.get();
        }
        assertEquals("delete 1", oldest);
    }

    @Test
    public void resetPosition_startsBrowsingFromNewestAgain() {
        CommandHistory history = new CommandHistory();
        history.add("list");
        history.add("help");
        history.previous("");
        history.previous("");
        history.resetPosition();
        assertEquals(Optional.of("help"), history.previous(""));
        assertEquals(Optional.empty(), new CommandHistory().next());
    }
}
