Contributing to the Project
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
* [Project and Milestone Planning](#project-and-milestone-planning)
  * [Milestones](#milestones)
  * [Projects](#projects)
* [Contact Us](#contact-us)

## Project Description

See [README.md](README.md) for a comprehensive project description.

## Code Of Conduct

See the [Eclipse Code Of Conduct](https://www.eclipse.org/org/documents/Community_Code_of_Conduct.php).

## Eclipse Contributor Agreement

Before your contribution can be accepted by the project, you need to create and electronically sign
a [Eclipse Contributor Agreement (ECA)](http://www.eclipse.org/legal/ecafaq.php):

1. Log in to the [Eclipse foundation website](https://accounts.eclipse.org/user/login/). You will 
   need to create an account within the Eclipse Foundation if you have not already done so.
2. Click on "Eclipse ECA", and complete the form.

Be sure to use the same email address in your Eclipse Account that you intend to use when you commit 
to GitHub.

## How to Contribute

### Discuss

If you feel there is a bug or an issue, contribute to the discussions in
[existing issues](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues?q=is%3Aissue+is%3Aopen),
otherwise [create a new issue](#create-an-issue).

### Create an Issue

If you have identified a bug or just want to share an idea to further enhance the project, feel free 
to create a new issue at our project's corresponding
[Github Issues page](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/new).

Before doing so, please consider searching for potentially suitable
[existing issues](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues?q=is%3Aissue+is%3Aopen).

We also use [Github's default label set](https://docs.github.com/en/issues/using-labels-and-milestones-to-track-work/managing-labels)
extended by custom ones to classify issues and improve findability.

### Adhere to coding style guide

We aim for a coherent and consistent code base, thus the coding style detailed in the 
[styleguide](styleguide.md) should be followed.

### Submit a Pull Request

In addition to the contribution guideline made available in the 
[Eclipse project handbook](https://www.eclipse.org/projects/handbook/#contributing),
we would appreciate if your pull request applies to the following points:

* Always apply the following copyright header to specific files in your work replacing the fields 
  enclosed by curly brackets "{}" with your own identifying information. (Don't include the curly 
  brackets!) Enclose the text in the appropriate comment syntax for the file format.

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
  As such, all parties involved in a contribution must have valid ECAs. Additionally, commits can 
  include a ["Signed-off-by" entry](https://wiki.eclipse.org/Development_Resources/Contributing_via_Git).
* We prefer small changes and short living branches. Please do not combine independent things in one
  pull request.


* Excessive branching and merging can make git history confusing. With that in mind please

    * [squash your commits](https://git-scm.com/book/en/v2/Git-Tools-Rewriting-History#_squashing)
      down to a few commits, or one commit, before submitting a pull request and
    * [rebase](https://git-scm.com/book/en/v2/Git-Branching-Rebasing) your pull request changes on 
      top of the current master. Pull requests shouldn't include merge commits.

* Add meaningful tests to verify your submission acts as expected.

* Where code is not self-explanatory, add documentation providing extra clarification.

* Submit a draft pull request at early-stage and add people previously working on the same code as 
  reviewer. Make sure automatically checks pass before marking it as "ready for review":

    * _Intellectual Property Validation_ verifying the [Eclipse CLA](#eclipse-contributor-agreement) 
      has been signed as well as commits have been signed-off and
    * _Continuous Integration_ performing various test conventions.

## Project and Milestone Planning

We use milestones to set a common focus for a period of 6 to 8 weeks. 
The group of committers chooses issues based on customer needs and contributions we expect.

### Projects

The [Github Projects page](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/projects)
provides a general overview of the project's working items. Every new issue needs to be assigned to
the ["Dataspace Connector" project](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/projects/1).
There, an issue passes five stages:
* `Open`: All assigned issues will automatically have this status.
* `Backlog`: An open issue does not automatically imply that it is going to be addressed in upcoming
  milestones and releases - maybe someone opened it to report a bug or ask a question. An issue that 
  will be resolved must be put to this column.
* `In progress`: Reopened issues and pull requests or new pull requests will be added to this column
  automatically if they previously have been assigned to the project.
* `Review in progress`: This covers pull requests that are no longer marked as draft and have 
  pending approvals.
* `Done`: All resolved or closed issues and pull requests will remain here.

If an issue covers a topic or the response to a question that may be interesting for other 
developers or contributors, it should be converted to a discussion and not be closed!

### Milestones

Milestones are organized at the 
[Github Milestones page](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/milestones).
They are numbered in ascending order, e.g. `Milestone 1`, `Milestone 2`, ... Closed milestones can
always be viewed in the according 
[tab](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/milestones?state=closed).

The ongoing milestone has a due date - that is **no guarantee** that all linked issues will be 
resolved by then. 
Further milestones, that are already known, can be created any time, and issues can be assigned
to them. However, for every new milestone, a due date will be voted in the committers round only as
soon as the current milestone is closed.

The milestone `Milestone Scoping` is a permanent milestone without a due date. Issues that should be 
considered for future milestones but already need to be targeted for the current one will be 
assigned here. This way, contributors, users, and adopters can track the progress.

When closing the current milestone, issues assigned to `Milestone Scoping` can stay there, be moved 
to the next milestone, or get removed from all milestones. In general, issues that were not resolved 
within a milestone phase will be reviewed to evaluate their relevance and priority, before being
assigned to the next milestone.

#### Issues

Every issue that should be addressed during a milestone phase is assigned to it by using the 
`Milestone` feature for linking both items. This way, the issues can easily be filtered by 
milestones.

#### Pull Requests

Pull requests are not assigned to milestones, as their linking to issues are sufficient to track 
the relations and progresses.

### Contact Us

If you have questions or suggestions, do not hesitate to contact the project developers via
the [project's "dev" list](https://dev.eclipse.org/mailman/listinfo/dataspaceconnector-dev). 
