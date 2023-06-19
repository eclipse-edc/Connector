# Implementation of a new triage process

## Decision

All issues of the EDC must go through a triage process before being accepted into release planning. To that end, all new
issues will be auto-labelled with a new `triage` label. Bug reports and feature requests will also be
labelled `bug_report` and `feature_request` accordingly.

## Rationale

Newly created issues and bug reports must go through vetting by the technical committee before they can get accepted
into release planning.

At times, bugs have been reported that turn out not to be bugs, or have nothing to do with the EDC code base, e.g. local
configuration issues. These new labels make a clear distinction between bug/feature _requests_, and things that actually
planned by the EDC committers.

## Approach

- The committers go through all issues labelled `triage` every once in a while.
  > _The EDC technical committee explicitly **does not** commit to
  a particular time frame for this!_
- The auto-close bot will be adapted in the following aspects:
    - only considers issues labelled with `triage`
    - issues are marked `stale` after 28 days
    - stale issues are closed after another 14 days
- Issue template must be adapted to auto-assign the `triage` and `bug_report`/`feature_request` label
- After triage, the `bug_report`/`feature_request` label is replaced with the existing labels, and the `triage` label is
  removed. Typically, triaged issues should have an assignee and a target milestone, or should be closed with an
  appropriate label, e.g. `Won't fix`. In case more information is needed from the reporter, the `question` label is
  applied, which resets the stale counter.
- Committers reserve the right to bypass the triage process to expedite urgent features or bug fixes.

