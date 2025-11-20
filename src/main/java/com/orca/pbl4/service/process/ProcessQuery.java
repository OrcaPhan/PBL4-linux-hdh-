package com.orca.pbl4.service.process;

import java.util.Objects;

/**
 * DTO mô tả việc lọc + sắp xếp bảng tiến trình.
 */
public class ProcessQuery {
    private final String searchText;
    private final ProcessFilter filter;
    private final ProcessSortKey sortKey;
    private final boolean descending;
    private final String stateFilter;

    private ProcessQuery(Builder builder) {
        this.searchText = builder.searchText;
        this.filter = builder.filter;
        this.sortKey = builder.sortKey;
        this.descending = builder.descending;
        this.stateFilter = builder.stateFilter;
    }

    public String getSearchText() {
        return searchText;
    }

    public ProcessFilter getFilter() {
        return filter;
    }

    public ProcessSortKey getSortKey() {
        return sortKey;
    }

    public boolean isDescending() {
        return descending;
    }

    public String getStateFilter() {
        return stateFilter;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String searchText;
        private ProcessFilter filter;
        private ProcessSortKey sortKey = ProcessSortKey.CPU_PERCENT;
        private boolean descending = true;
        private String stateFilter;

        public Builder searchText(String searchText) {
            this.searchText = searchText;
            return this;
        }

        public Builder filter(ProcessFilter filter) {
            this.filter = filter;
            return this;
        }

        public Builder sortKey(ProcessSortKey sortKey) {
            this.sortKey = Objects.requireNonNull(sortKey);
            return this;
        }

        public Builder descending(boolean descending) {
            this.descending = descending;
            return this;
        }

        public Builder stateFilter(String stateFilter) {
            this.stateFilter = stateFilter;
            return this;
        }

        public ProcessQuery build() {
            return new ProcessQuery(this);
        }
    }
}