package org.eclipse.dataspaceconnector.sql.asset.index;

/**
 * Defines queries used by the SqlAssetIndexServiceExtension.
 */
public interface SqlAssetQueries extends SqlAssetTables {

    /**
     * INSERT clause for assets.
     */
    String getSqlAssetInsertClause();

    /**
     * INSERT clause for data addresses.
     */
    String getSqlDataAddressInsertClause();

    /**
     * INSERT clause for properties.
     */
    String getSqlPropertyInsertClause();

    /**
     * SELECT COUNT clause for assets.
     */
    String getSqlAssetCountByIdClause();

    /**
     * SELECT clause for properties.
     */
    String getSqlPropertyFindByIdClause();

    /**
     * SELECT clause for data addresses.
     */
    String getSqlDataAddressFindByIdClause();

    /**
     * SELECT clause for all assets.
     */
    String getSqlAssetListClause();

    /**
     * DELETE clause for assets.
     */
    String getSqlAssetDeleteByIdClause();

    /**
     * DELETE clause for data addresses.
     */
    String getSqlDataAddressDeleteByIdClause();

    /**
     * DELETE clause for properties.
     */
    String getSqlPropertyDeleteByIdClause();

    /**
     * The COUNT variable used in SELECT COUNT queries.
     */
    String getCountVariableName();
}
