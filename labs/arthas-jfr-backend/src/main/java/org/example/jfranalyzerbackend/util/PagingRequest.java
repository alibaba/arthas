package org.example.jfranalyzerbackend.util;

public class PagingRequest {
    // 页码从1开始
    private int page;

    // 页面大小，必须大于0
    private int pageSize;

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    /**
     * 创建新的分页请求
     *
     * @param page     页码，从1开始
     * @param pageSize 页面大小，必须大于0
     */
    public PagingRequest(int page, int pageSize) {
//        Validate.isTrue(page >= 1 && pageSize >= 1);
        this.page = page;
        this.pageSize = pageSize;
    }

    /**
     * @return 起始索引（包含），从0开始
     */
    public int from() {
        return (page - 1) * pageSize;
    }

    /**
     * @param totalSize 元素总数
     * @return 结束索引（不包含）
     */
    public int to(int totalSize) {
        return Math.min(from() + pageSize, totalSize);
    }
}
