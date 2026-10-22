package tutorconnect.model.student;

import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import tutorconnect.commons.util.StudentText;
import tutorconnect.commons.util.ToStringBuilder;

/**
 * Tests that a {@code Student}'s {@code Name} matches any of the keywords given.
 */
public class NameContainsKeywordsPredicate implements Predicate<Student> {
    private final List<String> keywords;

    public NameContainsKeywordsPredicate(List<String> keywords) {
        this.keywords = keywords.stream().map(NameContainsKeywordsPredicate::comparisonKey).toList();
    }

    @Override
    public boolean test(Student student) {
        String name = comparisonKey(student.getName().fullName);
        return keywords.stream().anyMatch(keyword -> !keyword.isEmpty() && name.contains(keyword));
    }

    private static String comparisonKey(String value) {
        return StudentText.normalize(value).toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof NameContainsKeywordsPredicate otherNameContainsKeywordsPredicate)) {
            return false;
        }

        return keywords.equals(otherNameContainsKeywordsPredicate.keywords);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("keywords", keywords).toString();
    }
}
