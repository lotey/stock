/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.kzz.task;

import com.td.common.common.GlobalConstant;
import com.td.common.model.SysDict;
import com.td.common.util.SpiderUtil;
import com.td.kzz.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * @auther lotey
 * @date 2019/7/29 21:58
 * @desc 实时监控任务
 */
@Component
@Slf4j
public class MonitorTask {

    @Autowired
    private StockService stockService;
    @Autowired
    private SendMsgTask sendMsgTask;

    private List<SysDict> qualityStockList = new ArrayList<>();

    public void work() {
        log.info("===========================实时监控任务开始启动===========================");
        boolean isScheduleModel = false;
        //控制是否需要初始化QQ监控数据，初始化时不用发送QQ通知消息，否则根据监控结果推送
        boolean isInitMonitorData = true;
        for (;;) {
            try {
                Calendar calendar = Calendar.getInstance();
                long curTime = calendar.getTimeInMillis();
                LocalDate date = LocalDate.now();
                //获取当前时间是周几
                DayOfWeek weekDay = SpiderUtil.getWeekDayOfToday(date);

                //测试注释开始位
                //每天从9：40开始抓取，先必须让定时任务抓取第一次初始化数据，后才可更新
                if (SpiderUtil.isWeekendOfToday(date)) {
                    if (weekDay == DayOfWeek.SATURDAY) {
                        calendar.add(Calendar.DAY_OF_MONTH,2);
                    } else  if (weekDay == DayOfWeek.SUNDAY) {
                        calendar.add(Calendar.DAY_OF_MONTH,1);
                    }
                    calendar.set(Calendar.HOUR_OF_DAY,9);
                    calendar.set(Calendar.MINUTE,32);
                    calendar.set(Calendar.SECOND,0);

                    //休眠到下周一开盘时间开始启动运行
                    long nextMinMorningTime = calendar.getTimeInMillis();
                    //调度模式时，不需要初始化监控数据
                    if (isScheduleModel) {
                        isInitMonitorData = false;
                    }
                    Thread.sleep(nextMinMorningTime - curTime);
                } else {
                    //正常工作日时间，查看现在是否在开盘时间内，即上午9：30~11：30，下午13：00~15：00
                    //设置上午开盘时间段
                    calendar.set(Calendar.HOUR_OF_DAY,9);
                    calendar.set(Calendar.MINUTE,32);
                    calendar.set(Calendar.SECOND,0);
                    long minMorningTime = calendar.getTimeInMillis();

                    //设置上午11:30关盘时间段
                    calendar.set(Calendar.HOUR_OF_DAY,11);
                    calendar.set(Calendar.MINUTE,30);
                    calendar.set(Calendar.SECOND,30);
                    long maxMorningTime = calendar.getTimeInMillis();

                    //设置下午开盘时间段
                    calendar.set(Calendar.HOUR_OF_DAY,13);
                    calendar.set(Calendar.MINUTE,0);
                    calendar.set(Calendar.SECOND,30);
                    long minAfternoonTime = calendar.getTimeInMillis();

                    //设置下午15:00关盘时间段
                    calendar.set(Calendar.HOUR_OF_DAY,15);
                    calendar.set(Calendar.MINUTE,1);
                    calendar.set(Calendar.SECOND,0);
                    long maxAfternoonTime = calendar.getTimeInMillis();

                    //查看当前时间处于哪个区间段
                    if (curTime < minMorningTime) {
                        //调度模式时，不需要初始化监控数据
                        if (isScheduleModel) {
                            isInitMonitorData = false;
                        }
                        Thread.sleep(minMorningTime - curTime);
                    } else if (curTime > maxMorningTime && curTime < minAfternoonTime) {//上午开盘结束，休眠到下午开盘开始
                        //调度模式时，不需要初始化监控数据
                        if (isScheduleModel) {
                            isInitMonitorData = false;
                        }
                        Thread.sleep(minAfternoonTime - curTime);
                    } else if (curTime > maxAfternoonTime) {//上午开盘结束，休眠到下午开盘开始
                        //查看当前是否是周五，周五则直接休眠到下周一开盘
                        if (weekDay == DayOfWeek.FRIDAY) {
                            calendar.add(Calendar.DAY_OF_MONTH,3);
                        } else {//其他时间，周一~周四，则直接休眠到第二天开盘即可
                            calendar.add(Calendar.DAY_OF_MONTH,1);
                        }
                        calendar.set(Calendar.HOUR_OF_DAY,9);
                        calendar.set(Calendar.MINUTE,32);
                        calendar.set(Calendar.SECOND,0);
                        //闭市以后，下一个交易日启动时必须初始化监控数据
                        isInitMonitorData = true;
                        Thread.sleep(calendar.getTimeInMillis()  - curTime);
                    } else {
                        if (isScheduleModel) {
                            isInitMonitorData = false;
                        }
                    }
                }
                //测试注释结束位
                //其他正常时间段，则直接5分钟刷新一次数据
                long startTime = System.currentTimeMillis();
                log.info("=====================开始任务，当前时间：{}=====================", SpiderUtil.getCurrentTimeStr());

                //初始化优质股字典
                if (isInitMonitorData) {
                    qualityStockList = stockService.selectSysDictByType(GlobalConstant.SYS_DICT_TYPE_QUALITY_KZZ);
                }

                //轮训任务链
                stockService.crewStockData();

                long endTime = System.currentTimeMillis();
                log.info("=====================任务完成，当前时间：{}=====================",SpiderUtil.getCurrentTimeStr());
                log.info("=====================本次刷新完成，共耗时：{}秒=====================",(endTime - startTime) / 1000);

                log.info("=====================开始推送钉钉消息，当前时间：{}=====================",SpiderUtil.getCurrentTimeStr());
                //更新完以后发送钉钉消息
                try {
                    sendMsgTask.sendDDMsg(isInitMonitorData,qualityStockList);
//                    //TODO 测试
//                    isInitMonitorData = false;
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("=====================推送钉钉消息异常=====================");
                }
                log.info("=====================推送钉钉消息完成，当前时间：{}=====================",SpiderUtil.getCurrentTimeStr());

                //设置为调度模式
                isScheduleModel = true;
                Thread.sleep(50);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
