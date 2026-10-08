package seedu.address.testutil;

import javafx.application.Platform;

/**
 * Shared toolkit startup for independently selectable desktop UI test classes.
 */
public final class JavaFxTestUtil {
    private JavaFxTestUtil() {}

    /**
     * Starts JavaFX once; additional test classes reuse its running event thread.
     */
    public static void startToolkit() {
        try {
            Platform.startup(() -> Platform.setImplicitExit(false));
        } catch (IllegalStateException alreadyStarted) {
            Platform.runLater(() -> Platform.setImplicitExit(false));
        }
    }
}
