package io.github.zforgo.firqua.common;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

import io.quarkus.panache.common.Sort;

public class PagingAndSorting {
    public static final String param_PageIndex = "page_index";
    public static final String param_PageSize = "page_size";
    public static final String param_SortCriteria = "sort_criteria";
    public static final String param_SortDirection = "sort_direction";

    private static final String directionAscending = "Ascending";
    public static final String pageUnlimited = "-1";
    public static final int pageUnlimitedValue = -1;

    @QueryParam(param_SortCriteria)
    public String sortingCriteria;

    @QueryParam(param_SortDirection)
    @DefaultValue(directionAscending)
    public Sort.Direction sortDirection;

    @PositiveOrZero
    @QueryParam(param_PageIndex)
    public int pageIndex;

    @QueryParam(param_PageSize)
    @DefaultValue(pageUnlimited)
    public int pageSize;
}
