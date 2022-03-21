# Creating "story" issues

## Decision

For larger features, for which it seems reasonable to create more than one issue, we will create overarching issues which 
we'll name "story issues". Every story issues has several sub-issues a.k.a sub-tasks.

## Rationale

There is no clear limit or rule that specifies the size at which issues need to be broken into sub-issues, rather, 
this should be done by intuition. Developers are expected to have an intrinsic understanding of the natural boundaries
of features. In case of doubt it is better to create several small issues and list them in an overarching
story issue. 

Sub-issues may initially just be placeholders. That shows readers how the story is intended to be implemented and 
which aspects have been taken into account, even if certain details might still be missing.

Finally, having small, focused issues and PRs makes them more digestible, which in turn will benefit the review process.  

## Approach

Issues should be broken up if
- there is more than one area of code involved (e.g. API, store,...)
- work streams can be parallelized
- there are orthogonal concerns

The overarching story issues should contain a bulleted list with references to all sub-issues, for example:
```markdown
- [ ] #123
- [ ] #456
...
```

Please add the `"story"` label to the overarching story issue.

This will cause GitHub to automatically track the sub-issues in the overarching story issue, and it will display a progress
indicator on overview pages. More information can be found in the [documentation](https://docs.github.com/en/issues/tracking-your-work-with-issues/about-task-lists).

For example, imagine a story that requires some lib analysis or evaluation before the lib is adopted into the code base could be split into the following sub-tasks:
- evaluate lib X, lib Y and lib Z, create ADR
- implement adapter for lib
- add overall system-test 

Generally speaking there should be one PR per sub-issue.
