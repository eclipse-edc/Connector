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

Class `ContractNegotiationCommandQueueIntegrationTest`
(
core\contract\src\test\java\org\eclipse\dataspaceconnector\contract\negotiation\command\ContractNegotiationCommandQueueIntegrationTest.java)

Instead of evaluating a latch that lasts for 15 seconds, an object await is created until the methods getState and
getErrorDetail are each one correctly asserted. Additionally, the CountDownLatch objects in the class TestCommandHandler
were removed

```java
...
@Test
    void submitTestCommand_providerManager(){
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

            await().untilAsserted(()->{
            assertThat(negotiation.getState()).isEqualTo(ContractNegotiationStates.ERROR.code());
            assertThat(negotiation.getErrorDetail()).isEqualTo(errorDetail);
            });

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

            // Wait for CommandHandler to modify negotiation 

            await().untilAsserted(()->{
            assertThat(negotiation.getState()).isEqualTo(ContractNegotiationStates.ERROR.code());
            assertThat(negotiation.getErrorDetail()).isEqualTo(errorDetail);
            });

            // Stop the negotiation manager
            negotiationManager.stop();
            }
            ...

private static class TestCommandHandler extends SingleContractNegotiationCommandHandler<TestCommand> {

    private final String errorDetail;

    TestCommandHandler(ContractNegotiationStore store, String errorDetail) {
        super(store);
        this.errorDetail = errorDetail;
    }

    @Override
    public Class<TestCommand> getType() {
        return TestCommand.class;
    }

    @Override
    protected boolean modify(ContractNegotiation negotiation) {
        negotiation.transitionError(errorDetail);
        store.save(negotiation);
        return true;
    }
}
```

class `DataPlaneManagerImplTest`
extensions\data-plane\data-plane-framework\src\test\java\org\eclipse\dataspaceconnector\dataplane\framework\manager\DataPlaneManagerImplTest.java

Same as above a method await().untilAsserted() or until() was used to replace the Time of the CountDownLatch.

```java
...
@Test
    void verifyWorkDispatch()throws InterruptedException{
            var dataPlaneManager=createDataPlaneManager();

            when(registry.resolveTransferService(request))
            .thenReturn(transferService);
            when(transferService.canHandle(isA(DataFlowRequest.class)))
        .thenReturn(true);

        when(transferService.transfer(isA(DataFlowRequest.class))).thenAnswer(i->{
        return completedFuture(Result.success("ok"));
        });

        performTransfer(dataPlaneManager);
        await().untilAsserted(()->{
        verify(registry).resolveTransferService(eq(request));
        verify(transferService).transfer(isA(DataFlowRequest.class));
        });
        }

/**
 * Verifies that the dispatch thread survives an error thrown by a worker.
 */
@Test
    void verifyRetryInitiateTransferRequest(){
            var dataPlaneManager=createDataPlaneManager();

            when(transferService.canHandle(request))
            .thenReturn(true);

            when(transferService.transfer(request))
            .thenAnswer(i->{
            throw new RuntimeException("Test exception");
            }).thenAnswer((i->{
            return completedFuture(Result.success("ok"));
            }));

            dataPlaneManager.start();

            dataPlaneManager.initiateTransfer(request);
            dataPlaneManager.initiateTransfer(request);

            await().untilAsserted(()->verify(transferService,times(2)).transfer(request));

            dataPlaneManager.stop();

            }

@Test
    void verifyWorkDispatch_onUnavailableTransferService_completesTransfer()throws InterruptedException{
            var dataPlaneManager=createDataPlaneManager();

            when(transferService.canHandle(isA(DataFlowRequest.class)))
        .thenReturn(false);

        performTransfer(dataPlaneManager);
        await().until(()->store.getState(request.getProcessId())==DataPlaneStore.State.COMPLETED);
        }
        ...
```

Class `LockManagerTest`
(common\util\src\test\java\org\eclipse\dataspaceconnector\common\concurrency\LockManagerTest.java)

Same as with the previous class, the await method waits until the assertion is finished.

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
            }
            });

            lockManager.readLock(()->{
            thread.start();
            counter.incrementAndGet();
            return null;
            });

            await().untilAsserted(()->assertThat(counter.get()).isEqualTo(1));
            }
            ...
```

class `AssetEventDispatchTest`
(
extensions\api\data-management\asset\src\test\java\org\eclipse\dataspaceconnector\api\datamanagement\asset\service\AssetEventDispatchTest.java)

```java
...
@Test
    void shouldDispatchEventsOnAssetCreationAndDeletion(AssetService service,EventRouter eventRouter)throws InterruptedException{

            eventRouter.register(eventSubscriber);
            var asset=Asset.Builder.newInstance().id("assetId").build();
            var dataAddress=DataAddress.Builder.newInstance().type("any").build();

            service.create(asset,dataAddress);

            service.delete(asset.getId());

            await().untilAsserted(()->{
            verify(eventSubscriber).on(isA(AssetCreated.class));
        verify(eventSubscriber).on(isA(AssetDeleted.class));
        });
```

class `PolicyDefinitionEventDispatchTest`
(
extensions\api\data-management\policydefinition\src\test\java\org\eclipse\dataspaceconnector\api\datamanagement\policy\service\PolicyDefinitionEventDispatchTest.java)

the same procedure as the previous class applies to this test.

```java
...
@Test
    void shouldDispatchEventOnPolicyDefinitionCreationAndDeletion(PolicyDefinitionService service,EventRouter eventRouter)throws InterruptedException{
            eventRouter.register(eventSubscriber);
            var policyDefinition=PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build();

            service.create(policyDefinition);

            service.deleteById(policyDefinition.getUid());

            await().untilAsserted(()->{
            verify(eventSubscriber).on(isA(PolicyDefinitionCreated.class));
        verify(eventSubscriber).on(isA(PolicyDefinitionDeleted.class));
        });
        }
```

Class `CrawlerImplTest`
(
extensions\catalog\federated-catalog-cache\src\test\java\org\eclipse\dataspaceconnector\catalog\cache\crawler\CrawlerImplTest.java)

```java
...
@Test
@DisplayName("Should insert one item into queue when request succeeds")
    void shouldInsertInQueue_whenSucceeds()throws InterruptedException{
            when(protocolAdapterMock.sendRequest(isA(UpdateRequest.class)))
        .thenAnswer(i->{
        return CompletableFuture.completedFuture(new UpdateResponse());
        });

        workQueue.put(createWorkItem());
        executorService.submit(crawler);

        await().untilAsserted(()->{
        assertThat(crawler.join()).isTrue();
        assertThat(queue).hasSize(1);
        verify(protocolAdapterMock).sendRequest(isA(UpdateRequest.class));
        });
        }

@Test
@DisplayName("Should not insert into queue when the request fails")
    void shouldNotInsertInQueue_whenRequestFails()throws InterruptedException{
            when(protocolAdapterMock.sendRequest(isA(UpdateRequest.class))).thenAnswer(i->{
        return CompletableFuture.failedFuture(new EdcException("not reachable"));
        });
        workQueue.put(createWorkItem());
        executorService.submit(crawler);

        await().untilAsserted(()->
        {
        assertThat(crawler.join()).isTrue();
        assertThat(queue).isEmpty();
        verify(protocolAdapterMock).sendRequest(isA(UpdateRequest.class));
        });

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

        await().untilAsserted(()->
        {
        assertThat(crawler.join()).isTrue();
        assertThat(queue).hasSize(1);
        verify(protocolAdapterMock).sendRequest(isA(UpdateRequest.class));
        verify(registry).findForProtocol(anyString());
        verify(secondAdapter).sendRequest(isA(UpdateRequest.class));
        });
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

        await().untilAsserted(()->
        {
        assertThat(crawler.join()).isTrue();
        assertThat(queue).hasSize(3);
        verify(protocolAdapterMock).sendRequest(isA(UpdateRequest.class));
        });
        }

@Test
    void shouldPauseWhenNoWorkItem()throws InterruptedException{

            executorService.submit(crawler);

            await().untilAsserted(()->{
            assertThat(crawler.join()).isTrue();
            assertThat(queue).hasSize(0);
            });
            }

@Test
    void shouldErrorOut_whenNoProtocolAdapterFound()throws InterruptedException{

            crawler=new CrawlerImpl(workQueue,monitorMock,queue,createRetryPolicy(),new NodeQueryAdapterRegistryImpl(),()->Duration.ofMillis(500),errorHandlerMock);

            workQueue.put(createWorkItem());

            doAnswer(i->{
            return null;
            }).when(errorHandlerMock).accept(isA(WorkItem.class));
        executorService.submit(crawler);

        await().untilAsserted(()->{
        assertThat(workQueue).hasSize(0);
        verify(errorHandlerMock).accept(isA(WorkItem.class));
        });
        }
        ...
```

class `LoaderManagerImplTest`
(
extensions\catalog\federated-catalog-cache\src\test\java\org\eclipse\dataspaceconnector\catalog\cache\loader\LoaderManagerImplTest.java)

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

            await().untilAsserted(()->verify(waitStrategyMock,atLeastOnce()).retryInMillis());

            }

@Test
@DisplayName("Verify that the LoaderManager does not sleep when a complete batch was processed")
    void batchSizeReachedWithinTimeframe()throws InterruptedException{
            range(0,batchSize).forEach(i->queue.offer(new UpdateResponse()));

            doAnswer(i->{
            return null;
            }).when(waitStrategyMock).success();

            loaderManager.start(queue);

            await().untilAsserted(()->{
            verify(loaderMock,times(1)).load(any());
            verify(waitStrategyMock).success();
            });
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

            await().untilAsserted(()->verify(queueListener,atLeastOnce()).unlocked());
            }

            ...
```

class `StateMachineManagerTest`
(common\state-machine-lib\src\test\java\org\eclipse\dataspaceconnector\common\statemachine\StateMachineManagerTest.java)

```java
...
@Test
    void shouldExecuteProcessorsAsyncAndCanBeStopped(){

            var processor=mock(StateProcessor.class);
        when(processor.process()).thenAnswer(i->{
        Thread.sleep(100L);
        return 1L;
        });
        var stateMachine=StateMachineManager.Builder.newInstance("test",monitor,instrumentation,waitStrategy)
        .processor(processor)
        .shutdownTimeout(1)
        .build();

        stateMachine.start();
        await().untilAsserted(()->{
        verify(processor,atLeastOnce()).process();
        assertThat(stateMachine.stop()).succeedsWithin(2,SECONDS);
        verifyNoMoreInteractions(processor);
        });
        }

@Test
    void shouldNotWaitForSomeTimeIfTheresAtLeastOneProcessedEntity(){
            var processor=mock(StateProcessor.class);
        when(processor.process()).thenReturn(1L);
        doAnswer(i->{
        return 1L;
        }).when(waitStrategy).success();
        var stateMachine=StateMachineManager.Builder.newInstance("test",monitor,instrumentation,waitStrategy)
        .processor(processor)
        .build();

        stateMachine.start();
        await().untilAsserted(()->{
        verify(waitStrategy,never()).waitForMillis();
        verify(waitStrategy,atLeastOnce()).success();
        });
        }

@Test
    void shouldWaitForSomeTimeIfNoEntityIsProcessed(){
            var processor=mock(StateProcessor.class);
        when(processor.process()).thenReturn(0L);
        var waitStrategy=mock(WaitStrategy.class);
        doAnswer(i->{
        return 0L;
        }).when(waitStrategy).waitForMillis();
        var stateMachine=StateMachineManager.Builder.newInstance("test",monitor,instrumentation,waitStrategy)
        .processor(processor)
        .build();

        stateMachine.start();
        await().untilAsserted(()->{
        verify(waitStrategy,atLeastOnce()).waitForMillis();
        verify(waitStrategy,atLeastOnce()).success();
        });

        ...

@Test
    void shouldWaitRetryTimeWhenAnExceptionIsThrownByAnProcessor(){
            var processor=mock(StateProcessor.class);
        when(processor.process()).thenThrow(new EdcException("exception")).thenReturn(0L);
        when(waitStrategy.retryInMillis()).thenAnswer(i->{
        return 1L;
        });
        var stateMachine=StateMachineManager.Builder.newInstance("test",monitor,instrumentation,waitStrategy)
        .processor(processor)
        .build();

        stateMachine.start();
        await().untilAsserted(()->
        {
        assertThat(stateMachine.isActive()).isTrue();
        verify(waitStrategy).retryInMillis();
        });
        }
```

class `TransferProcessManagerImplIntegrationTest`
(
core\transfer\src\test\java\org\eclipse\dataspaceconnector\transfer\core\transfer\TransferProcessManagerImplIntegrationTest.java)

```java
...
@Test
@DisplayName("Verify that no process 'starves' during two consecutive runs, when the batch size > number of processes")
    void verifyProvision_shouldNotStarve(){
            var numProcesses=TRANSFER_MANAGER_BATCHSIZE*2;
            when(provisionManager.provision(any(),any(Policy.class))).thenAnswer(i->{
        return completedFuture(List.of(ProvisionResponse.Builder.newInstance().resource(new TestProvisionedDataDestinationResource("any","1")).build()));
        });

        var manifest=ResourceManifest.Builder.newInstance().definitions(List.of(new TestResourceDefinition())).build();
        var processes=IntStream.range(0,numProcesses)
        .mapToObj(i->provisionedResourceSet())
        .map(resourceSet->createUnsavedTransferProcess().resourceManifest(manifest).provisionedResourceSet(resourceSet).build())
        .peek(TransferProcess::transitionInitial)
        .peek(store::create)
        .collect(Collectors.toList());

        transferProcessManager.start();

        await().untilAsserted(()->
        {
        assertThat(processes).describedAs("All transfer processes state should be greater than INITIAL")
        .allSatisfy(process->{
        var id=process.getId();
        var storedProcess=store.find(id);
        assertThat(storedProcess).describedAs("Should exist in the TransferProcessStore").isNotNull();
        assertThat(storedProcess.getState()).isGreaterThan(INITIAL.code());
        });
        verify(provisionManager,times(numProcesses)).provision(any(),any());
        });
        ...        
```

--------------------------------------------

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
        return null;
        });

        extension.initialize(context);
        await().timeout(Duration.ofMillis(TIMEOUT_MS));
        }
        }

@Test
    void onlySecretProvided_authenticateWithSecret()throws InterruptedException{
            when(context.getSetting("edc.vault.clientsecret",null)).thenReturn(CLIENT_SECRET);

            try(MockedStatic<AzureVault> utilities=mockStatic(AzureVault.class)){
        utilities.when(()->AzureVault.authenticateWithSecret(monitor,CLIENT_ID,TENANT_ID,CLIENT_SECRET,KEYVAULT_NAME))
        .then(i->{
        return null;
        });

        extension.initialize(context);
        await().timeout(Duration.ofMillis(TIMEOUT_MS));
        }
        }

@Test
    void bothSecretAndCertificateProvided_authenticateWithCertificate()throws InterruptedException{
            when(context.getSetting("edc.vault.certificate",null)).thenReturn(CERTIFICATE_PATH);
            when(context.getSetting("edc.vault.clientsecret",null)).thenReturn(CLIENT_SECRET);

            try(MockedStatic<AzureVault> utilities=mockStatic(AzureVault.class)){
        utilities.when(()->AzureVault.authenticateWithCertificate(monitor,CLIENT_ID,TENANT_ID,CERTIFICATE_PATH,KEYVAULT_NAME))
        .then(i->{
        return null;
        });

        extension.initialize(context);

        await().timeout(Duration.ofMillis(TIMEOUT_MS));
        }
        }
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

class 'HttpProvisionerExtensionEndToEndTest'
extensions\http-provisioner\src\test\java\org\eclipse\dataspaceconnector\transfer\provision\http\impl\HttpProvisionerExtensionEndToEndTest.java

It was necessary to add an extra await method in order to receive the response 200. Without this delay, there is only an
answer with code 100, meaning that the result of the invocation was still not ready.

```java
@Test
    void processProviderRequestRetry(TransferProcessManager processManager,
            ContractNegotiationStore negotiationStore,
            AssetLoader loader,
            TransferProcessStore store,PolicyDefinitionStore policyStore)throws Exception{

            when(delegate.intercept(any()))
            .thenAnswer(invocation->createResponse(503,invocation))
            .thenAnswer(invocation->{
            return createResponse(200,invocation);
            });

            loadNegotiation(negotiationStore,policyStore);

            loadAsset(loader);

            var result=processManager.initiateProviderRequest(createRequest());

            await().timeout(10000,MILLISECONDS);

            var transferProcess=store.find(result.getContent());
            await().timeout(10000,MILLISECONDS);
            assertThat(transferProcess).isNotNull();
            assertThat(transferProcess.ge
```

class `TransferProcessManagerImplTest`
(core\transfer\src\test\java\org\eclipse\dataspaceconnector\transfer\core\transfer\TransferProcessManagerImplTest.java)
*** method verifyCompleted_noCheckerForManaged(): A latch was only instantiated but not used ***