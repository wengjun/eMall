package com.emall.common.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import org.junit.jupiter.api.Test;

class BoundedQueryTest {
    @Test
    void capsEveryRequestAtTheGlobalMaximumPageSize() {
        BaseMapper<String> mapper = mapper();
        when(mapper.selectPage(any(Page.class), isNull())).thenAnswer(invocation -> {
            Page<String> page = invocation.getArgument(0);
            assertThat(page.getSize()).isEqualTo(BoundedQuery.MAXIMUM_PAGE_SIZE);
            return page.setRecords(List.of("bounded"));
        });

        assertThat(BoundedQuery.page(mapper, Integer.MAX_VALUE)).containsExactly("bounded");
    }

    @SuppressWarnings("unchecked")
    private BaseMapper<String> mapper() {
        return mock(BaseMapper.class);
    }
}
