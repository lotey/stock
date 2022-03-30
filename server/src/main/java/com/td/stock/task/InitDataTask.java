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
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * @auther lotey
 * @date 7/14/20 10:57 PM
 * @desc 初始化行情数据
 */
//@Component
@Slf4j
public class InitDataTask implements CommandLineRunner {

    @Autowired
    private StockService stockService;

    @Override
    public void run(String... args) throws Exception {
        log.info("=====================开始初始化数据，当前时间：{}=====================", SpiderUtil.getCurrentTimeStr());
        long startTime = System.currentTimeMillis();
        DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        int nDays = 130;
        LocalDate initDate = LocalDate.now();
//        initDate = LocalDate.of(2020,8,10);
        stockService.crewDictData();
        stockService.crewNetEaseStockList(initDate,nDays);
        stockService.saveQuotationFromCSVFile(null);
        stockService.updateCirMarketValue(initDate);

        //先统计基础数据
        for (int i = 0 ; i <= nDays ; i++) {
            stockService.calcLimitUpDownList(LocalDate.parse(initDate.plusDays(-i).format(ymdFormatter)));
            stockService.calcUpDownList(LocalDate.parse(initDate.plusDays(-i).format(ymdFormatter)));
        }
        stockService.crewHisSingAvgDataByMutiThread(initDate,nDays);
        stockService.clearVolumeData(GlobalConstant.RECOMMAND_TYPE_VOLUME);
        //先统计聚合数据
        for (int i = 0 ; i <= nDays - 30 ; i++) {
            stockService.updateLast10Trend(LocalDate.parse(initDate.plusDays(-i).format(ymdFormatter)));
            stockService.fetchQualityStockList(LocalDate.parse(initDate.plusDays(-i).format(ymdFormatter)),30,0.5);
            stockService.calcVolumeList(LocalDate.parse(initDate.plusDays(-i).format(ymdFormatter)));
        }
        long endTime = System.currentTimeMillis();
        log.info("=====================初始化数据完成，当前时间：{}，共耗时:{}秒=====================", SpiderUtil.getCurrentTimeStr(),(endTime - startTime) / 1000);
    }
}
