package org.eclipse.dataspaceconnector.spi.query;

import org.eclipse.dataspaceconnector.spi.asset.Criterion;

import java.util.List;
import java.util.regex.Pattern;

public class PagingSpec {
    private int offset = 0;
    private int limit = 50;
    private List<Criterion> filterExpression;
    private SortOrder sortOrder = SortOrder.DESC;
    private String sortField;

    public String getSortField() {
        return sortField;
    }

    @Override
    public String toString() {
        return "PagingSpec{" +
                "offset=" + offset +
                ", pageSize=" + limit +
                ", filterExpression=" + filterExpression +
                ", sortOrder=" + sortOrder +
                ", sortField=" + sortField +
                '}';
    }

    public int getOffset() {
        return offset;
    }

    public int getLimit() {
        return limit;
    }

    public List<Criterion> getFilterExpression() {
        return filterExpression;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public static final class Builder {
        private final PagingSpec pagingSpec;

        private Builder() {
            pagingSpec = new PagingSpec();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder offset(int offset) {
            pagingSpec.offset = offset;
            return this;
        }

        public Builder limit(int limit) {
            pagingSpec.limit = limit;
            return this;
        }

        public Builder sortOrder(SortOrder sortOrder) {
            pagingSpec.sortOrder = sortOrder;
            return this;
        }

        public Builder sortField(String sortField) {
            pagingSpec.sortField = sortField;
            return this;
        }

        public PagingSpec build() {
            if (pagingSpec.offset < 0) {
                throw new IllegalArgumentException("offset");
            }
            if (pagingSpec.limit <= 0) {
                throw new IllegalArgumentException("limit");
            }
            return pagingSpec;
        }

        public Builder filter(String filterExpression) {

            if (filterExpression != null) {
                if (Pattern.matches("[^\\s\\\\]*(\\s*)=(\\s*)[^\\s\\\\]*", filterExpression)) { // something like X = Y
                    // remove whitespaces
                    filterExpression = filterExpression.replace(" ", "");
                    // we'll interpret the "=" as "contains"
                    var tokens = filterExpression.split("=");
                    pagingSpec.filterExpression = List.of(new Criterion(tokens[0], "contains", tokens[1]));
                } else {
                    var sanitized = filterExpression.replaceAll(" +", " ");
                    var s = sanitized.split(" ");

                    //generic LEFT OPERAND RIGHT expression
                    if (s.length == 3) {
                        pagingSpec.filterExpression = List.of(new Criterion(s[0], s[1], s[2]));
                    } else {
                        // unsupported filter expression
                        throw new IllegalArgumentException("Cannot convert " + filterExpression + " into a Criterion");
                    }
                }
            }

            return this;
        }
    }
}
