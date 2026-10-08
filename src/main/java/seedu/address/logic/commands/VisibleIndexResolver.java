package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.person.Person;

/** Resolves a user-visible one-based index against the model's currently displayed list. */
public final class VisibleIndexResolver {

    private VisibleIndexResolver() {}

    /** Returns the person at {@code index}, or an error if that displayed entry does not exist. */
    public static Person resolvePerson(Index index, Model model) throws CommandException {
        requireNonNull(index);
        requireNonNull(model);

        if (!index.isWithinSize(model.getFilteredPersonList().size())) {
            throw new CommandException(String.format(
                    Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX, index.getOneBasedString()));
        }
        return model.getFilteredPersonList().get(index.getZeroBasedExact());
    }
}
