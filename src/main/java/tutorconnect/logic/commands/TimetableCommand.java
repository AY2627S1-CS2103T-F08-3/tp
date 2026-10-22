package tutorconnect.logic.commands;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

import tutorconnect.commons.core.LogsCenter;
import tutorconnect.model.Model;

/** Requests a day or week view of the existing full-register schedule. */
public class TimetableCommand extends Command {
    public static final String COMMAND_WORD = "timetable";
    private static final Logger logger = LogsCenter.getLogger(TimetableCommand.class);
    private final TimetableView view;

    public TimetableCommand(Optional<DayOfWeek> day) {
        view = new TimetableView(day);
    }

    @Override
    public CommandResult execute(Model model) {
        Objects.requireNonNull(model);
        logger.fine("Requesting timetable view for " + view.day().map(DayOfWeek::toString).orElse("the full week"));
        String message = view.day().map(day -> "Showing timetable for "
                + day.getDisplayName(TextStyle.FULL, Locale.ENGLISH) + ".")
                .orElse("Showing weekly timetable.");
        return CommandResult.timetable(message, view);
    }
}
