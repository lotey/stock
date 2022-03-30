/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.stock.controller;

import com.td.common.common.GlobalConstant;
import com.td.common.util.R;
import com.td.common.util.SpiderUtil;
import com.td.stock.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Date;
import java.util.Map;

/**
 * @auther lotey
 * @date 2019/7/26 11:45
 * @desc 股票RESTFUL控制器
 */
@RestController
@RequestMapping("/rest/stock")
@Slf4j
public class StockController {

    @Autowired
    private StockService stockService;

    /**
     * 统计从某天以来涨停/跌停数据列表
     * @param startDate
     * @param maxDays
     */
    @GetMapping(value = "/limitUpDownList/{startDate}/{maxDays}")
    public Map<String,Object> limitUpDownList(@PathVariable("startDate") String startDate, @PathVariable("maxDays") Integer maxDays) {
        log.info("====================开始请求N天内涨停跌停数据列表====================");
        return stockService.queryLimitUpDownList(startDate,maxDays);
    }

    /**
     * 统计从某天以来涨/跌数据列表
     * @param startDate
     * @param maxDays
     */
    @GetMapping(value = "/upDownList/{startDate}/{maxDays}/{isBest}")
    public Map<String,Object> upDownList(@PathVariable("startDate") String startDate, @PathVariable("maxDays") Integer maxDays,@PathVariable("isBest") Integer isBest) {
        log.info("====================开始请求N天内连续涨跌数据列表====================");
        return stockService.queryUpDownList(startDate,maxDays,isBest);
    }

    /**
     * 统计从某天开始N天内开始转向数据
     * @param startDate
     * @param maxDays
     */
    @GetMapping(value = "/upward/{startDate}/{maxDays}")
    public Map<String,Object> upward(@PathVariable("startDate") String startDate, @PathVariable("maxDays") Integer maxDays) {
        log.info("====================开始请求转向推荐数据列表====================");
        return stockService.queryUpwardList(startDate,maxDays);
    }

    /**
     * 统计从某天开始N天内放量数据
     * @param startDate
     * @param maxDays
     */
    @GetMapping(value = "/recommandList/{startDate}/{maxDays}")
    public Map<String,Object> volumeList(@PathVariable("startDate") String startDate, @PathVariable("maxDays") Integer maxDays) {
        log.info("====================开始请求放量数据列表====================");
        return stockService.queryVolumeMap(startDate,maxDays);
    }

    /**
     * 添加黑名单列表
     * @param codeList
     * @return
     */
    @GetMapping(value = "/addBackList")
    public R addBackList(@RequestParam String codeList) {
        log.info("====================开始添加黑名单====================");
        stockService.addBackList(codeList);
        log.info("====================添加黑名单完成====================");
        return R.ok();
    }

    /**
     * 抓取股票数据列表
     * @return
     */
    @GetMapping(value = "/crewQuotationList")
    public R crewQuotationList() {
        log.info("====================开始抓取股票数据列表====================");
        stockService.crewStockData(GlobalConstant.CREW_STOCK_ALL,true);
        log.info("====================抓取股票数据列表完成====================");
        return R.ok();
    }

    /**
     * 执行单次任务
     * @return
     */
    @GetMapping(value = "/singleTask")
    public R singleTask() {
        log.info("=====================执行单次任务启动=====================");
        long startTime = System.currentTimeMillis();
        log.info("=====================开始任务，当前时间：{}=====================", SpiderUtil.getCurrentTimeStr());

        LocalDate curDate = LocalDate.now();
        stockService.crewStockData(GlobalConstant.CREW_STOCK_ALL,true);
        stockService.updateAvgPrice(curDate);

        long endTime = System.currentTimeMillis();
        log.info("=====================任务完成，当前时间：{}=====================",SpiderUtil.getCurrentTimeStr());
        log.info("=====================共耗时：{}秒=====================",(endTime - startTime) / 1000);
        return R.ok();
    }

    /**
     * 执行最终任务
     * @return
     */
    @GetMapping(value = "/finalTask")
    public R finalTask() {
        log.info("=====================分析统计数据任务启动=====================");
        long startTime = System.currentTimeMillis();
        log.info("=====================开始任务，当前时间：{}=====================", SpiderUtil.getCurrentTimeStr());

        LocalDate date = LocalDate.now();
        stockService.crewStockData(GlobalConstant.CREW_STOCK_ALL,true);
        stockService.updateAvgPrice(date);
        stockService.updateCirMarketValue(date);
        stockService.fetchQualityStockList(date,10,0.6);
        stockService.crewDictData();

        long endTime = System.currentTimeMillis();
        log.info("=====================任务完成，当前时间：{}=====================",SpiderUtil.getCurrentTimeStr());
        log.info("=====================共耗时：{}秒=====================",(endTime - startTime) / 1000);
        return R.ok();
    }
}
