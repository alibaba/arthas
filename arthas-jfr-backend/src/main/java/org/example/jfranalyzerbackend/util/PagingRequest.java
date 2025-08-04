package org.example.jfranalyzerbackend.util;

public class PagingRequest {
    // page index starts with 1
    private int page;

    // page size, must be greater than 0
    private int pageSize;

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    /**
     * Create a new PagingRequest
     *
     * @param page     page index, starts with 1
     * @param pageSize page size, must be greater than 0
     */
    public PagingRequest(int page, int pageSize) {
//        Validate.isTrue(page >= 1 && pageSize >= 1);
        this.page = page;
        this.pageSize = pageSize;
    }

    /**
     * @return from index (inclusive), starts with 0
     */
    public int from() {
        return (page - 1) * pageSize;
    }

    /**
     * @param totalSize total size of the elements
     * @return end index (exclusive)
     */
    public int to(int totalSize) {
        return Math.min(from() + pageSize, totalSize);
    }
}
