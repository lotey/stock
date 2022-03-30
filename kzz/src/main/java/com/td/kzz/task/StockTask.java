/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.kzz.task;

import com.td.common.util.SpiderUtil;
import com.td.kzz.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Date;

/**
 * @auther lotey
 * @date 2019/7/26 11:51
 * @desc 定时调度任务
 */
@Component
@EnableScheduling
@Configurable
@Slf4j
public class StockTask {

    @Autowired
    private StockService stockService;

    //=============================分析统计数据任务开始=============================

    @Scheduled(cron = "0 30 16 * * ?")
    public void analyStatData() {
        log.info("=====================分析统计数据任务启动=====================");
        long startTime = System.currentTimeMillis();
        log.info("=====================开始任务，当前时间：{}=====================", SpiderUtil.getCurrentTimeStr());
        LocalDate date = LocalDate.now();
        if (SpiderUtil.isWeekendOfToday(date)) {
            log.info("==========================周末时间不开盘，忽略本次任务==========================");
            return;
        }
        stockService.crewDictData();
        stockService.updateCirMarketValue();
        stockService.fetchQualityStockList();
        stockService.crewStockData();

        long endTime = System.currentTimeMillis();
        log.info("=====================任务完成，当前时间：{}=====================",SpiderUtil.getCurrentTimeStr());
        log.info("=====================共耗时：{}秒=====================",(endTime - startTime) / 1000);
    }

    //=============================分析统计数据任务结束=============================
}
