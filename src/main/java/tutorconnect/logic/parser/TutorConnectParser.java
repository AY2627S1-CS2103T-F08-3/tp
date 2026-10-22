package tutorconnect.logic.parser;

import static tutorconnect.logic.Messages.MESSAGE_UNKNOWN_COMMAND;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tutorconnect.commons.core.LogsCenter;
import tutorconnect.commons.util.StudentText;
import tutorconnect.logic.commands.AddCommand;
import tutorconnect.logic.commands.AssessCommand;
import tutorconnect.logic.commands.AssessmentsCommand;
import tutorconnect.logic.commands.ClearCommand;
import tutorconnect.logic.commands.Command;
import tutorconnect.logic.commands.DeleteCommand;
import tutorconnect.logic.commands.EditCommand;
import tutorconnect.logic.commands.ExitCommand;
import tutorconnect.logic.commands.FindCommand;
import tutorconnect.logic.commands.GuardianCommand;
import tutorconnect.logic.commands.HelpCommand;
import tutorconnect.logic.commands.LevelCommand;
import tutorconnect.logic.commands.ListCommand;
import tutorconnect.logic.commands.RateCommand;
import tutorconnect.logic.commands.ScheduleCommand;
import tutorconnect.logic.commands.SubjectCommand;
import tutorconnect.logic.commands.UnassessCommand;
import tutorconnect.logic.parser.exceptions.ParseException;

/**
 * Parses user input.
 */
public class TutorConnectParser {

    /**
     * Used for initial separation of command word and args.
     */
    private static final Pattern BASIC_COMMAND_FORMAT = Pattern.compile("(?<commandWord>\\S+)(?<arguments>.*)",
            Pattern.DOTALL | Pattern.UNICODE_CHARACTER_CLASS);
    private static final Logger logger = LogsCenter.getLogger(TutorConnectParser.class);

    /**
     * Parses user input into command for execution.
     *
     * @param userInput full user input string
     * @return the command based on the user input
     * @throws ParseException if the user input does not conform to the expected format
     */
    public Command parseCommand(String userInput) throws ParseException {
        final Matcher matcher = BASIC_COMMAND_FORMAT.matcher(userInput.replaceFirst("^\\p{Zs}+", "").stripLeading());
        if (!matcher.matches()) {
            throw new ParseException(MESSAGE_UNKNOWN_COMMAND);
        }

        final String commandWord = matcher.group("commandWord");
        final String arguments = matcher.group("arguments");
        if (commandWord.equals(ScheduleCommand.COMMAND_WORD) && StudentText.hasProhibitedCharacters(userInput)) {
            throw new ParseException(StudentParameters.UNEXPECTED_TEXT);
        }

        if ((commandWord.equals(DeleteCommand.COMMAND_WORD) || commandWord.equals(ListCommand.COMMAND_WORD))
                && userInput.codePoints().anyMatch(c -> Character.isISOControl(c)
                || Character.getType(c) == Character.FORMAT || c == 0x2028 || c == 0x2029)) {
            throw new ParseException(commandWord.equals(DeleteCommand.COMMAND_WORD)
                    ? VisibleIndex.MESSAGE_INVALID : ListCommandParser.MESSAGE_PARAMETERS);
        }

        // Note to developers: Change LOG_LEVEL in LogsCenter to enable lower level (i.e., FINE, FINER and lower)
        // log messages such as the one below.
        // Lower level log messages are used sparingly to minimize noise in the code.
        logger.fine("Command word: " + commandWord + "; Arguments: " + arguments);

        return switch (commandWord) {
            case AddCommand.COMMAND_WORD -> new AddCommandParser().parse(arguments);
            case EditCommand.COMMAND_WORD -> new EditCommandParser().parse(arguments);
            case DeleteCommand.COMMAND_WORD -> new DeleteCommandParser().parse(arguments);
            case AssessCommand.COMMAND_WORD -> new AssessCommandParser().parse(arguments);
            case AssessmentsCommand.COMMAND_WORD -> new AssessmentsCommandParser().parse(arguments);
            case UnassessCommand.COMMAND_WORD -> new UnassessCommandParser().parse(arguments);
            case SubjectCommand.COMMAND_WORD -> new SubjectCommandParser().parse(arguments);
            case GuardianCommand.COMMAND_WORD -> new GuardianCommandParser().parse(arguments);
            case RateCommand.COMMAND_WORD -> new RateCommandParser().parse(arguments);
            case LevelCommand.COMMAND_WORD -> new LevelCommandParser().parse(arguments);
            case ClearCommand.COMMAND_WORD -> new ClearCommand();
            case FindCommand.COMMAND_WORD -> new FindCommandParser().parse(arguments);
            case ListCommand.COMMAND_WORD -> new ListCommandParser().parse(arguments);
            case ScheduleCommand.COMMAND_WORD -> new ScheduleCommandParser().parse(arguments);
            case ExitCommand.COMMAND_WORD -> new ExitCommand();
            case HelpCommand.COMMAND_WORD -> new HelpCommand();
            default -> {
                logger.finer("This user input caused a ParseException: " + userInput);
                throw new ParseException(MESSAGE_UNKNOWN_COMMAND);
            }
        };
    }

}
