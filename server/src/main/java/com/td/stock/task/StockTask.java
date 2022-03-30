/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.stock.task;

import com.td.common.common.GlobalConstant;
import com.td.common.util.SpiderUtil;
import com.td.stock.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

/**
 * @auther lotey
 * @date 2019/7/26 11:51
 * @desc 定时调度任务
 */
@Component
@EnableScheduling
@Slf4j
public class StockTask {

    @Autowired
    private StockService stockService;

    //=============================分析统计数据任务开始=============================

    @Scheduled(cron = "0 0 18 * * ?")
    public void analyStatData() {
        log.info("=====================分析统计数据任务启动=====================");
        long startTime = System.currentTimeMillis();
        log.info("=====================开始任务，当前时间：{}=====================", SpiderUtil.getCurrentTimeStr());
        LocalDate date = LocalDate.now();
        if (SpiderUtil.isWeekendOfToday(date)) {
            log.info("==========================周末时间不开盘，忽略本次任务==========================");
            stockService.clearInvalidDataList(date);
            return;
        }
        boolean isRestDay = stockService.checkIsRestDay(date);
        if (isRestDay) {
            log.info("==========================非交易日，清除无效数据，忽略本次任务==========================");
            stockService.clearInvalidDataList(date);
            return;
        }
        stockService.crewDictData();
        stockService.crewStockData(GlobalConstant.CREW_STOCK_ALL,true);
        stockService.updateCirMarketValue(date);
        stockService.crewHisSingAvgDataByMutiThread(date,5);
        stockService.fetchQualityStockList(date,30,0.5);
        stockService.updateLast10Trend(date);
        stockService.fetchCuDataList(date);
        stockService.truncateHisData();

        long endTime = System.currentTimeMillis();
        log.info("=====================任务完成，当前时间：{}=====================",SpiderUtil.getCurrentTimeStr());
        log.info("=====================共耗时：{}秒=====================",(endTime - startTime) / 1000);
    }

    //=============================分析统计数据任务结束=============================

//    @Scheduled(cron = "0 0 21 * * ?")
    public void checkSyncAvgDataStatus() {
        log.info("=====================钉钉监控同步任务完成启动=====================");
        stockService.checkSyncTaskStatus();
    }
}
