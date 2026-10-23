---
  layout: default.md
  title: "Setting up and getting started"
  pageNav: 3
---

# Setting up and getting started

<!-- * Table of Contents -->
<page-nav-print />

--------------------------------------------------------------------------------------------------------------------

## Setting up the project on your computer

<box type="warning" seamless>

**Caution:** Follow the steps below precisely. The setup may fail if you skip or change a step.
</box>

First, **fork** this repo, and **clone** the fork into your computer.
Check that `origin` points to your personal fork and `upstream` points to the team repository.
Before creating a branch or PR, follow the [fork-based contribution workflow](DevOps.md#contributing-with-a-fork).

If you plan to use IntelliJ IDEA (highly recommended):

1. **Configure the JDK**: Follow the guide [_[se-edu/guides] IDEA: Configuring the JDK_](https://se-education.org/guides/tutorials/intellijJdk.html) to ensure IntelliJ and Gradle use **JDK 25**, matching `build.gradle`.
1. **Import the project as a Gradle project**: Follow the guide [_[se-edu/guides] IDEA: Importing a Gradle project_](https://se-education.org/guides/tutorials/intellijImportGradleProject.html) to import the project into IDEA.
   <box type="warning" seamless>
   Note: Importing a Gradle project is slightly different from importing a normal Java project.
   </box>
1. **Verify the setup**:
   1. Run `tutorconnect.TutorConnectLauncher` and try a few commands.
   1. [Run the tests](Testing.md) to ensure they all pass.

--------------------------------------------------------------------------------------------------------------------

## Building and running from a terminal

Use JDK 25 with JavaFX, matching the Zulu JDK+FX distribution in CI. Set `JAVA_HOME`
to its installation directory and add its `bin` directory to `PATH`. Do not use
the literal placeholder below; substitute your installed JDK path.

```sh
export JAVA_HOME="/path/to/your/jdk-25-with-javafx"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
./gradlew --version
```

On Windows, set `JAVA_HOME` and `PATH` using your environment settings and use
`gradlew.bat` in place of `./gradlew`. Run these commands from the cloned repository
(the directory containing `build.gradle`), not its parent folder. Internet access
is needed on the first build to download the pinned wrapper and dependencies.
The wrapper verifies new Gradle downloads against the official SHA-256 checksum.
The Java toolchain requires an installed JDK 25; it does not install one for you.

To launch from source:

```sh
./gradlew run
```

Wait for the JavaFX window, then enter commands in its command box. The terminal
is for startup and logs, not student commands. Close the window or enter `exit`
in the app to quit. A desktop display is required; Linux CI uses Xvfb.

`./gradlew run` and `./gradlew test` run with Java assertions enabled (`-ea`), as
set in `build.gradle`. `AssertionsEnabledTest` fails if tests ever run without
them. When launching from IntelliJ IDEA, add `-ea` under **Run > Edit
Configurations > Modify options > Add VM options**. `java -jar` leaves assertions
off; use `java -ea -jar build/libs/tutorconnect.jar` when developing.

To build and launch a standalone executable JAR:

```sh
./gradlew build
java -jar build/libs/tutorconnect.jar
```

Running `./gradlew` with no task also runs `build`. It compiles, runs tests and
Checkstyle, packages the application, and checks the executable manifest and
essential UI resources. It does not delete existing build outputs first; use
`./gradlew clean build` when a clean rebuild is needed. `shadowJar` alone packages
the JAR without running tests; do not treat it as complete verification.

Desktop tests require explicit opt-in. On macOS/Linux with a display, run:

```sh
F01_UI_TESTS=true F06_UI_TESTS=true F08_UI_TESTS=true ./gradlew build coverage
```

Without these flags, UI tests are skipped. See [Testing](Testing.md) for Windows
environment-variable examples and the full acceptance workflow.

Data remains in `data/addressbook.json` and `preferences.json` relative to the
launch working directory. `./gradlew run` uses the repository directory. A JAR
launched elsewhere uses that other directory, so an apparently different student
list can simply mean a different data folder. For disposable testing, launch the
JAR by its absolute path from a fresh temporary directory. Do not clear real data.

If Java is not found or is the wrong version, correct `JAVA_HOME` and `PATH` and
check both version commands above. If Gradle cannot find a Java 25 toolchain,
install/select JDK 25 with JavaFX and retry. Native-access warnings from JavaFX
under Java 25 are known; a warning alone is not an application startup failure.

## Before writing code

1. **Configure the coding style**

   If using IDEA, follow the guide [_[se-edu/guides] IDEA: Configuring the code style_](https://se-education.org/guides/tutorials/intellijCodeStyle.html) to set up IDEA's coding style to match ours.

   <box type="tip" seamless>

   **Tip:**
    Optionally, follow the guide [_[se-edu/guides] Using Checkstyle_](https://se-education.org/guides/tutorials/checkstyle.html) to learn how to use Checkstyle in IDEA, for example to report problems _as_ you write code.
   </box>

1. **Set up CI**

   This project includes GitHub Actions configuration files in `.github/workflows`. GitHub runs CI automatically for every push and pull request. No setup is required.

1. **Learn the design**

   When you are ready to start coding, we recommend that you get some sense of the overall design by reading about [TutorConnect’s architecture](DeveloperGuide.md#architecture).

1. **Do the tutorials**
   These tutorials will help you get acquainted with the codebase.

   * [Tracing code](https://se-education.org/guides/tutorials/ab3TracingCode.html)
   * [Adding a new command](https://se-education.org/guides/tutorials/ab3AddRemark.html)
   * [Removing fields](https://se-education.org/guides/tutorials/ab3RemovingFields.html)
