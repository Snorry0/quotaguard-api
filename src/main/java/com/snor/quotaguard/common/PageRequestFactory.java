package com.snor.quotaguard.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public final class PageRequestFactory {

    private static final int MAX_PAGE_SIZE = 100;

    private PageRequestFactory() {
    }

    public static PageRequest of(int page, int size, Sort sort) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(Math.max(page, 0), safeSize, sort);
    }
}
