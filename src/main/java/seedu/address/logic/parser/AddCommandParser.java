package seedu.address.logic.parser;

import java.util.Map;

import seedu.address.logic.commands.AddCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.Address;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.model.person.Phone;

/** Parses F01 without trimming controls out of field values or accepting optional add parameters. */
public class AddCommandParser implements Parser<AddCommand> {
    @Override
    public AddCommand parse(String args) throws ParseException {
        Map<String, String> values = StudentParameters.parse(args, "n/NAME", "p/PHONE", "a/ADDRESS");
        Name name = ParserUtil.parseName(values.get("n/"));
        Phone phone = ParserUtil.parsePhone(values.get("p/"));
        Address address = ParserUtil.parseAddress(values.get("a/"));
        return new AddCommand(new Person(name, phone, address));
    }
}
