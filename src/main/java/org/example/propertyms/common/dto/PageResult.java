package org.example.propertyms.common.dto;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * 通用分页结果包装类。
 */
@Getter
public class PageResult<T> {
    private final List<T> items;
    private final int page;
    private final int pageSize;
    private final long totalItems;
    private final int totalPages;

    public PageResult(List<T> items, int page, int pageSize, long totalItems) {
        this.items = items == null ? Collections.emptyList() : items;
        this.page = Math.max(page, 1);
        this.pageSize = Math.max(pageSize, 1);
        this.totalItems = Math.max(totalItems, 0);
        this.totalPages = (int) Math.ceil((double) this.totalItems / this.pageSize);
    }

    public boolean isHasPrev() {
        return page > 1;
    }

    public boolean isHasNext() {
        return page < totalPages;
    }

    /** Returns a window of page numbers around the current page for display. */
    public List<Integer> getPageWindow() {
        if (totalPages <= 0) {
            return Collections.emptyList();
        }
        int windowSize = 7;
        int half = windowSize / 2;
        int start = Math.max(1, page - half);
        int end = Math.min(totalPages, start + windowSize - 1);
        if (end - start + 1 < windowSize) {
            start = Math.max(1, end - windowSize + 1);
        }
        List<Integer> pages = new java.util.ArrayList<>();
        for (int i = start; i <= end; i++) {
            pages.add(i);
        }
        return pages;
    }

    public static int calcOffset(int page, int pageSize) {
        return (Math.max(page, 1) - 1) * Math.max(pageSize, 1);
    }
}


