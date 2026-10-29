package tutorconnect.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

public class CommandGuideTest {
    @Test
    public void entries_coverEveryParserCommandOnceInAKnownGroup() {
        Set<String> words = CommandGuide.entries().stream().map(CommandGuide.Entry::word).collect(Collectors.toSet());
        assertEquals(Set.of("add", "edit", "delete", "list", "find", "filter", "clear", "guardian", "level",
                "subject", "rate", "note", "clearnote", "schedule", "unschedule", "timetable", "assess",
                "assessments", "unassess", "payment", "payments", "help", "exit"), words);
        assertEquals(words.size(), CommandGuide.entries().size());
        CommandGuide.entries().forEach(entry -> {
            assertTrue(entry.format().startsWith(entry.word()), entry.word());
            assertTrue(CommandGuide.GROUPS.contains(entry.group()), entry.group());
        });
    }

    @Test
    public void complete_uniquePrefix_completesWordWithSpace() {
        assertEquals(Optional.of("schedule "), CommandGuide.complete("sch"));
        assertEquals(Optional.of("timetable "), CommandGuide.complete("  ti"));
        assertEquals(Optional.of("delete "), CommandGuide.complete("delete"));
    }

    @Test
    public void complete_sharedPrefix_extendsToCommonPrefixOrExactWord() {
        assertEquals(Optional.of("payment"), CommandGuide.complete("pay"));
        assertEquals(Optional.of("payment "), CommandGuide.complete("payment"));
        assertEquals(Optional.of("clear"), CommandGuide.complete("cl"));
        assertEquals(Optional.of("assess "), CommandGuide.complete("assess"));
    }

    @Test
    public void complete_nothingToComplete_returnsEmpty() {
        assertEquals(Optional.empty(), CommandGuide.complete(""));
        assertEquals(Optional.empty(), CommandGuide.complete("xyz"));
        assertEquals(Optional.empty(), CommandGuide.complete("a"));
        assertEquals(Optional.empty(), CommandGuide.complete("sch 1"));
    }

    @Test
    public void hintFor_showsFormatCandidatesOrNothing() {
        assertEquals("", CommandGuide.hintFor("   "));
        assertEquals("schedule INDEX d/DAY t/TIME   (Tab to complete)", CommandGuide.hintFor("sched"));
        assertEquals("schedule INDEX d/DAY t/TIME", CommandGuide.hintFor("schedule 1 d/Mon"));
        assertEquals("payment   payments", CommandGuide.hintFor("pay"));
        assertEquals("payment INDEX m/YYYY-MM s/pending|completed", CommandGuide.hintFor("payment "));
        assertEquals("", CommandGuide.hintFor("xyz"));
        assertEquals("", CommandGuide.hintFor("sched 1"));
    }

    @Test
    public void wordsStartingWith_keepsGuideOrder() {
        assertEquals(List.of("assess", "assessments"), CommandGuide.wordsStartingWith("ass"));
    }
}
