/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.stock.task;

import com.td.common.common.GlobalConstant;
import com.td.common.model.CuMonitor;
import com.td.common.model.Quotation;
import com.td.common.util.SpiderUtil;
import com.td.common.vo.CodeIdpsVo;
import com.td.common.vo.DictPropVo;
import com.td.common.vo.IdpCodesVo;
import com.td.stock.service.StockQueryService;
import com.td.stock.service.StockService;
import com.td.stock.vo.CodeLimitUDPVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private StockQueryService stockQueryService;
    @Autowired
    private SendMsgTask sendMsgTask;

    //维护监控股票池，只监控股票池的（非全量）
    public static Map<String, CuMonitor> codeMonitorMap = new HashMap<>();
    private Map<String, DictPropVo> codePropMap = new HashMap<>();
    //计算每日涨停跌停的价格
    public static Map<String, CodeLimitUDPVo> codeLimitUDPMap = new HashMap<>();
    //记录昨天弱转强原始股票列表
    public static Map<String,Integer> lastDayWTSSourceCodeMap = new HashMap<>();
    //股票和行业映射
    public static Map<String,String[]> codeIdpsMap = new HashMap<>();
    //行业和股票映射
    public static Map<String,List<String>> idpCodesMap = new HashMap<>();

    //统计总监控次数
    public static int crewCount = 0;

    public void work() {
        log.info("===========================实时监控任务开始启动===========================");
        //是否初始化不变化的数据，不变的只用抓取一次，不用每次轮训都抓取
        boolean isInit = true;
        for (;;) {
            try {
                Calendar calendar = Calendar.getInstance();
                long curTime = calendar.getTimeInMillis();
                LocalDate date = LocalDate.now();
                //获取当前时间是周几
                DayOfWeek weekDay = SpiderUtil.getWeekDayOfToday(date);

                //测试注释开始位
                //先检测是否是周末，周末则休眠到下一个工作日
                if (SpiderUtil.isWeekendOfToday(date)) {
                    log.info("=====================周末不开盘，休眠到下一个工作日=====================");
                    if (weekDay == DayOfWeek.SATURDAY) {
                        calendar.add(Calendar.DAY_OF_MONTH,2);
                    } else  if (weekDay == DayOfWeek.SUNDAY) {
                        calendar.add(Calendar.DAY_OF_MONTH,1);
                    }
                    calendar.set(Calendar.HOUR_OF_DAY,9);
                    calendar.set(Calendar.MINUTE,30);
                    calendar.set(Calendar.SECOND,2);

                    //休眠到下周一开盘时间开始启动运行
                    long nextMinMorningTime = calendar.getTimeInMillis();
                    Thread.sleep(nextMinMorningTime - curTime);
                    isInit = true;
                } else {
                    //正常工作日时间，查看现在是否在开盘时间内，即上午9：30~11：30，下午13：00~15：00
                    //设置上午开盘时间段
                    calendar.set(Calendar.HOUR_OF_DAY,9);
                    calendar.set(Calendar.MINUTE,26);
                    calendar.set(Calendar.SECOND,30);
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
                        Thread.sleep(minMorningTime - curTime);
                    } else if (curTime > maxMorningTime && curTime < minAfternoonTime) {//上午开盘结束，休眠到下午开盘开始
                        Thread.sleep(minAfternoonTime - curTime);
                    } else if (curTime > maxAfternoonTime) {//下午收盘结束，休眠到下个交易日开盘开始
                        //查看当前是否是周五，周五则直接休眠到下周一开盘
                        if (weekDay == DayOfWeek.FRIDAY) {
                            calendar.add(Calendar.DAY_OF_MONTH,3);
                        } else {//其他时间，周一~周四，则直接休眠到第二天开盘即可
                            calendar.add(Calendar.DAY_OF_MONTH,1);
                        }
                        calendar.set(Calendar.HOUR_OF_DAY,9);
                        calendar.set(Calendar.MINUTE,26);
                        calendar.set(Calendar.SECOND,30);
                        Thread.sleep(calendar.getTimeInMillis()  - curTime);
                        //闭市以后，下一个交易日启动时必须初始化监控数据
                        isInit = true;
                    }
                }

                //每天第一次抓取的时候检测今天是否正常交易日，用成交量检测，减少非交易日的无效抓取
                DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                if (isInit) {
                    date = LocalDate.now();
                    while (stockService.checkIsRestDay(date)) {
                        log.info("=====================日期[{}]，非正常交易日，休眠到下一天=====================",date.format(ymdFormatter));
                        //非正常交易日，休眠到下一天
                        calendar.add(Calendar.DAY_OF_MONTH,1);
                        calendar.set(Calendar.HOUR_OF_DAY,9);
                        calendar.set(Calendar.MINUTE,26);
                        calendar.set(Calendar.SECOND,30);

                        //休眠到下周一开盘时间开始启动运行
                        curTime = System.currentTimeMillis();
                        long nextMinMorningTime = calendar.getTimeInMillis();
                        try {
                            Thread.sleep(nextMinMorningTime - curTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        //重新初始化当前时间
                        date = LocalDate.now();
                    }
                }
                //测试注释结束位
                //其他正常时间段，则直接5分钟刷新一次数据
                long startTime = System.currentTimeMillis();
                log.info("=====================开始任务，当前时间：{}=====================", SpiderUtil.getCurrentTimeStr());

                //监控池初始化
                if (isInit) {
                    //查询股票的属性信息并按code映射，code -> prop
                    List<DictPropVo> dictPropVoList = stockQueryService.selectAllDictPropList();
                    codePropMap = dictPropVoList.stream().collect(Collectors.toMap(DictPropVo::getCode, Function.identity(),(o,n) ->n));

                    List<CuMonitor> cuMonitorList = stockQueryService.selectNewestMonitorList();
                    codeMonitorMap = cuMonitorList.stream().collect(Collectors.toMap(CuMonitor::getCode, Function.identity(),(o, n) ->n));

                    //初始化股票和板块概念映射
                    List<CodeIdpsVo> codeIdpsVoList = stockQueryService.queryCodeIdpsList();
                    if (codeIdpsVoList.size() > 0) {
                        codeIdpsVoList.forEach(cip -> {
                            if (StringUtils.isNotEmpty(cip.getIdpNames())) {
                                codeIdpsMap.put(cip.getCode(),cip.getIdpNames().split(","));
                            }
                        });
                    }
                    //初始化板块概念和股票映射
                    List<IdpCodesVo> idpCodesVoList = stockQueryService.queryIdpCodesList();
                    if (idpCodesVoList.size() > 0) {
                        idpCodesVoList.forEach(ics -> {
                            if (StringUtils.isNotEmpty(ics.getCodes())) {
                                idpCodesMap.put(ics.getIdpName(), Arrays.asList(ics.getCodes().split(",")));
                            }
                        });
                    }

                    //初始化上个交易日的涨停跌停价
                    List<Quotation> lastDayQuoList = stockQueryService.selectLastDayQuoList();
                    codeLimitUDPMap = new HashMap<>();
                    if (lastDayQuoList.size() > 0) {
                        lastDayQuoList.forEach(q -> {
                            CodeLimitUDPVo vo = new CodeLimitUDPVo();
                            vo.setCode(q.getCode());
                            vo.setUpLimitVal(q.getClose().multiply(BigDecimal.valueOf(1.1)).setScale(2, RoundingMode.HALF_UP).doubleValue());
                            vo.setDownLimitVal(q.getClose().multiply(BigDecimal.valueOf(0.9)).setScale(2, RoundingMode.HALF_UP).doubleValue());
                            //设置连板数
                            String pDesc = null;
                            if (codeMonitorMap.containsKey(q.getCode())) {
                                int type = Integer.parseInt(codeMonitorMap.get(q.getCode()).getType().split("_")[1]);
                                switch (type) {
                                    case 0:
                                        pDesc = "首版";
                                        break;
                                    case 1:
                                        pDesc = "1进2";
                                        break;
                                    case 2:
                                        pDesc = "2进3";
                                        break;
                                    default:
                                        break;
                                }
                            } else {
                                pDesc = "首版";
                            }
                            vo.setPDesc(pDesc);
                            codeLimitUDPMap.put(q.getCode(),vo);
                        });
                    }

                    //初始化上个交易日放量暴跌或假阴的股票列表
                    LocalDate lastDate = lastDayQuoList.stream().filter(x -> GlobalConstant.STOCK_REFER_CODE.equals(x.getCode())).map(Quotation::getDate).findFirst().get();
                    lastDayWTSSourceCodeMap = stockService.getWTSSourceCodeTypeMap(lastDate);

                    //初始化抓取次数
                    crewCount = 1;
                }

                //抓取股票行情实时数据
                stockService.crewStockData(GlobalConstant.CREW_STOCK_MAIN_BOARD,false);

                long endTime = System.currentTimeMillis();
                log.info("=====================任务完成，当前时间：{}=====================",SpiderUtil.getCurrentTimeStr());
                log.info("=====================本次刷新完成，共耗时：{}秒=====================",(endTime - startTime) / 1000);

                log.info("=====================开始推送钉钉消息，当前时间：{}=====================",SpiderUtil.getCurrentTimeStr());
                //更新完以后发送钉钉消息
                try {
                    sendMsgTask.sendDDMsg(isInit,codePropMap);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("=====================推送钉钉消息异常=====================");
                }
                log.info("=====================推送钉钉消息完成，当前时间：{}=====================",SpiderUtil.getCurrentTimeStr());
                isInit = false;
                //累计监控次数
                crewCount++;

                Thread.sleep(500);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
