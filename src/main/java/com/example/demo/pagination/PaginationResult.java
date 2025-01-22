package com.example.demo.pagination;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.query.Query;

public class PaginationResult<E> {

	private final int totalRecords;
	private final int currentPage;
	private final List<E> list;
	private final int maxResult;
	private final int totalPages;
	private final List<Integer> navigationPages;

	public PaginationResult(Query<E> query, int page, int maxResult, int maxNavigationPage) {
		this.currentPage = Math.max(page, 1); // Ensure page is at least 1
		this.maxResult = maxResult;

		int fromRecordIndex = (this.currentPage - 1) * maxResult;
		List<E> results = new ArrayList<>();
		int totalRecordsTemp = 0;

		try (ScrollableResults resultScroll = query.scroll(ScrollMode.SCROLL_INSENSITIVE)) {
			if (resultScroll.first() && resultScroll.scroll(fromRecordIndex)) {
				do {
					results.add((E) resultScroll.get(0));
				} while (resultScroll.next() && resultScroll.getRowNumber() < fromRecordIndex + maxResult);

				resultScroll.last();
				totalRecordsTemp = resultScroll.getRowNumber() + 1; // Adjust row count for zero-based index
			}
		} catch (Exception e) {
			throw new RuntimeException("Error during pagination processing", e);
		}

		this.list = results;
		this.totalRecords = totalRecordsTemp;
		this.totalPages = calculateTotalPages();
		this.navigationPages = calculateNavigationPages(maxNavigationPage);
	}

	private int calculateTotalPages() {
		return (int) Math.ceil((double) totalRecords / maxResult);
	}

	private List<Integer> calculateNavigationPages(int maxNavigationPage) {
		List<Integer> navigation = new ArrayList<>();
		int begin = Math.max(1, currentPage - maxNavigationPage / 2);
		int end = Math.min(totalPages, currentPage + maxNavigationPage / 2);

		if (begin > 1) navigation.add(1); // First page
		if (begin > 2) navigation.add(-1); // Ellipsis

		for (int i = begin; i <= end; i++) {
			navigation.add(i);
		}

		if (end < totalPages - 1) navigation.add(-1); // Ellipsis
		if (end < totalPages) navigation.add(totalPages); // Last page

		return navigation;
	}

	public int getTotalPages() {
		return totalPages;
	}

	public int getTotalRecords() {
		return totalRecords;
	}

	public int getCurrentPage() {
		return currentPage;
	}

	public List<E> getList() {
		return list;
	}

	public List<Integer> getNavigationPages() {
		return navigationPages;
	}
}
