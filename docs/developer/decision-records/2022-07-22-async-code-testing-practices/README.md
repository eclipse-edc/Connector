# Unify asynchronous code testing practices

## Decision

Currently, the testing of async code is performed in two different ways:

* using the CoundDownLatch object
* using the Awaitility library

The focus on CountDownLatch objects is to cause a thread to block until other threads have completed a given task.

The idea is to avoid using the first one and start only using the second one.

## Rationale

Testing asynchronous systems is hard. Not only does it require handling threads, timeouts and concurrency issues, but
the intent of the test code can be obscured by all these details while using CountDownLatch. Awaitility is a DSL that
allows to express expectations of an asynchronous system in a conciser and easier to read manner. Additionally
CoundDownLatch works only based on latches while Awaitility works based on time (seconds, nanoseconds), which can be
more useful to do time testing on threads.

## Approach

