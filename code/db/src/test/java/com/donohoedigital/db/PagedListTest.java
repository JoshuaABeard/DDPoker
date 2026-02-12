/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file
 * in the root directory of this project.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.db;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for PagedList - specialized ArrayList that tracks total result set size
 * for paginated queries.
 */
class PagedListTest {

    // =================================================================
    // Constructor Tests
    // =================================================================

    @Test
    void should_CreateEmptyList_When_DefaultConstructorUsed() {
        PagedList<String> list = new PagedList<>();

        assertThat(list).isEmpty();
        assertThat(list.getTotalSize()).isZero();
    }

    @Test
    void should_CreateListWithCapacity_When_CapacityConstructorUsed() {
        PagedList<String> list = new PagedList<>(100);

        assertThat(list).isEmpty();
        assertThat(list.getTotalSize()).isZero();
    }

    @Test
    void should_CreateListFromCollection_When_CollectionConstructorUsed() {
        List<String> source = Arrays.asList("a", "b", "c");
        PagedList<String> list = new PagedList<>(source);

        assertThat(list).containsExactly("a", "b", "c");
        assertThat(list.getTotalSize()).isZero(); // Total size not automatically set
    }

    @Test
    void should_ThrowException_When_NegativeCapacity() {
        assertThatThrownBy(() -> new PagedList<String>(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    // =================================================================
    // TotalSize Tests
    // =================================================================

    @Test
    void should_ReturnZero_When_TotalSizeNotSet() {
        PagedList<String> list = new PagedList<>();

        assertThat(list.getTotalSize()).isZero();
    }

    @Test
    void should_ReturnSetValue_When_TotalSizeSet() {
        PagedList<String> list = new PagedList<>();
        list.setTotalSize(100);

        assertThat(list.getTotalSize()).isEqualTo(100);
    }

    @Test
    void should_UpdateTotalSize_When_SetMultipleTimes() {
        PagedList<String> list = new PagedList<>();
        list.setTotalSize(50);
        list.setTotalSize(100);

        assertThat(list.getTotalSize()).isEqualTo(100);
    }

    @Test
    void should_AllowZeroTotalSize_When_SetExplicitly() {
        PagedList<String> list = new PagedList<>();
        list.setTotalSize(100);
        list.setTotalSize(0);

        assertThat(list.getTotalSize()).isZero();
    }

    @Test
    void should_AllowNegativeTotalSize_When_Set() {
        // Note: No validation on totalSize - allows negative (though semantically
        // invalid)
        PagedList<String> list = new PagedList<>();
        list.setTotalSize(-1);

        assertThat(list.getTotalSize()).isEqualTo(-1);
    }

    // =================================================================
    // ArrayList Behavior Tests
    // =================================================================

    @Test
    void should_BehaveAsArrayList_When_AddingElements() {
        PagedList<String> list = new PagedList<>();
        list.add("first");
        list.add("second");

        assertThat(list).containsExactly("first", "second");
        assertThat(list.size()).isEqualTo(2);
    }

    @Test
    void should_BehaveAsArrayList_When_RemovingElements() {
        PagedList<String> list = new PagedList<>(Arrays.asList("a", "b", "c"));
        list.remove("b");

        assertThat(list).containsExactly("a", "c");
        assertThat(list.size()).isEqualTo(2);
    }

    @Test
    void should_PreserveTotalSize_When_ListModified() {
        PagedList<String> list = new PagedList<>();
        list.setTotalSize(100);

        list.add("item");
        list.remove("item");

        // Total size should remain unchanged by list operations
        assertThat(list.getTotalSize()).isEqualTo(100);
    }

    // =================================================================
    // Pagination Use Case Tests
    // =================================================================

    @Test
    void should_TrackPagedResults_When_UsedForPagination() {
        // Simulate search with 100 total results, showing page 2 (10 items per page)
        PagedList<String> page2Results = new PagedList<>();
        page2Results.setTotalSize(100);

        // Add 10 results for this page
        for (int i = 10; i < 20; i++) {
            page2Results.add("Result " + i);
        }

        assertThat(page2Results.size()).isEqualTo(10); // Current page size
        assertThat(page2Results.getTotalSize()).isEqualTo(100); // Total result set
    }

    @Test
    void should_IndicateMorePages_When_TotalSizeExceedsPageSize() {
        PagedList<String> page = new PagedList<>();
        page.setTotalSize(100);

        for (int i = 0; i < 10; i++) {
            page.add("Item " + i);
        }

        boolean hasMorePages = page.getTotalSize() > page.size();
        assertThat(hasMorePages).isTrue();
    }

    @Test
    void should_IndicateLastPage_When_TotalSizeMatchesItemsShown() {
        PagedList<String> page = new PagedList<>();
        page.setTotalSize(10);

        for (int i = 0; i < 10; i++) {
            page.add("Item " + i);
        }

        boolean isLastPage = page.getTotalSize() == page.size();
        assertThat(isLastPage).isTrue();
    }

    @Test
    void should_CalculatePageCount_When_TotalSizeKnown() {
        PagedList<String> page = new PagedList<>();
        page.setTotalSize(95);

        int pageSize = 10;
        int totalPages = (int) Math.ceil((double) page.getTotalSize() / pageSize);

        assertThat(totalPages).isEqualTo(10); // 95 items = 10 pages
    }

    // =================================================================
    // Edge Case Tests
    // =================================================================

    @Test
    void should_HandleEmptyPage_When_NoResults() {
        PagedList<String> emptyPage = new PagedList<>();
        emptyPage.setTotalSize(0);

        assertThat(emptyPage).isEmpty();
        assertThat(emptyPage.getTotalSize()).isZero();
    }

    @Test
    void should_HandleMismatch_When_TotalSizeDiffersFromActualSize() {
        // Simulate case where total size doesn't match actual items
        // (e.g., concurrent modification during pagination)
        PagedList<String> list = new PagedList<>();
        list.setTotalSize(100);
        list.add("only one item");

        assertThat(list.size()).isEqualTo(1);
        assertThat(list.getTotalSize()).isEqualTo(100);
        // This is valid - totalSize represents the full result set,
        // not necessarily what's in this page
    }

    @Test
    void should_WorkWithGenericTypes_When_UsingDifferentTypes() {
        PagedList<Integer> intList = new PagedList<>();
        intList.setTotalSize(1000);
        intList.add(1);
        intList.add(2);

        assertThat(intList).containsExactly(1, 2);
        assertThat(intList.getTotalSize()).isEqualTo(1000);
    }

    @Test
    void should_MaintainTypeSignature_When_UsingCustomObjects() {
        PagedList<TestObject> objectList = new PagedList<>();
        objectList.setTotalSize(50);
        objectList.add(new TestObject("test"));

        assertThat(objectList).hasSize(1);
        assertThat(objectList.get(0).value).isEqualTo("test");
        assertThat(objectList.getTotalSize()).isEqualTo(50);
    }

    // =================================================================
    // Test Helper Classes
    // =================================================================

    private static class TestObject {
        final String value;

        TestObject(String value) {
            this.value = value;
        }
    }
}
