package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;

class VisibleIndexTest {
    @Test
    void parse_invalidSyntax_rejected() {
        for (String value : List.of("", "0", "01", "+1", "-1", "1.0", "1 2", "1\n", "a")) {
            assertThrows(ParseException.class, VisibleIndex.MESSAGE_INVALID, () -> VisibleIndex.parse(value));
        }
    }

    @Test
    void resolve_firstMiddleLast_usesDisplayedOrder() throws Exception {
        List<String> displayed = List.of("third stored", "first stored", "second stored");
        for (int i = 1; i <= displayed.size(); i++) {
            assertEquals(displayed.get(i - 1), VisibleIndex.parse(Integer.toString(i)).resolve(displayed));
        }
    }

    @Test
    void resolve_nonexistentIncludingHugeIndex_safeError() throws Exception {
        for (String value : List.of("1", "2147483648", "9".repeat(1000))) {
            VisibleIndex index = VisibleIndex.parse(value);
            assertThrows(CommandException.class, "Error: No student exists at index " + value + ".", () ->
                    index.resolve(List.of()));
        }
    }
}
