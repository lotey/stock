/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.stock;

import com.td.stock.backtrace.EnlargeVolumeBackTrace;
import com.td.stock.backtrace.HotStockBackTrace;
import com.td.stock.config.StockConfig;
import com.td.stock.service.StockQueryService;
import com.td.stock.service.StockService;
import com.td.stock.task.MonitorTask;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication(exclude = MongoAutoConfiguration.class)
@MapperScan("com.td.common.mapper")
@Slf4j
public class StockApplication implements CommandLineRunner {

    @Autowired
    private MonitorTask monitorTask;
    @Autowired
    private StockService stockService;
    @Autowired
    private StockQueryService stockQueryService;
    @Autowired
    private EnlargeVolumeBackTrace enlargeVolumeBackTrace;
    @Autowired
    private HotStockBackTrace hotStockBackTrace;
    @Autowired
    private StockConfig stockConfig;

    public static void main(String[] args) {
        SpringApplication.run(StockApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.setProperty("webdriver.chrome.driver", String.format("%s%s%s", stockConfig.getSeleniumPath(), File.separator,"chromedriver.exe"));
        log.info("===========================主调度任务开始启动===========================");
        log.info("===========================开始启动监控任务===========================");
        ExecutorService threadPool = Executors.newFixedThreadPool(2);
        threadPool.execute(() -> monitorTask.work());
        log.info("===========================开始启动其他任务===========================");
        threadPool.execute(() -> {
            try {
                LocalDate date = LocalDate.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//                Calendar calendar = Calendar.getInstance();
//                calendar.add(Calendar.DAY_OF_MONTH,-2);
//                Date maxDate = calendar.getTime();
//                date = calendar.getTime();
//                stockService.crewDictData();
//                stockService.crewStockData(GlobalConstant.CREW_STOCK_ALL,true);
//                stockService.crewStockDataDetail(4);
//                stockService.updateAvgPrice(date);
//                stockService.calcLimitUpDownList(ymdFormat.parse("2020-02-05"));
//                stockService.calcUpDownList(date);
//                stockService.updateCirMarketValue(LocalDate.parse("2021-03-31"));
//                stockService.crewHisSingAvgDataByMutiThread(LocalDate.parse("2021-03-24"),5);
//                stockService.fetchQualityStockList(ymdFormat.parse("2020-07-01"),30,0.6);
//                stockService.crewDictProp();
//                stockService.genQuoIdpDataList();
//                stockService.crewDictPropWithSelenium();
//                stockService.checkSyncTaskStatus();
//                stockService.updateLast10Trend(ymdFormat.parse("2020-06-12"));
//                stockService.calcVolumeList(ymdFormat.parse("2020-08-10"));
//                stockService.queryRecommandList(ymdFormat.parse("2020-08-10"));

                LocalDate localeData = LocalDate.now();
//                localeData = LocalDate.of(2022,2,7);
                int nDays = 130;
//                int nDays = 30;


//                log.info("============{}天之前的日期为{}",nDays,localeData.plusDays(-nDays).format(formatter));
//                stockService.crewNetEaseStockList(LocalDate.parse("2021-03-24"),nDays);
//                stockService.saveQuotationFromCSVFile(null);
//                stockService.updateCirMarketValue(ymdFormat.parse(localeData.plusDays(-1).format(formatter)));
//                stockService.crewHisSingAvgDataByMutiThread(ymdFormat.parse(localeData.plusDays(-1).format(formatter)),nDays);
//                stockService.clearVolumeData(GlobalConstant.RECOMMAND_TYPE_VOLUME);
//                stockService.clearVolumeData(GlobalConstant.RECOMMAND_TYPE_BACK);

//                stockService.fetchCuDataList(localeData);
//                stockService.truncateHisData();
//                stockService.getLastNUpLimitDataList("2021-12-29",10,3);
//                stockService.getWTSSourceCodeTypeMap(localeData);
//                stockQueryService.selectAllDictPropList();
                for (int i = 0 ; i <= nDays ; i++) {
//                    stockService.calcLimitUpDownList(ymdFormat.parse(localeData.plusDays(-i).format(formatter)));
//                    stockService.calcUpDownList(ymdFormat.parse(localeData.plusDays(-i).format(formatter)));
//                    stockService.updateLast10Trend(ymdFormat.parse(localeData.plusDays(-i).format(formatter)));
//                    stockService.fetchQualityStockList(ymdFormat.parse(localeData.plusDays(-i).format(formatter)),30,0.5);
//                    stockService.calcVolumeList(ymdFormat.parse(localeData.plusDays(-i).format(formatter)));
//                    stockService.queryRecommandList(ymdFormat.parse(localeData.plusDays(-i).format(formatter)));
                }
//                enlargeVolumeBackTrace.process(ymdFormat.parse(localeData.plusDays(-4).format(formatter)),nDays);
//                hotStockBackTrace.process(ymdFormat.parse(localeData.plusDays(-4).format(formatter)),nDays);
                log.info("=====================任务全部完成=====================");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        threadPool.shutdown();
        log.info("===========================主调度任务启动完成===========================");
    }
}
