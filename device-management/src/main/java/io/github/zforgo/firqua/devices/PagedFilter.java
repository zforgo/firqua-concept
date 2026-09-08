package io.github.zforgo.firqua.devices;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collector;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;

import io.github.zforgo.firqua.common.PagingAndSorting;
import io.github.zforgo.firqua.filter.FilterResult;

public interface PagedFilter<T> {

    default Sort defaultSort() {
        return Sort.ascending("id");
    }

    default Sort createSort(PagingAndSorting pas) {
        return Optional.ofNullable(pas.sortingCriteria)
                .map(cr -> Sort.by(cr, pas.sortDirection))
                .map(s -> mergeSort(s, defaultSort()))
                .orElse(defaultSort());

    }

    PanacheQuery<T> baseQuery(Sort sort);

    default <R> FilterResult<R> pagedResult(PagingAndSorting pas, Function<T, R> mapper) {
        var q = baseQuery(createSort(pas));

        Optional.of(pas)
                .filter(ps -> ps.pageSize > 0)
                .map(ps -> Page.of(ps.pageIndex, ps.pageSize))
                .ifPresent(q::page);

        var count = q.count();
        var items = q.stream()
                .map(mapper)
                .toList();
        return new FilterResult<>(items, count, pas.pageIndex, pas.pageSize);

    }

    private static Sort mergeSort(Sort... parts) {
        return Arrays.stream(parts)
                .flatMap(part -> part.getColumns().stream())
                .collect(
                        Collector.of(
                                Sort::empty,
                                (s, c) -> s.and(c.getName(), c.getDirection(), c.getNullPrecedence()),
                                (a, _) -> a
                        )
                );
    }
}
