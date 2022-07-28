# Unify asynchronous code testing practices

## Decision

Currently, the testing of async code is performed in two different ways:

* using the `CoundDownLatch` object
* using the `Awaitility` library

The goal of `CountDownLatch` objects and the `Awaitility` library is to cause a thread to block until other threads have
completed a given task. Something useful in order to test Asynchronous threads.

The idea is to avoid using `CoundDownLatch` and start only using `Awaitility`.

## Rationale

Testing asynchronous systems is difficult. It requires not only handling threads, timeouts and concurrency issues, but
also the intent of the test code can be obscured by all these details e.g while using CountDownLatch. Awaitility is a
DSL that allows to express expectations of an asynchronous system in a conciser and easier to read manner. Additionally
CoundDownLatch works only based on latches while Awaitility works based on time (seconds, nanoseconds), which can be
more useful to do time testing on threads.

## Approach

Adding the dependency Awaitility into every build.gradle.kts

```java
...
        dependencies{
        ...
        testImplementation("org.awaitility:awaitility:${awaitility}")
        }
        ...
```

Class `AzureVaultExtensionTest`
(extensions\azure\vault\src\test\java\org\eclipse\dataspaceconnector\core\security\azure\AzureVaultExtensionTest.java)

Since it is possible to set the waiting timeout on `Waitility` the tests instead of testing time, now check that the
methods are working correctly.

```java
@Test
    void onlyCertificateProvided_authenticateWithCertificate()throws InterruptedException{
            when(context.getSetting("edc.vault.certificate",null)).thenReturn(CERTIFICATE_PATH);

            try(MockedStatic<AzureVault> utilities=mockStatic(AzureVault.class)){
        utilities.when(()->AzureVault.authenticateWithCertificate(monitor,CLIENT_ID,TENANT_ID,CERTIFICATE_PATH,KEYVAULT_NAME))
        .then(i->{
        await().timeout(Duration.ofMillis(TIMEOUT_MS));
        return null;
        });
        extension.initialize(context);
        }
        }

@Test
    void onlySecretProvided_authenticateWithSecret()throws InterruptedException{
            when(context.getSetting("edc.vault.clientsecret",null)).thenReturn(CLIENT_SECRET);

            try(MockedStatic<AzureVault> utilities=mockStatic(AzureVault.class)){
        utilities.when(()->AzureVault.authenticateWithSecret(monitor,CLIENT_ID,TENANT_ID,CLIENT_SECRET,KEYVAULT_NAME))
        .then(i->{
        await().timeout(Duration.ofMillis(TIMEOUT_MS));
        return null;
        });

        extension.initialize(context);
        }
        }

@Test
    void bothSecretAndCertificateProvided_authenticateWithCertificate()throws InterruptedException{
            when(context.getSetting("edc.vault.certificate",null)).thenReturn(CERTIFICATE_PATH);
            when(context.getSetting("edc.vault.clientsecret",null)).thenReturn(CLIENT_SECRET);

            try(MockedStatic<AzureVault> utilities=mockStatic(AzureVault.class)){
        utilities.when(()->AzureVault.authenticateWithCertificate(monitor,CLIENT_ID,TENANT_ID,CERTIFICATE_PATH,KEYVAULT_NAME))
        .then(i->{
        await().timeout(Duration.ofMillis(TIMEOUT_MS));
        return null;
        });
        extension.initialize(context);
        }
        }
```

Class `LockManagerTest`
(common\util\src\test\java\org\eclipse\dataspaceconnector\common\concurrency\LockManagerTest.java)

Same as with the previous class, the same timeout is set and it is tested only the correct flow of the code.

```java
...
@Test
    void verifyTimeoutOnWriteLockAttempt(){
            var lockManager=new LockManager(new ReentrantReadWriteLock(),10);
            var counter=new AtomicInteger();

            // Attempt to acquire a write lock in another thread, which should timeout as the current thread holds a read lock
            var thread=new Thread(()->{
            try{
            lockManager.writeLock(()->{
            throw new AssertionError();  // lock should never be acquired
            });
            }catch(LockException e){
            await().timeout(Duration.ofMillis(1000L));
            }
            });

            lockManager.readLock(()->{
            thread.start();
            counter.incrementAndGet();
            return null;
            });

            assertThat(counter.get()).isEqualTo(1);
            }
            ...

```

Class `ContractNegotiationCommandQueueIntegrationTest`
(
core\contract\src\test\java\org\eclipse\dataspaceconnector\contract\negotiation\command\ContractNegotiationCommandQueueIntegrationTest.java)

Instead of evaluating a latch that lasts for 15 seconds, an object await is created with a duration of 15 seconds and
the methods getState and getErrorDetail are the only ones evaluated.

```java
...
@Test
    void submitTestCommand_providerManager()throws Exception{
            var negotiationManager=ProviderContractNegotiationManagerImpl.Builder.newInstance()
            .monitor(monitor)
            .validationService(validationService)
            .dispatcherRegistry(dispatcherRegistry)
            .commandQueue(commandQueue)
            .commandRunner(commandRunner)
            .observable(observable)
            .store(store)
            .policyStore(policyStore)
            .sendRetryManager(sendRetryManager)
            .build();

            negotiationManager.start();

            negotiationManager.enqueueCommand(command);

            // Wait for CommandHandler to modify negotiation with time out at 15 seconds
            await().timeout(java.time.Duration.ofSeconds(15));

            assertThat(negotiation.getState()).isEqualTo(ContractNegotiationStates.ERROR.code());
            assertThat(negotiation.getErrorDetail()).isEqualTo(errorDetail);

            // Stop the negotiation manager
            negotiationManager.stop();
            }

@Test
    void submitTestCommand_consumerManager()throws Exception{
            when(store.find(negotiationId)).thenReturn(negotiation);

            // Create and start the negotiation manager
            var negotiationManager=ConsumerContractNegotiationManagerImpl.Builder.newInstance()
            .monitor(monitor)
            .validationService(validationService)
            .dispatcherRegistry(dispatcherRegistry)
            .commandQueue(commandQueue)
            .commandRunner(commandRunner)
            .observable(observable)
            .store(store)
            .policyStore(policyStore)
            .sendRetryManager(sendRetryManager)
            .build();
            negotiationManager.start();

            // Enqueue command
            negotiationManager.enqueueCommand(command);

            // Wait for CommandHandler to modify negotiation with time out at 15 seconds
            await().timeout(java.time.Duration.ofSeconds(15));

            assertThat(negotiation.getState()).isEqualTo(ContractNegotiationStates.ERROR.code());
            assertThat(negotiation.getErrorDetail()).isEqualTo(errorDetail);

            // Stop the negotiation manager
            negotiationManager.stop();
            }
            ...
```

class `TransferProcessManagerImplIntegrationTest`
(
core\transfer\src\test\java\org\eclipse\dataspaceconnector\transfer\core\transfer\TransferProcessManagerImplIntegrationTest.java)
***No tests found for given includes***

class `TransferProcessManagerImplTest`
(core\transfer\src\test\java\org\eclipse\dataspaceconnector\transfer\core\transfer\TransferProcessManagerImplTest.java)
*** method verifyCompleted_noCheckerForManaged(): A latch was only instantiated but not used ***

class `AssetEventDispatchTest`
(
extensions\api\data-management\asset\src\test\java\org\eclipse\dataspaceconnector\api\datamanagement\asset\service\AssetEventDispatchTest.java)

Instead of creating and testing latches which lasts for 10 seconds, an timeout is set and it is only the events of
creation and deletion of assets are tested

```java
...
@Test
    void shouldDispatchEventsOnAssetCreationAndDeletion(AssetService service,EventRouter eventRouter)throws InterruptedException{

            eventRouter.register(eventSubscriber);
            var asset=Asset.Builder.newInstance().id("assetId").build();
            var dataAddress=DataAddress.Builder.newInstance().type("any").build();

            service.create(asset,dataAddress);
            await().timeout(Duration.ofSeconds(10));

            verify(eventSubscriber).on(isA(AssetCreated.class));

        service.delete(asset.getId());
        await().timeout(Duration.ofSeconds(10));

        verify(eventSubscriber).on(isA(AssetDeleted.class));
        }
```

class `PolicyDefinitionEventDispatchTest`
(
extensions\api\data-management\policydefinition\src\test\java\org\eclipse\dataspaceconnector\api\datamanagement\policy\service\PolicyDefinitionEventDispatchTest.java)

the same procedure as the previous class applies to this tests

```java
...
@Test
    void shouldDispatchEventOnPolicyDefinitionCreationAndDeletion(PolicyDefinitionService service,EventRouter eventRouter)throws InterruptedException{
            eventRouter.register(eventSubscriber);
            var policyDefinition=PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build();

            service.create(policyDefinition);
            await().timeout(Duration.ofSeconds(10));
            verify(eventSubscriber).on(isA(PolicyDefinitionCreated.class));

        service.deleteById(policyDefinition.getUid());
        await().timeout(Duration.ofSeconds(10));
        verify(eventSubscriber).on(isA(PolicyDefinitionDeleted.class));
        }
```

Class `CrawlerImplTest`
(
extensions\catalog\federated-catalog-cache\src\test\java\org\eclipse\dataspaceconnector\catalog\cache\crawler\CrawlerImplTest.java)

Instead of testing that the runned time was 5 seconds, it is possible to set the awaiting time in 5 seconds and check
the assert and verify methods

```java

@Test
@DisplayName("Should insert one item into queue when request succeeds")
    void shouldInsertInQueue_whenSucceeds()throws InterruptedException{
            when(protocolAdapterMock.sendRequest(isA(UpdateRequest.class)))
        .thenAnswer(i->{
        return CompletableFuture.completedFuture(new UpdateResponse());
        });

        workQueue.put(createWorkItem());
        executorService.submit(crawler);
        await().timeout(JOIN_WAIT_TIME,TimeUnit.MILLISECONDS);
        assertThat(crawler.join()).isTrue();
        assertThat(queue).hasSize(1);
        verify(protocolAdapterMock).sendRequest(isA(UpdateRequest.class));
        }

@Test
@DisplayName("Should not insert into queue when the request fails")
    void shouldNotInsertInQueue_whenRequestFails()throws InterruptedException{
            when(protocolAdapterMock.sendRequest(isA(UpdateRequest.class))).thenAnswer(i->{
        return CompletableFuture.failedFuture(new EdcException("not reachable"));
        });
        workQueue.put(createWorkItem());
        executorService.submit(crawler);

        await().timeout(JOIN_WAIT_TIME,TimeUnit.MILLISECONDS);
        assertThat(crawler.join()).isTrue();
        assertThat(queue).isEmpty();
        verify(protocolAdapterMock).sendRequest(isA(UpdateRequest.class));
        }

@Test
@DisplayName("Should insert only those items into queue that have succeeded")
    void shouldInsertInQueue_onlySuccessfulProtocolRequests()throws InterruptedException{

            NodeQueryAdapter secondAdapter=mock(NodeQueryAdapter.class);
        when(registry.findForProtocol(anyString())).thenReturn(Arrays.asList(protocolAdapterMock,secondAdapter));

        when(protocolAdapterMock.sendRequest(isA(UpdateRequest.class))).thenAnswer(i->{
        return CompletableFuture.completedFuture(new UpdateResponse());
        });

        when(secondAdapter.sendRequest(isA(UpdateRequest.class))).thenAnswer(i->{
        return CompletableFuture.failedFuture(new RuntimeException());
        });
        workQueue.put(createWorkItem());
        executorService.submit(crawler);

        await().timeout(JOIN_WAIT_TIME,TimeUnit.MILLISECONDS);
        assertThat(crawler.join()).isTrue();
        assertThat(queue).hasSize(1);
        verify(protocolAdapterMock).sendRequest(isA(UpdateRequest.class));
        verify(registry).findForProtocol(anyString());
        verify(secondAdapter).sendRequest(isA(UpdateRequest.class));
        }

@Test
@DisplayName("Should not insert when Queue is at capacity")
    void shouldLogError_whenQueueFull()throws InterruptedException{
            range(0,QUEUE_CAPACITY).forEach(i->queue.add(new UpdateResponse())); //queue is full now

            when(protocolAdapterMock.sendRequest(isA(UpdateRequest.class))).thenAnswer(i->{
        return CompletableFuture.completedFuture(new UpdateResponse());
        });

        workQueue.put(createWorkItem());

        executorService.submit(crawler);

        await().timeout(JOIN_WAIT_TIME,TimeUnit.MILLISECONDS);
        assertThat(crawler.join()).isTrue();
        assertThat(queue).hasSize(3);
        verify(protocolAdapterMock).sendRequest(isA(UpdateRequest.class));
        }

        ...
@Test
    void shouldErrorOut_whenNoProtocolAdapterFound()throws InterruptedException{

            crawler=new CrawlerImpl(workQueue,monitorMock,queue,createRetryPolicy(),new NodeQueryAdapterRegistryImpl(),()->Duration.ofMillis(500),errorHandlerMock);
            workQueue.put(createWorkItem());
            doAnswer(i->{
            return null;
            }).when(errorHandlerMock).accept(isA(WorkItem.class));
        executorService.submit(crawler);
        await().timeout(5,SECONDS);
        assertThat(workQueue).hasSize(0); //1).allSatisfy(wi -> assertThat(wi.getErrors()).isNotNull().hasSize(1));
        verify(errorHandlerMock).accept(isA(WorkItem.class));
        }

```

class `LoaderManagerImplTest`
(
extensions\catalog\federated-catalog-cache\src\test\java\org\eclipse\dataspaceconnector\catalog\cache\loader\LoaderManagerImplTest.java)

Same as with the test class above, the time assertion is directly replaced with the method wait and the other assertions
and vertifications are kept the same

```java
...
@Test
@DisplayName("Verify that the loader manager waits one pass when the queue does not yet contain sufficient elements")
    void batchSizeNotReachedWithinTimeframe(){
            range(0,batchSize-1).forEach(i->queue.offer(new UpdateResponse()));
            when(waitStrategyMock.retryInMillis()).thenAnswer(i->{
            return 2L;
            });

            loaderManager.start(queue);

            await().timeout(Duration.ofMillis(300L));
            verify(waitStrategyMock,atLeastOnce()).retryInMillis();
            }

@Test
@DisplayName("Verify that the LoaderManager does not sleep when a complete batch was processed")
    void batchSizeReachedWithinTimeframe()throws InterruptedException{
            range(0,batchSize).forEach(i->queue.offer(new UpdateResponse()));

            doAnswer(i->{
            return null;
            }).when(waitStrategyMock).success();

            loaderManager.start(queue);

            await().timeout(Duration.ofSeconds(5L));

            verify(loaderMock,times(1)).load(any());
            verify(waitStrategyMock).success();
            }
            ...            
```

class `PartitionManagerImplIntegrationTest`
(
extensions\catalog\federated-catalog-cache\src\test\java\org\eclipse\dataspaceconnector\catalog\cache\management\PartitionManagerImplIntegrationTest.java)

```java
...
@ParameterizedTest
@ValueSource(ints = { 10, 50, 500 })
@DisplayName("Verify that " + WORK_ITEM_COUNT + " work items are correctly processed by a number of crawlers")
    void runManyCrawlers_verifyCompletion(int crawlerCount)throws InterruptedException{
            doAnswer(i->{
            return null;
            }).when(queueListener).unlocked();
            var partitionManager=new PartitionManagerImpl(monitorMock,signallingWorkItemQueue,generatorFunction,crawlerCount,()->staticWorkLoad);

            partitionManager.schedule(new RunOnceExecutionPlan());

            await().timeout(1,MINUTES);
            verify(queueListener,atLeastOnce()).unlocked();
            }

            ...
```

class `PartitionManagerImplTest`
(
extensions\catalog\federated-catalog-cache\src\test\java\org\eclipse\dataspaceconnector\catalog\cache\management\PartitionManagerImplTest.java)

```java
...
@Test
    void stop_allCrawlersJoinSuccessfully()throws InterruptedException{
            partitionManager=new PartitionManagerImpl(monitorMock,workItemQueueMock,workItems->{
            Crawler crawler=mock(Crawler.class);
        doAnswer(i->{
        return null;
        }).when(crawler).run();
        when(crawler.join()).thenReturn(true);
        return crawler;
        },5,()->staticWorkload);

        await().timeout(10,SECONDS);
        partitionManager.stop();
        }

        ...
@Test
    void schedule_planThrowsIllegalStateException_shouldLogException()throws InterruptedException{
            var plan=mock(ExecutionPlan.class);
        when(workItemQueueMock.addAll(any()))
        .thenReturn(true) // first time works
        .thenThrow(new IllegalStateException("Queue full")); //second time fails

        doAnswer(invocation->{
        var runnable=(Runnable)invocation.getArgument(0);
        runnable.run();
        Thread.sleep(100);
        runnable.run();
        return null;
        }).when(plan).run(any());
        partitionManager.schedule(plan);

        // assert exception was thrown and logged
        verify(workItemQueueMock,times(2)).addAll(any());
        await().timeout(1000,MILLISECONDS);
        verify(workItemQueueMock,times(2)).lock();
        verify(workItemQueueMock,times(2)).unlock();
        verify(monitorMock).warning(startsWith("Cannot add 1 elements to the queue"),isA(IllegalStateException.class));
        }

@Test
    void schedule_planThrowsAnyException_shouldLogException()throws InterruptedException{
            var plan=mock(ExecutionPlan.class);
        when(workItemQueueMock.addAll(any()))
        .thenReturn(true) // first time works
        .thenThrow(new RuntimeException("Any random error")); //second time fails

        doAnswer(invocation->{
        var runnable=(Runnable)invocation.getArgument(0);
        runnable.run();
        Thread.sleep(100);
        runnable.run();
        return null;
        }).when(plan).run(any());
        partitionManager.schedule(plan);

        // assert exception was thrown and logged
        verify(workItemQueueMock,times(2)).addAll(any());
        await().timeout(1000,MILLISECONDS);
        verify(workItemQueueMock,times(2)).lock();
        verify(workItemQueueMock,times(2)).unlock();
        verify(monitorMock).severe(startsWith("Error populating the queue"),isA(RuntimeException.class));
        }    
```

class `DataPlaneManagerImplTest`
extensions\data-plane\data-plane-framework\src\test\java\org\eclipse\dataspaceconnector\dataplane\framework\manager\DataPlaneManagerImplTest.java

common\state-machine-lib\src\test\java\org\eclipse\dataspaceconnector\common\statemachine\StateMachineManagerTest.java
"Wanted but not invoked:
stateProcessor.process(); -> at
org.eclipse.dataspaceconnector.common.statemachine.StateMachineManagerTest.shouldExecuteProcessorsAsyncAndCanBeStopped(
StateMachineManagerTest.java:73)
Actually, there were zero interactions with this mock."

--> Not test but main folder

extensions\http-provisioner\src\test\java\org\eclipse\dataspaceconnector\transfer\provision\http\impl\HttpProvisionerExtensionEndToEndTest.java