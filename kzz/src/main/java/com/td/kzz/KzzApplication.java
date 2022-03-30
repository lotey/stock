/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.kzz;

import com.td.kzz.service.StockService;
import com.td.kzz.task.MonitorTask;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication(exclude = MongoAutoConfiguration.class)
@MapperScan("com.td.common.mapper")
@Slf4j
public class KzzApplication implements CommandLineRunner {

    @Autowired
    private MonitorTask monitorTask;
    @Autowired
    private StockService stockService;

    public static void main(String[] args) {
        SpringApplication.run(KzzApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("===========================主调度任务开始启动===========================");
        log.info("===========================开始启动监控任务===========================");
        ExecutorService threadPool = Executors.newFixedThreadPool(2);
        threadPool.execute(() -> monitorTask.work());
        log.info("===========================开始启动其他任务===========================");
        threadPool.execute(() -> {
            try {
//                stockService.crewDictData();
//                stockService.updateCirMarketValue();
//                stockService.fetchQualityStockList();
//                stockService.crewStockData();
                log.info("=====================任务全部完成=====================");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        threadPool.shutdown();
        log.info("===========================主调度任务启动完成===========================");
    }
}
