package tutorconnect.logic.parser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Tokenizes the legacy edit grammar, retaining repeated and empty values.
 * Only supplied prefixes preceded by an ASCII space are recognized; embedded
 * slashes and unknown prefixes remain data. Values and the preamble are trimmed.
 * The stricter student-command grammar belongs to {@link StudentParameters}.
 */
public class ArgumentTokenizer {

    /** Maps each recognized prefix to its values in input order. */
    public static ArgumentMultimap tokenize(String argsString, Prefix... prefixes) {
        List<PrefixPosition> positions = new ArrayList<>();
        for (Prefix prefix : prefixes) {
            String boundary = " " + prefix.getPrefix();
            int position = argsString.indexOf(boundary);
            while (position != -1) {
                positions.add(new PrefixPosition(prefix, position + 1));
                position = argsString.indexOf(boundary, position + 1);
            }
        }
        positions.sort(Comparator.comparingInt(PrefixPosition::start));
        // Empty-prefix markers let the same loop extract both the preamble and final value.
        positions.addFirst(new PrefixPosition(new Prefix(""), 0));
        positions.add(new PrefixPosition(new Prefix(""), argsString.length()));

        ArgumentMultimap arguments = new ArgumentMultimap();
        for (int i = 0; i < positions.size() - 1; i++) {
            PrefixPosition current = positions.get(i);
            int valueStart = current.start() + current.prefix().getPrefix().length();
            String value = argsString.substring(valueStart, positions.get(i + 1).start()).trim();
            arguments.put(current.prefix(), value);
        }
        return arguments;
    }

    /** Immutable location of a recognized prefix. */
    private record PrefixPosition(Prefix prefix, int start) {}
}
