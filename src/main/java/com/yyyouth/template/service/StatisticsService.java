package com.yyyouth.template.service;

import com.yyyouth.template.model.vo.StatisticsVO;

/**
 * 统计服务
 *
 * @author yyyouth
 */
public interface StatisticsService {

    /**
     * 获取系统统计数据
     *
     * @return 统计数据
     */
    StatisticsVO getStatistics();
}
