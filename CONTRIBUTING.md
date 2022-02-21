Contributing to Dataspace Connector
===================================

Thank you for your interest in contributing to
the [Eclipse Dataspace Connector](https://projects.eclipse.org/projects/technology.dataspaceconnector)!

## Table of Contents

* [Project Description](#project-description)
* [Code Of Conduct](#code-of-conduct)
* [Eclipse Contributor Agreement](#eclipse-contributor-agreement)
* [How to Contribute](#how-to-contribute)
    * [Discuss](#discuss)
    * [Create an Issue](#create-an-issue)
    * [Submit a Pull Request](#submit-a-pull-request)
* [Contact Us](#contact-us)

## Project Description

See [README.md](README.md) for a comprehensive project description.

## Code Of Conduct

See the [Eclipse Code Of Conduct](https://www.eclipse.org/org/documents/Community_Code_of_Conduct.php).

## Eclipse Contributor Agreement

Before your contribution can be accepted by the project, you need to create and electronically sign
a [Eclipse Contributor Agreement (ECA)](http://www.eclipse.org/legal/ecafaq.php):

1. Log in to the [Eclipse foundation website](https://accounts.eclipse.org/user/login/). You will need to create an
   account with the Eclipse Foundation if you have not already done so.
2. Click on "Eclipse ECA", and complete the form.

Be sure to use the same email address in your Eclipse Account that you intend to use when you commit to GitHub.

## How to Contribute

### Discuss

If you feel there is a bug or an issue contribute to discussions in
[existing issues](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues?q=is%3Aissue+is%3Aopen)
otherwise [create a new issue](#create-an-issue).

### Create an Issue

If you have identified a bug or just want to share an idea to further enhance the project feel free to create a new
issue at our projects corresponding
[Github Issues page](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/new).

Before doing so please consider searching for potentially suitable
[existing issues](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues?q=is%3Aissue+is%3Aopen).

We also
use [Github's default label set](https://docs.github.com/en/issues/using-labels-and-milestones-to-track-work/managing-labels)
to classify issues and improve findability.

### Adhere to coding style guide

We aim for a coherent and consistent code base, thus the coding style detailed in the [styleguide](styleguide.md) should
be followed.

### Submit a Pull Request

In addition to the contribution guideline made available in
the [Eclipse project handbook](https://www.eclipse.org/projects/handbook/#contributing)
we would appreciate if your pull request applies to the following points:

* Always apply the following copyright header to specific files in your work replacing the fields enclosed by curly
  brackets "{}" with your own identifying information. (Don't include the curly brackets!) Enclose the text in the
  appropriate comment syntax for the file format.

    ```text
    Copyright (c) {year} {owner}[ and others]

    This program and the accompanying materials are made available under the
    terms of the Apache License, Version 2.0 which is available at
    https://www.apache.org/licenses/LICENSE-2.0

    SPDX-License-Identifier: Apache-2.0

    Contributors:
      {name} - {description}
    ```

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
  As such, all parties involved in a contribution must have valid ECAs. Additionally commits can include
  a ["Signed-off-by" entry](https://wiki.eclipse.org/Development_Resources/Contributing_via_Git).
* We prefer small changes and short living branches. Please do not combine independent things in one pull request.


* Excessive branching and merging can make git history confusing. With that in mind please

    * [Squash your commits](https://git-scm.com/book/en/v2/Git-Tools-Rewriting-History#_squashing)
      down to a few commits, or one commit, before submitting a pull request and
    * [Rebase](https://git-scm.com/book/en/v2/Git-Branching-Rebasing) your pull request changes on top of the current
      master. Pull requests shouldn't include merge commits.

* Add meaningful tests to verify your submission acts as expected.

* Where code is not self-explanatory add documentation providing extra clarification.

* PR descriptions should use the current [PR template](.github/PULL_REQUEST_TEMPLATE.md)

* Submit a draft pull request at early-stage and add people previously working on the same code as reviewer. Make sure
  automatically checks pass before marking it as "ready for review":

    * _Intellectual Property Validation_ verifying the [Eclipse CLA](#eclipse-contributor-agreement) has been signed as
      well as commits have been signed-off and
    * _Continuous Integration_ performing various test conventions.

### Contact Us

If you have questions or suggestions do not hesitate to contact the project developers via
the [project's "dev" list](https://dev.eclipse.org/mailman/listinfo/dataspaceconnector-dev). 
