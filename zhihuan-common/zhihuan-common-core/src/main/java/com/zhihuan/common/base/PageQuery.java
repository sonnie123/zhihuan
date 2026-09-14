package com.zhihuan.common.base;

import lombok.Data;
import java.io.Serializable;

@Data
public class PageQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long pageNum = 1L;
    private Long pageSize = 10L;

    public Long getOffset() {
        return (pageNum - 1) * pageSize;
    }
}
