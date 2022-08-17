# Unify asynchronous code testing practices

## Decision

Currently, the testing of async code is performed in two different ways:

* using the `CoundDownLatch` object
* using the `Awaitility` library

The goal of `CountDownLatch` objects and the `Awaitility` library is to block a thread until other threads have
completed a particular task. This is useful for testing asynchronous threads.

The idea is to avoid using `CoundDownLatch` and start only using `Awaitility`.

## Rationale

Testing asynchronous systems is difficult. It requires not only handling threads, timeouts and concurrency issues, but
also the intent of the test code can be obscured by all these details. Awaitility is a DSL that allows to express
expectations of an asynchronous system in a conciser and easier to read manner than CountdownLatch.

## Approach

Awaitility permits to check multiple asynchronous behaviors at the same time with assertions that are included in the
untilAsserted block that will by default poll the assertions every 100 ms until either the assertions are valid or the
timeout expires which is by default 10 seconds.

The following is a comparative of the current code with CountDownLatch and the proposal with Awaitility in one test:

### class `AssetEventDispatchTest`

#### with CountDownLatch

```java
public class AssetEventDispatchTest {
    //...
    @Test
    void shouldDispatchEventsOnAssetCreationAndDeletion(AssetService service, EventRouter eventRouter) throws InterruptedException {
        var createdLatch = onDispatchLatch(AssetCreated.class);
        var deletedLatch = onDispatchLatch(AssetDeleted.class);
        eventRouter.register(eventSubscriber);
        var asset = Asset.Builder.newInstance().id("assetId").build();
        var dataAddress = DataAddress.Builder.newInstance().type("any").build();

        service.create(asset, dataAddress);

        assertThat(createdLatch.await(10, SECONDS)).isTrue();
        verify(eventSubscriber).on(isA(AssetCreated.class));

        service.delete(asset.getId());

        assertThat(deletedLatch.await(10, SECONDS)).isTrue();
        verify(eventSubscriber).on(isA(AssetDeleted.class));
    }

    private CountDownLatch onDispatchLatch(Class<? extends Event> eventType) {
        var latch = new CountDownLatch(1);

        doAnswer(i -> {
            latch.countDown();
            return null;
        }).when(eventSubscriber).on(isA(eventType));

        return latch;
    }
}
```

#### with awaitility

```java
public class AssetEventDispatchTest {
    //...
    @Test
    void shouldDispatchEventsOnAssetCreationAndDeletion(AssetService service, EventRouter eventRouter) {

        doAnswer(i -> {
            return null;
        }).when(eventSubscriber).on(isA(AssetCreated.class));

        doAnswer(i -> {
            return null;
        }).when(eventSubscriber).on(isA(AssetDeleted.class));

        eventRouter.register(eventSubscriber);
        var asset = Asset.Builder.newInstance().id("assetId").build();
        var dataAddress = DataAddress.Builder.newInstance().type("any").build();

        service.create(asset, dataAddress);
        await().untilAsserted(() -> {
            verify(eventSubscriber).on(isA(AssetCreated.class));
        });

        service.delete(asset.getId());
        await().untilAsserted(() -> {
            verify(eventSubscriber).on(isA(AssetDeleted.class));
        });
    }
}
```

### Additional Note:

class `TransferProcessManagerImplTest`
(core\transfer\src\test\java\org\eclipse\dataspaceconnector\transfer\core\transfer\TransferProcessManagerImplTest.java)
*** In method verifyCompleted_noCheckerForManaged(): A latch was only instantiated but not used ***