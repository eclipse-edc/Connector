Contributing to DataSpaceConnector
==================================

Thank you for your interest contributing to
the [Eclipse DataSpace Connector](https://projects.eclipse.org/projects/dataspaceconnector)!

## Table of Contents

* [Project Description](#project-description)
* [Code Of Conduct](#code-of-conduct)
* [Eclipse Contributor Agreement](#eclipse-contributor-agreement)
* [How to Contribute](#how-to-contribute)
    * [Discuss](#discuss)
    * [File an Issue](#file-an-issue)
    * [Submit a Pull Request](#submit-a-pull-request)

## Project Description

See [README.md](README.md) for a comprehensive project description.

## Code Of Conduct

This project is governed by
the [Eclipse Code Of Conduct](https://www.eclipse.org/org/documents/Community_Code_of_Conduct.php). You are expected to
keep up to the code of conduct while participating in this project.

## Eclipse Contributor Agreement

Before your contribution can be accepted by the project, you need to create and electronically sign
a [Eclipse Contributor Agreement (ECA)](http://www.eclipse.org/legal/ecafaq.php):

1. Log in to the [Eclipse foundation website](https://accounts.eclipse.org/user/login/). You will need to create an
   account with the Eclipse Foundation if you have not already done so.
2. Click on "Eclipse ECA", and complete the form.

Be sure to use the same email address in your Eclipse account that you intend to use when you commit to GitHub.

## How to contribute

### Discuss

If you have questions or suggestions feel free to contact the project developers via
the [project's "dev" list](https://dev.eclipse.org/mailman/listinfo/dataspaceconnector-dev). If you suppose there is a
bug or issue contribute to discussions in
[existing issues](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues?q=is%3Aissue+is%3Aopen)
otherwise [file a new issue](#file-and-issue)

### File an issue

Although your contributions are always welcome please remember to search
for [existing issues](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues?q=is%3Aissue+is%3Aopen)
before creating one.

If none of the existing issues meet your expectation feel free to file a new one at our projects
corresponding [Github Issues page](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/new).

We
use [Github's default label set](https://docs.github.com/en/issues/using-labels-and-milestones-to-track-work/managing-labels)
to classify issues.

### Submit a Pull Request

* If you have not previously done so, please sign
  the [Eclipse Contributor Agreement (ECA)](http://www.eclipse.org/legal/ecafaq.php).

* The git commit messages should comply to the following format:
    ```
    <component>: <description>
    ```

  Use the [imperative mood](https://github.com/git/git/blob/master/Documentation/SubmittingPatches)
  as in "Fix bug" or "Add feature" rather than "Fixed bug" or "Added feature" and
  [mention the GitHub issue](https://docs.github.com/en/issues/tracking-your-work-with-issues/linking-a-pull-request-to-an-issue)
  e.g. `transfer process: improve logging, closes #3`.

  All committers, and all commits, are bound to
  the [Developer Certificate of Origin.](https://www.eclipse.org/legal/DCO.php)
  As such, all parties involved in a contribution must have valid ECAs and commits must
  include [valid "Signed-off-by" entries](https://wiki.eclipse.org/Development_Resources/Contributing_via_Git). Commits
  can be signed off by included the `-s` attribute in your commit message, for
  example, `git commit -s -am 'Interesting Commit Message.`
  
* We prefer small changes and short living branches. Please do not combine independent things in one pull request.

* Prefix the branch you're submitting with an issue number, e.g. `725-add-new-feature`.

* Excessive branching and merging can make git history confusing. With that in mind please

    * [Squash your commits](https://git-scm.com/book/en/v2/Git-Tools-Rewriting-History#_squashing)
      down to a few commits, or one commit, before submitting a pull request and
    * [Rebase](https://git-scm.com/book/en/v2/Git-Branching-Rebasing) your pull request changes on top of the current
      master. Pull requests shouldn't include merge commits.

* Add meaningful tests to verify your submission acts as expected.

* Where code is not self-explanatory add documentation providing extra clarification.

* Submit your pull request when ready. Make sure automatically kicked off checks pass, thus as

    * _Intellectual Property Validation_ verifying the [Eclipse CLA](#eclipse-contributor-agreement) as well as commits
      have been signed and
    * _Continuous Integration_ performing various test proving functional integrity and alignment with
      our [coding conventions](#coding-conventions).

 
