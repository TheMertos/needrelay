package com.yagci.needrelay.web.dto;

import java.util.List;

/**
 * Generic paginated API response.
 *
 * @param items page content
 * @param page zero-based page index
 * @param size page size
 * @param totalElements total matching rows
 * @param totalPages total pages
 * @param <T> item type
 */
public record PageResponse<T>(
		List<T> items,
		int page,
		int size,
		long totalElements,
		int totalPages
) {
}
