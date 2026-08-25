package io.github.zforgo.firqua.common;

import java.util.List;

public record FilterResult<T>(List<T> items, Pagination pagination) {

    public FilterResult(List<T> items, long total, int pageIndex, int pageSize) {
        this(items, new Pagination(total, pageIndex, pageSize));
    }

    public record Pagination(long total, int pageIndex, int pageSize) {
    }
}
