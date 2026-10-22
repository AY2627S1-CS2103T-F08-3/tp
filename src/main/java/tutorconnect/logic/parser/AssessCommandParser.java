package tutorconnect.logic.parser;

import tutorconnect.commons.core.index.Index;
import tutorconnect.commons.util.StudentText;
import tutorconnect.logic.commands.AssessCommand;
import tutorconnect.logic.parser.exceptions.ParseException;
import tutorconnect.model.student.AssessmentRecord;

/**
 * Parses {@code assess INDEX d/DATE t/TITLE g/GRADE}. The prefixes may come in any order. Structure is checked
 * first, then the index, then the date, title and grade in that order.
 */
public class AssessCommandParser implements Parser<AssessCommand> {

    @Override
    public AssessCommand parse(String arguments) throws ParseException {
        StudentParameters.IndexedParameters parameters =
                StudentParameters.parseIndexedValues(arguments, "d/DATE", "t/TITLE", "g/GRADE");
        Index index = parseIndex(parameters.index());
        try {
            return new AssessCommand(index, AssessmentRecord.of(parameters.values().get("d/"),
                    parameters.values().get("t/"), parameters.values().get("g/")));
        } catch (IllegalArgumentException e) {
            throw new ParseException(e.getMessage(), e);
        }
    }

    private static Index parseIndex(String rawIndex) throws ParseException {
        try {
            return ParserUtil.parseIndex(StudentText.normalize(rawIndex));
        } catch (IllegalArgumentException e) {
            throw new ParseException(ParserUtil.MESSAGE_INVALID_INDEX, e);
        }
    }
}
