package tutorconnect.logic.parser;

import java.util.Map;

import tutorconnect.logic.commands.AddCommand;
import tutorconnect.logic.parser.exceptions.ParseException;
import tutorconnect.model.student.Address;
import tutorconnect.model.student.Name;
import tutorconnect.model.student.Phone;
import tutorconnect.model.student.Student;

/** Parses F01 without trimming controls out of field values or accepting optional add parameters. */
public class AddCommandParser implements Parser<AddCommand> {
    @Override
    public AddCommand parse(String args) throws ParseException {
        Map<String, String> values = StudentParameters.parse(args, "n/NAME", "p/PHONE", "a/ADDRESS");
        Name name = ParserUtil.parseName(values.get("n/"));
        Phone phone = ParserUtil.parsePhone(values.get("p/"));
        Address address = ParserUtil.parseAddress(values.get("a/"));
        return new AddCommand(new Student(name, phone, address));
    }
}
