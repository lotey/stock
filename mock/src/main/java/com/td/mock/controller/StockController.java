/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.mock.controller;

import com.td.common.util.R;
import com.td.mock.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @auther lotey
 * @date 2019/7/26 11:45
 * @desc 股票抓取新浪数据测试控制器
 */
@RestController
@RequestMapping("/rest/mock")
@Slf4j
public class StockController {

    @Autowired
    private StockService stockService;

    /**
     *  模拟行情列表
     * @param codeStrList
     */
    @GetMapping(value = "/list={codeStrList}/{count}")
    public String mocDatakList(@PathVariable("codeStrList") String codeStrList,@PathVariable("count") Integer count) {
        log.info("====================开始请求mock行情数据列表====================");
        String result = stockService.getMockDataList(codeStrList,count);
        log.debug("==================result => {}",result);
        return result;
    }

    /**
     * 设置行情请求次数
     * @param date
     * @return
     */
    @GetMapping(value = "/setCrewCount/{date}")
    public R setCrewCount(@PathVariable("date") String date) {
        log.info("====================开始设置行情次数列表====================");
        stockService.setCrewCount(date);
        return R.ok();
    }

    /**
     * 模拟列表
     * @param num
     * @param isRestart
     * @return
     */
    @GetMapping(value = "/data/{num}/{isRestart}")
    public String getMockDataList(@PathVariable("num") int num,@PathVariable("isRestart") int isRestart) {
        log.info("====================开始请求随机行情数据列表====================");
        String result = stockService.getMockDataList(num,isRestart);
        log.debug("==================result => {}",result);
        return result;
    }

//    /**
//     * 抓取米扑代理列表
//     * @param count
//     * @return
//     */
//    @GetMapping(value = "/crewMimvpProxyList/{count}")
//    public Map<String,Object> crewMimvpProxyList(@PathVariable("count") Integer count) {
//        log.info("=====================crew sina avg data list start=====================");
//        long startTime = System.currentTimeMillis();
//        log.info("=====================task start，current time：{}=====================", SpiderUtil.getCurrentTimeStr());
//        List<ProxyEntity> proxyList = stockService.crewMimvpFreeProxyList(count);
//        long endTime = System.currentTimeMillis();
//        log.info("=====================task finished，current time：{}=====================",SpiderUtil.getCurrentTimeStr());
//        log.info("=====================total cost ：{}s=====================",(endTime - startTime) / 1000);
//        Map<String,Object> resultMap = new HashMap<>();
//        resultMap.put("code",200);
//        resultMap.put("message","操作成功");
//        resultMap.put("data",proxyList);
//        return resultMap;
//    }
}
