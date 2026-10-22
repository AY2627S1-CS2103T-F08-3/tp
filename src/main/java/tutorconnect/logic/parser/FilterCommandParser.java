package tutorconnect.logic.parser;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import tutorconnect.commons.core.LogsCenter;
import tutorconnect.commons.util.StudentText;
import tutorconnect.logic.commands.FilterCommand;
import tutorconnect.logic.parser.exceptions.ParseException;
import tutorconnect.model.student.EducationLevel;
import tutorconnect.model.student.StudentCriteriaPredicate;
import tutorconnect.model.student.Subject;

/** Parses optional level/subject criteria without changing any student field. */
public class FilterCommandParser implements Parser<FilterCommand> {
    private static final Logger logger = LogsCenter.getLogger(FilterCommandParser.class);

    @Override
    public FilterCommand parse(String arguments) throws ParseException {
        if (StudentText.hasProhibitedCharacters(arguments)) {
            logger.fine("Criteria query rejected: prohibited text");
            throw new ParseException(StudentParameters.UNEXPECTED_TEXT);
        }
        Map<String, String> values;
        try {
            values = StudentParameters.parseOptional(arguments, "l/LEVEL", "s/SUBJECT");
        } catch (ParseException e) {
            logger.fine("Criteria query rejected: invalid parameter structure");
            throw e;
        }
        if (values.isEmpty()) {
            logger.fine("Criteria query rejected: missing criteria");
            throw new ParseException(FilterCommand.MESSAGE_MISSING_CRITERIA);
        }
        try {
            Optional<EducationLevel> level = Optional.ofNullable(values.get("l/")).map(EducationLevel::new);
            Optional<Subject> subject = Optional.ofNullable(values.get("s/")).map(Subject::new);
            return new FilterCommand(new StudentCriteriaPredicate(level, subject));
        } catch (IllegalArgumentException e) {
            logger.fine("Criteria query rejected: invalid criterion value");
            throw new ParseException(e.getMessage(), e);
        }
    }
}
