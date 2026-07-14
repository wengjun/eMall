package com.emall.common.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;

public final class BoundedQuery {
    public static final int DEFAULT_PAGE_SIZE = 500;
    public static final int MAXIMUM_PAGE_SIZE = 1_000;

    private BoundedQuery() {
    }

    public static <T> List<T> firstPage(BaseMapper<T> mapper) {
        return page(mapper, DEFAULT_PAGE_SIZE);
    }

    public static <T> List<T> firstPage(BaseMapper<T> mapper, Wrapper<T> query) {
        return page(mapper, query, DEFAULT_PAGE_SIZE);
    }

    public static <T> List<T> page(BaseMapper<T> mapper, int requestedSize) {
        return page(mapper, null, requestedSize);
    }

    public static <T> List<T> page(BaseMapper<T> mapper, Wrapper<T> query, int requestedSize) {
        int pageSize = limit(requestedSize);
        return mapper.selectPage(Page.of(1, pageSize, false), query).getRecords();
    }

    public static int limit(int requestedSize) {
        return Math.max(1, Math.min(requestedSize, MAXIMUM_PAGE_SIZE));
    }
}
