package com.competition.invoice.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

/**
 * 分页响应包装
 */
@Data
public class PageResult<T> {

    private long page;
    private long size;
    private long total;
    private long totalPages;
    private List<T> records;

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> r = new PageResult<>();
        r.page = page.getCurrent();
        r.size = page.getSize();
        r.total = page.getTotal();
        r.totalPages = page.getPages();
        r.records = page.getRecords();
        return r;
    }
}
