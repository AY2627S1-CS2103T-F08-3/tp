package tutorconnect.logic.parser;

import java.util.Map;
import java.util.Optional;

import tutorconnect.commons.util.StudentText;
import tutorconnect.logic.commands.FilterCommand;
import tutorconnect.logic.parser.exceptions.ParseException;
import tutorconnect.model.student.EducationLevel;
import tutorconnect.model.student.StudentCriteriaPredicate;
import tutorconnect.model.student.Subject;

/** Parses optional level/subject criteria without changing any student field. */
public class FilterCommandParser implements Parser<FilterCommand> {
    @Override
    public FilterCommand parse(String arguments) throws ParseException {
        if (StudentText.hasProhibitedCharacters(arguments)) {
            throw new ParseException(StudentParameters.UNEXPECTED_TEXT);
        }
        Map<String, String> values = StudentParameters.parseOptional(arguments, "l/LEVEL", "s/SUBJECT");
        if (values.isEmpty()) {
            throw new ParseException(FilterCommand.MESSAGE_MISSING_CRITERIA);
        }
        try {
            Optional<EducationLevel> level = Optional.ofNullable(values.get("l/")).map(EducationLevel::new);
            Optional<Subject> subject = Optional.ofNullable(values.get("s/")).map(Subject::new);
            return new FilterCommand(new StudentCriteriaPredicate(level, subject));
        } catch (IllegalArgumentException e) {
            throw new ParseException(e.getMessage(), e);
        }
    }
}
