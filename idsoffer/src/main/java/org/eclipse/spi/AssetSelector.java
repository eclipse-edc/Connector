package org.eclipse.spi;

import java.util.List;

@FunctionalInterface
public interface AssetSelector<T extends SelectorExpression> {

    /*
     * Selector should be pumped into a stream
     */

    List<Asset> select(T selector, Iterable<Asset> assets);

}
