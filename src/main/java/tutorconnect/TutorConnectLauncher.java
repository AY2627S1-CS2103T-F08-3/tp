package tutorconnect;

import java.util.logging.Logger;

import javafx.application.Application;
import tutorconnect.commons.core.LogsCenter;

/**
 * The main entry point to the application.
 *
 * This is a workaround for the following error when TutorConnectApp is made the
 * entry point of the application:
 *
 *     Error: JavaFX runtime components are missing, and are required to run this application
 *
 * The reason is that TutorConnectApp extends Application. In that case, the
 * LauncherHelper will check for the javafx.graphics module to be present
 * as a named module. We don't use JavaFX via the module system so it can't
 * find the javafx.graphics module, and so the launch is aborted.
 *
 * By having a separate main class (TutorConnectLauncher) that doesn't extend Application
 * to be the entry point of the application, we avoid this issue.
 */
public class TutorConnectLauncher {
    private static Logger logger = LogsCenter.getLogger(TutorConnectLauncher.class);

    public static void main(String[] args) {

        logger.warning("The warnings about a 'restricted method in java.lang.System' "
            + "and 'enabling native access' appearing below (if any) can be ignored.");
        Application.launch(TutorConnectApp.class, args);
    }
}
