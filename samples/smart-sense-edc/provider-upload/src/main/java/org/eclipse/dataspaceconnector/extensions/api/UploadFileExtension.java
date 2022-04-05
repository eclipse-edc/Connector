package org.eclipse.dataspaceconnector.extensions.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.aws.s3.core.AwsTemporarySecretToken;
import org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema;
import org.eclipse.dataspaceconnector.aws.s3.operator.S3BucketWriter;
import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowController;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.spi.transfer.inline.DataOperatorRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.sql.asset.index.PostgresSqlAssetQueries;
import org.eclipse.dataspaceconnector.sql.asset.index.SqlAssetIndex;
import org.eclipse.dataspaceconnector.sql.datasource.ConnectionFactoryDataSource;
import org.eclipse.dataspaceconnector.sql.datasource.ConnectionPoolDataSource;
import org.eclipse.dataspaceconnector.sql.pool.commons.CommonsConnectionPool;
import org.eclipse.dataspaceconnector.sql.pool.commons.CommonsConnectionPoolConfig;
import org.eclipse.dataspaceconnector.transaction.local.LocalDataSourceRegistry;
import org.eclipse.dataspaceconnector.transaction.local.LocalTransactionContext;
import org.eclipse.dataspaceconnector.transfer.core.inline.InlineDataFlowController;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;

import static org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema.TYPE;

@Provides({AssetLoader.class, AssetIndex.class, DataAddressResolver.class})
public class UploadFileExtension implements ServiceExtension {
    public static final String USE_POLICY = "use-eu";
    private static final String EDC_ASSET_PATH = "edc.samples.04.asset.path";
    @Inject
    private DataFlowManager dataFlowMgr;
    @Inject
    private DataAddressResolver dataAddressResolver;
    @Inject
    private DataOperatorRegistry dataOperatorRegistry;
    @Inject
    private ContractDefinitionStore contractStore;
    @Inject
    private AssetLoader loader;
    @Inject
    private DataSourceRegistry dataSourceRegistry;
    @Inject
    private TransactionContext transactionContext;
    @Inject
    private WebService webService;
    @Inject
    private DtoTransformerRegistry transformerRegistry;


    @Override
    public void initialize(ServiceExtensionContext context) {
        var vault = context.getService(Vault.class);
        var typeManager = context.getTypeManager();
        var address = DataAddress.Builder.newInstance()
                .type(TYPE)
                .keyName(" ")
                .property(S3BucketSchema.REGION, " ")
                .property(S3BucketSchema.BUCKET_NAME, " ")
                .build();
        S3Configurations s3Configurations = new S3Configurations();
        S3BucketWriter writer = new S3BucketWriter(context.getMonitor(), context.getTypeManager(), new RetryPolicy<>(), s3Configurations);
        String secretToken = typeManager.writeValueAsString(new AwsTemporarySecretToken(" ", " ", "", 3600));
        try {
            writer.write(address, " ", new FileInputStream(new File(" ")), secretToken);
        } catch (Exception e) {
            System.out.println("Error =========================== ");
        }
        dataOperatorRegistry.registerWriter(writer);
        DataFlowController dataFlowController = new InlineDataFlowController(vault, context.getMonitor(), dataOperatorRegistry, dataAddressResolver);
        dataFlowMgr.register(dataFlowController);
        var policy = createPolicy();
        registerDataEntries(context);
        registerContractDefinition(policy);
        context.getMonitor().info("File Transfer Extension initialized!");
        var txManager = new LocalTransactionContext(context.getMonitor());
        var transactionContext = txManager;
        try {
            Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/edc-test", "root", "root");
            var datasource = new ConnectionFactoryDataSource(() -> connection);
            CommonsConnectionPool connectionPool = new CommonsConnectionPool(datasource, CommonsConnectionPoolConfig.Builder.newInstance().build());
            var poolDataSource = new ConnectionPoolDataSource(connectionPool);
            LocalDataSourceRegistry dataSourceRegistry = new LocalDataSourceRegistry(txManager);
            dataSourceRegistry.register("PostGres", poolDataSource);
            var sqlAssetIndex = new SqlAssetIndex(dataSourceRegistry, "PostGres", transactionContext, new ObjectMapper(), new PostgresSqlAssetQueries());
            context.registerService(AssetLoader.class, sqlAssetIndex);
            context.registerService(AssetIndex.class, sqlAssetIndex);
            context.registerService(DataAddressResolver.class, sqlAssetIndex);
            webService.registerResource(new ProviderAssetsController(context.getMonitor(), new S3Configurations(), new AssetServiceImplementation(sqlAssetIndex, loader, context.getService(ContractNegotiationStore.class), txManager), transformerRegistry));
        } catch (Exception e) {
            System.out.println("Enable to do DB connection.");
        }
    }

    private Policy createPolicy() {

        var usePermission = Permission.Builder.newInstance()
                .action(Action.Builder.newInstance().type("USE").build())
                .build();

        return Policy.Builder.newInstance()
                .id(USE_POLICY)
                .permission(usePermission)
                .build();
    }

    private void registerDataEntries(ServiceExtensionContext context) {
        String assetPathSetting = context.getSetting(EDC_ASSET_PATH, "/tmp/provider/test-document.txt");
        Path assetPath = Path.of(assetPathSetting);

        DataAddress dataAddress = DataAddress.Builder.newInstance()
                .property("type", "File")
                .property("path", assetPath.getParent().toString())
                .property("filename", assetPath.getFileName().toString())
                .build();

        String assetId = "test-document";
        Asset asset = Asset.Builder.newInstance().id(assetId).build();

        loader.accept(asset, dataAddress);
    }

    private void registerContractDefinition(Policy policy) {

        ContractDefinition contractDefinition = ContractDefinition.Builder.newInstance()
                .id("1")
                .accessPolicy(policy)
                .contractPolicy(policy)
                .selectorExpression(AssetSelectorExpression.Builder.newInstance().whenEquals(Asset.PROPERTY_ID, "test-document").build())
                .build();

        contractStore.save(contractDefinition);
    }
}
