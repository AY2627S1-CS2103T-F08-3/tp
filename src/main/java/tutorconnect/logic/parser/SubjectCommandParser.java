package tutorconnect.logic.parser;

import static tutorconnect.logic.parser.CliSyntax.PREFIX_SUBJECT;

import tutorconnect.logic.commands.SubjectCommand;
import tutorconnect.logic.parser.IndexedParameterParserUtil.ParsedParameter;
import tutorconnect.logic.parser.exceptions.ParseException;
import tutorconnect.model.student.Subject;

/** Parses input arguments for the {@code subject} command. */
public class SubjectCommandParser implements Parser<SubjectCommand> {

    @Override
    public SubjectCommand parse(String arguments) throws ParseException {
        ParsedParameter parsed = IndexedParameterParserUtil.parse(arguments, PREFIX_SUBJECT, "s/SUBJECT");
        try {
            return new SubjectCommand(parsed.index(), new Subject(parsed.value()));
        } catch (IllegalArgumentException e) {
            throw new ParseException(Subject.MESSAGE_CONSTRAINTS, e);
        }
    }
}
