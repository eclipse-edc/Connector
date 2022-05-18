# Etiquette for pull requests

## As an author

Submitting pull requests in EDC should be done while adhering to a couple of simple rules.

- Familiarize yourself with [coding style](styleguide.md), [architectural patterns](docs/architecture-principles.md) and
  other contribution guidelines.
- No surprise PRs please. Before you submit a PR, open a discussion or an issue outlining your planned work and give
  people time to comment. It may even be advisable to contact committers using the `@mention` feature. Unsolicited PRs
  may get ignored or rejected.
- Create focused PRs: your work should be focused on one particular feature or bug. Do not create broad-scoped PRs that
  solve multiple issues as reviewers may reject those PR bombs outright.
- Provide a clear description and motivation in the PR description in GitHub. This makes the reviewer's life much
  easier. It is also helpful to outline the broad changes that were made, e.g. "Changes the schema of XYZ-Entity:
  the `age` field changed from `long` to `String`".
- If you introduce new 3rd party dependencies, be sure to note them in the PR description and explain why they are
  necessary.
- Stick to the established code style, please refer to the [styleguide document](styleguide.md).
- All tests should be green, especially when your PR is in `"Ready for review"`
- Mark PRs as `"Ready for review"` only when you're prepared to defend your work. By that time you have completed your
  work and shouldn't need to push any more commits other than to incorporate review comments.
- Merge conflicts should be resolved by squashing all commits on the PR branch, rebasing onto `main` and
  force-pushing. Do this when your PR is ready to review.
- If you require a reviewer's input while it's still in draft, please contact the designated reviewer using
  the `@mention` feature and let them know what you'd like them to look at.
- Request a review from one of the [technical committers](pr_etiquette.md#the-technical-committers-as-of-may-13-2022). Requesting a review from anyone else is still possible, and
  sometimes may be advisable, but only committers can merge PRs, so be sure to include them early on.
- Re-request reviews after all remarks have been adopted. This helps reviewers track their work in GitHub.
- If you disagree with a committer's remarks, feel free to object and argue, but if no agreement is reached, you'll have
  to either accept the decision or withdraw your PR.
- Be civil and objective. No foul language, insulting or otherwise abusive language will be tolerated.

## As a reviewer

- Please complete reviews within two business days or delegate to another committer, removing yourself as a reviewer.
- If you have been requested as reviewer, but cannot do the review for any reason (time, lack of knowledge in particular
  area, etc.) please comment that in the PR and remove yourself as a reviewer, suggesting a stand-in. The [code
  owners document](CODEOWNERS) should help with that.
- Don't be overly pedantic.
- Don't argue basic principles (code style, architectural decisions, etc.)
- Use the `suggestion` feature of GitHub for small/simple changes.
- The following could serve you as a review checklist:
    - no unnecessary dependencies in `build.gradle.kts`
    - sensible unit tests, prefer unit tests over integration tests wherever possible (test runtime). Also check the
      usage of test tags.
    - code style
    - simplicity and "uncluttered-ness" of the code
    - overall focus of the PR
- Don't just wave through any PR. Please take the time to look at them carefully.
- Be civil and objective. No foul language, insulting or otherwise abusive language will be tolerated. The goal is to
  _encourage_ contributions.

## The technical committers (as of May 18, 2022)

- @MoritzKeppler
- @jimmarino
- @bscholtes1A
- @ndr_brt
- @ronjaquensel
- @juliapampus
- @paullatzelsperger
