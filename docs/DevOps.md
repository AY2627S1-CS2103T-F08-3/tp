---
  layout: default.md
  title: "DevOps guide"
  pageNav: 3
---

# DevOps guide

<!-- * Table of Contents -->
<page-nav-print />

<!-- -------------------------------------------------------------------------------------------------------------------- -->

## Contributing with a fork

For the course's v1.1–v1.3 iterations, create each PR from a separate branch of your **personal fork** into
the team repository. A branch created directly in the team repository does not meet the forking requirement,
even when its PR is merged and assigned to a milestone.
See the [course GitHub workflow](https://nus-cs2103-ay2627-s1.github.io/website/admin/appendixE-gitHub.html#d-using-prs-to-track-task-progress).

After cloning your fork, run `git remote -v`. `origin` must point to `YOUR_GITHUB_USERNAME/tp`;
`upstream` must point to `AY2627S1-CS2103T-F08-3/tp`. If `upstream` is missing, add it:

```sh
git remote add upstream https://github.com/AY2627S1-CS2103T-F08-3/tp.git
```

Set this checkout's default push destination to your fork:

```sh
git config --local remote.pushDefault origin
git config --local push.default simple
```

Update local `master`, then create a separate branch for the task. Replace `feature/short-description`
with your branch name, and keep any unfinished work on its existing branch before switching:

```sh
git switch master
git fetch upstream master
git merge --ff-only upstream/master
git push origin master
git switch -c feature/short-description
```

Commit the changes, run the relevant checks, and push the branch to **origin**:

```sh
git push -u origin feature/short-description
```

Open the PR in the team repository with your fork as its source. With GitHub CLI, replace
`YOUR_GITHUB_USERNAME`, the branch name and `MILESTONE` with the intended values:

```sh
gh pr create --repo AY2627S1-CS2103T-F08-3/tp --base master \
  --head YOUR_GITHUB_USERNAME:feature/short-description --milestone MILESTONE
```

Before merging, verify the source repository is your personal fork, the source branch is not `master`,
and the PR has the correct milestone. Wait for CI and review, then merge using a merge commit to preserve
the original commit timestamps. Keep the remote feature branch for course verification.

The submission dashboard requires a merged PR as well as the milestone and fork conditions.
After merging, sync `master` with the fetch/fast-forward/push steps above and wait for the next
[dashboard update](https://nus-cs2103-ay2627-s1.github.io/dashboards/contents/tp-progress.html).
Pushing an already-merged source branch to a fork later does not change the original PR's source repository.

--------------------------------------------------------------------------------------------------------------------

## Build automation

This project uses Gradle for **build automation and dependency management**. **We recommend reading [this Gradle tutorial from se-edu/guides](https://se-education.org/guides/tutorials/gradle.html).**


The following commands perform common Gradle tasks.


* **`clean`**: Deletes the files created during the previous build tasks (e.g. files in the `build` folder).<br>
  For example: `./gradlew clean`

* **`shadowJar`**: Uses the Shadow plugin to create the fat JAR file `build/libs/addressbook.jar`.<br>
  For example: `./gradlew shadowJar`

* **`run`**: Builds and runs the application.<br>
  **`runShadow`**: Builds the application as a fat JAR, then runs it.

* **`checkstyleMain`**: Runs the code style check for the main code base.<br>
  **`checkstyleTest`**: Runs the code style check for the test code base.

* **`test`**
  * `./gradlew test`: Runs all tests.
  * `./gradlew clean test`: Cleans the project before running all tests

--------------------------------------------------------------------------------------------------------------------

## Continuous integration (CI)

This project uses GitHub Actions for CI. The necessary workflow configuration files are in `.github/workflows`. No further setup is required.

### Code coverage

As part of CI, Gradle generates JaCoCo coverage reports from the tests, and CI uploads the coverage data to Codecov. Codecov then provides information about test coverage.

However, because Codecov is known to run into intermittent problems (e.g., report upload fails) due to issues on the Codecov service side, the CI is configured to pass even if the Codecov task failed. Therefore, developers are advised to check the code coverage levels periodically and take corrective actions if the coverage level falls below desired levels.

To enable Codecov for forks of this project, follow the steps given in [this se-edu guide](https://se-education.org/guides/tutorials/codecov.html).

### Repository-wide checks

In addition to Gradle checks, CI runs repository-wide checks. Unlike Gradle checks, which cover files used in the build, these checks cover every repository file and enforce rules that are hard to apply on development machines, such as line-ending requirements.

These checks are implemented as POSIX shell scripts, and thus can only be run on POSIX-compliant operating systems such as macOS and Linux. To run all checks locally on these operating systems, execute the following in the repository root directory:

`./.github/run-checks.sh`

Any warnings or errors will be printed out to the console.

**If adding new checks:**

* Checks are implemented as executable `check-*` scripts within the `.github` directory. The `run-checks.sh` script will automatically pick up and run files named as such. That is, you can add more such files if you need and the CI will do the rest.

* Check scripts should print out errors in the format `SEVERITY:FILENAME:LINE: MESSAGE`
  * SEVERITY is either ERROR or WARN.
  * FILENAME is the path to the file relative to the current directory.
  * LINE is the line of the file where the error occurred and MESSAGE is the message explaining the error.

* Check scripts must exit with a non-zero exit code if any errors occur.

--------------------------------------------------------------------------------------------------------------------

## Making a release

Here are the steps to create a new release.

1. Update the version number in [`MainApp.java`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/java/seedu/address/MainApp.java).
1. Generate a fat JAR file using Gradle (i.e., `./gradlew shadowJar`).
1. Tag the repo with the version number. e.g. `v0.1`
1. [Create a new release using GitHub](https://help.github.com/articles/creating-releases/). Upload the JAR file you created.
