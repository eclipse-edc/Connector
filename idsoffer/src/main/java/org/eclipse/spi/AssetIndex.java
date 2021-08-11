package org.eclipse.spi;

import java.util.List;

public interface AssetIndex {
    List<Asset> queryAssets(SelectorExpression expression);
}
