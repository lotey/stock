/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.stock.task;

import com.td.common.common.GlobalConstant;
import com.td.common.model.LimitUpDown;
import com.td.common.model.Quotation;
import com.td.common.util.SpiderUtil;
import com.td.common.vo.DictPropVo;
import com.td.stock.service.StockQueryService;
import com.td.stock.service.StockService;
import com.td.stock.vo.QuoORateVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @auther lotey
 * @date 2019/8/1 17:10
 * @desc 发送钉钉消息任务
 */
@Component
@Slf4j
public class SendMsgTask {

    //记录上一次行情列表
    private List<Quotation> lastQuoList = null;
    //记录之前涨停的股票代码列表
    private Set<String> lastUpLimitCodeSet = null;
    //记录之前跌停的股票代码列表
    private Set<String> lastDownLimitCodeSet = null;
    //第一档，2s粒度
    private List<Set<String>> level1UpCodeList = new LinkedList<>();
    //第二档，5s粒度
    private List<Set<String>> level2UpCodeList = new LinkedList<>();
    //第三档，10s粒度
    private List<Set<String>> level3UpCodeList = new LinkedList<>();
    //第四档，30s粒度
    private List<Set<String>> level4UpCodeList = new LinkedList<>();
    //第五档，1m粒度
    private List<Set<String>> level5UpCodeList = new LinkedList<>();
    //第一档，5s粒度
    private Map<String,List<Quotation>> level1HisQuoMap = new HashMap<>();
    //第二档，10s粒度
    private Map<String,List<Quotation>> level2HisQuoMap = new HashMap<>();
    //第三档，30s粒度
    private Map<String,List<Quotation>> level3HisQuoMap = new HashMap<>();
    //第四档，1min粒度
    private Map<String,List<Quotation>> level4HisQuoMap = new HashMap<>();
    //第五档，2min粒度
    private Map<String,List<Quotation>> level5HisQuoMap = new HashMap<>();
    //记录今天即将涨停的股票列表
    private Map<String,Integer> wUpLimitCodeMap = new HashMap<>();
    //直线拉升监控期数
    private static final int MONITOR_CYCLE_NUM = 5;
    //最低上涨次数
    private static final int MIN_UP_COUNT = 3;
    //最小上涨率
    private static final double MIN_UP_RATE = 1.5;
    //最小涨幅
    private static final double MIN_OFFSET_RATE = 4;
    //最小成交量
    private static final double MIN_VOLUME_AMT = 100000000;
    //记录股票启动拉升位置，1档和2档
    private Map<String,Quotation> codeStartCUpMap = new HashMap<>();
    //记录最近10分钟的行情涨幅数据，计算最大回撤
    private List<QuoORateVo> last10ORateList = new ArrayList<>();
    //监控回撤周期数，最近10分钟的=200次
    private static final double MONITOR_DRAWDOWN_CYCLE_NUM = 200;
    //所有的进攻型股票捕获次数映射
    private Map<String,Integer> hAttackCodeMap = new HashMap<>();
    //记录每个股票连续拉升的次数
    private Map<String,List<Integer>> cUpCodeCountMap = new HashMap<>();
    //交易量大的股票列表
    private Map<String,Integer> largeVolCodeMap = new HashMap<>();
    //记录今天竞价高开的弱转强列表
    private List<String> weakToStrongCodeList = new ArrayList<>();
    //每个行业涨幅前N的股票列表
    Map<String,List<String>> idpTopNCodesMap = new HashMap<>();

    @Autowired
    private StockService stockService;
    @Autowired
    private StockQueryService stockQueryService;

    /**
     * 根据当天监控数据的变化，发送钉钉通知消息
     * @param isInit
     * @param codePropMap
     */
    public void sendDDMsg(boolean isInit, Map<String, DictPropVo> codePropMap) {
        LocalDate date = LocalDate.now();
        DateTimeFormatter ymdFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String strDate = date.format(ymdFormat);

        //查询当前行情列表
        List<Quotation> quotationList = stockQueryService.selectListByDate(strDate);
        //全部code->Quotation映射
        Map<String,Quotation> aCodeQuoMap = quotationList.stream().collect(Collectors.toMap(Quotation::getCode,Function.identity(),(o,n) ->n));
        //只监控价值股，其他的过滤掉
        List<Quotation> mQuoList = quotationList.stream().filter(x -> MonitorTask.codeMonitorMap.containsKey(x.getCode())).collect(Collectors.toList());
        //查询当前涨停跌停列表
        LimitUpDown limitUpDown = stockQueryService.selectLimitUpDownByDate(strDate);

        //添加历史行情数据
        if (MonitorTask.crewCount < 400) {
            addHisQuoMap(level1HisQuoMap, mQuoList, 1);
        }
        addHisQuoMap(level2HisQuoMap,mQuoList,2);
        addHisQuoMap(level3HisQuoMap,mQuoList,5);
        addHisQuoMap(level4HisQuoMap,mQuoList,15);
        addHisQuoMap(level5HisQuoMap,mQuoList,30);

        //获取每个行业涨幅前N的股票列表
        if (MonitorTask.idpCodesMap.size() > 0) {
            //必须每次都重新获取
            int topN = 2;
            idpTopNCodesMap = new HashMap<>();
            MonitorTask.idpCodesMap.forEach((k,v) -> {
                if (v != null && v.size() > 0) {
                    idpTopNCodesMap.put(k,quotationList.stream().filter(x -> v.contains(x.getCode())).sorted(Comparator.comparing(Quotation::getOffsetRate).reversed()).limit(topN).map(Quotation::getCode).collect(Collectors.toList()));
                }
            });
        }

        if (isInit) {
            log.info("=======================推送数据初始化开始=======================");
            initMonitorData(mQuoList, limitUpDown);
            //每天重新初始化历史行情数据
            level1UpCodeList = new ArrayList<>();
            level2UpCodeList = new ArrayList<>();
            level3UpCodeList = new ArrayList<>();
            level4UpCodeList = new ArrayList<>();
            level5UpCodeList = new ArrayList<>();
            //每天重新初始化股票历史交易数据
            level1HisQuoMap = new HashMap<>();
            level2HisQuoMap = new HashMap<>();
            level3HisQuoMap = new HashMap<>();
            level4HisQuoMap = new HashMap<>();
            level5HisQuoMap = new HashMap<>();
            //每天初始化即将涨停列表
            wUpLimitCodeMap = new HashMap<>();
            //每天初始化拉升列表
            codeStartCUpMap = new HashMap<>();
            //每天初始化最大回撤行情列表
            last10ORateList = new ArrayList<>();
            //每天初始化进攻型股票映射列表
            hAttackCodeMap = new HashMap<>();
            //每天初始化股票连续拉升次数
            cUpCodeCountMap = new HashMap<>();
            //每天初始化交易量大的股票列表
            largeVolCodeMap = new HashMap<>();

            //根据竞价过滤掉股票池中高开和低开太多的股票-5%-5%
            mQuoList.forEach(q -> {
                BigDecimal initOffSetRate = (q.getOpen().subtract(q.getInit())).multiply(BigDecimal.valueOf(100)).divide(q.getInit(),4, RoundingMode.HALF_UP);
                if (initOffSetRate.doubleValue() < -3 || initOffSetRate.doubleValue() > 5) {
                    MonitorTask.codeMonitorMap.remove(q.getCode());
                }
            });
            //每天提取竞价弱转强列表
            if (MonitorTask.lastDayWTSSourceCodeMap.size() > 0) {
                MonitorTask.lastDayWTSSourceCodeMap.forEach((code,type) -> {
                    if (aCodeQuoMap.containsKey(code)) {
                        BigDecimal initOffSetRate = (aCodeQuoMap.get(code).getOpen().subtract(aCodeQuoMap.get(code).getInit())).multiply(BigDecimal.valueOf(100)).divide(aCodeQuoMap.get(code).getInit(),4, RoundingMode.HALF_UP);
                        if (type == 0) {//巨量反包弱转强
                            if (initOffSetRate.doubleValue() > -2) {
                                weakToStrongCodeList.add(code);
                            }
                        }
                        if (type == 1) {//炸板高开弱转强
                            if (initOffSetRate.doubleValue() > 0) {
                                weakToStrongCodeList.add(code);
                            }
                        }
                    }
                });
            }

            log.info("=======================推送数据初始化结束=======================");
        } else {
            //提取本次快速上涨和快速下跌的数据，并发送钉钉消息
            //先获取两次的交集，再比对，以本次抓取的最新数据为准
            //创建映射，并比对结果
            Map<String, Quotation> codeQuoMap = mQuoList.stream().collect(Collectors.toMap(Quotation::getCode, Function.identity()));
            Map<String, Quotation> lastCodeQuoMap = lastQuoList.stream().collect(Collectors.toMap(Quotation::getCode, Function.identity()));

            //过滤出code列表，再取交集
            List<String> curCodeList = mQuoList.stream().map(Quotation::getCode).collect(Collectors.toList());
            List<String> lastCodeList = lastQuoList.stream().map(Quotation::getCode).collect(Collectors.toList());
            //取交集
            curCodeList.retainAll(lastCodeList);
            //映射出交集
            List<Quotation> unionQuoList = curCodeList.stream().map(codeQuoMap::get).collect(Collectors.toList());
            Set<String> upCodeSet = new HashSet<>();
            for (Quotation q : unionQuoList) {
                //统计上涨列表
                if (q.getOffsetRate().doubleValue() >= lastCodeQuoMap.get(q.getCode()).getOffsetRate().doubleValue()) {
                    upCodeSet.add(q.getCode());
                }
            }

            //统计进攻型股票列表
            List<String> attackCodeList = getAttackCodeList(mQuoList);

            //将本次上涨的数据添加进队列
            Set<String> cUpCodeSet = new HashSet<>();
            if (MonitorTask.crewCount < 400) {
                addCUpCodeSet(level1HisQuoMap,level1UpCodeList,upCodeSet,cUpCodeSet,1);
            }
            addCUpCodeSet(level2HisQuoMap,level2UpCodeList,upCodeSet,cUpCodeSet,2);
            addCUpCodeSet(level3HisQuoMap,level3UpCodeList,upCodeSet,cUpCodeSet,5);
            addCUpCodeSet(level4HisQuoMap,level4UpCodeList,upCodeSet,cUpCodeSet,15);
            addCUpCodeSet(level5HisQuoMap,level5UpCodeList,upCodeSet,cUpCodeSet,30);

            //构造最终推送字符串
            StringBuilder sendBuilder = new StringBuilder();

            sendBuilder.append(String.format("### 第%s次抓取行情数据:\r\n",MonitorTask.crewCount));
            //统计竞价弱转强列表
            StringBuilder weekToStrongBuilder = new StringBuilder();
            if (weakToStrongCodeList.size() > 0) {
                sendBuilder.append("===============华丽的分隔符===============\r\n");
                sendBuilder.append("### 竞价弱转强列表:\r\n");
                weekToStrongBuilder.append("### 竞价弱转强列表:\r\n");
                for (String code : weakToStrongCodeList) {
                    if (codePropMap.containsKey(code)) {
                        sendBuilder.append(String.format("- %s -> %s\r\n", code, codePropMap.get(code).getName()));
                        weekToStrongBuilder.append(String.format("- %s -> %s\r\n", code, codePropMap.get(code).getName()));
                    }
                }

                //每天只用推送一次即可
                weakToStrongCodeList = new ArrayList<>();
            }

            //早上开盘交易量大的提取出来,9:40之前交易量大的提取出来
            StringBuilder largeVolBuilder = new StringBuilder();
            if (MonitorTask.crewCount < 400) {
                List<String> newLargeVolCodeList = mQuoList.stream().filter(x -> x.getVolumeAmt().doubleValue() > 100000000).map(Quotation::getCode).collect(Collectors.toList());
                sendBuilder.append("===============华丽的分隔符===============\r\n");
                if (newLargeVolCodeList.size() > 0) {
                    List<String> realLargeVolCodeList = new ArrayList<>();
                    for (String code : newLargeVolCodeList) {
                        if (!largeVolCodeMap.containsKey(code)) {
                            largeVolCodeMap.put(code,1);
                            realLargeVolCodeList.add(code);
                        }
                    }

                    if (realLargeVolCodeList.size() > 0) {
                        sendBuilder.append("### 交易活跃列表:\r\n");
                        largeVolBuilder.append("### 交易活跃列表:\r\n");
                        for (String code : realLargeVolCodeList) {
                            if (codePropMap.containsKey(code)) {
                                sendBuilder.append(String.format("- %s -> %s\r\n", code, codePropMap.get(code).getName()));
                                largeVolBuilder.append(String.format("- %s -> %s\r\n", code, codePropMap.get(code).getName()));
                            }
                        }
                    }
                }
            }

            //重点关注，连续上涨列表
            StringBuilder cUpBuilder = new StringBuilder();
            if (cUpCodeSet.size() > 0) {
                //过滤推送次数后的结果再推送，减少推送次数
                sendBuilder.append("### ****************【连续拉升，重点关注】****************\r\n");
                sendBuilder.append("### 连续拉升列表:\r\n");
                cUpCodeSet.forEach(x -> {
                    String plate = "";
                    DictPropVo dictPropVo = null;
                    if (codePropMap.containsKey(x)) {
                        dictPropVo = codePropMap.get(x);
                        plate = dictPropVo.getIndustry() + " -> " + dictPropVo.getPlate();
                    }
                    sendBuilder.append(String.format("- %s -> %s -> 概念：【%s】\r\n", x, codePropMap.get(x).getName(), plate));
                });
                sendBuilder.append("****************【连续拉升，重点关注】****************\r\n");

                //减少推送次数，重新组装，避免每轮都推送
                List<String> cUpCodeList = new ArrayList<>();
                //根据连续拉升次数过滤
                if (cUpCodeCountMap.size() > 0) {
                    //先过滤出从本轮开始不再连续的
                    Set<String> unCUpCodeList = SpiderUtil.deepCopyByProtobuff(new HashSet<>(cUpCodeCountMap.keySet()));
                    unCUpCodeList.removeAll(cUpCodeSet);
                    if (unCUpCodeList.size() > 0) {
                        //5次以上没连续的说明连续拉升断档了，直接移除
                        unCUpCodeList.forEach(x -> {
                            if (MonitorTask.crewCount - cUpCodeCountMap.get(x).get(cUpCodeCountMap.get(x).size() - 1) > 5) {
                                cUpCodeCountMap.remove(x);
                                log.info("=====================股票{}不再连续上涨，从连续池中移除=====================",x);
                            }
                        });
                    }
                    //剩下的还在连续的，记录每次连续的期数
                    cUpCodeSet.forEach(x -> {
                        if (cUpCodeCountMap.containsKey(x)) {
                            List<Integer> cCycleNumList = cUpCodeCountMap.get(x);
                            cCycleNumList.add(MonitorTask.crewCount);
                            cUpCodeCountMap.put(x,cCycleNumList);
                            //区分进攻型拉升和普通型拉升，普通型拉升必须连续拉N次才算
                            if (hAttackCodeMap.containsKey(x)) {
                                //此处需要减少推送次数,连续2次一下每次都推送，超过2次每5次推送1次
                                if (cCycleNumList.size() <= 2 || cCycleNumList.size() % 20 == 0) {
                                    cUpCodeList.add(x);
                                }
                            } else {
                                if (cCycleNumList.size() % 5 == 0) {
                                    cUpCodeList.add(x);
                                }
                            }
                        } else {
                            cUpCodeCountMap.put(x,new LinkedList<Integer>(){{
                                add(MonitorTask.crewCount);
                            }});
                            cUpCodeList.add(x);
                        }
                    });
                } else {
                    cUpCodeSet.forEach(x -> {
                        cUpCodeCountMap.put(x,new LinkedList<Integer>(){{
                            add(MonitorTask.crewCount);
                        }});
                    });
                    cUpCodeList.addAll(cUpCodeSet);
                }
                //此处根据板块和概念进一步过滤，只推送板块概念涨幅前5的股票，后排股票不容涨停故过滤掉
                List<String> filterCUpCodeList = filterCUpCodeListByIdp(cUpCodeList);

                //过滤推送次数后的结果再推送，减少推送次数
                if (filterCUpCodeList.size() > 0) {
                    cUpBuilder.append("### 连续拉升列表:\r\n");
                    filterCUpCodeList.forEach(x -> {
                        //此处区分，进攻型拉升和普通型拉升
                        if (hAttackCodeMap.containsKey(x)) {
                            cUpBuilder.append(String.format("- %s -> %s\\[市值%.2fE\\](进攻型)\r\n", x,  codePropMap.get(x).getName(),codePropMap.get(x).getCirMarketValue()));
                        } else {
                            cUpBuilder.append(String.format("- %s -> %s\\[市值%.2fE\\](直线型)\r\n", x, codePropMap.get(x).getName(),codePropMap.get(x).getCirMarketValue()));
                        }
                    });
                }
            }

            //即将涨停列表
            List<String> newWUpLimitCodeList = quotationList.stream().filter(x -> x.getOffsetRate().doubleValue() > 8).map(Quotation::getCode).collect(Collectors.toList());
            sendBuilder.append("===============华丽的分隔符===============\r\n");
            StringBuilder wUpLimitBuilder = new StringBuilder();
            if (newWUpLimitCodeList.size() > 0) {
                List<String> realWUpLimitCodeList = new ArrayList<>();
                for (String code : newWUpLimitCodeList) {
                    //加入待涨停列表
                    if (wUpLimitCodeMap.containsKey(code)) {
                        wUpLimitCodeMap.put(code, wUpLimitCodeMap.get(code) + 1);
                    } else {
                        wUpLimitCodeMap.put(code,1);
                        realWUpLimitCodeList.add(code);
                    }
                }

                //真正的需要推送的涨停列表，过滤掉第一次抓取
                if (realWUpLimitCodeList.size() > 0) {
                    sendBuilder.append("### 即将涨停列表:\r\n");
                    wUpLimitBuilder.append("### 即将涨停列表:\r\n");
                    for (String code : realWUpLimitCodeList) {
                        if (codePropMap.containsKey(code)) {
                            if (largeVolCodeMap.containsKey(code)) {
                                sendBuilder.append(String.format("- %s -> %s(开盘放量型)(%s)\r\n", code, codePropMap.get(code).getName(),MonitorTask.codeLimitUDPMap.get(code).getPDesc()));
                                wUpLimitBuilder.append(String.format("- %s -> %s(开盘放量型)(%s)\r\n", code, codePropMap.get(code).getName(),MonitorTask.codeLimitUDPMap.get(code).getPDesc()));
                            } else {
                                sendBuilder.append(String.format("- %s -> %s(普通型)(%s)\r\n", code, codePropMap.get(code).getName(),MonitorTask.codeLimitUDPMap.get(code).getPDesc()));
                                wUpLimitBuilder.append(String.format("- %s -> %s(普通型)(%s)\r\n", code, codePropMap.get(code).getName(),MonitorTask.codeLimitUDPMap.get(code).getPDesc()));
                            }
                        }
                    }
                }

                //去除快涨停又跌下去的股票
                if (wUpLimitCodeMap.size() > 0) {
                    wUpLimitCodeMap.entrySet().removeIf(entry -> !aCodeQuoMap.containsKey(entry.getKey()) || aCodeQuoMap.get(entry.getKey()).getOffsetRate().doubleValue() < 7);
                }
            }

            //进攻型股票列表
            StringBuilder attackBuilder = new StringBuilder();
            if (attackCodeList.size() > 0) {
                //重新过滤防止每次都推送，10次之前，每20s推送一次，10次以后每分钟推1次
                List<String> rAttackCodeList = new ArrayList<>();
                for (String x : attackCodeList) {
                    //成交量过滤，低于8000w的直接过滤掉
                    if (codeQuoMap.get(x).getVolumeAmt().doubleValue() < 80000000) {
                        continue;
                    }
                    if (hAttackCodeMap.get(x) <= 2 || (hAttackCodeMap.get(x) > 2 && hAttackCodeMap.get(x) <= 30 && hAttackCodeMap.get(x) % 10 == 0)) {
                        rAttackCodeList.add(x);
                    }
                    if (hAttackCodeMap.get(x) > 30 && hAttackCodeMap.get(x) % 30 == 0) {
                        rAttackCodeList.add(x);
                    }
                }
                //根据重新过滤后的结果推送
                if (rAttackCodeList.size() > 0) {
                    attackBuilder.append("### 进攻型列表:\r\n");
                    sendBuilder.append("### 进攻型列表:\r\n");
                    for (String x : rAttackCodeList) {
                        attackBuilder.append(String.format("- %s -> %s\r\n", x, codePropMap.get(x).getName()));
                        sendBuilder.append(String.format("- %s -> %s\r\n", x, codePropMap.get(x).getName()));
                    }
                }
            }

            log.info("===============推送消息开始===============\r\n");
            log.info(sendBuilder.toString());
            ExecutorService pool = Executors.newFixedThreadPool(1);
            pool.execute(() -> {
                String ddWeekToStrongMsg = weekToStrongBuilder.toString();
                String ddContinueUpMsg = cUpBuilder.toString();
                String ddWUpLimitMsg = wUpLimitBuilder.toString();
                String ddAttackMsg = attackBuilder.toString();
                String largeVolMsg = largeVolBuilder.toString();
                StringBuilder fBuilder = new StringBuilder();
                if (StringUtils.isNotEmpty(ddWeekToStrongMsg)) {
                    fBuilder.append(ddWeekToStrongMsg).append("\r\n");
                }
                if (StringUtils.isNotEmpty(ddContinueUpMsg)) {
                    fBuilder.append(ddContinueUpMsg).append("\r\n");
                }
//                if (StringUtils.isNotEmpty(ddWUpLimitMsg)) {
//                    fBuilder.append(ddWUpLimitMsg).append("\r\n");
//                }
//                if (StringUtils.isNotEmpty(ddAttackMsg)) {
//                    fBuilder.append(ddAttackMsg).append("\r\n");
//                }
                if (StringUtils.isNotEmpty(largeVolMsg)) {
                    fBuilder.append(largeVolMsg);
                }
                //所有信息合并成一条，一次性发送减少发送次数
                String ddFinalMsg = fBuilder.toString();
                if (StringUtils.isNotEmpty(ddFinalMsg)) {
                    stockService.sendDingDingGroupMsg(ddFinalMsg);
                }
            });
            pool.shutdown();

            log.info("===============推送消息结束===============");
            //将当前最新的数据赋值给监控数据
            initMonitorData(mQuoList, limitUpDown);
        }
    }

    /**
     * 初始化监控数据
     * @param mQuoList
     * @param limitUpDown
     */
    private void initMonitorData(List<Quotation> mQuoList, LimitUpDown limitUpDown) {
        //初始化行情数据
        lastQuoList = mQuoList;

        //初始化涨停跌停列表
        lastUpLimitCodeSet = new HashSet<>();
        lastDownLimitCodeSet = new HashSet<>();
        if (limitUpDown != null) {
            if (StringUtils.isNotEmpty(limitUpDown.getUpList())) {
                lastUpLimitCodeSet.addAll(Arrays.asList(limitUpDown.getUpList().split(",")));
            }
            if (StringUtils.isNotEmpty(limitUpDown.getDownList())) {
                lastDownLimitCodeSet.addAll(Arrays.asList(limitUpDown.getDownList().split(",")));
            }
        }
    }

    /**
     * 保存历史行情数据
     * @param hisQuoMap
     * @param monitorQuoList
     * @param interval
     */
    private void addHisQuoMap(Map<String,List<Quotation>> hisQuoMap, List<Quotation> monitorQuoList, int interval) {
        if (hisQuoMap.isEmpty()) {
            monitorQuoList.forEach(q -> {
                hisQuoMap.put(q.getCode(),new LinkedList<Quotation>(){{
                    add(q);
                }});
            });
        } else {
            monitorQuoList.forEach(q -> {
                List<Quotation> curQuoList = null;
                if (hisQuoMap.containsKey(q.getCode())) {
                    curQuoList = hisQuoMap.get(q.getCode());
                    //固定维护最近5期的行情数据
                    if (curQuoList.size() == MONITOR_CYCLE_NUM * interval) {
                        curQuoList.remove(0);
                    }
                    curQuoList.add(q);
                } else {
                    curQuoList = new LinkedList<Quotation>(){{
                        add(q);
                    }};
                }
                hisQuoMap.put(q.getCode(),curQuoList);
            });
        }
    }

    /**
     * 添加连续上涨股票编码列表
     * @param hisQuoMap
     * @param upCodeList
     * @param upCodeSet
     * @param cUpCodeSet
     * @param interval
     */
    private void addCUpCodeSet(Map<String,List<Quotation>> hisQuoMap, List<Set<String>> upCodeList, Set<String> upCodeSet, Set<String> cUpCodeSet, int interval) {
        upCodeList.add(upCodeSet);
        //计算需要维护的历史行情轮数
        int upCodeListCount = MONITOR_CYCLE_NUM * interval;
        if (upCodeList.size() == upCodeListCount) {
            //此算法为简化版本，需要想通监控逻辑，为保证无缝监控，所以间隔为interval的监控需要interval个，但每次只会有一个有效，因为其他的会等待interval轮，只会有一个正好转到
            List<Set<String>> mUpCodeList = new ArrayList<>();
            for (int i = 0 ; i < upCodeList.size() ; i += interval) {
                mUpCodeList.add(upCodeList.get(i));
            }

            //提取此档的所有股票的最近MONITOR_CYCLE_NUM期行情数据，按照interval提取
            Map<String,List<Quotation>> mHisQuoMap = new HashMap<>();
            //重新构造
            Iterator<Map.Entry<String,List<Quotation>>> ite = hisQuoMap.entrySet().iterator();
            Map.Entry<String,List<Quotation>> entry = null;
            List<Quotation> orderedList = null;
            while (ite.hasNext()) {
                entry = ite.next();
                orderedList = new LinkedList<>();
                for (int i = 0 ; i < entry.getValue().size() ; i += interval) {
                    orderedList.add(entry.getValue().get(i));
                }
                mHisQuoMap.put(entry.getKey(),orderedList);
            }

            //随着每轮抓取迭代，淘汰第一个，再按固定interval装配，即可实现无缝监控，因为interval轮监控，每轮更新总是会有一个达到监控的cycle，本组的其他周期会不满足条件
            //分组聚合计算出每个股票这MONITOR_CYCLE_NUM的上涨次数
            Map<String, Long> codeUpCountMap = mUpCodeList.stream().flatMap(Set::stream).collect(Collectors.groupingBy(x -> x,Collectors.counting()));
            //查看5次中同一个股票出现3次上涨，则视为连续上涨，加入推送列表，重点关注
            //过滤出符合条件的，加入推送列表
            for (String code : codeUpCountMap.keySet()) {
                int len = mHisQuoMap.get(code).size();
                //上涨次数达到阀值，且股价必须上升2%，当前股价 > 5%
                if (codeUpCountMap.get(code) >= MIN_UP_COUNT && (len > 0 && mHisQuoMap.get(code).get(len - 1).getOffsetRate().doubleValue() - mHisQuoMap.get(code).get(0).getOffsetRate().doubleValue() >= MIN_UP_RATE)) {
                    //添加拉升起始位过滤
                    if (interval < 10 && !codeStartCUpMap.containsKey(code)) {
                        codeStartCUpMap.put(code,mHisQuoMap.get(code).get(0));
                    }
                    //如果低粒度拉升起始位置太低，且成交量很低，直接过滤掉
                    if (codeStartCUpMap.containsKey(code) && codeStartCUpMap.get(code).getOffsetRate().doubleValue() < 2 && mHisQuoMap.get(code).get(len - 1).getVolumeAmt().doubleValue() < MIN_VOLUME_AMT) {
                        continue;
                    }
                    if(mHisQuoMap.get(code).get(len - 1).getOffsetRate().doubleValue() >= MIN_OFFSET_RATE && mHisQuoMap.get(code).get(len - 1).getVolumeAmt().doubleValue() > MIN_VOLUME_AMT) {
                        log.info("===============捕捉到code => {}连续拉升，interval => {}===============", code, interval);
                        cUpCodeSet.add(code);
                    }
                }
            }
            //达到最大数后，移除最后一个
            if (upCodeList.size() == upCodeListCount) {
                //移除最前面一次加入的数据
                upCodeList.remove(0);
            }
        }
    }

    /**
     * 统计进攻型股票
     * 即10分钟内涨幅 > 4,最大回撤 < 2
     * @param mQuoList
     * @return
     */
    private List<String> getAttackCodeList(List<Quotation> mQuoList) {
        //10:00以前监控拉升进攻型，10点以后不再监控，10点时拉升次数为900，这里设置为1000
//        if (MonitorTask.crewCount > 1000) {
//            hAttackCodeMap = new HashMap<>();
//            last10ORateList = new ArrayList<>();
//            return new ArrayList<>();
//        }
        List<String> resultList = new ArrayList<>();
        //先添加每期涨幅，按照code统计
        if (last10ORateList.isEmpty()) {
            mQuoList.forEach(q -> {
                last10ORateList.add(new QuoORateVo(q.getCode(),new LinkedList<Double>(){{
                    add(q.getOffsetRate().doubleValue());
                }}));
            });
        } else {
            //映射为map
            Map<String,QuoORateVo> codeQuoORateVoMap = last10ORateList.stream().collect(Collectors.toMap(QuoORateVo::getCode,Function.identity(),(o,n) ->n));
            mQuoList.forEach(q -> {
                if (codeQuoORateVoMap.containsKey(q.getCode())) {
                    QuoORateVo oRateVo = codeQuoORateVoMap.get(q.getCode());
                    //固定维护最近5期的行情数据
                    if (oRateVo.getORateList().size() == MONITOR_DRAWDOWN_CYCLE_NUM) {
                        oRateVo.getORateList().remove(0);
                    }
                    oRateVo.getORateList().add(q.getOffsetRate().doubleValue());
                } else {
                    last10ORateList.add(new QuoORateVo(q.getCode(),new LinkedList<Double>(){{
                        add(q.getOffsetRate().doubleValue());
                    }}));
                }
            });

            //计算最大回撤率
            //最低抓取100次才统计
            if (MonitorTask.crewCount > 100) {
                List<List<QuoORateVo>> twoDeepList = SpiderUtil.partitionList(last10ORateList,GlobalConstant.MAX_THREAD_COUNT);
                ExecutorService threadPool = Executors.newFixedThreadPool(twoDeepList.size());
                if (twoDeepList.size() > 0) {
                    CountDownLatch latch = new CountDownLatch(twoDeepList.size());
                    Set<List<String>> sCodeSet = new HashSet<>();
                    twoDeepList.forEach(innerList -> {
                        threadPool.execute(() -> {
                            try {
                                List<String> sCodeList = new ArrayList<>();
                                for (QuoORateVo oRateVo : innerList) {
                                    //第一个不是最小值的直接过滤掉,即先下跌再涨的过滤掉
                                    if (oRateVo.getORateList().get(0) > Collections.min(oRateVo.getORateList())) {
                                        continue;
                                    }
                                    //涨幅需要大于2
                                    if (oRateVo.getORateList().get(oRateVo.getORateList().size() - 1) - oRateVo.getORateList().get(0) < 3) {
                                        continue;
                                    }
                                    //回撤0.5-2之间
                                    double maxDrawdown = SpiderUtil.getMaxDrawdown(oRateVo.getORateList());
                                    if (maxDrawdown > 0.15 && maxDrawdown < 2) {
                                        sCodeList.add(oRateVo.getCode());
                                    }
                                }
                                sCodeSet.add(sCodeList);
                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                latch.countDown();
                            }
                        });
                    });
                    try {
                        latch.await();
                        threadPool.shutdown();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    resultList.addAll(sCodeSet.stream().flatMap(List::stream).collect(Collectors.toSet()));
                    //加入进攻型股票列表，统计捕获次数
                    resultList.forEach(code -> {
                        if (!hAttackCodeMap.containsKey(code)) {
                            hAttackCodeMap.put(code,1);
                        } else {
                            hAttackCodeMap.put(code,hAttackCodeMap.get(code) + 1);
                        }
                    });
                }
            }
        }
        return resultList;
    }

    /**
     * 根据板块前N过滤连续上涨的股票
     * @param cUpCodeList
     * @return
     */
    private List<String> filterCUpCodeListByIdp(List<String> cUpCodeList) {
        List<String> newCUpCodeList = new ArrayList<>();
        if (cUpCodeList.size() > 0) {
            cUpCodeList.forEach(code -> {
                if (MonitorTask.codeIdpsMap.containsKey(code)) {
                    String []idpArr = MonitorTask.codeIdpsMap.get(code);
                    Set<String> codeSet = new HashSet<>();
                    if (idpArr.length > 0) {
                        for (String idp : idpArr) {
                            if (idpTopNCodesMap.containsKey(idp)) {
                                codeSet.addAll(idpTopNCodesMap.get(idp));
                            }
                        }
                    }
                    if (codeSet.contains(code)) {
                        newCUpCodeList.add(code);
                    }
                }
            });
        }
        return newCUpCodeList;
    }
}
