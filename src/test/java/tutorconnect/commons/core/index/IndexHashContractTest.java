package tutorconnect.commons.core.index;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import tutorconnect.logic.commands.RateCommand;
import tutorconnect.model.student.HourlyRate;

public class IndexHashContractTest {
    @Test
    public void equalIndices_workAsHashKeysAcrossFactories() {
        Index first = Index.fromOneBased(1);
        for (Index equal : List.of(Index.fromZeroBased(0), Index.fromOneBased("1"))) {
            assertEquals(first, equal);
            assertEquals(first.hashCode(), equal.hashCode());
            assertTrue(new HashSet<>(List.of(first)).contains(equal));
            var lookup = new HashMap<Index, String>();
            lookup.put(first, "student");
            assertEquals("student", lookup.get(equal));
        }
    }

    @Test
    public void hugeIndices_hashWithoutNarrowing() {
        String value = "9".repeat(100);
        Index first = Index.fromOneBased(value);
        Index equal = Index.fromOneBased(value);
        assertEquals(first.hashCode(), equal.hashCode());
        assertTrue(new HashSet<>(List.of(first)).contains(equal));
    }

    @Test
    public void equalRateCommands_haveEqualHashes() {
        RateCommand first = new RateCommand(Index.fromOneBased(1), new HourlyRate(new BigDecimal("40")));
        RateCommand equal = new RateCommand(Index.fromZeroBased(0), new HourlyRate(new BigDecimal("40.00")));
        assertEquals(first, equal);
        assertEquals(first.hashCode(), equal.hashCode());
        assertTrue(new HashSet<>(List.of(first)).contains(equal));
    }
}
