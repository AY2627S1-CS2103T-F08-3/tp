package seedu.address.logic.parser;

import static seedu.address.logic.parser.CliSyntax.PREFIX_SUBJECT;

import seedu.address.logic.commands.SubjectCommand;
import seedu.address.logic.parser.IndexedParameterParserUtil.ParsedParameter;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.Subject;

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
