/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.stock.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.td.common.common.GlobalConstant;
import com.td.common.common.MybatisBatchHandler;
import com.td.common.common.ResponseEntity;
import com.td.common.mapper.AvgPriceMapper;
import com.td.common.mapper.CuMonitorMapper;
import com.td.common.mapper.DictMapper;
import com.td.common.mapper.DictPropMapper;
import com.td.common.mapper.HisQuotationMapper;
import com.td.common.mapper.LimitUpDownMapper;
import com.td.common.mapper.ProxyIpMapper;
import com.td.common.mapper.QuoIdpMapper;
import com.td.common.mapper.QuotationMapper;
import com.td.common.mapper.RecommandMapper;
import com.td.common.mapper.SysDictMapper;
import com.td.common.mapper.UpDownMapper;
import com.td.common.model.AvgPrice;
import com.td.common.model.CuMonitor;
import com.td.common.model.Dict;
import com.td.common.model.DictProp;
import com.td.common.model.HisQuotation;
import com.td.common.model.LimitUpDown;
import com.td.common.model.ProxyIp;
import com.td.common.model.QuoIdp;
import com.td.common.model.Quotation;
import com.td.common.model.Recommand;
import com.td.common.model.SysDict;
import com.td.common.model.UpDown;
import com.td.common.util.HttpClientUtil;
import com.td.common.util.SnowflakeGenIdUtil;
import com.td.common.util.SpiderUtil;
import com.td.common.vo.IdpCodesVo;
import com.td.common.vo.LastNPriceVo;
import com.td.common.vo.RelativePositionVo;
import com.td.common.vo.UpLimitCountVo;
import com.td.stock.config.DingdingConfig;
import com.td.stock.config.StockConfig;
import com.td.stock.config.XiongmaoConfig;
import com.td.stock.task.MonitorTask;
import com.td.stock.util.MimvpOCRUtil;
import com.td.stock.vo.CodeLimitUDPVo;
import com.td.stock.vo.CodeNameVo;
import com.td.stock.vo.AvgPriceVo;
import com.td.stock.vo.LastNUpLimitVo;
import com.td.stock.vo.VolumeVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @auther lotey
 * @date 2019/7/26 22:27
 * @desc 股票业务类
 */
@Service
@Slf4j
public class StockService {

    @Autowired
    private HttpClientUtil clientUtil;
    @Autowired
    private SnowflakeGenIdUtil genIdUtil;
    @Autowired
    private DictMapper dictMapper;
    @Autowired
    private QuotationMapper quotationMapper;
    @Autowired
    private LimitUpDownMapper limitUpDownMapper;
    @Autowired
    private UpDownMapper upDownMapper;
    @Autowired
    private AvgPriceMapper avgPriceMapper;
    @Autowired
    private SysDictMapper sysDictMapper;
    @Autowired
    private DictPropMapper dictPropMapper;
    @Autowired
    private ProxyIpMapper proxyIpMapper;
    @Autowired
    private RecommandMapper recommandMapper;
    @Autowired
    private CuMonitorMapper cuMonitorMapper;
    @Autowired
    private HisQuotationMapper hisQuotationMapper;
    @Autowired
    private QuoIdpMapper quoIdpMapper;
    @Autowired
    private MybatisBatchHandler mybatisBatchHandler;
    @Autowired
    private StockQueryService stockQueryService;
    @Autowired
    private StockConfig stockConfig;
    @Autowired
    private DingdingConfig dingdingConfig;
    @Autowired
    private ProxyTool proxyTool;
    @Autowired
    private XiongmaoConfig xiongmaoConfig;

    /**
     * 抓取股票代码，并入库
     */
    public void crewDictData() {
        long startTime = System.currentTimeMillis();
        log.info("==========================开始抓取股票代码数据，当前时间：{}==========================", SpiderUtil.getCurrentTimeStr());
        //单次批量抓取数量
        int batchSize = 50;
        //启动多线程抓取
        int threadCount = 3;
        ExecutorService threadPool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        log.info("============================本次共启动{}个线程,当前时间：{}============================",threadCount,SpiderUtil.getCurrentTimeStr());
        List<List<Dict>> twoDeepDictList = new ArrayList<>();
        //抓取上海的股票
        threadPool.execute(() -> {
            try {
                long shStartTime = System.currentTimeMillis();
                log.info("============================开始抓取上海股票数据，当前时间：{}============================",SpiderUtil.getCurrentTimeStr());
                //600000, 605999
                List<String> shCodeRangeList = IntStream.rangeClosed(600000,605999).mapToObj(x -> String.format("%s%s","sh",x)).collect(Collectors.toList());
                //调用google算法分配
                List<List<String>> groupCodeList =  Lists.partition(shCodeRangeList, batchSize);
                List<Dict> tmpDictList = new ArrayList<>();
                groupCodeList.forEach(codeList -> {
                    String url = String.format(GlobalConstant.SINA_STOCK_QUOTATION_URL, String.join(",",codeList));
                    ResponseEntity responseEntity = clientUtil.recursiveHttpRequest(GlobalConstant.HTTP_GET, 0, url, null, GlobalConstant.DEFAULTSINAHEADER,GlobalConstant.CHARASET_GBK);
                    if (responseEntity.getCode() == 200) {
                        //根据换行符分组，提取值
                        String[] dataArr = responseEntity.getContent().split("\n");
                        if (dataArr.length > 0) {
                            Dict dict = null;
                            String code = null;
                            String name = null;
                            String[] splitArr = null;
                            String data = null;
                            for (String rData : dataArr) {
                                //组装字典数据
                                splitArr = rData.split("=");
                                //无效的股票字典数据过滤掉
                                if (splitArr[1].length() > 10 && Double.parseDouble(splitArr[1].split(",")[1]) > 0) {
                                    code = splitArr[0].substring(splitArr[0].lastIndexOf("_") + 1);
                                    data = splitArr[1].replaceAll("\"", "").replaceAll(";", "");
                                    name = data.split(",")[0];
                                    dict = new Dict();
                                    dict.setCode(code);
                                    dict.setName(name);
                                    tmpDictList.add(dict);
                                }
                            }
                        }
                    }
                });
                twoDeepDictList.add(tmpDictList);
                long shEndTime = System.currentTimeMillis();
                log.info("============================抓取上海股票数据完成，当前时间：{}============================",SpiderUtil.getCurrentTimeStr());
                log.info("============================共耗时{}秒============================", (shEndTime - shStartTime) / 1000);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        //抓取深圳的股票
        threadPool.execute(() -> {
            try {
                long szStartTime = System.currentTimeMillis();
                log.info("============================开始抓取深圳股票数据，当前时间：{}============================",SpiderUtil.getCurrentTimeStr());
                //0, 40000
                List<String> shCodeRangeList = IntStream.rangeClosed(0,4000).mapToObj(x -> String.format("%s%s","sz",String.format("%6d", x).replace(" ", "0"))).collect(Collectors.toList());
                //调用google算法分配
                List<List<String>> groupCodeList =  Lists.partition(shCodeRangeList, batchSize);
                List<Dict> tmpDictList = new ArrayList<>();
                groupCodeList.forEach(codeList -> {
                    String url = String.format(GlobalConstant.SINA_STOCK_QUOTATION_URL, String.join(",",codeList));
                    ResponseEntity responseEntity = clientUtil.recursiveHttpRequest(GlobalConstant.HTTP_GET, 0, url, null, GlobalConstant.DEFAULTSINAHEADER,GlobalConstant.CHARASET_GBK);
                    if (responseEntity.getCode() == 200) {
                        //根据换行符分组，提取值
                        String[] dataArr = responseEntity.getContent().split("\n");
                        if (dataArr.length > 0) {
                            Dict dict = null;
                            String code = null;
                            String name = null;
                            String[] splitArr = null;
                            String data = null;
                            for (String rData : dataArr) {
                                //组装字典数据
                                splitArr = rData.split("=");
                                //无效的股票字典数据过滤掉
                                if (splitArr[1].length() > 10 && Double.parseDouble(splitArr[1].split(",")[1]) > 0) {
                                    code = splitArr[0].substring(splitArr[0].lastIndexOf("_") + 1);
                                    data = splitArr[1].replaceAll("\"", "").replaceAll(";", "");
                                    name = data.split(",")[0];
                                    dict = new Dict();
                                    dict.setCode(code);
                                    dict.setName(name);
                                    tmpDictList.add(dict);
                                }
                            }
                        }
                    }
                });
                twoDeepDictList.add(tmpDictList);
                long szEndTime = System.currentTimeMillis();
                log.info("============================抓取深圳股票数据完成，当前时间：{}============================",SpiderUtil.getCurrentTimeStr());
                log.info("============================共耗时{}秒============================", (szEndTime - szStartTime) / 1000);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        //抓取创业板股票
        threadPool.execute(() -> {
            try {
                long szStartTime = System.currentTimeMillis();
                log.info("============================开始抓取创业板股票数据，当前时间：{}============================",SpiderUtil.getCurrentTimeStr());
                //300001, 300999
                List<String> shCodeRangeList = IntStream.rangeClosed(300001,300999).mapToObj(x -> String.format("%s%s","sz",x)).collect(Collectors.toList());
                //调用google算法分配
                List<List<String>> groupCodeList =  Lists.partition(shCodeRangeList, batchSize);
                List<Dict> tmpDictList = new ArrayList<>();
                groupCodeList.forEach(codeList -> {
                    String url = String.format(GlobalConstant.SINA_STOCK_QUOTATION_URL, String.join(",",codeList));
                    ResponseEntity responseEntity = clientUtil.recursiveHttpRequest(GlobalConstant.HTTP_GET, 0, url, null, GlobalConstant.DEFAULTSINAHEADER,GlobalConstant.CHARASET_GBK);
                    if (responseEntity.getCode() == 200) {
                        //根据换行符分组，提取值
                        String[] dataArr = responseEntity.getContent().split("\n");
                        if (dataArr.length > 0) {
                            Dict dict = null;
                            String code = null;
                            String name = null;
                            String[] splitArr = null;
                            String data = null;
                            for (String rData : dataArr) {
                                //组装字典数据
                                splitArr = rData.split("=");
                                //无效的股票字典数据过滤掉
                                if (splitArr[1].length() > 10 && Double.parseDouble(splitArr[1].split(",")[1]) > 0) {
                                    code = splitArr[0].substring(splitArr[0].lastIndexOf("_") + 1);
                                    data = splitArr[1].replaceAll("\"", "").replaceAll(";", "");
                                    name = data.split(",")[0];
                                    dict = new Dict();
                                    dict.setCode(code);
                                    dict.setName(name);
                                    tmpDictList.add(dict);
                                }
                            }
                        }
                    }
                });
                twoDeepDictList.add(tmpDictList);
                long szEndTime = System.currentTimeMillis();
                log.info("============================抓取创业板股票数据完成，当前时间：{}============================",SpiderUtil.getCurrentTimeStr());
                log.info("============================共耗时{}秒============================", (szEndTime - szStartTime) / 1000);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

//        //抓取科创板股票
//        threadPool.execute(() -> {
//            try {
//                long szStartTime = System.currentTimeMillis();
//                log.info("============================开始抓取科创板股票数据，当前时间：{}============================",SpiderUtil.getCurrentTimeStr());
//                //688001, 689001
//                List<String> shCodeRangeList = IntStream.rangeClosed(688001,689001).mapToObj(x -> String.format("%s%s","sh",x)).collect(Collectors.toList());
//                //调用google算法分配
//                List<List<String>> groupCodeList =  Lists.partition(shCodeRangeList, batchSize);
//                List<Dict> tmpDictList = new ArrayList<>();
//                groupCodeList.forEach(codeList -> {
//                    String url = String.format(GlobalConstant.SINA_STOCK_QUOTATION_URL, String.join(",",codeList));
//                    ResponseEntity responseEntity = clientUtil.recursiveHttpRequest(GlobalConstant.HTTP_GET, 0, url, null, GlobalConstant.DEFAULTSINAHEADER,GlobalConstant.CHARASET_GBK);
//                    if (responseEntity.getCode() == 200) {
//                        //根据换行符分组，提取值
//                        String[] dataArr = responseEntity.getContent().split("\n");
//                        if (dataArr.length > 0) {
//                            Dict dict = null;
//                            String code = null;
//                            String name = null;
//                            String[] splitArr = null;
//                            String data = null;
//                            for (String rData : dataArr) {
//                                //组装字典数据
//                                splitArr = rData.split("=");
//                                //无效的股票字典数据过滤掉
//                                if (splitArr[1].length() > 10 && Double.parseDouble(splitArr[1].split(",")[1]) > 0) {
//                                    code = splitArr[0].substring(splitArr[0].lastIndexOf("_") + 1);
//                                    data = splitArr[1].replaceAll("\"", "").replaceAll(";", "");
//                                    name = data.split(",")[0];
//                                    dict = new Dict();
//                                    dict.setCode(code);
//                                    dict.setName(name);
//                                    tmpDictList.add(dict);
//                                }
//                            }
//                        }
//                    }
//                });
//                twoDeepDictList.add(tmpDictList);
//                long szEndTime = System.currentTimeMillis();
//                log.info("============================抓取科创板股票数据完成，当前时间：{}============================",SpiderUtil.getCurrentTimeStr());
//                log.info("============================共耗时{}秒============================", (szEndTime - szStartTime) / 1000);
//            } catch (Exception e) {
//                e.printStackTrace();
//            } finally {
//                latch.countDown();
//            }
//        });

        //等待全部子线程完成，再统一获取结果
        try {
            latch.await();
            threadPool.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long cEndTime = System.currentTimeMillis();
        log.info("==========================抓取股票字典数据完成，当前时间：{}==========================", SpiderUtil.getCurrentTimeStr());
        log.info("==========================共耗时{}秒==========================", (cEndTime - startTime) / 1000);

        //将最终结果拉平变一纬，直接进行DB保存操作
        List<Dict> dictList = twoDeepDictList.stream().flatMap(List::stream).collect(Collectors.toList());
        //过滤掉ST和退市的股票
        dictList = dictList.stream().filter(x -> !x.getName().endsWith("退") && !x.getName().toUpperCase().contains("ST")).collect(Collectors.toList());

        long dbStartTime = System.currentTimeMillis();
        log.info("==========================开始入库，当前时间：{}==========================", SpiderUtil.getCurrentTimeStr());
        //清空全部股票数据
        dictMapper.deleteByType(GlobalConstant.DICT_TYPE_STOCK);
        //批量更新股票行情数据
        dictList.forEach(x -> {
            x.setId(genIdUtil.nextId());
            x.setType(GlobalConstant.DICT_TYPE_STOCK);
        });
        mybatisBatchHandler.batchInsertOrUpdate(dictList,DictMapper.class,GlobalConstant.BATCH_MODDEL_INSERT);
        long dbEndTime = System.currentTimeMillis();
        log.info("==========================入库完成，当前时间：{}==========================", SpiderUtil.getCurrentTimeStr());
        log.info("==========================共耗时{}秒==========================", (dbEndTime - dbStartTime) / 1000);

        long endTime = System.currentTimeMillis();
        log.info("==========================股票字典数据入库完成，当前时间：{}==========================", SpiderUtil.getCurrentTimeStr());
        log.info("==========================共耗时{}秒==========================", (endTime - startTime) / 1000);
    }

    /**
     * 抓取股票代码，并入库
     * @param date
     */
    public void updateCirMarketValue(LocalDate date) {
        long startTime = System.currentTimeMillis();
        log.info("==========================开始抓取股票市值数据，当前时间：{}==========================", SpiderUtil.getCurrentTimeStr());
//        if (SpiderUtil.isWeekendOfToday(date)) {
//            log.error("==========================周末时间不开盘，忽略本次任务==========================");
//            return;
//        }
        //查询当前股票列表
        List<Dict> dbDictList = dictMapper.selectListByType(GlobalConstant.DICT_TYPE_STOCK);
        Map<String,Dict> codeDictMap = dbDictList.stream().collect(Collectors.toMap(Dict::getCode,Function.identity()));
        //查询当前股票行情信息
        DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<Quotation> dbQuotationList = quotationMapper.selectListByDate(date.format(ymdFormatter));
        Map<String,Double> codePriceMap = dbQuotationList.stream().collect(Collectors.toMap(Quotation::getCode, x -> x.getCurrent().doubleValue()));
        //单次批量抓取数量
        int batchSize = 50;
        //根据线程数分配
        List<List<Dict>> twoDeepList = SpiderUtil.partitionList(dbDictList,GlobalConstant.MAX_THREAD_COUNT);
        log.info("============================本次共启动{}个线程，抓取{}个股票数据,当前时间：{}============================",twoDeepList.size(),dbDictList.size(),SpiderUtil.getCurrentTimeStr());
        if (twoDeepList.size() > 0) {
            ExecutorService threadPool = Executors.newFixedThreadPool(GlobalConstant.MAX_THREAD_COUNT);
            CountDownLatch latch = new CountDownLatch(twoDeepList.size());
            List<List<Dict>> dictResultList = new ArrayList<>();
            twoDeepList.forEach(innerList -> {
                threadPool.execute(() -> {
                    //存储计算好市值的dict列表
                    List<Dict> curDictList = new ArrayList<>();
                    List<List<Dict>> batchDictList = Lists.partition(innerList,batchSize);
                    batchDictList.forEach(curList -> {
                        StringBuffer codeBuf = new StringBuffer();
                        StringBuffer codeIBuf = new StringBuffer();
                        curList.forEach(dict -> {
                            codeBuf.append(dict.getCode()).append(",");
                            codeIBuf.append(dict.getCode()).append("_i").append(",");
                        });
                        String codeStr = codeBuf.toString().substring(0,codeBuf.toString().lastIndexOf(","));
                        String codeIStr = codeIBuf.toString().substring(0,codeIBuf.toString().lastIndexOf(","));
                        String url = String.format(GlobalConstant.SINA_STOCK_I_URL, codeStr,codeIStr);
                        ResponseEntity responseEntity = clientUtil.recursiveHttpRequest(GlobalConstant.HTTP_GET, 0, url, null, GlobalConstant.DEFAULTSINAHEADER,GlobalConstant.CHARASET_GBK);
                        if (responseEntity.getCode() == 200) {
                            //根据换行符分组，提取值
                            String[] dataArr = responseEntity.getContent().split("\n");
                            if (dataArr.length > 0) {
                                String []itemArr = null;
                                Dict curDict = null;
                                String []infArr = null;
                                BigDecimal mValue = null;
                                for (String dictI : dataArr) {
                                    itemArr = dictI.split("=")[0].split("_");
                                    if (codeDictMap.containsKey(itemArr[2])) {
                                        //计算市值
                                        infArr = dictI.split("=")[1].split(",");
                                        if (!codePriceMap.containsKey(itemArr[2])) {
                                            continue;
                                        }
                                        curDict = codeDictMap.get(itemArr[2]);
                                        mValue = BigDecimal.valueOf(codePriceMap.get(itemArr[2]) * Double.parseDouble(infArr[8]) / 10000).setScale(2,BigDecimal.ROUND_HALF_UP);
                                        curDict.setCirMarketValue(mValue);
                                        curDictList.add(curDict);
                                    }
                                }
                            }
                        }
                    });
                    dictResultList.add(curDictList);
                    latch.countDown();
                });
            });
            try {
                latch.await();
                threadPool.shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            long dbStartTime = System.currentTimeMillis();
            List<Dict> updatedDictList = dictResultList.stream().flatMap(List::stream).collect(Collectors.toList());
            mybatisBatchHandler.batchInsertOrUpdate(updatedDictList,DictMapper.class,GlobalConstant.BATCH_MODDEL_UPDATE);
            long dbEndTime = System.currentTimeMillis();
            log.info("==========================入库完成，当前时间：{}==========================", SpiderUtil.getCurrentTimeStr());
            log.info("==========================共耗时{}秒==========================", (dbEndTime - dbStartTime) / 1000);
        }
        long endTime = System.currentTimeMillis();
        log.info("==========================更新股票市值数据完成，当前时间：{}==========================", SpiderUtil.getCurrentTimeStr());
        log.info("==========================共耗时{}秒==========================", (endTime - startTime) / 1000);
    }

    /**
     * 抓取股票数据，并提取入库
     * @param type 抓取类型
     * @param isMarketClosed 是否闭市
     */
    public void crewStockData(int type,boolean isMarketClosed) {
        long startTime = System.currentTimeMillis();
        log.info("==========================开始抓取股票数据，当前时间：{}==========================", SpiderUtil.getCurrentTimeStr());
        LocalDate today = LocalDate.now();

        //此开关控制抓取，若盘中抓取，每天调度任务时会自动初始化，故此处不需要再初始化，节省时间，盘后或测试抓取，此处必须初始化，否则涨停跌停没有数据
        if (isMarketClosed) {
            //初始化上个交易日的涨停跌停价
            List<Quotation> lastDayQuoList = stockQueryService.selectLastDayQuoList();
            MonitorTask.codeLimitUDPMap = new HashMap<>();
            if (lastDayQuoList.size() > 0) {
                lastDayQuoList.forEach(q -> {
                    CodeLimitUDPVo vo = new CodeLimitUDPVo();
                    vo.setCode(q.getCode());
                    vo.setUpLimitVal(q.getClose().multiply(BigDecimal.valueOf(1.1)).setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue());
                    vo.setDownLimitVal(q.getClose().multiply(BigDecimal.valueOf(0.9)).setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue());
                    MonitorTask.codeLimitUDPMap.put(q.getCode(),vo);
                });
            }
        }

        DateTimeFormatter ymdFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String todayStr = today.format(ymdFormat);
        //获取全部股票列表
        List<Dict> dictList = new ArrayList<>();
        List<CuMonitor> cuMonitorList = cuMonitorMapper.selectNewestList();
        if (GlobalConstant.CREW_STOCK_POOL == type) {//开盘期只抓取股票监控池的股票行情
            if (cuMonitorList.size() > 0) {
                for (CuMonitor m : cuMonitorList) {
                    Dict dict = new Dict();
                    BeanUtils.copyProperties(m,dict);
                    dictList.add(dict);
                }
            }
        } else if (GlobalConstant.CREW_STOCK_REFER == type){//只抓取工行，主要检测是否正常交易日使用
            Dict referDict = new Dict();
            referDict.setCode(GlobalConstant.STOCK_REFER_CODE);
            dictList.add(referDict);
        } else if (GlobalConstant.CREW_STOCK_MAIN_BOARD == type) {//开盘期只抓取主板，排除创业版
            dictList = dictMapper.selectListByType(GlobalConstant.DICT_TYPE_STOCK);
            dictList = dictList.stream().filter(x -> !x.getCode().contains("sz300")).collect(Collectors.toList());
        } else {//全量抓取，用于更新完整数据
            dictList = dictMapper.selectListByType(GlobalConstant.DICT_TYPE_STOCK);
        }
        //单次批量抓取数量
        int batchSize = 50;
        //启动多线程抓取
        List<List<Dict>> twoDeepList = SpiderUtil.partitionList(dictList,GlobalConstant.MAX_THREAD_COUNT);
        ExecutorService threadPool = Executors.newFixedThreadPool(twoDeepList.size());
        log.info("============================本次共启动{}个线程，抓取{}个股票数据,当前时间：{}============================",twoDeepList.size(),dictList.size(),SpiderUtil.getCurrentTimeStr());
        if (twoDeepList.size() > 0) {
            Set<List<Quotation>> crewResultSet = new HashSet<>();
            List<List<String>> upCodeList = new ArrayList<>();
            List<List<String>> downCodeList = new ArrayList<>();
            Set<List<String>> upLimitCodeList = new HashSet<>();
            Set<List<String>> downLimitCodeList = new HashSet<>();
            CountDownLatch latch = new CountDownLatch(twoDeepList.size());
            twoDeepList.forEach(innerList -> {
                threadPool.execute(() -> {
                    //调用google算法分配
                    List<List<Dict>> groupList =  Lists.partition(innerList, batchSize);
                    List<String> crewCodeList = groupList.stream().map(x -> x.stream().map(Dict::getCode).collect(Collectors.joining(","))).collect(Collectors.toList());
                    if (crewCodeList.size() > 0) {
                        List<Quotation> quotationList = new ArrayList<>();
                        List<String> curUpCodeList = new ArrayList<>();
                        List<String> curDownCodeList = new ArrayList<>();
                        List<String> curUpLimitCodeList = new ArrayList<>();
                        List<String> curDownLimitCodeList = new ArrayList<>();
                        crewCodeList.forEach(codeList -> {
                            String url = String.format(GlobalConstant.SINA_STOCK_QUOTATION_URL,codeList);
                            ResponseEntity responseEntity = clientUtil.recursiveHttpRequest(GlobalConstant.HTTP_GET,0,url,null,GlobalConstant.DEFAULTSINAHEADER,GlobalConstant.CHARASET_GBK);
                            if (responseEntity.getCode() == 200) {
                                //根据换行符分组，提取值
                                String []dataArr = responseEntity.getContent().split("\n");
                                if (dataArr.length > 0) {
                                    Quotation quotation = null;
                                    String code = null;
                                    String []splitArr = null;
                                    String []dArr = null;
                                    BigDecimal initAmt = null;
                                    BigDecimal currentAmt = null;
                                    BigDecimal offSetRate = null;
                                    for (String rData : dataArr) {
                                        //组装行情数据
                                        splitArr = rData.split("=");
                                        code = splitArr[0].substring(splitArr[0].lastIndexOf("_") + 1);

                                        if (splitArr.length == 1) {
                                            continue;
                                        }
                                        dArr = splitArr[1].replaceAll("\"","").replaceAll(";","").split(",");
                                        if (dArr.length < 10) {
                                            continue;
                                        }
                                        initAmt = BigDecimal.valueOf(Double.parseDouble(dArr[2]));
                                        currentAmt = BigDecimal.valueOf(Double.parseDouble(dArr[3]));

                                        //构建股票行情对象
                                        quotation = new Quotation();
                                        quotation.setId(genIdUtil.nextId());
                                        quotation.setCode(code);
                                        quotation.setName(dArr[0]);
                                        quotation.setDate(today);
                                        quotation.setInit(initAmt);
                                        quotation.setOpen(BigDecimal.valueOf(Double.parseDouble(dArr[1])));
                                        quotation.setCurrent(currentAmt);
                                        quotation.setHigh(BigDecimal.valueOf(Double.parseDouble(dArr[4])));
                                        quotation.setLow(BigDecimal.valueOf(Double.parseDouble(dArr[5])));
                                        //关盘价只有等关盘后才产生，默认=当前价
                                        quotation.setClose(BigDecimal.valueOf(Double.parseDouble(dArr[3])));
                                        quotation.setVolume(BigDecimal.valueOf(Double.parseDouble(dArr[8])));
                                        quotation.setVolumeAmt(BigDecimal.valueOf(Double.parseDouble(dArr[9])));
                                        offSetRate = (currentAmt.subtract(initAmt)).multiply(BigDecimal.valueOf(100)).divide(initAmt,4, RoundingMode.HALF_UP);
                                        quotation.setOffsetRate(offSetRate);
                                        quotation.setSourceData(rData);

                                        if (MonitorTask.codeLimitUDPMap.containsKey(quotation.getCode()) && MonitorTask.codeLimitUDPMap.get(quotation.getCode()).getUpLimitVal() == quotation.getCurrent().doubleValue()) {
                                            curUpLimitCodeList.add(code);
                                        } else if (offSetRate.doubleValue() > 0) {
                                            curUpCodeList.add(code);
                                        } else if (MonitorTask.codeLimitUDPMap.containsKey(quotation.getCode()) && MonitorTask.codeLimitUDPMap.get(quotation.getCode()).getDownLimitVal() == quotation.getCurrent().doubleValue()) {
                                            curDownLimitCodeList.add(code);
                                        } else if (offSetRate.doubleValue() < 0) {
                                            curDownCodeList.add(code);
                                        }

                                        quotationList.add(quotation);
                                    }
                                }
                            }
                        });
                        crewResultSet.add(quotationList);
                        upLimitCodeList.add(curUpLimitCodeList);
                        downLimitCodeList.add(curDownLimitCodeList);
                        upCodeList.add(curUpCodeList);
                        downCodeList.add(curDownCodeList);
                    }
                    latch.countDown();
                });
            });
            //等待全部子线程完成，再统一获取结果
            try {
                latch.await();
                threadPool.shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //将最终结果拉平变一纬，直接进行DB保存操作
            List<Quotation> quotationList = crewResultSet.stream().flatMap(List::stream).collect(Collectors.toList());
            log.info("==========================抓取股票数据完成，总条数{} 开始入库，当前时间：{}==========================", quotationList.size(), SpiderUtil.getCurrentTimeStr());
            long startDBTime = System.currentTimeMillis();
            //全部重新写入
            quotationMapper.deleteByDate(todayStr);
            //批量添加股票行情数据
            mybatisBatchHandler.batchInsertOrUpdate(quotationList, QuotationMapper.class, GlobalConstant.BATCH_MODDEL_INSERT);
            long endTime = System.currentTimeMillis();
            log.info("==========================股票行情数据入库完成，当前时间：{} DB入库共耗时{}秒==========================", SpiderUtil.getCurrentTimeStr(),(endTime - startDBTime) / 1000);

            //批量写入实时数据，监控的才需要写入，其他不需要
            //过滤出监控的写入瞬时时间方便调试,其他不需要
            List<String> cuCodeList = cuMonitorList.stream().map(CuMonitor::getCode).collect(Collectors.toList());
            List<Quotation> copyQuotationList = quotationList.stream().filter(x -> cuCodeList.contains(x.getCode())).collect(Collectors.toList());
            List<HisQuotation> hisQuotationList = new ArrayList<>();
            LocalDateTime curTime = LocalDateTime.now();
            copyQuotationList.forEach(q -> {
                HisQuotation hq = new HisQuotation();
                BeanUtils.copyProperties(q,hq);
                hq.setAvg(hq.getVolumeAmt().compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : hq.getVolumeAmt().divide(hq.getVolume(),2,BigDecimal.ROUND_HALF_UP));
                hq.setCount(MonitorTask.crewCount);
                hq.setCreateTime(curTime);
                hq.setUpdateTime(curTime);
                hisQuotationList.add(hq);
            });
            mybatisBatchHandler.batchInsertOrUpdate(hisQuotationList, HisQuotationMapper.class, GlobalConstant.BATCH_MODDEL_INSERT);

            //涨停跌停
            //先删除当天之前统计的涨停跌停数据
            limitUpDownMapper.deleteByDate(todayStr);
            //构造涨停跌停对象
            //涨停跌停拉平，并用逗号相连转成字符串存储
            String upLimitCodeListStr = upLimitCodeList.stream().flatMap(List::stream).collect(Collectors.joining(","));
            String downLimitCodeListStr = downLimitCodeList.stream().flatMap(List::stream).collect(Collectors.joining(","));
            LimitUpDown limitUpDown = new LimitUpDown();
            limitUpDown.setId(genIdUtil.nextId());
            limitUpDown.setDate(today);
            limitUpDown.setUpList(upLimitCodeListStr);
            limitUpDown.setDownList(downLimitCodeListStr);
            limitUpDownMapper.insertSelective(limitUpDown);
            log.info("==========================统计涨停跌停数据入库完成，当前时间：{}==========================", SpiderUtil.getCurrentTimeStr());

            //上涨下跌
            //先删除当天之前统计的涨停跌停数据
            upDownMapper.deleteByDate(todayStr);
            //构造上涨下跌对象
            //涨停跌停拉平，并用逗号相连转成字符串存储
            String upCodeListStr = upCodeList.stream().flatMap(List::stream).collect(Collectors.joining(","));
            String downCodeListStr = downCodeList.stream().flatMap(List::stream).collect(Collectors.joining(","));
            UpDown upDown = new UpDown();
            upDown.setId(genIdUtil.nextId());
            upDown.setDate(today);
            upDown.setUpList(upCodeListStr);
            upDown.setDownList(downCodeListStr);
            upDownMapper.insertSelective(upDown);
            log.info("==========================统计上涨下跌数据入库完成，当前时间：{}==========================", SpiderUtil.getCurrentTimeStr());

            log.info("==========================共耗时{}秒==========================", (endTime - startTime) / 1000);
        }
    }

    /**
     * 计算股票平均价格，包括5日，10日，20日，30日均价
     * @param date
     */
    public void updateAvgPrice(LocalDate date) {
        long startTime = System.currentTimeMillis();
        SimpleDateFormat ymdFormat = new SimpleDateFormat("yyyy-MM-dd");
        String strDate = ymdFormat.format(date);
        log.info("==========================开始更新{}均价，当前时间：{}==========================",strDate, SpiderUtil.getCurrentTimeStr());
//        if (SpiderUtil.isWeekendOfToday(date)) {
//            log.error("==========================周末时间不开盘，忽略本次任务==========================");
//            return;
//        }
        List<Quotation> dbQuotationList = quotationMapper.selectListByDate(strDate);
        if (dbQuotationList.size() == 0) {
            log.info("=========================第一次还未抓取，忽略本次计算=========================");
            return;
        }
        long cStartTime = System.currentTimeMillis();
        if (dbQuotationList.size() > 0) {
            //启动多线程计算
            List<List<Quotation>> twoDeepList = SpiderUtil.partitionList(dbQuotationList, GlobalConstant.MAX_THREAD_COUNT);
            //=======================================================计算当日均价开始=======================================================
            ExecutorService threadPool = Executors.newFixedThreadPool(twoDeepList.size());
            log.info("============================本次共启动{}个线程计算{}的{}个股票均价,当前时间：{}============================", twoDeepList.size(), strDate, dbQuotationList.size(), SpiderUtil.getCurrentTimeStr());
            CountDownLatch latch = new CountDownLatch(twoDeepList.size());
            List<List<AvgPrice>> twoDeepPriceList = new ArrayList<>();
            twoDeepList.forEach(innerList -> {
                threadPool.execute(() -> {
                    List<AvgPrice> avgPriceList = new ArrayList<>();
                    AvgPrice avgPrice = null;
                    for (Quotation quotation : innerList) {
                        if (quotation.getVolume().doubleValue() > 0) {
                            avgPrice = new AvgPrice();
                            avgPrice.setId(genIdUtil.nextId());
                            avgPrice.setCode(quotation.getCode());
                            avgPrice.setName(quotation.getName());
                            avgPrice.setDate(date);
                            avgPrice.setAvg(quotation.getVolumeAmt().divide(quotation.getVolume(), 2, BigDecimal.ROUND_HALF_UP));
                            avgPriceList.add(avgPrice);
                        }
                    }
                    twoDeepPriceList.add(avgPriceList);
                    latch.countDown();
                });
            });
            //等待子线程全部计算完毕
            try {
                latch.await();
                threadPool.shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            List<AvgPrice> insertAvgPriceList = twoDeepPriceList.stream().flatMap(List::stream).collect(Collectors.toList());
            Map<String,AvgPrice> codeAvgPriceMap = insertAvgPriceList.stream().collect(Collectors.toMap(AvgPrice::getCode,Function.identity(),(o,n) -> n));
            long cEndTime = System.currentTimeMillis();
            log.info("==========================计算当日均价耗时：{}秒==========================", (cEndTime - cStartTime) / 1000);
            //=======================================================计算当日均价结束=======================================================

            //=======================================================计算N日均价开始=======================================================
            //计算60天以前的日期
            LocalDate minDate = date.plusDays(-60);
            //查询全部股票60天内的行情数据
            List<Quotation> dbHisQuotationList = quotationMapper.selectListByDateRange(ymdFormat.format(minDate),strDate);
            //根据股票编码分组股票列表
            Map<String,List<Quotation>> codeQuotationMap = dbHisQuotationList.stream().collect(Collectors.groupingBy(Quotation::getCode));
            cStartTime = System.currentTimeMillis();
            //获取当前天的股票列表
            List<Quotation> curDateQuotationList = dbQuotationList.stream().filter(x -> ymdFormat.format(x.getDate()).equals(strDate)).collect(Collectors.toList());
            //启动多线程统计
            twoDeepList = SpiderUtil.partitionList(curDateQuotationList,GlobalConstant.MAX_THREAD_COUNT);
            ExecutorService threadPool2 = Executors.newFixedThreadPool(twoDeepList.size());
            log.info("============================本次共启动{}个线程计算{}的{}个股票历史均价,当前时间：{}============================",twoDeepList.size(),strDate,curDateQuotationList.size(),SpiderUtil.getCurrentTimeStr());
            CountDownLatch latch2 = new CountDownLatch(twoDeepList.size());
            twoDeepList.forEach(innerList -> {
                threadPool2.execute(() -> {
                    BigDecimal avg5Price = null;
                    BigDecimal avg10Price = null;
                    BigDecimal avg20Price = null;
                    BigDecimal avg30Price = null;

                    double volumeAmt = 0d;
                    double volume = 0d;

                    List<Quotation> recent5QuotationList = null;
                    List<Quotation> recent10QuotationList = null;
                    List<Quotation> recent20QuotationList = null;
                    List<Quotation> recent30QuotationList = null;

                    AvgPrice avgPrice = null;
                    for (Quotation quotation : innerList) {
                        //获取当前数据的历史行情列表
                        List<Quotation> hisQuotationList = codeQuotationMap.get(quotation.getCode());
                        if (hisQuotationList != null && hisQuotationList.size() > 5) {
                            //将历史数据按照时间排序，并只保留30条数据
                            hisQuotationList.sort(Comparator.comparing(Quotation::getDate).reversed());

                            //计算4个均价
                            //5日均价
                            recent5QuotationList = hisQuotationList.stream().limit(5).collect(Collectors.toList());
                            volumeAmt = recent5QuotationList.stream().collect(Collectors.summarizingDouble(x -> x.getVolumeAmt().doubleValue())).getSum();
                            volume = recent5QuotationList.stream().collect(Collectors.summarizingDouble(x -> x.getVolume().doubleValue())).getSum();
                            if (volume < 1) {
                                continue;
                            }
                            avg5Price = BigDecimal.valueOf(volumeAmt).divide(BigDecimal.valueOf(volume),2,BigDecimal.ROUND_HALF_UP);
                            //10日均价
                            recent10QuotationList = hisQuotationList.stream().limit(10).collect(Collectors.toList());
                            if (recent10QuotationList.size() >= 10) {
                                volumeAmt = recent10QuotationList.stream().collect(Collectors.summarizingDouble(x -> x.getVolumeAmt().doubleValue())).getSum();
                                volume = recent10QuotationList.stream().collect(Collectors.summarizingDouble(x -> x.getVolume().doubleValue())).getSum();
                                avg10Price = BigDecimal.valueOf(volumeAmt).divide(BigDecimal.valueOf(volume),2,BigDecimal.ROUND_HALF_UP);
                            }
                            //20日均价
                            recent20QuotationList = hisQuotationList.stream().limit(20).collect(Collectors.toList());
                            if (recent20QuotationList.size() >= 20) {
                                volumeAmt = recent20QuotationList.stream().collect(Collectors.summarizingDouble(x -> x.getVolumeAmt().doubleValue())).getSum();
                                volume = recent20QuotationList.stream().collect(Collectors.summarizingDouble(x -> x.getVolume().doubleValue())).getSum();
                                avg20Price = BigDecimal.valueOf(volumeAmt).divide(BigDecimal.valueOf(volume),2,BigDecimal.ROUND_HALF_UP);
                            }
                            //30日均价
                            recent30QuotationList = hisQuotationList.stream().limit(30).collect(Collectors.toList());
                            if (recent30QuotationList.size() >= 30) {
                                volumeAmt = recent30QuotationList.stream().collect(Collectors.summarizingDouble(x -> x.getVolumeAmt().doubleValue())).getSum();
                                volume = recent30QuotationList.stream().collect(Collectors.summarizingDouble(x -> x.getVolume().doubleValue())).getSum();
                                avg30Price = BigDecimal.valueOf(volumeAmt).divide(BigDecimal.valueOf(volume),2,BigDecimal.ROUND_HALF_UP);
                            }

                            //填充均价数据
                            if (codeAvgPriceMap.containsKey(quotation.getCode())) {
                                avgPrice = codeAvgPriceMap.get(quotation.getCode());
                                avgPrice.setAvg5(avg5Price);
                                avgPrice.setAvg10(avg10Price);
                                avgPrice.setAvg20(avg20Price);
                                avgPrice.setAvg30(avg30Price);
                            }
                        } else {
                            log.info("======================股票【{} -> {}】上市不足5天，忽略本次计算======================",quotation.getCode(),quotation.getName());
                        }
                    }
                    latch2.countDown();
                });
            });
            //等待子线程全部计算完毕
            try {
                latch2.await();
                threadPool2.shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            cEndTime = System.currentTimeMillis();
            log.info("==========================计算历史均价完成：当前时间:{}==========================", SpiderUtil.getCurrentTimeStr());
            log.info("==========================计算历史均价耗时：{}秒==========================", (cEndTime - cStartTime) / 1000);
            //=======================================================计算N日均价结束=======================================================

            //批量更新均价，选择先删除再插入方式，提升效率
            avgPriceMapper.deleteByDate(strDate);
            mybatisBatchHandler.batchInsertOrUpdate(insertAvgPriceList,AvgPriceMapper.class,GlobalConstant.BATCH_MODDEL_INSERT);
        }
        long endTime = System.currentTimeMillis();
        log.info("==========================更新{}均价完成，当前时间：{}==========================",strDate, SpiderUtil.getCurrentTimeStr());
        log.info("==========================共耗时{}秒==========================", (endTime - startTime) / 1000);
    }

    /**
     * 统计某天的涨停跌停数据
     * 恢复数据时使用
     * @param date
     */
    public void calcLimitUpDownList(LocalDate date) {
        long startTime = System.currentTimeMillis();
        DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String strDate = date.format(ymdFormatter);
        log.info("==========================开始统计{}涨停跌停数据，当前时间：{}==========================",strDate, SpiderUtil.getCurrentTimeStr());
//        if (SpiderUtil.isWeekendOfToday(date)) {
//            log.error("==========================周末时间不开盘，忽略本次任务==========================");
//            return;
//        }
        List<Quotation> dbQuotationList = quotationMapper.selectListByDate(strDate);
        long cStartTime = System.currentTimeMillis();
        if (dbQuotationList.size() > 0) {
            //启动多线程统计
            List<List<Quotation>> twoDeepList = SpiderUtil.partitionList(dbQuotationList,GlobalConstant.MAX_THREAD_COUNT);
            ExecutorService threadPool = Executors.newFixedThreadPool(twoDeepList.size());
            log.info("============================本次共启动{}个线程统计{}的{}个股票涨停跌停数据,当前时间：{}============================",twoDeepList.size(),strDate,dbQuotationList.size(),SpiderUtil.getCurrentTimeStr());
            CountDownLatch latch = new CountDownLatch(twoDeepList.size());
            List<List<String>> upCodeList = new ArrayList<>();
            List<List<String>> downCodeList = new ArrayList<>();
            twoDeepList.forEach(innerList -> {
                threadPool.execute(() -> {
                    List<String> tmpUpCodeList = new ArrayList<>();
                    List<String> tmpDownCodeList = new ArrayList<>();
                    for (Quotation quotation : innerList) {
                        //过滤掉无效数据
                        if (quotation.getCurrent().compareTo(BigDecimal.valueOf(0.5)) < 0) {
                            continue;
                        }
                        //统计涨停的
                        if (quotation.getOffsetRate().doubleValue() >= 9.6) {
                            tmpUpCodeList.add(quotation.getCode());
                        }
                        //统计跌停的
                        if (quotation.getOffsetRate().doubleValue() <= -9.6) {
                            tmpDownCodeList.add(quotation.getCode());
                        }
                    }
                    upCodeList.add(tmpUpCodeList);
                    downCodeList.add(tmpDownCodeList);
                    latch.countDown();
                });
            });
            //等待子线程全部计算完毕
            try {
                latch.await();
                threadPool.shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            long cEndTime = System.currentTimeMillis();
            log.info("==========================统计涨停跌停耗时：{}秒==========================",(cEndTime - cStartTime) / 1000);

            //先删除当天之前统计的涨停跌停数据
            limitUpDownMapper.deleteByDate(strDate);
            //构造涨停跌停对象
            //涨停跌停拉平，并用逗号相连转成字符串存储
            List<List<String>> fUpCodeList = upCodeList.stream().filter(x -> x != null && x.size() > 0).collect(Collectors.toList());
            String upCodeListStr = null;
            if (fUpCodeList.size() > 0) {
                upCodeListStr = fUpCodeList.stream().flatMap(List::stream).collect(Collectors.joining(","));
            }
            List<List<String>> fDownCodeList = downCodeList.stream().filter(x -> x != null && x.size() > 0).collect(Collectors.toList());
            String downCodeListStr = null;
            if (fDownCodeList.size() > 0) {
                downCodeListStr = fDownCodeList.stream().flatMap(List::stream).collect(Collectors.joining(","));
            }
            LimitUpDown limitUpDown = new LimitUpDown();
            limitUpDown.setId(genIdUtil.nextId());
            limitUpDown.setDate(date);
            limitUpDown.setUpList(upCodeListStr);
            limitUpDown.setDownList(downCodeListStr);
            limitUpDownMapper.insertSelective(limitUpDown);
        }
        long endTime = System.currentTimeMillis();
        log.info("==========================统计{}涨停跌停数据完成，当前时间：{}==========================",strDate, SpiderUtil.getCurrentTimeStr());
        log.info("==========================共耗时{}秒==========================", (endTime - startTime) / 1000);
    }

    /**
     * 统计某天的涨跌数据
     * 恢复数据时使用
     * @param date
     */
    public void calcUpDownList(LocalDate date) {
        long startTime = System.currentTimeMillis();
        DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String strDate = date.format(ymdFormatter);
        log.info("==========================开始统计{}涨跌数据，当前时间：{}==========================",strDate, SpiderUtil.getCurrentTimeStr());
//        if (SpiderUtil.isWeekendOfToday(date)) {
//            log.error("==========================周末时间不开盘，忽略本次任务==========================");
//            return;
//        }
        List<Quotation> dbQuotationList = quotationMapper.selectListByDate(strDate);
        long cStartTime = System.currentTimeMillis();
        if (dbQuotationList.size() > 0) {
            //启动多线程统计
            List<List<Quotation>> twoDeepList = SpiderUtil.partitionList(dbQuotationList,GlobalConstant.MAX_THREAD_COUNT);
            ExecutorService threadPool = Executors.newFixedThreadPool(twoDeepList.size());
            log.info("============================本次共启动{}个线程统计{}的{}个股票涨跌数据,当前时间：{}============================",twoDeepList.size(),strDate,dbQuotationList.size(),SpiderUtil.getCurrentTimeStr());
            CountDownLatch latch = new CountDownLatch(twoDeepList.size());
            List<List<String>> upCodeList = new ArrayList<>();
            List<List<String>> downCodeList = new ArrayList<>();
            twoDeepList.forEach(innerList -> {
                threadPool.execute(() -> {
                    List<String> tmpUpCodeList = new ArrayList<>();
                    List<String> tmpDownCodeList = new ArrayList<>();
                    for (Quotation quotation : innerList) {
                        //过滤掉无效数据
                        if (quotation.getCurrent().compareTo(BigDecimal.valueOf(0.5)) < 0) {
                            continue;
                        }
                        //涨跌判断，当前价 》 开盘价，就是涨，否则是跌
                        if (quotation.getCurrent().compareTo(quotation.getInit()) > 0) {
                            tmpUpCodeList.add(quotation.getCode());
                        } else if (quotation.getCurrent().compareTo(quotation.getInit()) < 0) {
                            tmpDownCodeList.add(quotation.getCode());
                        }
                    }
                    upCodeList.add(tmpUpCodeList);
                    downCodeList.add(tmpDownCodeList);
                    latch.countDown();
                });
            });
            //等待子线程全部计算完毕
            try {
                latch.await();
                threadPool.shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            long cEndTime = System.currentTimeMillis();
            log.info("==========================统计涨跌耗时：{}秒==========================",(cEndTime - cStartTime) / 1000);

            //先删除当天之前统计的涨停跌停数据
            upDownMapper.deleteByDate(strDate);
            //构造涨停跌停对象
            //涨停跌停拉平，并用逗号相连转成字符串存储
            List<List<String>> fUpCodeList = upCodeList.stream().filter(x -> x != null && x.size() > 0).collect(Collectors.toList());
            String upCodeListStr = null;
            if (fUpCodeList.size() > 0) {
                upCodeListStr = fUpCodeList.stream().flatMap(List::stream).collect(Collectors.joining(","));
            }
            List<List<String>> fDownCodeList = downCodeList.stream().filter(x -> x != null && x.size() > 0).collect(Collectors.toList());
            String downCodeListStr = null;
            if (fDownCodeList.size() > 0) {
                downCodeListStr = fDownCodeList.stream().flatMap(List::stream).collect(Collectors.joining(","));
            }
            UpDown upDown = new UpDown();
            upDown.setId(genIdUtil.nextId());
            upDown.setDate(date);
            upDown.setUpList(upCodeListStr);
            upDown.setDownList(downCodeListStr);
            upDownMapper.insertSelective(upDown);
        }
        long endTime = System.currentTimeMillis();
        log.info("==========================统计{}涨跌数据完成，当前时间：{}==========================",strDate, SpiderUtil.getCurrentTimeStr());
        log.info("==========================共耗时{}秒==========================", (endTime - startTime) / 1000);
    }

    /**
     * 统计某天的放量数据
     * @param date
     */
    public void calcVolumeList(LocalDate date) {
        long startTime = System.currentTimeMillis();
        DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String strDate = date.format(ymdFormatter);
        log.info("==========================开始统计{}涨跌数据，当前时间：{}==========================",strDate, SpiderUtil.getCurrentTimeStr());
        int days = 7;
        //先查询字典列表
        List<Dict> dictList = dictMapper.selectListByType(GlobalConstant.DICT_TYPE_STOCK);
        //过滤掉科创板，创业版和ST
        List<String> normalCodeList = dictList.stream().map(Dict::getCode).filter(code -> !code.contains("sz300")).collect(Collectors.toList());

        //查询过去8个月涨停次数大于2次的股票
        List<UpLimitCountVo> upLimitCountVoList = quotationMapper.selectUpLimitGtN(60,date,3);
        Map<String,UpLimitCountVo> codeLimitCountMap = upLimitCountVoList.stream().collect(Collectors.toMap(UpLimitCountVo::getCode,Function.identity(),(o,n) -> n));

        //查询行情列表，并过滤掉辣鸡
        List<Quotation> dbQuotationList = quotationMapper.selectListByRangeOfNDay(strDate,days);
        dbQuotationList = dbQuotationList.stream().filter(x -> normalCodeList.contains(x.getCode()) && !codeLimitCountMap.containsKey(x.getCode())).collect(Collectors.toList());

//        //查询行情列表，并过滤掉辣鸡
//        List<Quotation> dbQuotationList = quotationMapper.selectListByRangeOfNDay(strDate,days);
//        dbQuotationList = dbQuotationList.stream().filter(x -> normalCodeList.contains(x.getCode())).collect(Collectors.toList());

        //根据日期分组
        Map<String,List<Quotation>> dateQuotationListMap = dbQuotationList.stream().collect(Collectors.groupingBy(x -> x.getDate().format(ymdFormatter)));

        if (!dateQuotationListMap.containsKey(strDate)) {
            log.info("=======================日期【{}】非正常交易日，忽略此次任务=======================",strDate);
            return;
        }

        //股票属性列表
        List<DictProp> propList = dictPropMapper.selectAll();
        Map<String,DictProp> codePropMap = propList.stream().collect(Collectors.toMap(DictProp::getCode,Function.identity(),(o,n) -> n));

        //查询最近7天的日期,以工行行情数据为准
        List<Quotation> lastNQuoList = quotationMapper.selectLastNDateList(strDate,days,GlobalConstant.STOCK_REFER_CODE);
        List<String> lastNDateList = lastNQuoList.stream().map(x -> x.getDate().format(ymdFormatter)).collect(Collectors.toList());

        //查询最近半个月的最高价
        List<LastNPriceVo> lastNPriceList = quotationMapper.selectLastNMaxPrice(strDate,30);
        Map<String,LastNPriceVo> lastNPriceMap = lastNPriceList.stream().collect(Collectors.toMap(LastNPriceVo::getCode,Function.identity(),(o,n) -> n));

        long cStartTime = System.currentTimeMillis();
        if (dateQuotationListMap.size() == days) {
            //倒叙排列
            lastNDateList.sort(Comparator.reverseOrder());
            String last1Date = lastNDateList.get(1);
            String last2Date = lastNDateList.get(2);
            String last3Date = lastNDateList.get(3);
            String last4Date = lastNDateList.get(4);
            String last5Date = lastNDateList.get(5);
            String last6Date = lastNDateList.get(6);

            //最近N天行情数据
            Map<String,Quotation> last1CodeQuotationMap = dateQuotationListMap.get(last1Date).stream().collect(Collectors.toMap(Quotation::getCode,Function.identity(),(o,n) -> n));
            Map<String,Quotation> last2CodeQuotationMap = dateQuotationListMap.get(last2Date).stream().collect(Collectors.toMap(Quotation::getCode,Function.identity(),(o,n) -> n));
            Map<String,Quotation> last3CodeQuotationMap = dateQuotationListMap.get(last3Date).stream().collect(Collectors.toMap(Quotation::getCode,Function.identity(),(o,n) -> n));
            Map<String,Quotation> last4CodeQuotationMap = dateQuotationListMap.get(last4Date).stream().collect(Collectors.toMap(Quotation::getCode,Function.identity(),(o,n) -> n));
            Map<String,Quotation> last5CodeQuotationMap = dateQuotationListMap.get(last5Date).stream().collect(Collectors.toMap(Quotation::getCode,Function.identity(),(o,n) -> n));
            Map<String,Quotation> last6CodeQuotationMap = dateQuotationListMap.get(last6Date).stream().collect(Collectors.toMap(Quotation::getCode,Function.identity(),(o,n) -> n));

            //查询最近两头均价列表
            List<AvgPrice> lastNAvgPriceList = avgPriceMapper.selectListByRangeOfNDay(strDate,4);
            Map<String,List<AvgPrice>> dateAvgPriceListMap = lastNAvgPriceList.stream().collect(Collectors.groupingBy(x -> x.getDate().format(ymdFormatter)));

            //今天均价信息
            Map<String,AvgPrice> codeAvgPriceMap = dateAvgPriceListMap.get(strDate).stream().collect(Collectors.toMap(AvgPrice::getCode,Function.identity(),(o,n) -> n));
            Map<String,AvgPrice> last1AvgPriceMap = dateAvgPriceListMap.get(last1Date).stream().collect(Collectors.toMap(AvgPrice::getCode,Function.identity(),(o,n) -> n));
            Map<String,AvgPrice> last2AvgPriceMap = dateAvgPriceListMap.get(last2Date).stream().collect(Collectors.toMap(AvgPrice::getCode,Function.identity(),(o,n) -> n));
            Map<String,AvgPrice> last3AvgPriceMap = dateAvgPriceListMap.get(last3Date).stream().collect(Collectors.toMap(AvgPrice::getCode,Function.identity(),(o,n) -> n));

            //按照黑名单过滤
            List<SysDict> backlistList = sysDictMapper.selectByType(GlobalConstant.SYS_DICT_TYPE_BACKLIST);
            Map<String,SysDict> codeBackListMap = backlistList.stream().collect(Collectors.toMap(SysDict::getValue,Function.identity(),(o,n) -> n));

            //按照盘子大小过滤
            Map<String,Dict> codeDictMap = dictList.stream().collect(Collectors.toMap(Dict::getCode,Function.identity(),(o,n) -> n));

            //按照当前的高地位过滤
            List<RelativePositionVo> positionList = quotationMapper.selectCurPosition(strDate);
            Map<String,RelativePositionVo> codePositionMap = positionList.stream().collect(Collectors.toMap(RelativePositionVo::getCode,Function.identity(),(o,n) -> n));

            //启动多线程统计
            List<List<Quotation>> twoDeepList = SpiderUtil.partitionList(dateQuotationListMap.get(strDate),GlobalConstant.MAX_THREAD_COUNT);
            ExecutorService threadPool = Executors.newFixedThreadPool(twoDeepList.size());
            log.info("============================本次共启动{}个线程统计{}的{}个股票放量数据,当前时间：{}============================",twoDeepList.size(),strDate,dateQuotationListMap.get(strDate).size(),SpiderUtil.getCurrentTimeStr());
            if (twoDeepList.size() > 0) {
                //记录当前放量时间
                SimpleDateFormat vFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String vTime = vFormat.format(new Date());

                CountDownLatch latch = new CountDownLatch(twoDeepList.size());
                List<List<VolumeVo>> twoDeepVolumeList = new ArrayList<>();
                //放量临界倍率
                BigDecimal minMultiple = BigDecimal.valueOf(2);
                BigDecimal maxMultiple = BigDecimal.valueOf(4);
                twoDeepList.forEach(innerList -> {
                    threadPool.execute(() -> {
                        List<VolumeVo> tmpVolumeList = new ArrayList<>();
                        VolumeVo volumeVo = null;
                        for (Quotation quotation : innerList) {
                            try {
                                if (last2CodeQuotationMap.containsKey(quotation.getCode()) && last1CodeQuotationMap.containsKey(quotation.getCode()) && last3CodeQuotationMap.containsKey(quotation.getCode())) {
                                    //当前交易量达到前两天的2倍以上即是放量
                                    if (quotation.getVolumeAmt() != null && last1CodeQuotationMap.get(quotation.getCode()).getVolumeAmt() != null && last2CodeQuotationMap.get(quotation.getCode()).getVolumeAmt() != null && last3CodeQuotationMap.get(quotation.getCode()).getVolumeAmt() != null &&
                                            quotation.getVolumeAmt().doubleValue() > last1CodeQuotationMap.get(quotation.getCode()).getVolumeAmt().doubleValue() * minMultiple.doubleValue() &&
                                            quotation.getVolumeAmt().doubleValue() > last2CodeQuotationMap.get(quotation.getCode()).getVolumeAmt().doubleValue() * minMultiple.doubleValue() &&
                                            quotation.getVolumeAmt().doubleValue() > last3CodeQuotationMap.get(quotation.getCode()).getVolumeAmt().doubleValue() * minMultiple.doubleValue()) {
                                        //过滤掉成交量低于8000w的辣鸡股
                                        if (quotation.getVolumeAmt().compareTo(BigDecimal.valueOf(80000000)) < 0) {
                                            continue;
                                        }
                                        //过滤掉银行
                                        if (quotation.getName().contains("银行")) {
                                            continue;
                                        }
                                        //过滤掉下跌的股票
                                        if (quotation.getOffsetRate().compareTo(BigDecimal.valueOf(0)) < 0) {
                                            continue;
                                        }
                                        //涨幅低于3个点的过滤掉
                                        if (quotation.getOffsetRate().doubleValue() < 4.8) {
                                            continue;
                                        }
                                        //过滤掉股价低于4元的
                                        if (quotation.getLow().doubleValue() < 4 || quotation.getLow().doubleValue() > 80) {
                                            continue;
                                        }
                                        //过滤掉黑名单的
                                        if (codeBackListMap.containsKey(quotation.getCode())) {
                                            continue;
                                        }
                                        //根据市值过滤
                                        if (!codeDictMap.containsKey(quotation.getCode()) || codeDictMap.get(quotation.getCode()) == null || codeDictMap.get(quotation.getCode()).getCirMarketValue() == null || codeDictMap.get(quotation.getCode()).getCirMarketValue().doubleValue() < 20 ||  codeDictMap.get(quotation.getCode()).getCirMarketValue().doubleValue() > 800) {
                                            continue;
                                        }
                                        //根据市盈率过滤,市盈率太低的过滤掉，小于40
                                        if (!codePropMap.containsKey(quotation.getCode()) || codePropMap.get(quotation.getCode()) == null || codePropMap.get(quotation.getCode()).getLyr() == null || codePropMap.get(quotation.getCode()).getLyr().compareTo(BigDecimal.valueOf(-1)) == 0 || (codePropMap.get(quotation.getCode()).getLyr().doubleValue() < 40 && codePropMap.get(quotation.getCode()).getTtm() != null && codePropMap.get(quotation.getCode()).getTtm().doubleValue() > 0 && codePropMap.get(quotation.getCode()).getTtm().doubleValue() < 40)) {
                                            continue;
                                        }
                                        //不存在均价信息的过滤掉
                                        if (codeAvgPriceMap.get(quotation.getCode()) == null || codeAvgPriceMap.get(quotation.getCode()).getAvg5() == null || codeAvgPriceMap.get(quotation.getCode()).getAvg10() == null || codeAvgPriceMap.get(quotation.getCode()).getAvg20() == null || codeAvgPriceMap.get(quotation.getCode()).getAvg30() == null) {
                                            continue;
                                        }
                                        //最近10日线下跌,且最近半个月最高价掉下来超过20%的过滤掉
                                        double lastNMaxPrice = lastNPriceMap.get(quotation.getCode()).getMaxPrice().doubleValue();
                                        double lastNMinPrice = lastNPriceMap.get(quotation.getCode()).getMinPrice().doubleValue();
                                        if (codeAvgPriceMap.get(quotation.getCode()).getLast10Trend() < 0 && (lastNMaxPrice - lastNMinPrice) / lastNMaxPrice > 0.15) {
                                            continue;
                                        }
                                        //根据3个月内的高地位过滤
                                        if (!codePositionMap.containsKey(quotation.getCode()) || codePositionMap.get(quotation.getCode()) == null || codePositionMap.get(quotation.getCode()).getPRate() > 80) {
                                            continue;
                                        }
                                        //高位超过70且高于5日线开盘的过滤掉
                                        if (codePositionMap.get(quotation.getCode()).getPRate() > 70 && quotation.getLow().doubleValue() > codeAvgPriceMap.get(quotation.getCode()).getAvg5().doubleValue()) {
                                            continue;
                                        }
                                        //高位超过70且高于5日线开盘的过滤掉
                                        if (codePositionMap.get(quotation.getCode()).getPRate() > 70 && codeAvgPriceMap.get(quotation.getCode()).getLast10Trend() > 0 && codeAvgPriceMap.get(quotation.getCode()).getLast10MonthTrend() > 0 && quotation.getLow().doubleValue() > codeAvgPriceMap.get(quotation.getCode()).getAvg10().doubleValue()) {
                                            continue;
                                        }
                                        //前两天有涨停的过滤掉
                                        if (last2CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() > 9.3 || last1CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() > 9.3) {
                                            continue;
                                        }
                                        //前两日必须有一天是下跌的
                                        if (last2CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() > 0 && last1CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() > 0) {
                                            continue;
                                        }
                                        //前两天有一天跌幅大于3%的过滤
                                        if (last2CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() < -4 || last1CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() < -4) {
                                            continue;
                                        }
                                        //昨天涨跌幅在-2.5~2.5以内
                                        if (last1CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() < -2.5 || last1CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() > 2.5) {
                                            continue;
                                        }
                                        //当天回调超过4%过滤掉
                                        if ((quotation.getHigh().doubleValue() - quotation.getInit().doubleValue()) * 100 / quotation.getInit().doubleValue() - quotation.getOffsetRate().doubleValue() > 4) {
                                            continue;
                                        }
                                        //跳空高开,高于昨天最高价开盘的过滤掉
                                        if (quotation.getOpen().doubleValue() > last1CodeQuotationMap.get(quotation.getCode()).getHigh().doubleValue()) {
                                            continue;
                                        }
                                        //高开并且放巨量的的过滤掉，即放量大于3倍了
                                        if (quotation.getOpen().doubleValue() > quotation.getInit().doubleValue() && quotation.getVolumeAmt().doubleValue() > last1CodeQuotationMap.get(quotation.getCode()).getVolumeAmt().doubleValue() * maxMultiple.doubleValue()) {
                                            continue;
                                        }
                                        //前3天均价都高于5日线或者低于5日线的过滤掉
                                        if ((last1CodeQuotationMap.get(quotation.getCode()).getOpen().doubleValue() < last1AvgPriceMap.get(quotation.getCode()).getAvg5().doubleValue() && last1CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() <= 0 &&
                                                last2CodeQuotationMap.get(quotation.getCode()).getOpen().doubleValue() < last2AvgPriceMap.get(quotation.getCode()).getAvg5().doubleValue() && last2CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() <= 0 &&
                                                last3CodeQuotationMap.get(quotation.getCode()).getOpen().doubleValue() < last3AvgPriceMap.get(quotation.getCode()).getAvg5().doubleValue() && last3CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() <= 0
                                        ) ||
                                            (last1CodeQuotationMap.get(quotation.getCode()).getOpen().doubleValue() > last1AvgPriceMap.get(quotation.getCode()).getAvg5().doubleValue() && last1CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() >= 0 &&
                                                    last2CodeQuotationMap.get(quotation.getCode()).getOpen().doubleValue() > last2AvgPriceMap.get(quotation.getCode()).getAvg5().doubleValue() && last2CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() >= 0 &&
                                                    last3CodeQuotationMap.get(quotation.getCode()).getOpen().doubleValue() > last3AvgPriceMap.get(quotation.getCode()).getAvg5().doubleValue() && last3CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() >= 0
                                            )
                                        ) {
                                            continue;
                                        }
                                        //处理均价信息
                                        List<Double> hisAvgList = new ArrayList<>();
                                        hisAvgList.add(codeAvgPriceMap.get(quotation.getCode()).getAvg5().doubleValue());
                                        hisAvgList.add(codeAvgPriceMap.get(quotation.getCode()).getAvg10().doubleValue());
                                        hisAvgList.add(codeAvgPriceMap.get(quotation.getCode()).getAvg20().doubleValue());
                                        hisAvgList.add(codeAvgPriceMap.get(quotation.getCode()).getAvg30().doubleValue());
                                        hisAvgList = hisAvgList.stream().sorted().collect(Collectors.toList());
                                        //多头向上,一阳穿四线且4线相差不大的过滤掉
                                        if (codeAvgPriceMap.get(quotation.getCode()).getLast10Trend() >= 0 && codeAvgPriceMap.get(quotation.getCode()).getLast10MonthTrend() == 99 && (hisAvgList.get(3) - hisAvgList.get(0)) / hisAvgList.get(3) <= 0.06) {
                                            continue;
                                        }
                                        //多头向上,10日线大于20日线,且开盘价高于20日线的过滤掉,放量大于3倍
                                        if (codeAvgPriceMap.get(quotation.getCode()).getLast10Trend() > 0 && codeAvgPriceMap.get(quotation.getCode()).getLast10MonthTrend() > 0 &&
                                                codeAvgPriceMap.get(quotation.getCode()).getAvg10().doubleValue() > codeAvgPriceMap.get(quotation.getCode()).getAvg20().doubleValue() &&
                                                last1CodeQuotationMap.get(quotation.getCode()).getLow().doubleValue() >= last1AvgPriceMap.get(quotation.getCode()).getAvg20().doubleValue() &&
                                                last1CodeQuotationMap.get(quotation.getCode()).getHigh().doubleValue() <= last1AvgPriceMap.get(quotation.getCode()).getAvg10().doubleValue()
                                        ) {
                                            continue;
                                        }
                                        //多头向上,一阳穿四线且4线相差不大的过滤掉
                                        if (codeAvgPriceMap.get(quotation.getCode()).getLast10Trend() == -1 && codeAvgPriceMap.get(quotation.getCode()).getLast10MonthTrend() == 99 && (hisAvgList.get(3) - hisAvgList.get(0)) / hisAvgList.get(3) <= 0.05 &&
                                            quotation.getOpen().doubleValue() < hisAvgList.get(0) && quotation.getCurrent().doubleValue() > hisAvgList.get(3)
                                        ) {
                                            continue;
                                        }
                                        //前几天有连续n天下跌的,且跌幅超过12的过滤掉
                                        if ((last6CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() < 0 && last5CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() < 0 && last4CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() < 0 &&
                                           (last6CodeQuotationMap.get(quotation.getCode()).getInit().doubleValue() - last4CodeQuotationMap.get(quotation.getCode()).getClose().doubleValue()) / last6CodeQuotationMap.get(quotation.getCode()).getInit().doubleValue() > 0.12) ||
                                           (last5CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() < 0 && last4CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() < 0 && last3CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() < 0 &&
                                           (last5CodeQuotationMap.get(quotation.getCode()).getInit().doubleValue() - last3CodeQuotationMap.get(quotation.getCode()).getClose().doubleValue()) / last5CodeQuotationMap.get(quotation.getCode()).getInit().doubleValue() > 0.12)) {
                                            continue;
                                        }
                                        //前两天有下跌，且涨幅<3且调整N天以后放量
                                        if ((last2CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() <= 0 || last1CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() <= 0) &&
                                                last2CodeQuotationMap.get(quotation.getCode()).getClose().doubleValue() < 3 && last1CodeQuotationMap.get(quotation.getCode()).getClose().doubleValue() < 3 &&
                                                quotation.getVolumeAmt().doubleValue() > last1CodeQuotationMap.get(quotation.getCode()).getVolumeAmt().doubleValue() * minMultiple.doubleValue() && quotation.getVolumeAmt().doubleValue() > last2CodeQuotationMap.get(quotation.getCode()).getVolumeAmt().doubleValue() * minMultiple.doubleValue() &&
                                                quotation.getVolumeAmt().doubleValue() > last3CodeQuotationMap.get(quotation.getCode()).getVolumeAmt().doubleValue() * minMultiple.doubleValue() && quotation.getVolumeAmt().doubleValue() > last4CodeQuotationMap.get(quotation.getCode()).getVolumeAmt().doubleValue() * minMultiple.doubleValue()
                                        ) {
                                            volumeVo = new VolumeVo();
                                            volumeVo.setCode(quotation.getCode());
                                            volumeVo.setName(quotation.getName());
                                            volumeVo.setTime(vTime);
                                            volumeVo.setMultiple(minMultiple);
                                            volumeVo.setVolumeAmt(quotation.getVolumeAmt());
                                            tmpVolumeList.add(volumeVo);
                                        } else {
                                            //1前两天最低点从高到低当然最低点 >= 倒数第二日
                                            //2当天一阳穿四线
                                            if ((last2CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() <= 0 || last1CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() <= 0) && last2CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() < 3 && last1CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() < 3) {
                                                if ((last2CodeQuotationMap.get(quotation.getCode()).getLow().doubleValue() >= last1CodeQuotationMap.get(quotation.getCode()).getLow().doubleValue() && quotation.getLow().doubleValue() > last1CodeQuotationMap.get(quotation.getCode()).getLow().doubleValue()) || (quotation.getLow().doubleValue() <= hisAvgList.get(0) * 1.002 && quotation.getHigh().doubleValue() >= hisAvgList.get(3))) {
                                                    volumeVo = new VolumeVo();
                                                    volumeVo.setCode(quotation.getCode());
                                                    volumeVo.setName(quotation.getName());
                                                    volumeVo.setTime(vTime);
                                                    volumeVo.setMultiple(minMultiple);
                                                    volumeVo.setVolumeAmt(quotation.getVolumeAmt());
                                                    tmpVolumeList.add(volumeVo);
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        twoDeepVolumeList.add(tmpVolumeList);
                        latch.countDown();
                    });
                });
                //等待子线程全部计算完毕
                try {
                    latch.await();
                    threadPool.shutdown();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                long cEndTime = System.currentTimeMillis();
                log.info("==========================统计放量数据耗时：{}秒==========================",(cEndTime - cStartTime) / 1000);

                try {
                    //先删除当天的放量记录
                    recommandMapper.deleteByDate(strDate,GlobalConstant.RECOMMAND_TYPE_VOLUME);
                    //获取已经存储的当日放量列表，去重后加入新列表存储
                    List<VolumeVo> curVolumeList = twoDeepVolumeList.stream().flatMap(List::stream).collect(Collectors.toList());
                    if (curVolumeList.size() > 0) {
                        //重新添加放量对象
                        Recommand newRecommand = new Recommand();
                        newRecommand.setId(genIdUtil.nextId());
                        newRecommand.setDate(date);
                        newRecommand.setType(GlobalConstant.RECOMMAND_TYPE_VOLUME);
                        newRecommand.setDataList(JSON.toJSONString(curVolumeList));
                        recommandMapper.insertSelective(newRecommand);
                    }
                } catch (Exception e) {
                    log.error("=======================放量列表为空，忽略本次添加=======================");
                    e.printStackTrace();
                }
            }
        }
        long endTime = System.currentTimeMillis();
        log.info("==========================统计{}放量列表完成，当前时间：{}==========================",strDate, SpiderUtil.getCurrentTimeStr());
        log.info("==========================共耗时{}秒==========================", (endTime - startTime) / 1000);
    }

    /**
     * 清空放量数据，重新跑提取
     */
    public void clearVolumeData(String type) {
        log.info("====================开始清空放量数据====================");
        List<Recommand> recommandList = recommandMapper.selectAll(type);
        if (recommandList != null && recommandList.size() > 0) {
            recommandList.forEach(vo -> {
                recommandMapper.deleteById(vo.getId());
            });
        }
        log.info("====================清空放量数据完成====================");
    }

    /**
     * 查询N天内涨停跌停数据列表
     * @param startDate
     * @param maxDays
     * @return
     */
    public Map<String, Object> queryLimitUpDownList(String startDate, int maxDays) {
        //获取全部的字典数据
        List<Dict> dictList = dictMapper.selectListByType(GlobalConstant.DICT_TYPE_STOCK);
        Map<String,Dict> dictMap = dictList.stream().collect(Collectors.toMap(Dict::getCode, Function.identity()));
        DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate sDate = LocalDate.parse(startDate,ymdFormatter);
        sDate = sDate.plusDays(-maxDays * 3L);
        List<LimitUpDown> dbLimitUpDownList = limitUpDownMapper.selectListByDateRange(sDate.format(ymdFormatter),startDate);
        Map<String,Object> resultMap = new LinkedHashMap<>();
        if (dbLimitUpDownList.size() >= maxDays) {
            //按日期降序排序后，获取前maxDays条
            List<LimitUpDown> usedLimitUpDownList = dbLimitUpDownList.stream().sorted(Comparator.comparing(LimitUpDown::getDate).reversed()).limit(maxDays).collect(Collectors.toList());
            Set<String> upCodeSet = new HashSet<>();
            Set<String> downCodeSet = new HashSet<>();

            //查询行业列表
            List<DictProp> propList = dictPropMapper.selectAll();
            //映射code->prop属性
            Map<String,DictProp> codePropMap = propList.stream().collect(Collectors.toMap(DictProp::getCode,Function.identity(),(oldP,newP) -> newP));

            Map<String,Object> map = null;
            Map<String,Object> upMap = null;
            Map<String,Object> downMap = null;

            List<Map<String,String>> codeNameList = null;
            Map<String,String> codeNameMap = null;
            int loopIndex = 0;
            for (LimitUpDown limitUpDown : usedLimitUpDownList) {
                map = new HashMap<>();

                //添加涨停数据
                upMap = new HashMap<>();
                if (loopIndex > 0) {
                    upCodeSet.retainAll(Arrays.asList(limitUpDown.getUpList().split(",")));
                } else {
                    upCodeSet.addAll(Arrays.asList(limitUpDown.getUpList().split(",")));
                }
                codeNameList = new ArrayList<>();
                for (String code : upCodeSet) {
                    codeNameMap = new HashMap<>();
                    if (code.contains("sz300")) {
                        continue;
                    }
                    if (!dictMap.containsKey(code)) {
                        continue;
                    }
                    codeNameMap.put("code",code);
                    codeNameMap.put("name",dictMap.get(code).getName());
                    String plate = "未知";
                    if (codePropMap.containsKey(code)) {
                        StringBuilder plateBuffer = new StringBuilder();
                        if (StringUtils.isNotEmpty(codePropMap.get(code).getIndustry())) {
                            plateBuffer.append(codePropMap.get(code).getIndustry()).append(" -> ");
                        }
                        if (StringUtils.isNotEmpty(codePropMap.get(code).getPlate())){
                            plateBuffer.append(codePropMap.get(code).getPlate());
                        }
                        plate = plateBuffer.toString();
                        plate = StringUtils.isEmpty(plate) ? "未知" : plate;
                    }
                    codeNameMap.put("plate",plate);
                    codeNameList.add(codeNameMap);
                }
                upMap.put("data",codeNameList);
                upMap.put("count",codeNameList.size());
                map.put("up",upMap);

                //添加跌停数据
                downMap = new HashMap<>();
                if (loopIndex > 0) {
                    downCodeSet.retainAll(Arrays.asList(limitUpDown.getDownList().split(",")));
                } else {
                    downCodeSet.addAll(Arrays.asList(limitUpDown.getDownList().split(",")));
                }
                codeNameList = new ArrayList<>();
                for (String code : downCodeSet) {
                    codeNameMap = new HashMap<>();
                    if (!dictMap.containsKey(code)) {
                        continue;
                    }
                    codeNameMap.put("code",code);
                    codeNameMap.put("name",dictMap.get(code).getName());
                    String plate = "未知";
                    if (codePropMap.containsKey(code)) {
                        StringBuilder plateBuffer = new StringBuilder();
                        if (StringUtils.isNotEmpty(codePropMap.get(code).getIndustry())) {
                            plateBuffer.append(codePropMap.get(code).getIndustry()).append(" -> ");
                        }
                        if (StringUtils.isNotEmpty(codePropMap.get(code).getPlate())){
                            plateBuffer.append(codePropMap.get(code).getPlate());
                        }
                        plate = plateBuffer.toString();
                        plate = StringUtils.isEmpty(plate) ? "未知" : plate;
                    }
                    codeNameMap.put("plate",plate);
                    codeNameList.add(codeNameMap);
                }
                downMap.put("data",codeNameList);
                downMap.put("count",codeNameList.size());
                map.put("down",downMap);
                resultMap.put(String.format("top%s",loopIndex+1),map);
                loopIndex++;
            }
        } else {
            log.error("============================当前涨停跌停数据不足{}天============================",maxDays);
        }
        return resultMap;
    }

    /**
     * 查询N天内连续涨跌数据列表
     * @param startDate
     * @param maxDays
     * @param isBest
     * @return
     */
    public Map<String,Object> queryUpDownList(String startDate,int maxDays,int isBest) {
        //获取全部的字典数据
        List<Dict> dictList = dictMapper.selectListByType(GlobalConstant.DICT_TYPE_STOCK);
        if (isBest == 1) {
            List<SysDict> qualityStockList = sysDictMapper.selectByType(GlobalConstant.SYS_DICT_TYPE_QUALITY_STOCKS);
            List<String> qualityStockCodeList = qualityStockList.stream().map(SysDict::getValue).collect(Collectors.toList());
            dictList = dictList.stream().filter(x -> qualityStockCodeList.contains(x.getCode())).collect(Collectors.toList());
        }
        Map<String,Dict> dictMap = dictList.stream().collect(Collectors.toMap(Dict::getCode, Function.identity()));
        SimpleDateFormat ymdFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar calendar= Calendar.getInstance();
        try {
            calendar.setTime(ymdFormat.parse(startDate));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        calendar.add(Calendar.DAY_OF_MONTH,-maxDays * 2);
        List<UpDown> dbLimitUpDownList = upDownMapper.selectListByDateRange(ymdFormat.format(calendar.getTime()),startDate);
        Map<String,Object> resultMap = new LinkedHashMap<>();
        if (dbLimitUpDownList.size() >= maxDays) {
            //按日期降序排序后，获取前maxDays条
            List<UpDown> usedUpDownList = dbLimitUpDownList.stream().sorted(Comparator.comparing(UpDown::getDate).reversed()).limit(maxDays).collect(Collectors.toList());
            Set<String> upCodeSet = new HashSet<>();
            Set<String> downCodeSet = new HashSet<>();

            Map<String,Object> map = null;
            Map<String,Object> upMap = null;
            Map<String,Object> downMap = null;

            //查询行业列表
            List<DictProp> propList = dictPropMapper.selectAll();
            //映射code->prop属性
            Map<String,DictProp> codePropMap = propList.stream().collect(Collectors.toMap(DictProp::getCode,Function.identity(),(oldP,newP) -> newP));

            List<Map<String,String>> codeNameList = null;
            Map<String,String> codeNameMap = null;
            int loopIndex = 0;
            for (UpDown upDown : usedUpDownList) {
                map = new HashMap<>();

                //添加涨停数据
                upMap = new HashMap<>();
                if (loopIndex > 0) {
                    upCodeSet.retainAll(Arrays.asList(upDown.getUpList().split(",")));
                } else {
                    upCodeSet.addAll(Arrays.asList(upDown.getUpList().split(",")));
                }
                codeNameList = new ArrayList<>();
                for (String code : upCodeSet) {
                    codeNameMap = new HashMap<>();
                    codeNameMap.put("code",code);
                    if (dictMap.containsKey(code)) {
                        codeNameMap.put("name",dictMap.get(code).getName());
                    } else {
                        continue;
                    }
                    String plate = "未知";
                    if (codePropMap.containsKey(code)) {
                        StringBuilder plateBuffer = new StringBuilder();
                        if (StringUtils.isNotEmpty(codePropMap.get(code).getIndustry())) {
                            plateBuffer.append(codePropMap.get(code).getIndustry()).append(" -> ");
                        }
                        if (StringUtils.isNotEmpty(codePropMap.get(code).getPlate())){
                            plateBuffer.append(codePropMap.get(code).getPlate());
                        }
                        plate = plateBuffer.toString();
                        plate = StringUtils.isEmpty(plate) ? "未知" : plate;
                    }
                    codeNameMap.put("plate",plate);
                    codeNameList.add(codeNameMap);
                }
                upMap.put("data",codeNameList);
                upMap.put("count",codeNameList.size());
                map.put("up",upMap);

                //添加跌停数据
                downMap = new HashMap<>();
                if (loopIndex > 0) {
                    downCodeSet.retainAll(Arrays.asList(upDown.getDownList().split(",")));
                } else {
                    downCodeSet.addAll(Arrays.asList(upDown.getDownList().split(",")));
                }
                codeNameList = new ArrayList<>();
                for (String code : downCodeSet) {
                    codeNameMap = new HashMap<>();
                    codeNameMap.put("code",code);
                    if (dictMap.containsKey(code)) {
                        codeNameMap.put("name",dictMap.get(code).getName());
                    } else {
                        continue;
                    }
                    String plate = "未知";
                    if (codePropMap.containsKey(code)) {
                        StringBuilder plateBuffer = new StringBuilder();
                        if (StringUtils.isNotEmpty(codePropMap.get(code).getIndustry())) {
                            plateBuffer.append(codePropMap.get(code).getIndustry()).append(" -> ");
                        }
                        if (StringUtils.isNotEmpty(codePropMap.get(code).getPlate())){
                            plateBuffer.append(codePropMap.get(code).getPlate());
                        }
                        plate = plateBuffer.toString();
                        plate = StringUtils.isEmpty(plate) ? "未知" : plate;
                    }
                    codeNameMap.put("plate",plate);
                    codeNameList.add(codeNameMap);
                }
                downMap.put("data",codeNameList);
                downMap.put("count",codeNameList.size());
                map.put("down",downMap);
                resultMap.put(String.format("top%s",loopIndex+1),map);
                loopIndex++;
            }
        } else {
            log.error("============================当前连续涨跌数据不足{}天============================",maxDays);
        }
        return resultMap;
    }

    /**
     * 提取推荐列表，规则如下
     * 1.5日线下行
     * 2.含有十字星
     * 3.有一天均价在4均线之间
     * 4.今日股价上涨，涨幅 > 2%
     * @param date
     * @return
     */
    public List<Quotation> queryRecommandList(LocalDate date) {
        SimpleDateFormat ymdFormat = new SimpleDateFormat("yyyy-MM-dd");
        String strDate = ymdFormat.format(date);
        //查询过去8个月涨停次数大于2次的股票
        List<UpLimitCountVo> upLimitCountVoList = quotationMapper.selectUpLimitGtN(30,date,3);
        Map<String,UpLimitCountVo> codeUpLimitVoMap = upLimitCountVoList.stream().collect(Collectors.toMap(UpLimitCountVo::getCode,Function.identity(),(o,n) -> n));
        //历史行情列表
        List<Quotation> quotationList = quotationMapper.selectRecommandListByRangeOfNDay(strDate,6);
        quotationList = quotationList.stream().filter(x -> !x.getCode().contains("sz300") && !codeUpLimitVoMap.containsKey(x.getCode())).collect(Collectors.toList());
        //根据日期分组
        Map<String,List<Quotation>> dateQuotationListMap = quotationList.stream().collect(Collectors.groupingBy(x -> ymdFormat.format(x.getDate())));
        List<String> sortDateList = dateQuotationListMap.keySet().stream().sorted(Comparator.comparing(x -> Integer.parseInt(x.replaceAll("-","")))).collect(Collectors.toList());
        //映射行情列表
        Map<String,Quotation> back5QuotationMap = dateQuotationListMap.get(sortDateList.get(0)).stream().collect(Collectors.toMap(Quotation::getCode,Function.identity(),(o,n) -> n));
        Map<String,Quotation> back4QuotationMap = dateQuotationListMap.get(sortDateList.get(1)).stream().collect(Collectors.toMap(Quotation::getCode,Function.identity(),(o,n) -> n));
        Map<String,Quotation> back3QuotationMap = dateQuotationListMap.get(sortDateList.get(2)).stream().collect(Collectors.toMap(Quotation::getCode,Function.identity(),(o,n) -> n));
        Map<String,Quotation> back2QuotationMap = dateQuotationListMap.get(sortDateList.get(3)).stream().collect(Collectors.toMap(Quotation::getCode,Function.identity(),(o,n) -> n));
        Map<String,Quotation> back1QuotationMap = dateQuotationListMap.get(sortDateList.get(4)).stream().collect(Collectors.toMap(Quotation::getCode,Function.identity(),(o,n) -> n));

        //历史均线列表
        List<AvgPrice> avgPriceList = avgPriceMapper.selectListByRangeOfNDay(strDate,6);
        avgPriceList = avgPriceList.stream().filter(x -> x.getAvg30() != null).collect(Collectors.toList());
        //根据日期分组
        Map<String,List<AvgPrice>> dateAvgListMap = avgPriceList.stream().collect(Collectors.groupingBy(x -> ymdFormat.format(x.getDate())));
        List<String> sortAvgDateList = dateAvgListMap.keySet().stream().sorted(Comparator.comparing(x -> Integer.parseInt(x.replaceAll("-","")))).collect(Collectors.toList());
        //映射行情列表
        Map<String,AvgPrice> back6AvgMap = dateAvgListMap.get(sortAvgDateList.get(0)).stream().collect(Collectors.toMap(AvgPrice::getCode,Function.identity(),(o,n) -> n));
        Map<String,AvgPrice> back5AvgMap = dateAvgListMap.get(sortAvgDateList.get(1)).stream().collect(Collectors.toMap(AvgPrice::getCode,Function.identity(),(o,n) -> n));
        Map<String,AvgPrice> back4AvgMap = dateAvgListMap.get(sortAvgDateList.get(2)).stream().collect(Collectors.toMap(AvgPrice::getCode,Function.identity(),(o,n) -> n));
        Map<String,AvgPrice> back3AvgMap = dateAvgListMap.get(sortAvgDateList.get(3)).stream().collect(Collectors.toMap(AvgPrice::getCode,Function.identity(),(o,n) -> n));
        Map<String,AvgPrice> back2AvgMap = dateAvgListMap.get(sortAvgDateList.get(4)).stream().collect(Collectors.toMap(AvgPrice::getCode,Function.identity(),(o,n) -> n));
        Map<String,AvgPrice> back1AvgMap = dateAvgListMap.get(sortAvgDateList.get(5)).stream().collect(Collectors.toMap(AvgPrice::getCode,Function.identity(),(o,n) -> n));
        //以今天的日期为标准，提取推荐列表
        List<Quotation> resultList = new ArrayList<>();
        for (Quotation q : dateQuotationListMap.get(sortDateList.get(5))) {
//            if ("sz300406".equals(q.getCode())) {
//                log.info("===========================测试===========================");
//            }
            //股票价格在5~30
            if (q.getCurrent().doubleValue() < 4 || q.getCurrent().doubleValue() > 100) {
                continue;
            }
            //交易额小于5000w的过滤掉
            if (q.getVolumeAmt().doubleValue() < 80000000) {
                continue;
            }
            //今天涨幅必须在1~8个点
            if (q.getOffsetRate().doubleValue() < 3 && q.getOffsetRate().doubleValue() > 7) {
                continue;
            }
            //当日或者上个交易日放量2倍的都过滤掉
            double vRate = 2.1;
            if (q.getVolumeAmt().doubleValue() > back1QuotationMap.get(q.getCode()).getVolumeAmt().doubleValue() * vRate || back1QuotationMap.get(q.getCode()).getVolumeAmt().doubleValue() > back2QuotationMap.get(q.getCode()).getVolumeAmt().doubleValue() * vRate) {
                continue;
            }
            if (back6AvgMap.containsKey(q.getCode()) && back5AvgMap.containsKey(q.getCode()) && back4AvgMap.containsKey(q.getCode()) && back3AvgMap.containsKey(q.getCode()) && back2AvgMap.containsKey(q.getCode()) && back1AvgMap.containsKey(q.getCode())) {
                int downCount = 0;
                if (back6AvgMap.get(q.getCode()).getAvg5().doubleValue() > back5AvgMap.get(q.getCode()).getAvg5().doubleValue()) {
                    ++downCount;
                }
                if (back5AvgMap.get(q.getCode()).getAvg5().doubleValue() > back4AvgMap.get(q.getCode()).getAvg5().doubleValue()) {
                    ++downCount;
                }
                if (back4AvgMap.get(q.getCode()).getAvg5().doubleValue() > back3AvgMap.get(q.getCode()).getAvg5().doubleValue()) {
                    ++downCount;
                }
                if (back3AvgMap.get(q.getCode()).getAvg5().doubleValue() > back2AvgMap.get(q.getCode()).getAvg5().doubleValue()) {
                    ++downCount;
                }
                if (back2AvgMap.get(q.getCode()).getAvg5().doubleValue() > back1AvgMap.get(q.getCode()).getAvg5().doubleValue()) {
                    ++downCount;
                }
                //5日线下降趋势，即有2天以上在下跌
                if (downCount >= 2) {
                    //必须有3天以上下跌
                    int dCount = 0;
                    if (back5QuotationMap.get(q.getCode()).getOffsetRate().doubleValue() <= 0) {
                        ++dCount;
                    }
                    if (back4QuotationMap.get(q.getCode()).getOffsetRate().doubleValue() <= 0) {
                        ++dCount;
                    }
                    if (back3QuotationMap.get(q.getCode()).getOffsetRate().doubleValue() <= 0) {
                        ++dCount;
                    }
                    if (back2QuotationMap.get(q.getCode()).getOffsetRate().doubleValue() <= 0) {
                        ++dCount;
                    }
                    if (back1QuotationMap.get(q.getCode()).getOffsetRate().doubleValue() <= 0) {
                        ++dCount;
                    }
                    //必须有2天以上下跌
                    if (dCount >= 2) {
                        //最近3天必须含有十字星
                        if (back3QuotationMap.get(q.getCode()).getOffsetRate().doubleValue() >= -0.5 && back3QuotationMap.get(q.getCode()).getOffsetRate().doubleValue() <= 0 ||
                                back2QuotationMap.get(q.getCode()).getOffsetRate().doubleValue() >= -0.5 && back2QuotationMap.get(q.getCode()).getOffsetRate().doubleValue() <= 0 ||
                                back1QuotationMap.get(q.getCode()).getOffsetRate().doubleValue() >= -0.5 && back1QuotationMap.get(q.getCode()).getOffsetRate().doubleValue() <= 0) {
                            //必须有2天被均线包裹
                            int cCount = 0;
                            //均价必须包含在N日均价内
                            //必须全部不能离线，离线直接过滤掉
                            List<Double> hisAvgList = new ArrayList<>();
                            hisAvgList.add(back6AvgMap.get(q.getCode()).getAvg5().doubleValue());
                            hisAvgList.add(back6AvgMap.get(q.getCode()).getAvg10().doubleValue());
                            hisAvgList.add(back6AvgMap.get(q.getCode()).getAvg20().doubleValue());
                            hisAvgList.add(back6AvgMap.get(q.getCode()).getAvg30().doubleValue());
                            hisAvgList = hisAvgList.stream().sorted().collect(Collectors.toList());
                            //当天均价在均线内
                            if (back5QuotationMap.get(q.getCode()).getHigh().doubleValue() <= hisAvgList.get(0)) {
                                continue;
                            }
                            //有大阴线，一阴破4线的直接过滤掉，如002556的2020-07-16日
                            if (back5QuotationMap.get(q.getCode()).getOffsetRate().doubleValue() < 0 && back5QuotationMap.get(q.getCode()).getLow().doubleValue() < hisAvgList.get(0) && back5QuotationMap.get(q.getCode()).getHigh().doubleValue() > hisAvgList.get(3)) {
                                continue;
                            }
                            if (back6AvgMap.get(q.getCode()).getAvg().doubleValue() >= hisAvgList.get(0) * 1.01 && back6AvgMap.get(q.getCode()).getAvg().doubleValue() <= hisAvgList.get(3) * 1.01) {
                                ++cCount;
                            }
                            hisAvgList = new ArrayList<>();
                            hisAvgList.add(back5AvgMap.get(q.getCode()).getAvg5().doubleValue());
                            hisAvgList.add(back5AvgMap.get(q.getCode()).getAvg10().doubleValue());
                            hisAvgList.add(back5AvgMap.get(q.getCode()).getAvg20().doubleValue());
                            hisAvgList.add(back5AvgMap.get(q.getCode()).getAvg30().doubleValue());
                            hisAvgList = hisAvgList.stream().sorted().collect(Collectors.toList());
                            if (back4QuotationMap.get(q.getCode()).getHigh().doubleValue() <= hisAvgList.get(0)) {
                                continue;
                            }
                            if (back4QuotationMap.get(q.getCode()).getOffsetRate().doubleValue() < 0 && back4QuotationMap.get(q.getCode()).getLow().doubleValue() < hisAvgList.get(0) && back4QuotationMap.get(q.getCode()).getHigh().doubleValue() > hisAvgList.get(3)) {
                                continue;
                            }
                            if (back5AvgMap.get(q.getCode()).getAvg().doubleValue() >= hisAvgList.get(0) * 1.01 && back5AvgMap.get(q.getCode()).getAvg().doubleValue() <= hisAvgList.get(3) * 1.01) {
                                ++cCount;
                            }
                            hisAvgList = new ArrayList<>();
                            hisAvgList.add(back4AvgMap.get(q.getCode()).getAvg5().doubleValue());
                            hisAvgList.add(back4AvgMap.get(q.getCode()).getAvg10().doubleValue());
                            hisAvgList.add(back4AvgMap.get(q.getCode()).getAvg20().doubleValue());
                            hisAvgList.add(back4AvgMap.get(q.getCode()).getAvg30().doubleValue());
                            hisAvgList = hisAvgList.stream().sorted().collect(Collectors.toList());
                            if (back3QuotationMap.get(q.getCode()).getHigh().doubleValue() <= hisAvgList.get(0)) {
                                continue;
                            }
                            if (back3QuotationMap.get(q.getCode()).getOffsetRate().doubleValue() < 0 && back3QuotationMap.get(q.getCode()).getLow().doubleValue() < hisAvgList.get(0) && back3QuotationMap.get(q.getCode()).getHigh().doubleValue() > hisAvgList.get(3)) {
                                continue;
                            }
                            if (back4AvgMap.get(q.getCode()).getAvg().doubleValue() >= hisAvgList.get(0) * 1.01 && back4AvgMap.get(q.getCode()).getAvg().doubleValue() <= hisAvgList.get(3) * 1.01) {
                                ++cCount;
                            }
                            hisAvgList = new ArrayList<>();
                            hisAvgList.add(back3AvgMap.get(q.getCode()).getAvg5().doubleValue());
                            hisAvgList.add(back3AvgMap.get(q.getCode()).getAvg10().doubleValue());
                            hisAvgList.add(back3AvgMap.get(q.getCode()).getAvg20().doubleValue());
                            hisAvgList.add(back3AvgMap.get(q.getCode()).getAvg30().doubleValue());
                            hisAvgList = hisAvgList.stream().sorted().collect(Collectors.toList());
                            if (back2QuotationMap.get(q.getCode()).getHigh().doubleValue() <= hisAvgList.get(0)) {
                                continue;
                            }
                            if (back2QuotationMap.get(q.getCode()).getOffsetRate().doubleValue() < 0 && back2QuotationMap.get(q.getCode()).getLow().doubleValue() < hisAvgList.get(0) && back2QuotationMap.get(q.getCode()).getHigh().doubleValue() > hisAvgList.get(3)) {
                                continue;
                            }
                            if (back3AvgMap.get(q.getCode()).getAvg().doubleValue() >= hisAvgList.get(0) * 1.01 && back3AvgMap.get(q.getCode()).getAvg().doubleValue() <= hisAvgList.get(3) * 1.01) {
                                ++cCount;
                            }
                            hisAvgList = new ArrayList<>();
                            hisAvgList.add(back2AvgMap.get(q.getCode()).getAvg5().doubleValue());
                            hisAvgList.add(back2AvgMap.get(q.getCode()).getAvg10().doubleValue());
                            hisAvgList.add(back2AvgMap.get(q.getCode()).getAvg20().doubleValue());
                            hisAvgList.add(back2AvgMap.get(q.getCode()).getAvg30().doubleValue());
                            hisAvgList = hisAvgList.stream().sorted().collect(Collectors.toList());
                            if (back1QuotationMap.get(q.getCode()).getHigh().doubleValue() <= hisAvgList.get(0)) {
                                continue;
                            }
                            //连续3天五日线下移的过滤掉，每天比前一天最低点低，并且收盘价小于上一天的开盘价+0.05
                            if (back3QuotationMap.get(q.getCode()).getLow().doubleValue() > back2QuotationMap.get(q.getCode()).getLow().doubleValue() && back2QuotationMap.get(q.getCode()).getLow().doubleValue() > back1QuotationMap.get(q.getCode()).getLow().doubleValue() &&
                                    back3QuotationMap.get(q.getCode()).getClose().doubleValue() + 0.05 >= back2QuotationMap.get(q.getCode()).getOpen().doubleValue()
                            ) {
                                continue;
                            }
                            //昨天跌幅大于4%过滤掉
                            if (back1QuotationMap.get(q.getCode()).getOffsetRate().doubleValue() <= -4) {
                                continue;
                            }
                            if (back2AvgMap.get(q.getCode()).getAvg().doubleValue() >= hisAvgList.get(0) * 1.01 && back2AvgMap.get(q.getCode()).getAvg().doubleValue() <= hisAvgList.get(3) * 1.01) {
                                ++cCount;
                            }
                            if (cCount >= 2) {
                                //今天必须穿过2条均线
                                hisAvgList = new ArrayList<>();
                                hisAvgList.add(back1AvgMap.get(q.getCode()).getAvg5().doubleValue());
                                hisAvgList.add(back1AvgMap.get(q.getCode()).getAvg10().doubleValue());
                                hisAvgList.add(back1AvgMap.get(q.getCode()).getAvg20().doubleValue());
                                hisAvgList.add(back1AvgMap.get(q.getCode()).getAvg30().doubleValue());
                                hisAvgList = hisAvgList.stream().sorted().collect(Collectors.toList());
                                //今天必须穿过2条均线
                                if (q.getOpen().doubleValue() <= hisAvgList.get(0) && q.getClose().doubleValue() >= hisAvgList.get(1) ||
                                        q.getOpen().doubleValue() <= hisAvgList.get(1) && q.getClose().doubleValue() >= hisAvgList.get(2) ||
                                        q.getOpen().doubleValue() <= hisAvgList.get(2) && q.getClose().doubleValue() >= hisAvgList.get(3)) {
                                    resultList.add(q);
                                }
                            }
                        }
                    }
                }
            }
        }
        //保存入库
        try {
            //先删除当天的推荐记录
            recommandMapper.deleteByDate(strDate,GlobalConstant.RECOMMAND_TYPE_BACK);
            List<CodeNameVo> dataList = new ArrayList<>();
            resultList.forEach(q -> {
                dataList.add(new CodeNameVo(q.getCode(),q.getName()));
            });
            if (resultList.size() > 0) {
                //重新添加推荐对象
                Recommand newRecommand = new Recommand();
                newRecommand.setId(genIdUtil.nextId());
                newRecommand.setDate(date);
                newRecommand.setType(GlobalConstant.RECOMMAND_TYPE_BACK);
                newRecommand.setDataList(JSON.toJSONString(dataList));
                recommandMapper.insertSelective(newRecommand);
            }
        } catch (Exception e) {
            log.error("=======================推荐列表为空，忽略本次添加=======================");
            e.printStackTrace();
        }
        return resultList;
    }

    /**
     * 查询N天内连续涨跌数据列表
     * @param startDate
     * @param maxDays
     * @return
     */
    public Map<String,Object> queryUpwardList(String startDate,int maxDays) {
        SimpleDateFormat ymdFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar calendar= Calendar.getInstance();
        try {
            calendar.setTime(ymdFormat.parse(startDate));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        calendar.add(Calendar.DAY_OF_MONTH,-maxDays * 2);
        //查询行情列表
        List<Quotation> dbQuotationList = quotationMapper.selectListByDateRange(ymdFormat.format(calendar.getTime()),startDate);
        //根据code分组
        Map<String,List<Quotation>> codeQuotationListMap = dbQuotationList.stream().collect(Collectors.groupingBy(Quotation::getCode));
        //查询涨跌列表
        List<UpDown> dbLimitUpDownList = upDownMapper.selectListByDateRange(ymdFormat.format(calendar.getTime()),startDate);
        //获取startDate均价数据
        List<AvgPrice> avgPriceList = avgPriceMapper.selectListByDateRange(ymdFormat.format(calendar.getTime()),startDate);

        //获取全部的字典数据
        List<Dict> dictList = dictMapper.selectListByType(GlobalConstant.DICT_TYPE_STOCK);
        Map<String,Dict> codeDictMap = dictList.stream().collect(Collectors.toMap(Dict::getCode, Function.identity()));

        Map<String,Object> fetchResultMap = null;
        Map<String,Object> resultMap = new LinkedHashMap<>();
        if (dbLimitUpDownList.size() > maxDays) {
            //按日期降序排序后，获取前maxDays条
            List<UpDown> usedUpDownList = dbLimitUpDownList.stream().sorted(Comparator.comparing(UpDown::getDate).reversed()).limit(maxDays).collect(Collectors.toList());

            int loopIndex = 0;
            Set<String> upCodeSet = new HashSet<>();
            Set<String> downCodeSet = new HashSet<>();
            int limitDay = 1;
            double downRate = 0.6d;
            int minDownDay = 0;
            int downDays = 0;
            for (UpDown upDown : usedUpDownList) {
                if (loopIndex < limitDay) {
                    if (upCodeSet.size() > 0) {
                        upCodeSet.retainAll(Arrays.asList(upDown.getUpList().split(",")));
                    } else {
                        upCodeSet.addAll(Arrays.asList(upDown.getUpList().split(",")));
                    }
                } else {
                    minDownDay = (int)((maxDays - limitDay) * downRate);
                    if (downCodeSet.size() > 0) {
                        if (downDays < minDownDay) {
                            downCodeSet.retainAll(Arrays.asList(upDown.getDownList().split(",")));
                        } else {
                            //达到最小下跌天数以后，后面的直接忽略继续下一个
                            continue;
                        }
                    } else {
                        downCodeSet.addAll(Arrays.asList(upDown.getDownList().split(",")));
                    }
                    downDays++;
                }
                loopIndex++;
            }

            //============================初始化根据下跌转上涨过滤============================
            //获取上涨和下跌的交集部分
            List<String> r1CodeList = new ArrayList<>(upCodeSet);
            r1CodeList.retainAll(downCodeSet);

            //============================根据5日均价过滤============================

            List<AvgPrice> curAvgPriceList = avgPriceList.stream().filter(x -> ymdFormat.format(x.getDate()).equals(startDate)).collect(Collectors.toList());
            Map<String,AvgPrice> codeAvgMap = curAvgPriceList.stream().collect(Collectors.toMap(AvgPrice::getCode, Function.identity()));

            //获取startDate行情数据
            List<Quotation> quotationList = dbQuotationList.stream().filter(x -> ymdFormat.format(x.getDate()).equals(startDate)).collect(Collectors.toList());
            Map<String,Quotation> codeQuotationMap = quotationList.stream().collect(Collectors.toMap(Quotation::getCode, Function.identity()));

            List<String> r2CodeList = r1CodeList.stream().filter(x -> codeQuotationMap.get(x) != null && codeAvgMap.get(x) != null && codeAvgMap.get(x).getAvg() != null && codeAvgMap.get(x).getAvg5() != null && codeAvgMap.get(x).getAvg().compareTo(codeAvgMap.get(x).getAvg5()) > 0).collect(Collectors.toList());

            //============================根据涨跌趋势过滤过滤,5日线 》 10日线认为是上涨趋势============================
            List<String> r3CodeList = r2CodeList.stream().filter(x -> codeAvgMap.get(x).getAvg5() != null && codeAvgMap.get(x).getAvg10() != null && codeAvgMap.get(x).getAvg5().compareTo(codeAvgMap.get(x).getAvg10()) > 0).collect(Collectors.toList());

            //构建结果数据
            Map<String,Object> rMap = null;
            List<Map<String,String>> rDataList = null;
            Map<String,String> tmpMap = null;

            //构建初始化提取后的结果
            if (r1CodeList.size() > 0) {
                rMap = new HashMap<>();
                rDataList = new ArrayList<>();
                for (String code : r1CodeList) {
                    tmpMap = new HashMap<>();
                    tmpMap.put("code",code);
                    if (codeDictMap.containsKey(code)) {
                        tmpMap.put("name",codeDictMap.get(code).getName());
                    } else {
                        tmpMap.put("name","未知");
                    }
                    rDataList.add(tmpMap);
                }
                rMap.put("data",rDataList);
                rMap.put("count",rDataList.size());
                resultMap.put("init",rMap);
            }

            //构建根据均价过滤后的结果
            if (r2CodeList.size() > 0) {
                rMap = new HashMap<>();
                rDataList = new ArrayList<>();
                for (String code : r2CodeList) {
                    tmpMap = new HashMap<>();
                    tmpMap.put("code",code);
                    if (codeDictMap.containsKey(code)) {
                        tmpMap.put("name",codeDictMap.get(code).getName());
                    } else {
                        continue;
                    }
                    rDataList.add(tmpMap);
                }
                rMap.put("data",rDataList);
                rMap.put("count",rDataList.size());
                resultMap.put("avg",rMap);
            }

            //构建根据趋势过滤后的结果
            //只从价值股里面提取
            List<SysDict> sysDictList = sysDictMapper.selectByType(GlobalConstant.SYS_DICT_TYPE_QUALITY_STOCKS);
            List<String> qualityCodeDictList = sysDictList.stream().map(SysDict::getValue).collect(Collectors.toList());
            r3CodeList = r3CodeList.stream().filter(qualityCodeDictList::contains).collect(Collectors.toList());
            if (r3CodeList.size() > 0) {
                rMap = new HashMap<>();
                rDataList = new ArrayList<>();
                for (String code : r3CodeList) {
                    tmpMap = new HashMap<>();
                    tmpMap.put("code",code);
                    if (codeDictMap.containsKey(code)) {
                        tmpMap.put("name",codeDictMap.get(code).getName());
                    } else {
                        continue;
                    }
                    rDataList.add(tmpMap);
                }
                rMap.put("data",rDataList);
                rMap.put("count",rDataList.size());
                resultMap.put("recommand",rMap);
            }

            //根据下跌收阴十字星策略提取
            fetchResultMap = this.fetchDownDoji(maxDays,dbLimitUpDownList,codeQuotationListMap,codeDictMap);
            if (fetchResultMap != null) {
                resultMap.put("阴十字星",fetchResultMap);
            }

            //根据早晨十字星策略提取
            fetchResultMap = this.fetchMorningDoji(r1CodeList,codeQuotationListMap,codeDictMap);
            if (fetchResultMap != null) {
                resultMap.put("早晨十字星",fetchResultMap);
            }

            //提取最近3天5日线和10日线有交叉的
            fetchResultMap = this.fetchCrossWith510(maxDays,avgPriceList,codeQuotationListMap,codeDictMap);
            if (fetchResultMap != null) {
                resultMap.put("交叉",fetchResultMap);
            }
        } else {
            log.error("============================当前连续涨跌数据不足{}天============================",maxDays);
        }

        fetchResultMap = this.fetchCross(avgPriceList,codeQuotationListMap,codeDictMap);
        if (fetchResultMap != null) {
            resultMap.put("三线交叉",fetchResultMap);
        }
        return resultMap;
    }

    /**
     * 查询某天的放量列表
     * @param startDate
     * @param maxDays
     * @return
     */
    public Map<String,Object> queryVolumeMap(String startDate,int maxDays) {
        SimpleDateFormat ymdFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(ymdFormat.parse(startDate));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        List<Recommand> recommandList = recommandMapper.selectByDateCount(startDate,maxDays,GlobalConstant.RECOMMAND_TYPE_VOLUME);
        Map<String,Object> dateVolumeMap = new HashMap<>();
        recommandList.forEach(v -> {
            dateVolumeMap.put(ymdFormat.format(v.getDate()),JSON.parseArray(v.getDataList(), VolumeVo.class));
        });
        return dateVolumeMap;
    }

    /**
     * 提取N天下跌后收阴字十字星走势的股票
     * 详细见：阴字十字星即(收盘价-初始化价) / 初始价 >= -0.6
     * @return
     */
    public Map<String,Object> fetchCross(List<AvgPrice> AvgPriceList,Map<String,List<Quotation>> codeQuotationMap,Map<String,Dict> codeDictMap) {
        //先根据code分组
        Map<String,List<AvgPrice>> codeAvgListMap = AvgPriceList.stream().collect(Collectors.groupingBy(AvgPrice::getCode));
        List<String> targetCodeList = new ArrayList<>();
        int nDays = 4;
        codeAvgListMap.forEach((code,hisAvgList) -> {
            if (codeQuotationMap.containsKey(code)) {
                //按时间倒叙排列选取5天
                List<AvgPrice> curHisAvgList = hisAvgList.stream().filter(x -> x.getAvg30() != null).sorted(Comparator.comparing(AvgPrice::getDate).reversed()).limit(nDays).collect(Collectors.toList());
                if (curHisAvgList.size() == nDays) {
                    //5天之内有3线相交的都提取出来
//                curHisAvgList = hisAvgList.stream().sorted(Comparator.comparing(AvgPrice::getDate)).collect(Collectors.toList());
                    //5日线追上10日线交叉
                    for (int i = 2; i < curHisAvgList.size(); i++) {
                        if (curHisAvgList.get(i - 2).getAvg5().compareTo(curHisAvgList.get(i - 2).getAvg10()) < 0 && curHisAvgList.get(i - 1).getAvg5().compareTo(curHisAvgList.get(i - 1).getAvg10()) > 0 &&
                                //同一天相交
                                (((curHisAvgList.get(i - 2).getAvg5().compareTo(curHisAvgList.get(i - 2).getAvg20()) < 0 && curHisAvgList.get(i - 1).getAvg5().compareTo(curHisAvgList.get(i - 1).getAvg20()) > 0 ||
                                        curHisAvgList.get(i - 2).getAvg10().compareTo(curHisAvgList.get(i - 2).getAvg20()) < 0 && curHisAvgList.get(i - 1).getAvg10().compareTo(curHisAvgList.get(i - 1).getAvg20()) > 0)) ||
                                        //隔一天相交
                                        ((curHisAvgList.get(i - 1).getAvg5().compareTo(curHisAvgList.get(i - 1).getAvg20()) < 0 && curHisAvgList.get(i).getAvg5().compareTo(curHisAvgList.get(i).getAvg20()) > 0 ||
                                                curHisAvgList.get(i - 1).getAvg10().compareTo(curHisAvgList.get(i - 1).getAvg20()) < 0 && curHisAvgList.get(i).getAvg10().compareTo(curHisAvgList.get(i).getAvg20()) > 0)) &&
                                                //必须有一根线是直线上涨的
                                                //5日线向上
                                                ((curHisAvgList.get(i - 2).getAvg5().compareTo(curHisAvgList.get(i - 1).getAvg5()) < 0 && curHisAvgList.get(i - 1).getAvg5().compareTo(curHisAvgList.get(i).getAvg5()) < 0) ||
                                                //月线向上
                                                (curHisAvgList.get(i - 2).getAvg30().compareTo(curHisAvgList.get(i - 1).getAvg30()) < 0 && curHisAvgList.get(i - 1).getAvg30().compareTo(curHisAvgList.get(i).getAvg30()) < 0))
                                )

                        ) {
                            targetCodeList.add(code);
                            break;
                        }
                    }
                }
            }
        });

        //构建最终结果
        Map<String,String> tmpMap = null;
        List<Map<String,String>> targetList = new ArrayList<>();
        for (String code : targetCodeList) {
            tmpMap = new HashMap<>();
            tmpMap.put("code",code);
            if (codeDictMap.containsKey(code)) {
                tmpMap.put("name",codeDictMap.get(code).getName());
                targetList.add(tmpMap);
            }
        }
        if (targetList.size() > 0) {
            Map<String,Object> resultMap = new HashMap<>();
            resultMap.put("data",targetList);
            resultMap.put("count",targetList.size());
            return resultMap;
        }
        return null;
    }

    /**
     * 提取N天下跌后收阴字十字星走势的股票
     * 详细见：阴字十字星即(收盘价-初始化价) / 初始价 >= -0.6
     * @return
     */
    public Map<String,Object> fetchCrossWith510(int maxDays,List<AvgPrice> avgPriceList,Map<String,List<Quotation>> codeQuotationMap,Map<String,Dict> codeDictMap) {
        //先根据code分组
        Map<String,List<AvgPrice>> codeAvgListMap = avgPriceList.stream().collect(Collectors.groupingBy(AvgPrice::getCode));
        List<String> targetCodeList = new ArrayList<>();
        codeAvgListMap.forEach((code,hisAvgList) -> {
            //按时间倒叙排列选取5天
            List<AvgPrice> curAvgList = hisAvgList.stream().sorted(Comparator.comparing(AvgPrice::getDate).reversed()).limit(maxDays).collect(Collectors.toList());
            double lVal = 0d;
            double sVal = 0d;
//            if ("sz002949".equals(code)) {
//                System.out.println(11);
//            }
            int maxRangeCount = (int)(maxDays * 0.8);
            int rangeCount = 0;
            boolean isBestRange = false;
            for (int i  = maxDays - 1 ; i > 0 ; i--) {
                if (curAvgList.size() == maxDays && curAvgList.get(i-1).getAvg5() != null && curAvgList.get(i-1).getAvg10() != null) {
                    if (curAvgList.get(i-1).getAvg5().compareTo(curAvgList.get(i-1).getAvg10()) >= 0) {
                        lVal = curAvgList.get(i-1).getAvg5().doubleValue();
                        sVal = curAvgList.get(i-1).getAvg10().doubleValue();
                    } else  {
                        lVal = curAvgList.get(i-1).getAvg10().doubleValue();
                        sVal = curAvgList.get(i-1).getAvg5().doubleValue();
                    }
//                    if ("sz002949".equals(code)) {
//                        System.out.println((lVal - sVal) / lVal);
//                    }
                    if ((lVal - sVal) / lVal <= 0.005) {
                        rangeCount++;
                        if (rangeCount >= maxRangeCount) {
                            isBestRange = true;
                            break;
                        }
                    }
                }
            }
            if (isBestRange) {
                //日线和10日线的变化率必须在1%以内
                targetCodeList.add(curAvgList.get(0).getCode());
            }
        });

        //TODO 5日均价和每日均价波动在2%以内

        //根据成交量提取
        List<String> newTargetCodeList = new ArrayList<>();
        int maxDojiCount = (int)(maxDays * 0.6);
        targetCodeList.forEach(code -> {
            if (codeQuotationMap.containsKey(code)) {
                List<Quotation> curQuotationList = codeQuotationMap.get(code).stream().sorted(Comparator.comparing(Quotation::getDate).reversed()).limit(maxDays - 1).collect(Collectors.toList());
                BigDecimal maxTradeAmt = BigDecimal.valueOf(30000000);
                boolean isBest = true;
                boolean isDojiOk = false;
                int dojiCount = 0;
                String []quotationDataArr = null;
                BigDecimal limitRate = BigDecimal.valueOf(0.005);
                //十字星数量达到60%
                for (Quotation quotation : curQuotationList) {
                    if (quotation.getVolumeAmt().compareTo(maxTradeAmt) > 0) {
                        isBest = false;
                        break;
                    }
                    if (quotation.getOpen().doubleValue() > 0 && (quotation.getCurrent().subtract(quotation.getOpen())).divide(quotation.getOpen(),3).compareTo(limitRate) <= 0) {
                        dojiCount++;
                    }
                    if (dojiCount >= maxDojiCount) {
                        isDojiOk = true;
                        break;
                    }
                }
                if (isBest && isDojiOk) {
                    newTargetCodeList.add(code);
                }
            }
        });

        //构建最终结果
        Map<String,String> tmpMap = null;
        List<Map<String,String>> targetList = new ArrayList<>();
        for (String code : newTargetCodeList) {
            tmpMap = new HashMap<>();
            tmpMap.put("code",code);
            if (codeDictMap.containsKey(code)) {
                tmpMap.put("name",codeDictMap.get(code).getName());
                targetList.add(tmpMap);
            }
        }
        if (targetList.size() > 0) {
            Map<String,Object> resultMap = new HashMap<>();
            resultMap.put("data",targetList);
            resultMap.put("count",targetList.size());
            return resultMap;
        }
        return null;
    }

    /**
     * 提取早晨十字星走势的股票
     * 详细见：https://cj.sina.com.cn/article/detail/5909404541/406694
     * @return
     */
    public Map<String,Object> fetchMorningDoji(List<String> codeList,Map<String,List<Quotation>> codeQuotationMap,Map<String,Dict> codeDictMap) {
        //直接从code列表中提取
        List<Quotation> quotationList = null;

        Quotation lastQuotation = null;
        Quotation midQuotation = null;
        Quotation curQuotation = null;
        List<String> targetCodeList = new ArrayList<>();
        for (String code : codeList) {
            quotationList = codeQuotationMap.get(code);
            //选择最近的3天并根据日期排序，涨跌率以前一天的收盘价为参考，故需要多往前获一天，此处不需要
            quotationList = quotationList.stream().sorted(Comparator.comparing(Quotation::getDate).reversed()).limit(3).collect(Collectors.toList());
            if (quotationList.size() < 3) {
                continue;
            }
            //获取N天的行情数据
           lastQuotation = quotationList.get(2);
           midQuotation = quotationList.get(1);
           curQuotation = quotationList.get(0);

            if (lastQuotation.getClose().doubleValue() < lastQuotation.getOpen().doubleValue()) {
                //第二天需收阴字十字星，即：最低价 < 收盘价 < 开盘价 && 下跌率在0.6以内
                log.debug("收盘价:{} 初始价:{} 变化价:{}",midQuotation.getCurrent().doubleValue(),midQuotation.getInit().doubleValue(),midQuotation.getCurrent().doubleValue() - lastQuotation.getInit().doubleValue());
                log.debug("上涨率:{}",(midQuotation.getCurrent().doubleValue() - midQuotation.getInit().doubleValue()) / midQuotation.getInit().doubleValue());
                if (midQuotation.getCurrent().doubleValue() < midQuotation.getOpen().doubleValue() &&
                        midQuotation.getHigh().doubleValue() < midQuotation.getCurrent().doubleValue() &&
                        (midQuotation.getCurrent().doubleValue() - midQuotation.getOpen().doubleValue()) / midQuotation.getOpen().doubleValue() <= GlobalConstant.MAX_DOJI_LIMIT_NUM ||//阳十字星
                        (midQuotation.getCurrent().doubleValue() - midQuotation.getOpen().doubleValue()) / midQuotation.getOpen().doubleValue() >= -GlobalConstant.MAX_DOJI_LIMIT_NUM) {//阴十字星
                    //当天出现的小阳线需要包含在最前一天内，并追加成交量过滤
                    if (curQuotation.getCurrent().doubleValue() > curQuotation.getOpen().doubleValue() && curQuotation.getOpen().doubleValue() >= lastQuotation.getClose().doubleValue() && curQuotation.getCurrent().doubleValue() <= lastQuotation.getOpen().doubleValue() && (int)curQuotation.getVolumeAmt().doubleValue() > GlobalConstant.MIN_TRADE_AMOUNT) {
                        targetCodeList.add(code);
                    }
                }
            }
        }
        //构建最终结果
        Map<String,String> tmpMap = null;
        List<Map<String,String>> targetList = new ArrayList<>();
        for (String code : targetCodeList) {
            tmpMap = new HashMap<>();
            tmpMap.put("code",code);
            if (codeDictMap.containsKey(code)) {
                tmpMap.put("name",codeDictMap.get(code).getName());
                targetList.add(tmpMap);
            }
        }
        if (targetList.size() > 0) {
            Map<String,Object> resultMap = new HashMap<>();
            resultMap.put("data",targetList);
            resultMap.put("count",targetList.size());
            return resultMap;
        }
        return null;
    }

    /**
     * 提取N天下跌后收阴字十字星走势的股票
     * 详细见：阴字十字星即(收盘价-初始化价) / 初始价 >= -0.6
     * @return
     */
    public Map<String,Object> fetchDownDoji(int maxDays,List<UpDown> upDownList,Map<String,List<Quotation>> codeQuotationMap,Map<String,Dict> codeDictMap) {
        Set<String> downCodeSet = new HashSet<>();
        int limitDay = 1;
        double downRate = 0.6d;
        //最少下跌天数 = 目标天数 * 下跌率
        int minDownDay = (int)((maxDays - limitDay) * downRate);
        int downDays = 0;
        for (UpDown upDown : upDownList) {
            if (downCodeSet.size() > 0) {
                if (downDays < minDownDay) {
                    downCodeSet.retainAll(Arrays.asList(upDown.getDownList().split(",")));
                } else {
                    //达到最小下跌天数以后，后面的直接忽略继续下一个
                    continue;
                }
            } else {
                downCodeSet.addAll(Arrays.asList(upDown.getDownList().split(",")));
            }
            downDays++;
        }

        //根据下跌股票编码提取出股票行情数据，并检测阴字十字星
        List<Quotation> quotationList = null;
        Quotation quotation = null;
        List<String> targetCodeList = new ArrayList<>();
        for (String code : downCodeSet) {
            quotationList = codeQuotationMap.get(code);
            if (quotationList == null || quotationList.size() == 0) {
                continue;
            }
            quotation = quotationList.stream().max(Comparator.comparing(Quotation::getDate)).get();
//            if ("sh600758".equals(code)) {
//                log.info("收盘价:{} 初始价:{} 变化价:{}",Double.parseDouble(curQuotationDataArr[3]),Double.parseDouble(curQuotationDataArr[1]),Double.parseDouble(curQuotationDataArr[3]) - Double.parseDouble(curQuotationDataArr[1]));
//                log.info("上涨率:{}",(Double.parseDouble(curQuotationDataArr[3]) - Double.parseDouble(curQuotationDataArr[1])) / Double.parseDouble(curQuotationDataArr[1]));
//            }

            if (quotation.getCurrent().doubleValue() <= quotation.getOpen().doubleValue() &&
                    quotation.getLow().doubleValue() < quotation.getCurrent().doubleValue() &&
                    (quotation.getCurrent().doubleValue() - quotation.getOpen().doubleValue()) / quotation.getOpen().doubleValue() >= -GlobalConstant.DOJI_LIMIT_NUM) {//阴字十字星
                //按照交易金额过滤，股价大于3元
                if (quotation.getCurrent().doubleValue() >= GlobalConstant.MIN_STOCK_PRICE && (int)quotation.getVolumeAmt().doubleValue() > GlobalConstant.MIN_TRADE_AMOUNT) {
                    targetCodeList.add(code);
                }
            }
        }
        //构建最终结果
        Map<String,String> tmpMap = null;
        List<Map<String,String>> targetList = new ArrayList<>();
        for (String code : targetCodeList) {
            tmpMap = new HashMap<>();
            tmpMap.put("code",code);
            if (codeDictMap.containsKey(code)) {
                tmpMap.put("name",codeDictMap.get(code).getName());
                targetList.add(tmpMap);
            }
        }
        if (targetList.size() > 0) {
            Map<String,Object> resultMap = new HashMap<>();
            resultMap.put("data",targetList);
            resultMap.put("count",targetList.size());
            return resultMap;
        }
        return null;
    }

    /**
     * 提取提取优质股列表
     * @param maxDate
     * @param rRate
     */
    public void fetchQualityStockList(LocalDate maxDate,int limitDays,double rRate) {
        log.info("==========================开始提取优质股任务，当前时间：{}==========================", SpiderUtil.getCurrentTimeStr());
        long startTime = System.currentTimeMillis();
        //查询黑名单股列表
        List<SysDict> backListDictList = sysDictMapper.selectByType(GlobalConstant.SYS_DICT_TYPE_BACKLIST);
        List<String> backListCodeList = backListDictList.stream().map(SysDict::getValue).collect(Collectors.toList());

        //过滤掉垃圾股票
        List<Dict> dictList = dictMapper.selectListByType(GlobalConstant.DICT_TYPE_STOCK);
        List<Dict> r1DictList = dictList.stream().filter(x -> x.getCirMarketValue() != null && x.getCirMarketValue().doubleValue() >= 20 && x.getCirMarketValue().doubleValue() <= 1000
                && !backListCodeList.contains(x.getCode())).collect(Collectors.toList());

        //股票属性列表
        List<DictProp> propList = dictPropMapper.selectAll();
        Map<String,DictProp> codePropMap = propList.stream().collect(Collectors.toMap(DictProp::getCode,Function.identity(),(o,n) -> n));

        DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String strDate = maxDate.format(ymdFormatter);


        //均价
        List<AvgPrice> avgPriceList = avgPriceMapper.selectListByRangeOfNDay(strDate,limitDays);
        Map<String,List<AvgPrice>> codeHisMap = avgPriceList.stream().collect(Collectors.groupingBy(AvgPrice::getCode));

        //行情
        List<Quotation> quotationList = quotationMapper.selectListByRangeOfNDay(strDate,limitDays);
        Map<String,List<Quotation>> codeQuoMap = quotationList.stream().collect(Collectors.groupingBy(Quotation::getCode));

        //存储第二层过滤的
        Set<String> r2Set = new HashSet<>();

        int minArrDays = (int) (limitDays * rRate);
        for (Dict x : r1DictList) {
//            if ("sz002166".equals(x.getCode())) {
//                System.out.println(x.getCode());
//            }
            //根据地域过滤，东北，西北的过滤掉
            if (!codePropMap.containsKey(x.getCode()) || StringUtils.isEmpty(codePropMap.get(x.getCode()).getProvince()) || codePropMap.get(x.getCode()).getProvince().contains("甘肃省") ||
                    codePropMap.get(x.getCode()).getProvince().contains("吉林省") || codePropMap.get(x.getCode()).getProvince().contains("西藏") || codePropMap.get(x.getCode()).getProvince().contains("新疆") ||
                    codePropMap.get(x.getCode()).getProvince().contains("辽宁省") || codePropMap.get(x.getCode()).getProvince().contains("黑龙江省") || codePropMap.get(x.getCode()).getProvince().contains("内蒙古") ||
                    codePropMap.get(x.getCode()).getProvince().contains("宁夏") || codePropMap.get(x.getCode()).getProvince().contains("青海省")) {
                continue;
            }
            //市盈率差的过滤掉
            if (codePropMap.get(x.getCode()).getLyr() == null || codePropMap.get(x.getCode()).getLyr().doubleValue() < 0 || codePropMap.get(x.getCode()).getLyr().doubleValue() > 200) {
                continue;
            }
            if (codeHisMap.containsKey(x.getCode()) && codeQuoMap.containsKey(x.getCode())) {
                List<AvgPrice> curHisList = codeHisMap.get(x.getCode());
                List<Quotation> curQuoList = codeQuoMap.get(x.getCode());
                if (curHisList.get(curHisList.size() - 1).getAvg30() != null && curHisList.get(curHisList.size() - 1).getAvg30().doubleValue() < 5 || curHisList.get(curHisList.size() - 1).getAvg30() != null && curHisList.get(curHisList.size() - 1).getAvg30().doubleValue() > 50) {
                    continue;
                }
                //根据日期排序
                List<Quotation> curLimitQuoList = SpiderUtil.deepCopyByProtobuff(curQuoList).stream().sorted(Comparator.comparing(Quotation::getDate).reversed()).limit(limitDays).collect(Collectors.toList());
                curLimitQuoList = curLimitQuoList.stream().sorted(Comparator.comparing(Quotation::getDate)).collect(Collectors.toList());
                List<AvgPrice> curLimitHisList = SpiderUtil.deepCopyByProtobuff(curHisList).stream().sorted(Comparator.comparing(AvgPrice::getDate).reversed()).limit(limitDays).collect(Collectors.toList());
                curLimitHisList = curLimitHisList.stream().sorted(Comparator.comparing(AvgPrice::getDate)).collect(Collectors.toList());

                boolean isOK = false;

//                //交易量有一天在2000w一下，直接去掉
//                Iterator<Quotation> quoIte = curLimitQuoList.iterator();
//                Quotation cQuotation = null;
//                while (quoIte.hasNext()) {
//                    cQuotation = quoIte.next();
//                    if (cQuotation.getVolumeAmt().doubleValue() < 20000000) {
//                        quoIte.remove();
//                        break;
//                    }
//                }

                //5日线上升
                if (curLimitHisList.size() == limitDays) {
                    int arrCount = 0;
                    AvgPrice lastAvgPrice = null;
                    AvgPrice newAvgPrice = null;
                    for (int i = 1;i<curLimitHisList.size();i++) {
                        lastAvgPrice = curLimitHisList.get(i -1);
                        newAvgPrice = curLimitHisList.get(i);
                        if (newAvgPrice.getAvg5() != null && lastAvgPrice.getAvg5() != null && newAvgPrice.getAvg5().doubleValue() >= lastAvgPrice.getAvg5().doubleValue()) {
                            arrCount++;
                            if (arrCount == minArrDays) {
                                r2Set.add(x.getCode());
                                isOK = true;
                                break;
                            }
                        }
                    }
                }

                //均线未跌破5日线
                if (!isOK) {
                    if (curLimitQuoList.size() == limitDays) {
                        int arrCount = 0;
                        Map<String,AvgPrice> dateHisMap = curLimitHisList.stream().collect(Collectors.toMap(h -> h.getDate().format(ymdFormatter), Function.identity(),(oldV,newV) ->{
                            System.out.println(String.format("重复：%s -> %s",newV.getCode(),newV.getDate().format(ymdFormatter)));
                            return newV;
                        }));
                        for (Quotation quotation : curLimitQuoList) {
                            String curDate = quotation.getDate().format(ymdFormatter);
                            if (dateHisMap.containsKey(curDate)) {
                                AvgPrice curAvgPrice = dateHisMap.get(curDate);
                                if (curAvgPrice.getAvg() != null && curAvgPrice.getAvg5() != null && curAvgPrice.getAvg().doubleValue() >= curAvgPrice.getAvg5().doubleValue()) {
                                    arrCount++;
                                    if (arrCount == minArrDays) {
                                        r2Set.add(x.getCode());
                                        isOK = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }

                //破月线拉起
                if (!isOK) {
                    Map<String, AvgPrice> dateHisMap = curLimitHisList.stream().collect(Collectors.toMap(h -> h.getDate().format(ymdFormatter), Function.identity(), (oldV, newV) -> {
                        log.info(String.format("重复：%s -> %s", newV.getCode(), newV.getDate().format(ymdFormatter)));
                        return newV;
                    }));
                    Quotation lastQuotation = null;
                    Quotation midQuotation = null;
                    Quotation nextQuotation = null;
                    int downMonthLineCount = 0;
                    int upMonthLineCount = 0;
                    for (int i = 1; i < curLimitQuoList.size() - 1; i++) {
                        lastQuotation = curLimitQuoList.get(i - 1);
                        midQuotation = curLimitQuoList.get(i);
                        nextQuotation = curLimitQuoList.get(i + 1);
                        String curDate = midQuotation.getDate().format(ymdFormatter);
                        if (dateHisMap.containsKey(curDate)) {
                            AvgPrice midAvgPrice = dateHisMap.get(curDate);
                            AvgPrice lastAvgPrice = dateHisMap.get(lastQuotation.getDate().format(ymdFormatter));
                            AvgPrice nextAvgPrice = dateHisMap.get(nextQuotation.getDate().format(ymdFormatter));
                            if (lastAvgPrice == null || nextAvgPrice == null) {
                                continue;
                            }
                            if (lastAvgPrice.getAvg() != null && midAvgPrice.getAvg() != null && midAvgPrice.getAvg5() != null && lastAvgPrice.getAvg5() != null && nextAvgPrice.getAvg() != null && nextAvgPrice.getAvg5() != null && lastAvgPrice.getAvg().doubleValue() < lastAvgPrice.getAvg5().doubleValue()) {
                                downMonthLineCount++;
                                if (midAvgPrice.getAvg().doubleValue() >= midAvgPrice.getAvg5().doubleValue() || nextAvgPrice.getAvg().doubleValue() >= nextAvgPrice.getAvg5().doubleValue()) {
                                    upMonthLineCount++;
                                }
                            }
                        }
                    }
                    if (downMonthLineCount > 1 && downMonthLineCount == upMonthLineCount) {
                        r2Set.add(x.getCode());
                    }
                }
            }
        }

        //查询股票当前价格的3个月内的相对高地位
        List<RelativePositionVo> relativePositionVoList = quotationMapper.selectCurPosition(strDate);
        Map<String,RelativePositionVo> codePositionMap = relativePositionVoList.stream().collect(Collectors.toMap(RelativePositionVo::getCode,Function.identity(),(o,n) -> n));
        //过滤掉高位股，当前股价的相对位置大于80的即认为是高位股
        r2Set = r2Set.stream().filter(x -> codePositionMap.containsKey(x) && codePositionMap.get(x).getPRate() < 80).collect(Collectors.toSet());

        //查询过去8个月涨停次数大于2次的股票
        List<UpLimitCountVo> upLimitCountVoList = quotationMapper.selectUpLimitGtN(250,maxDate,0);
        Map<String,UpLimitCountVo> codeLimitCountMap = upLimitCountVoList.stream().collect(Collectors.toMap(UpLimitCountVo::getCode,Function.identity(),(o,n) -> n));
        //过滤掉涨停次数少于2次的
        r2Set = r2Set.stream().filter(codeLimitCountMap::containsKey).collect(Collectors.toSet());

        //汇总最终结果
        Map<String,Dict> codeNameMap = dictList.stream().collect(Collectors.toMap(Dict::getCode,Function.identity()));
        List<CodeNameVo> resultList = r2Set.stream().map(x -> new CodeNameVo(x,codeNameMap.get(x).getName())).collect(Collectors.toList());

        log.info("==============================提取结果为：\r\n");
        resultList.forEach(x -> log.info("{} -> {}",x.getCode(),x.getName()));
        log.info("==============================共{}个==============================\r\n",resultList.size());

        //先清空优质股字典列表
        sysDictMapper.deleteByType(GlobalConstant.SYS_DICT_TYPE_QUALITY_STOCKS);

        List<SysDict> sysDictList = new ArrayList<>();
        SysDict sysDict = null;
        for (CodeNameVo entity : resultList) {
            sysDict = new SysDict();
            sysDict.setId(genIdUtil.nextId());
            sysDict.setLabel(entity.getName());
            sysDict.setValue(entity.getCode());
            sysDict.setType(GlobalConstant.SYS_DICT_TYPE_QUALITY_STOCKS);
            sysDict.setSort(0L);
            sysDict.setParentId(0L);
            sysDict.setCreateBy(0L);
            sysDict.setUpdateBy(0L);
            sysDict.setDelFlag("0");
            sysDictList.add(sysDict);
        }
        //保存到字典表
        mybatisBatchHandler.batchInsertOrUpdate(sysDictList, SysDictMapper.class,GlobalConstant.BATCH_MODDEL_INSERT);
        long endTime = System.currentTimeMillis();
        log.info("==========================提取优质股列表任务完成，当前时间：{}==========================", SpiderUtil.getCurrentTimeStr());
        log.info("==========================共耗时：{}秒==========================", (endTime - startTime) / 1000);
    }

    /**
     * 添加黑名单
     * @param codeList
     */
    public void addBackList(String codeList) {
        List<Dict> dictList = dictMapper.selectAll();
        Map<String,String> codeNameMap = dictList.stream().collect(Collectors.toMap(Dict::getCode,Dict::getName));

        List<SysDict> sysDictList = sysDictMapper.selectByType(GlobalConstant.SYS_DICT_TYPE_BACKLIST);
        List<String> backListCodeList = sysDictList.stream().map(SysDict::getValue).collect(Collectors.toList());

        //新增黑名单列表
        List<SysDict> newSysDictList = new ArrayList<>();
        SysDict sysDict = null;
        for (String code : codeList.split(",")) {
            if (backListCodeList.contains(code) || !codeNameMap.containsKey(code)) {
                continue;
            }
            sysDict = new SysDict();
            sysDict.setId(genIdUtil.nextId());
            sysDict.setValue(code);
            sysDict.setLabel(codeNameMap.get(code));
            sysDict.setType(GlobalConstant.SYS_DICT_TYPE_BACKLIST);
            sysDict.setSort(0L);
            sysDict.setParentId(0L);
            sysDict.setCreateBy(0L);
            sysDict.setUpdateBy(0L);
            sysDict.setDelFlag("0");
            newSysDictList.add(sysDict);
        }
        mybatisBatchHandler.batchInsertOrUpdate(newSysDictList, SysDictMapper.class,GlobalConstant.BATCH_MODDEL_INSERT);
    }

    /**
     * 抓取股票基本信息
     * 从同花顺抓取
     */
    public void crewDictProp() {
        long startTime = System.currentTimeMillis();
        log.info("==========================开始抓取股票基础数据，当前时间：{}==========================",SpiderUtil.getCurrentTimeStr());
        List<Dict> dictList = dictMapper.selectListByType(GlobalConstant.DICT_TYPE_STOCK);
        //不抓ST股和创业板和科创版
        dictList = dictList.stream().filter(x -> !x.getName().toUpperCase().contains("ST") && !x.getCode().contains("sz300") && !x.getCode().contains("sh688")).collect(Collectors.toList());
        if (dictList.size() > 0) {
            //先打散列表顺序
            Collections.shuffle(dictList);

//            //TODO 测试
//            List<String> codeList = Arrays.asList("sh600497","sh601566","sz300285","sz002837","sh603936","sz002380","sh600183","sz002622","sz000533","sz002116");
//            List<String> codeList = Arrays.asList("sz002256","sz002873");
//            dictList = dictList.stream().filter(x -> codeList.contains(x.getCode())).collect(Collectors.toList());

            //抓取米铺代理次数
            List<List<Dict>> twoDeepList = SpiderUtil.partitionList(dictList,GlobalConstant.MAX_THREAD_COUNT);
            SimpleDateFormat ymdFormat = new SimpleDateFormat("yyyy-MM-dd");

            log.info("============================本次共启动{}个线程抓取{}个股票基本数据,当前时间：{}============================",twoDeepList.size(),dictList.size(),SpiderUtil.getCurrentTimeStr());
            ExecutorService threadPool = Executors.newFixedThreadPool(twoDeepList.size());
            CountDownLatch latch = new CountDownLatch(twoDeepList.size());
            List<List<DictProp>> twoDeepDictPropList = new ArrayList<>();

            //先抓取代理列表
            proxyIpList = this.crewProxyList(twoDeepList.size(),1);
            //代理ip也分给每个线程
            twoDeepProxyList = SpiderUtil.partitionList(proxyIpList,twoDeepList.size());
            if (twoDeepProxyList.size() < twoDeepList.size()) {
                log.error("============================代理IP不足，请检查============================");
                return;
            }
            int loopIndex = 0;
            //计数使用，识别所有线程是否已经全部使用完代理
            AtomicInteger counter = new AtomicInteger(0);
            //记录阻塞数量
            AtomicInteger waitThreadCounter = new AtomicInteger(0);
            //记录重新抓取米扑次数
            AtomicInteger switchCounter = new AtomicInteger(0);
            Map<String,Integer> banIpMap = new ConcurrentHashMap<>();
            Map<String,Integer> threadNameIndexMap = new HashMap<>();
            //设置线程池中的线程名称和索引映射
            List<String> threadNameList = new CopyOnWriteArrayList<>();
            for (int i = 0 ; i < latch.getCount() ; i++) {
                threadNameList.add(String.format("crew-jqka-thread-%s",(i+1)));
                threadNameIndexMap.put(threadNameList.get(i),i);
            }

            //公共header
            Map<String,String> headerMap = new HashMap<>();
            headerMap.put("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
            headerMap.put("Accept-Language","zh-CN,zh;q=0.9");
            headerMap.put("Cache-Control","max-age=0");
            headerMap.put("Connection","keep-alive");
            headerMap.put("Host","basic.10jqka.com.cn");
            headerMap.put("sec-ch-ua","\" Not A;Brand\";v=\"99\", \"Chromium\";v=\"96\", \"Google Chrome\";v=\"96\"");
            headerMap.put("sec-ch-ua-mobile","?0");
            headerMap.put("sec-ch-ua-platform","\"Windows\"");
            headerMap.put("Sec-Fetch-Dest","document");
            headerMap.put("Sec-Fetch-Mode","navigate");
            headerMap.put("Sec-Fetch-Site","none");
            headerMap.put("Sec-Fetch-User","?1");
            headerMap.put("Upgrade-Insecure-Requests","1");
            headerMap.put("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.110 Safari/537.36");

            for (List<Dict> innerList : twoDeepList) {
                try {
                    Thread.sleep(SpiderUtil.getRandomNum(1,10));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                final int index = loopIndex;
                threadPool.execute(() -> {
                    //设置线程名
                    Thread.currentThread().setName(threadNameList.get(index));
                    //当前线程使用的代理列表
                    List<ProxyIp> curProxyList = twoDeepProxyList.get(threadNameIndexMap.get(Thread.currentThread().getName()));
                    ProxyIp proxyIp = curProxyList.get(0);

                    //配置独立的httpclient客户端，抓取时只用重试1次，提升效率
                    HttpClientUtil clientUtil = new HttpClientUtil();
                    clientUtil.setMaxRetryCount(1);
                    clientUtil.setProxyPropArr(new String[]{proxyIp.getIp(),String.valueOf(proxyIp.getPort())});

                    //初始化抓取结果集
                    ResponseEntity responseEntity = null;

                    String homeHtml = null;
                    String companyHtml = null;
                    List<DictProp> groupedDictPropList = new ArrayList<>();
                    for (Dict dict : innerList) {
                        try {
                            DictProp dictProp = new DictProp();

                            //访问首页
                            boolean isFetchSuccess = false;
                            int rCount = 0;
                            for (;;) {
                                //每个线程都还剩下最后一个时，就要重新抓取代理，并分配了
                                if (curProxyList.size() == 0) {
                                    //先需要计数，再检查是否全部线程都使用完毕，最后一个完成的，需重新抓取，并唤醒所有阻塞的线程继续工作
                                    counter.getAndAdd(1);
                                    //每个线程都代理都剩下最后一个
                                    if (counter.get() == latch.getCount()) {
                                        log.info("=======================线程{}最后使用完代理，即将重新抓取米铺代理",Thread.currentThread().getName());
                                        switchCounter.getAndAdd(1);
                                        log.info("==========================线程{}第{}次抓取米铺代理==========================",Thread.currentThread().getName(),switchCounter.get());
                                        //可能抓取的代理全部被新浪加入了黑名单，需要重复抓取，直到有未加入黑名单的代理ip为止
                                        int fetchCount = 0;
                                        for (;;) {
                                            //重新抓取代理ip列表，并加入代理ip池
                                            proxyIpList.addAll(this.crewProxyList((int)latch.getCount(),1));
                                            //过滤掉已经被拉黑的ip
                                            proxyIpList = proxyIpList.stream().filter(x -> !banIpMap.containsKey(x.getIp())).collect(Collectors.toList());
                                            if (proxyIpList.size() >= latch.getCount()) {
                                                break;
                                            }
                                            fetchCount++;
                                            log.info("==========================抓取的代理都已加入黑名单，第{}次重新抓取==========================",fetchCount);
                                        }
                                        //重新分配代理列表
                                        twoDeepProxyList = SpiderUtil.partitionList(proxyIpList,(int)latch.getCount());
                                        //重新分配线程和索引映射
                                        threadNameIndexMap.clear();
                                        for (int i = 0 ; i < latch.getCount() ; i++) {
                                            threadNameIndexMap.put(threadNameList.get(i),i);
                                        }
                                        threadNameIndexMap.forEach((k,v) -> log.info("=============线程索引映射 {} => {}",k,v));
                                        //新分配的列表重新加入
                                        curProxyList.addAll(twoDeepProxyList.get(threadNameIndexMap.get(Thread.currentThread().getName())));
                                        if (latch.getCount() > 1) {
                                            //重新初始化计数器
                                            counter.getAndSet(0);
                                            //唤醒全部阻塞都线程继续抓取
                                            for (;;) {
                                                synchronized (lock) {
                                                    lock.notifyAll();
                                                }
                                                log.info("=======================线程{}已唤醒其他阻塞线程，所有线程即将重新工作",Thread.currentThread().getName());
                                                break;
                                            }
                                        }
                                    } else {
                                        //自己完成，其他线程未用完，先阻塞等其他都完成了，再重新抓取
                                        synchronized (lock) {
                                            try {
                                                log.info("=======================线程{}代理IP已用完，开始阻塞",Thread.currentThread().getName());
                                                waitThreadCounter.getAndAdd(1);
                                                lock.wait();
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                        //唤醒后也重新抓取分配好了
                                        waitThreadCounter.getAndAdd(-1);
                                        log.info("=======================线程{}被唤醒，继续抓取",Thread.currentThread().getName());
                                        curProxyList.addAll(twoDeepProxyList.get(threadNameIndexMap.get(Thread.currentThread().getName())));
                                    }
                                }
                                try {
                                    responseEntity = clientUtil.recursiveHttpRequest(GlobalConstant.HTTP_GET,0, String.format(GlobalConstant.JQKA_STOCK_BASIC_URL,dict.getCode().substring(2)),null,headerMap, GlobalConstant.CHARASET_GBK);
                                    homeHtml = responseEntity.getContent();
                                } catch (Exception e) {
                                    log.info("=========================加载页面缓慢或异常出现异常，尝试重新切换代理=========================");
                                    homeHtml = null;
                                }
                                //抓取成功就直接抓取下一个股票了
                                if (StringUtils.isNotEmpty(homeHtml) && homeHtml.contains("公司概要")) {
                                    isFetchSuccess = true;
                                    break;
                                }

                                //有问题的代理ip加入黑名单
                                banIpMap.put(proxyIp.getIp(),0);
                                //移除已使用的代理
                                curProxyList.remove(proxyIp);
                                //抓取异常或者代理失效时切换新代理继续抓取
                                if (curProxyList.size() > 0) {
                                    rCount++;
                                    //切换代理
                                    proxyIp = curProxyList.get(0);
                                    log.info("==========================线程{}第{}次切换代理，IP【{}】==========================",Thread.currentThread().getName(),rCount,proxyIp.getIp());
                                    //切换代理
                                    clientUtil.setProxyPropArr(new String[]{proxyIp.getIp(),String.valueOf(proxyIp.getPort())});
                                }
                            }
                            if (isFetchSuccess) {
                                //提取板块
                                StringBuilder sBuff = new StringBuilder();
                                Pattern pattern = Pattern.compile("ifind\">(.*?)</a>");
                                Matcher matcher = pattern.matcher(homeHtml);
                                String sText = null;
                                while (matcher.find()) {
                                    sText = matcher.group(1);
                                    if (!sText.contains("详情")) {
                                        if (sText.contains("em")) {
                                            sBuff.append(sText, 0, sText.indexOf("<")).append(",");
                                        } else {
                                            sBuff.append(sText).append(",");
                                        }
                                    }
                                }
                                String plate = sBuff.toString();
                                if (StringUtils.isNotEmpty(plate)) {
                                    dictProp.setPlate(plate.substring(0,plate.lastIndexOf(",")));
                                }

                                //提取静市盈率
                                pattern = Pattern.compile("id=\"jtsyl\">(.*?)</span>");
                                matcher = pattern.matcher(homeHtml);
                                if (matcher.find()) {
                                    if ("亏损".equals(matcher.group(1))) {
                                        dictProp.setLyr(new BigDecimal(-1));
                                    } else {
                                        try {
                                            dictProp.setLyr(new BigDecimal(matcher.group(1)));
                                        } catch (Exception e) {
                                            log.error("==================抓取{}市盈率（静）失败==================",dict.getName());
                                            e.printStackTrace();
                                        }
                                    }
                                }

                                //提取动态市盈率
                                pattern = Pattern.compile("id=\"dtsyl\">(.*?)</span>");
                                matcher = pattern.matcher(homeHtml);
                                if (matcher.find()) {
                                    if ("亏损".equals(matcher.group(1))) {
                                        dictProp.setTtm(new BigDecimal(-1));
                                    } else {
                                        try {
                                            dictProp.setTtm(new BigDecimal(matcher.group(1)));
                                        } catch (Exception e) {
                                            log.error("==================抓取{}市盈率（动）失败==================",dict.getName());
                                            e.printStackTrace();
                                        }
                                    }
                                }

                                //提取解禁信息
                                if (homeHtml.contains("解禁股份类型")) {
                                    pattern = Pattern.compile("<span class=\"tip f12\">(.*?)</span>");
                                    matcher = pattern.matcher(homeHtml);
                                    while (matcher.find()) {
                                        if (matcher.group(1).contains("-") && !matcher.group(1).contains(".")) {
                                            dictProp.setLatestLiftBan(ymdFormat.parse(matcher.group(1)));
                                        }
                                    }
                                }
                            }

                            //访问公司首页
                            for (;;) {
                                //每个线程都还剩下最后一个时，就要重新抓取代理，并分配了
                                if (curProxyList.size() == 0) {
                                    //先需要计数，再检查是否全部线程都使用完毕，最后一个完成的，需重新抓取，并唤醒所有阻塞的线程继续工作
                                    counter.getAndAdd(1);
                                    //每个线程都代理都剩下最后一个
                                    if (counter.get() == latch.getCount()) {
                                        log.info("=======================线程{}最后使用完代理，即将重新抓取米铺代理",Thread.currentThread().getName());
                                        switchCounter.getAndAdd(1);
                                        log.info("==========================线程{}第{}次抓取米铺代理==========================",Thread.currentThread().getName(),switchCounter.get());
                                        //可能抓取的代理全部被新浪加入了黑名单，需要重复抓取，直到有未加入黑名单的代理ip为止
                                        int fetchCount = 0;
                                        for (;;) {
                                            //重新抓取代理ip列表，并加入代理ip池
                                            proxyIpList.addAll(this.crewProxyList((int)latch.getCount(),1));
                                            //过滤掉已经被拉黑的ip
                                            proxyIpList = proxyIpList.stream().filter(x -> !banIpMap.containsKey(x.getIp())).collect(Collectors.toList());
                                            if (proxyIpList.size() >= latch.getCount()) {
                                                break;
                                            }
                                            fetchCount++;
                                            log.info("==========================抓取的米铺代理都已加入黑名单，第{}次重新抓取==========================",fetchCount);
                                        }
                                        //重新分配代理列表
                                        twoDeepProxyList = SpiderUtil.partitionList(proxyIpList,(int)latch.getCount());
                                        //重新分配线程和索引映射
                                        threadNameIndexMap.clear();
                                        for (int i = 0 ; i < latch.getCount() ; i++) {
                                            threadNameIndexMap.put(threadNameList.get(i),i);
                                        }
                                        threadNameIndexMap.forEach((k,v) -> log.info("=============线程索引映射 {} => {}",k,v));
                                        //新分配的列表重新加入
                                        curProxyList.addAll(twoDeepProxyList.get(threadNameIndexMap.get(Thread.currentThread().getName())));
                                        //如果不是最后一个未完成任务的，需要唤醒其他休眠线程继续工作
                                        if (latch.getCount() > 1) {
                                            //重新初始化计数器
                                            counter.getAndSet(0);
                                            //唤醒全部阻塞都线程继续抓取
                                            for (;;) {
                                                synchronized (lock) {
                                                    lock.notifyAll();
                                                }
                                                log.info("=======================线程{}已唤醒其他阻塞线程，所有线程即将重新工作",Thread.currentThread().getName());
                                                break;
                                            }
                                        }
                                    } else {
                                        //自己完成，其他线程未用完，先阻塞等其他都完成了，再重新抓取
                                        synchronized (lock) {
                                            try {
                                                log.info("=======================线程{}代理IP已用完，开始阻塞",Thread.currentThread().getName());
                                                waitThreadCounter.getAndAdd(1);
                                                lock.wait();
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                        //唤醒后也重新抓取分配好了
                                        waitThreadCounter.getAndAdd(-1);
                                        log.info("=======================线程{}被唤醒，继续抓取",Thread.currentThread().getName());
                                        curProxyList.addAll(twoDeepProxyList.get(threadNameIndexMap.get(Thread.currentThread().getName())));
                                    }
                                }
                                try {
                                    responseEntity = clientUtil.recursiveHttpRequest(GlobalConstant.HTTP_GET,0, String.format(GlobalConstant.JQKA_STOCK_COMPANY_URL,dict.getCode().substring(2)),null,headerMap,GlobalConstant.CHARASET_GBK);
                                    companyHtml = responseEntity.getContent();
                                } catch (Exception e) {
                                    log.error("=========================加载页面缓慢或异常出现异常，尝试重新切换代理=========================");
                                    companyHtml = null;
                                }
                                //抓取成功直接就退出继续下一个股票抓取了
                                if (StringUtils.isNotEmpty(companyHtml) && companyHtml.contains("详细情况")) {
                                    isFetchSuccess = true;
                                    break;
                                }

                                //有问题的代理ip加入黑名单
                                banIpMap.put(proxyIp.getIp(),0);
                                //移除已使用的代理
                                curProxyList.remove(proxyIp);
                                //抓取异常或者代理失效时切换新代理继续抓取
                                if (curProxyList.size() > 0) {
                                    rCount++;
                                    //切换代理
                                    proxyIp = curProxyList.get(0);
                                    log.info("==========================线程{}第{}次切换代理，IP【{}】==========================",Thread.currentThread().getName(),rCount,proxyIp.getIp());
                                    //切换代理
                                    clientUtil.setProxyPropArr(new String[]{proxyIp.getIp(),String.valueOf(proxyIp.getPort())});
                                }
                            }
                            if (isFetchSuccess) {
                                companyHtml = responseEntity.getContent();

                                //提取公司名称
                                Pattern pattern = Pattern.compile("公司名称：</strong><span>(.*?)</span>");
                                Matcher matcher = pattern.matcher(companyHtml);
                                if (matcher.find()) {
                                    dictProp.setCompanyName(matcher.group(1));
                                }

                                //提取行业和行业细分
                                pattern = Pattern.compile("所属申万行业：</strong><span>(.*?)</span>");
                                matcher = pattern.matcher(companyHtml);
                                if (matcher.find()) {
                                    String matchText = matcher.group(1);
                                    if (StringUtils.isNotEmpty(matchText)) {
                                        if (matchText.contains("—")) {
                                            matchText = matchText.replaceAll(" ","");
                                            String []matchArr = matchText.split("—");
                                            dictProp.setIndustry(matchArr[0]);
                                            dictProp.setIdySegment(matchArr[1]);
                                        } else {
                                            dictProp.setIndustry(matchText);
                                        }
                                    }
                                }

                                //提取省份
                                pattern = Pattern.compile("所属地域：</strong><span>(.*?)</span>");
                                matcher = pattern.matcher(companyHtml);
                                if (matcher.find()) {
                                    dictProp.setProvince(matcher.group(1));
                                }

                                //提取各种人信息
                                pattern = Pattern.compile("<a person_id=\"(.*?)\" class=\"turnto\" href=\"javascript:void\\(0\\)\">(.*?)</a>");
                                matcher = pattern.matcher(companyHtml);
                                int nLoopCount = 0;
                                while (matcher.find()) {
                                    if (nLoopCount == 0) {
                                        dictProp.setCeo(matcher.group(2));
                                    }
                                    if (nLoopCount == 2) {
                                        if (StringUtils.isNotEmpty(matcher.group(2)) && matcher.group(2).length() < 50) {
                                            dictProp.setArtificialPerson(matcher.group(2));
                                        } else {
                                            log.error("==============================={} -> {}抓取有误，请核对===============================",dict.getCode(),String.format(GlobalConstant.JQKA_STOCK_COMPANY_URL,dict.getCode().substring(2)));
                                        }
                                        break;
                                    }
                                    nLoopCount++;
                                }

                                pattern = Pattern.compile("<td colspan=\"3\">(.*?)</td>",Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                                matcher = pattern.matcher(companyHtml);
                                while (matcher.find()) {
                                    if (matcher.group(1).contains("办公地址")) {
                                        pattern = Pattern.compile("<span>(.*?)</span>");
                                        matcher = pattern.matcher(matcher.group(1));
                                        if (matcher.find()) {
                                            dictProp.setAddress(matcher.group(1));
                                        }
                                    }
                                }
                            }
                            //添加其他数据
                            dictProp.setId(genIdUtil.nextId());
                            dictProp.setCode(dict.getCode());
                            dictProp.setName(dict.getName());
                            groupedDictPropList.add(dictProp);
                            log.info("====================================成功抓取股票 => {}",dict.getName());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    twoDeepDictPropList.add(groupedDictPropList);
                    latch.countDown();
                    log.info("=======================线程{}已完成任务，剩余未完成线程数:{}=======================",Thread.currentThread().getName(),latch.getCount());

                    //移除当前线程
                    threadNameList.remove(Thread.currentThread().getName());
                    //如果当前线程不是最后一个完成任务的，且其他线程都在休眠，则唤醒其他线程继续执行
                    if (!threadNameList.isEmpty() && waitThreadCounter.get() == latch.getCount()) {
                        //重新初始化计数器
                        counter.getAndSet(0);
                        //还有最后一个未完成时，无需继续抓取了，直接给当前剩下的代理池给最后一个未完成的线程
                        if (latch.getCount() == 1) {
                            log.info("=======================剩余最后一个线程，将当前线程的所有未使用的代理池转移过去=======================");
                            twoDeepProxyList.get(threadNameIndexMap.get(threadNameList.get(0))).addAll(curProxyList);
                        }
                        //唤醒其他阻塞的线程
                        for (;;) {
                            log.info("=======================线程{}已重置计数器",Thread.currentThread().getName());
                            synchronized (lock) {
                                lock.notifyAll();
                            }
                            log.info("=======================线程{}已唤醒其他阻塞线程，所有线程即将重新工作",Thread.currentThread().getName());
                            break;
                        }
                    }
                });
                loopIndex++;
            }
            //等待子线程全部计算完毕
            try {
                latch.await();
                threadPool.shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            long cEndTime = System.currentTimeMillis();
            log.info("==========================抓取股票基础数据工耗时：{}秒==========================",(cEndTime - startTime) / 1000);

            //删除验证码图片目录
            File dirFile = new File(stockConfig.getImgDownadDir());
            if (dirFile.exists()) {
                try {
                    SpiderUtil.deleteFile(dirFile);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("====================删除验证码目录失败====================");
                }
            }

            List<DictProp> dbDictPropList = twoDeepDictPropList.stream().flatMap(List::stream).collect(Collectors.toList());
            //先删除之前的基本信息
            dictPropMapper.deleteAll();
            mybatisBatchHandler.batchInsertOrUpdate(dbDictPropList,DictPropMapper.class,GlobalConstant.BATCH_MODDEL_INSERT);
            long endTime = System.currentTimeMillis();
            log.info("==========================抓取股票基本数据完成，当前时间：{}==========================",SpiderUtil.getCurrentTimeStr());

            //最后生成股票-行业-概念关系数据
            genQuoIdpDataList();
            log.info("==========================共耗时{}秒==========================", (endTime - startTime) / 1000);
        }
    }

    /**
     * 抓取股票基本信息
     * 使用Selenium自动化从同花顺抓取
     */
    public void crewDictPropWithSelenium() {
        long startTime = System.currentTimeMillis();
        log.info("==========================开始抓取股票基础数据，当前时间：{}==========================",SpiderUtil.getCurrentTimeStr());
        List<Dict> dictList = dictMapper.selectListByType(GlobalConstant.DICT_TYPE_STOCK);
        //不抓ST股和创业板和科创版
        dictList = dictList.stream().filter(x -> !x.getName().toUpperCase().contains("ST") && !x.getCode().contains("sz300") && !x.getCode().contains("sh688")).collect(Collectors.toList());
        if (dictList.size() > 0) {
            //先打散列表顺序
            Collections.shuffle(dictList);

            //TODO 测试
//            List<String> codeList = Arrays.asList("sh600497","sh601566","sz300285","sz002837","sh603936","sz002380","sh600183","sz002622","sz000533","sz002116");
//            List<String> codeList = Arrays.asList("sz000777");
//            dictList = dictList.stream().filter(x -> codeList.contains(x.getCode())).collect(Collectors.toList());

            //抓取米铺代理次数
            int crewMimvpCount = 2;
            List<List<Dict>> twoDeepList = SpiderUtil.partitionList(dictList,GlobalConstant.MAX_THREAD_COUNT);
            SimpleDateFormat ymdFormat = new SimpleDateFormat("yyyy-MM-dd");

            log.info("============================本次共启动{}个线程抓取{}个股票基本数据,当前时间：{}============================",twoDeepList.size(),dictList.size(),SpiderUtil.getCurrentTimeStr());
            ExecutorService threadPool = Executors.newFixedThreadPool(twoDeepList.size());
            CountDownLatch latch = new CountDownLatch(twoDeepList.size());
            int timeoutMs = 10;
            List<List<DictProp>> twoDeepDictPropList = new ArrayList<>();

            long initMimvpTime = System.currentTimeMillis() / 1000 - 20 * 60 * 1000;
            //先抓取代理列表
            proxyIpList = this.crewProxyList(8,1);
            //代理ip也分给每个线程
            twoDeepProxyList = SpiderUtil.partitionList(proxyIpList,twoDeepList.size());
            if (twoDeepProxyList.size() < twoDeepList.size()) {
                log.error("============================代理IP不足，请检查============================");
                return;
            }
            int loopIndex = 0;
            //计数使用，识别所有线程是否已经全部使用完代理
            AtomicInteger counter = new AtomicInteger(0);
            //记录阻塞数量
            AtomicInteger waitThreadCounter = new AtomicInteger(0);
            //记录重新抓取米扑次数
            AtomicInteger switchCounter = new AtomicInteger(0);
            Map<String,Integer> banIpMap = new ConcurrentHashMap<>();
            Map<String,Integer> threadNameIndexMap = new HashMap<>();
            //设置线程池中的线程名称和索引映射
            List<String> threadNameList = new CopyOnWriteArrayList<>();
            for (int i = 0 ; i < latch.getCount() ; i++) {
                threadNameList.add(String.format("crew-jqka-thread-%s",(i+1)));
                threadNameIndexMap.put(threadNameList.get(i),i);
            }
            for (List<Dict> innerList : twoDeepList) {
                //随机休眠，方便协作
                try {
                    Thread.sleep(SpiderUtil.getRandomNum(1,20));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                final int index = loopIndex;
                threadPool.execute(() -> {
                    //设置线程名
                    Thread.currentThread().setName(threadNameList.get(index));
                    //当前线程使用的代理列表
                    List<ProxyIp> curProxyList = twoDeepProxyList.get(threadNameIndexMap.get(Thread.currentThread().getName()));

                    //初始化自动化抓取工具
                    ChromeOptions options = new ChromeOptions();
                    //options.addArguments("--headless"); //无浏览器模式
                    options.addArguments("--no-sandbox");
                    options.addArguments("--disable-gpu");
                    options.addArguments("blink-settings=imagesEnabled=false");
                    options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.70 Safari/537.36");
                    ProxyIp proxyIp = curProxyList.get(0);
                    options.addArguments("--proxy-server=http://" + String.format("%s:%s",proxyIp.getIp(),proxyIp.getPort()));
                    //options.addExtensions(new File(proxyTool.getRandomZipProxy()));//增加代理扩展
                    WebDriver webDriver = new ChromeDriver(options);//实例化
                    //设置超时时间为3S
                    webDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(timeoutMs));

                    String homeHtml = null;
                    String companyHtml = null;
                    List<DictProp> groupedDictPropList = new ArrayList<>();
                    for (Dict dict : innerList) {
                        try {
                            DictProp dictProp = new DictProp();

                            //访问首页
                            boolean isFetchSuccess = false;
                            String tmpHomeHtml = null;
                            int rCount = 0;
                            for (;;) {
                                //每个线程都还剩下最后一个时，就要重新抓取代理，并分配了
                                if (curProxyList.size() == 0) {
                                    //先需要计数，再检查是否全部线程都使用完毕，最后一个完成的，需重新抓取，并唤醒所有阻塞的线程继续工作
                                    counter.getAndAdd(1);
                                    //每个线程都代理都剩下最后一个
                                    if (counter.get() == latch.getCount()) {
                                        log.info("=======================线程{}最后使用完代理，即将重新抓取米铺代理",Thread.currentThread().getName());
                                        switchCounter.getAndAdd(1);
                                        log.info("==========================线程{}第{}次抓取米铺代理==========================",Thread.currentThread().getName(),switchCounter.get());
                                        //可能抓取的代理全部被新浪加入了黑名单，需要重复抓取，直到有未加入黑名单的代理ip为止
                                        int fetchCount = 0;
                                        for (;;) {
                                            //重新抓取代理ip列表，并加入代理ip池
                                            proxyIpList.addAll(this.crewProxyList((int)latch.getCount(),1));
                                            //过滤掉已经被拉黑的ip
                                            proxyIpList = proxyIpList.stream().filter(x -> !banIpMap.containsKey(x.getIp())).collect(Collectors.toList());
                                            if (proxyIpList.size() >= latch.getCount()) {
                                                break;
                                            }
                                            fetchCount++;
                                            log.info("==========================抓取的米铺代理都已加入黑名单，第{}次重新抓取==========================",fetchCount);
                                        }
                                        //重新分配代理列表
                                        twoDeepProxyList = SpiderUtil.partitionList(proxyIpList,(int)latch.getCount());
                                        //重新分配线程和索引映射
                                        threadNameIndexMap.clear();
                                        for (int i = 0 ; i < latch.getCount() ; i++) {
                                            threadNameIndexMap.put(threadNameList.get(i),i);
                                        }
                                        threadNameIndexMap.forEach((k,v) -> log.info("=============线程索引映射 {} => {}",k,v));
                                        //新分配的列表重新加入
                                        curProxyList.addAll(twoDeepProxyList.get(threadNameIndexMap.get(Thread.currentThread().getName())));
                                        if (latch.getCount() > 1) {
                                            //重新初始化计数器
                                            counter.getAndSet(0);
                                            //唤醒全部阻塞都线程继续抓取
                                            for (;;) {
                                                synchronized (lock) {
                                                    lock.notifyAll();
                                                }
                                                log.info("=======================线程{}已唤醒其他阻塞线程，所有线程即将重新工作",Thread.currentThread().getName());
                                                break;
                                            }
                                        }
                                    } else {
                                        //自己完成，其他线程未用完，先阻塞等其他都完成了，再重新抓取
                                        synchronized (lock) {
                                            try {
                                                log.info("=======================线程{}代理IP已用完，开始阻塞",Thread.currentThread().getName());
                                                waitThreadCounter.getAndAdd(1);
                                                lock.wait();
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                        //唤醒后也重新抓取分配好了
                                        waitThreadCounter.getAndAdd(-1);
                                        log.info("=======================线程{}被唤醒，继续抓取",Thread.currentThread().getName());
                                        curProxyList.addAll(twoDeepProxyList.get(threadNameIndexMap.get(Thread.currentThread().getName())));
                                    }
                                }
                                try {
                                    webDriver.get(String.format(GlobalConstant.JQKA_STOCK_BASIC_URL,dict.getCode().substring(2)));
                                    tmpHomeHtml = webDriver.getPageSource();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    log.info("=========================加载页面缓慢或异常出现异常，尝试重新切换代理=========================");
                                    tmpHomeHtml = null;
                                }
                                //抓取成功就直接抓取下一个股票了
                                if (StringUtils.isNotEmpty(tmpHomeHtml) && tmpHomeHtml.contains("公司概要")) {
                                    isFetchSuccess = true;
                                    break;
                                }

                                //有问题的代理ip加入黑名单
                                banIpMap.put(proxyIp.getIp(),0);
                                //移除已使用的代理
                                curProxyList.remove(proxyIp);
                                //抓取异常或者代理失效时切换新代理继续抓取
                                if (curProxyList.size() > 0) {
                                    rCount++;
                                    //切换代理
                                    proxyIp = curProxyList.get(0);
                                    log.info("==========================线程{}第{}次切换代理，IP【{}】==========================",Thread.currentThread().getName(),rCount,proxyIp.getIp());
                                    //重新初始化代理浏览器
                                    webDriver.quit();
                                    options = new ChromeOptions();
                                    //options.addArguments("--headless"); //无浏览器模式
                                    options.addArguments("--no-sandbox");
                                    options.addArguments("--disable-gpu");
                                    options.addArguments("blink-settings=imagesEnabled=false");
                                    options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.70 Safari/537.36");
                                    proxyIp = curProxyList.get(0);
                                    options.addArguments("--proxy-server=http://" + String.format("%s:%s",proxyIp.getIp(),proxyIp.getPort()));
                                    //options.addExtensions(new File(proxyTool.getRandomZipProxy()));//增加代理扩展
                                    webDriver = new ChromeDriver(options);//实例化
                                    //设置超时时间为3S
                                    webDriver.manage().timeouts().pageLoadTimeout(timeoutMs, TimeUnit.SECONDS);
                                }
                            }
                            if (isFetchSuccess) {
                                log.info("====================================成功抓取行业信息股票 => {}",dict.getName());
                                homeHtml = webDriver.getPageSource();

                                //提取行业
                                Pattern pattern = Pattern.compile("<span class=\"tip f14\">(.*?)</span>");
                                Matcher matcher = pattern.matcher(homeHtml);
                                if (matcher.find()) {
                                    dictProp.setIndustry(matcher.group(1));
                                }

                                //提取板块
                                StringBuilder sBuff = new StringBuilder();
                                pattern = Pattern.compile("ifind\">(.*?)</a>");
                                matcher = pattern.matcher(homeHtml);
                                String sText = null;
                                while (matcher.find()) {
                                    sText = matcher.group(1);
                                    if (!sText.contains("详情")) {
                                        if (sText.contains("em")) {
                                            sBuff.append(sText, 0, sText.indexOf("<")).append(",");
                                        } else {
                                            sBuff.append(sText).append(",");
                                        }
                                    }
                                }
                                String plate = sBuff.toString();
                                if (StringUtils.isNotEmpty(plate)) {
                                    dictProp.setPlate(plate.substring(0,plate.lastIndexOf(",")));
                                }

                                //提取静市盈率
                                pattern = Pattern.compile("id=\"jtsyl\">(.*?)</span>");
                                matcher = pattern.matcher(homeHtml);
                                if (matcher.find()) {
                                    if ("亏损".equals(matcher.group(1))) {
                                        dictProp.setLyr(new BigDecimal(-1));
                                    } else {
                                        try {
                                            dictProp.setLyr(new BigDecimal(matcher.group(1)));
                                        } catch (Exception e) {
                                            log.error("==================抓取{}市盈率（静）失败==================",dict.getName());
                                            e.printStackTrace();
                                        }
                                    }
                                }

                                //提取动态市盈率
                                pattern = Pattern.compile("id=\"dtsyl\">(.*?)</span>");
                                matcher = pattern.matcher(homeHtml);
                                if (matcher.find()) {
                                    if ("亏损".equals(matcher.group(1))) {
                                        dictProp.setTtm(new BigDecimal(-1));
                                    } else {
                                        try {
                                            dictProp.setTtm(new BigDecimal(matcher.group(1)));
                                        } catch (Exception e) {
                                            log.error("==================抓取{}市盈率（动）失败==================",dict.getName());
                                            e.printStackTrace();
                                        }
                                    }
                                }

                                //提取解禁信息
                                if (homeHtml.contains("解禁股份类型")) {
                                    pattern = Pattern.compile("<span class=\"tip f12\">(.*?)</span>");
                                    matcher = pattern.matcher(homeHtml);
                                    while (matcher.find()) {
                                        if (matcher.group(1).contains("-") && !matcher.group(1).contains(".")) {
                                            dictProp.setLatestLiftBan(ymdFormat.parse(matcher.group(1)));
                                        }
                                    }
                                }
                            }

                            //访问公司首页
                            String tmpCompanyHtml = null;
                            for (;;) {
                                //每个线程都还剩下最后一个时，就要重新抓取代理，并分配了
                                if (curProxyList.size() == 0) {
                                    //先需要计数，再检查是否全部线程都使用完毕，最后一个完成的，需重新抓取，并唤醒所有阻塞的线程继续工作
                                    counter.getAndAdd(1);
                                    //每个线程都代理都剩下最后一个
                                    if (counter.get() == latch.getCount()) {
                                        log.info("=======================线程{}最后使用完代理，即将重新抓取米铺代理",Thread.currentThread().getName());
                                        switchCounter.getAndAdd(1);
                                        log.info("==========================线程{}第{}次抓取米铺代理==========================",Thread.currentThread().getName(),switchCounter.get());
                                        //可能抓取的代理全部被新浪加入了黑名单，需要重复抓取，直到有未加入黑名单的代理ip为止
                                        int fetchCount = 0;
                                        for (;;) {
                                            //重新抓取代理ip列表，并加入代理ip池
                                            proxyIpList.addAll(this.crewProxyList((int)latch.getCount(),1));
                                            //过滤掉已经被拉黑的ip
                                            proxyIpList = proxyIpList.stream().filter(x -> !banIpMap.containsKey(x.getIp())).collect(Collectors.toList());
                                            if (proxyIpList.size() >= latch.getCount()) {
                                                break;
                                            }
                                            fetchCount++;
                                            log.info("==========================抓取的米铺代理都已加入黑名单，第{}次重新抓取==========================",fetchCount);
                                        }
                                        //重新分配代理列表
                                        twoDeepProxyList = SpiderUtil.partitionList(proxyIpList,(int)latch.getCount());
                                        //重新分配线程和索引映射
                                        threadNameIndexMap.clear();
                                        for (int i = 0 ; i < latch.getCount() ; i++) {
                                            threadNameIndexMap.put(threadNameList.get(i),i);
                                        }
                                        threadNameIndexMap.forEach((k,v) -> log.info("=============线程索引映射 {} => {}",k,v));
                                        //新分配的列表重新加入
                                        curProxyList.addAll(twoDeepProxyList.get(threadNameIndexMap.get(Thread.currentThread().getName())));
                                        //如果不是最后一个未完成任务的，需要唤醒其他休眠线程继续工作
                                        if (latch.getCount() > 1) {
                                            //重新初始化计数器
                                            counter.getAndSet(0);
                                            //唤醒全部阻塞都线程继续抓取
                                            for (;;) {
                                                synchronized (lock) {
                                                    lock.notifyAll();
                                                }
                                                log.info("=======================线程{}已唤醒其他阻塞线程，所有线程即将重新工作",Thread.currentThread().getName());
                                                break;
                                            }
                                        }
                                    } else {
                                        //自己完成，其他线程未用完，先阻塞等其他都完成了，再重新抓取
                                        synchronized (lock) {
                                            try {
                                                log.info("=======================线程{}代理IP已用完，开始阻塞",Thread.currentThread().getName());
                                                waitThreadCounter.getAndAdd(1);
                                                lock.wait();
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                        //唤醒后也重新抓取分配好了
                                        waitThreadCounter.getAndAdd(-1);
                                        log.info("=======================线程{}被唤醒，继续抓取",Thread.currentThread().getName());
                                        curProxyList.addAll(twoDeepProxyList.get(threadNameIndexMap.get(Thread.currentThread().getName())));
                                    }
                                }
                                try {
                                    webDriver.get(String.format(GlobalConstant.JQKA_STOCK_COMPANY_URL,dict.getCode().substring(2)));
                                    tmpCompanyHtml = webDriver.getPageSource();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    log.error("=========================加载页面缓慢或异常出现异常，尝试重新切换代理=========================");
                                    tmpCompanyHtml = null;
                                }
                                //抓取成功直接就退出继续下一个股票抓取了
                                if (StringUtils.isNotEmpty(tmpCompanyHtml) && tmpCompanyHtml.contains("详细情况")) {
                                    isFetchSuccess = true;
                                    break;
                                }

                                //有问题的代理ip加入黑名单
                                banIpMap.put(proxyIp.getIp(),0);
                                //移除已使用的代理
                                curProxyList.remove(proxyIp);
                                //抓取异常或者代理失效时切换新代理继续抓取
                                if (curProxyList.size() > 0) {
                                    rCount++;
                                    //切换代理
                                    proxyIp = curProxyList.get(0);
                                    log.info("==========================线程{}第{}次切换代理，IP【{}】==========================",Thread.currentThread().getName(),rCount,proxyIp.getIp());
                                    //重新初始化代理浏览器
                                    webDriver.quit();
                                    options = new ChromeOptions();
                                    //options.addArguments("--headless"); //无浏览器模式
                                    options.addArguments("--no-sandbox");
                                    options.addArguments("--disable-gpu");
                                    options.addArguments("blink-settings=imagesEnabled=false");
                                    options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.70 Safari/537.36");
                                    proxyIp = curProxyList.get(0);
                                    options.addArguments("--proxy-server=http://" + String.format("%s:%s",proxyIp.getIp(),proxyIp.getPort()));
                                    //options.addExtensions(new File(proxyTool.getRandomZipProxy()));//增加代理扩展
                                    webDriver = new ChromeDriver(options);//实例化
                                    //设置超时时间为3S
                                    webDriver.manage().timeouts().pageLoadTimeout(timeoutMs, TimeUnit.SECONDS);
                                    //移除已使用的代理
                                    curProxyList.remove(proxyIp);
                                }
                            }
                            if (isFetchSuccess) {
                                log.info("====================================成功抓取公司信息股票 => {}",dict.getName());
                                companyHtml = webDriver.getPageSource();

                                //提取公司名称
                                Pattern pattern = Pattern.compile("公司名称：</strong><span>(.*?)</span>");
                                Matcher matcher = pattern.matcher(companyHtml);
                                if (matcher.find()) {
                                    dictProp.setCompanyName(matcher.group(1));
                                }

                                //提取省份
                                pattern = Pattern.compile("所属地域：</strong><span>(.*?)</span>");
                                matcher = pattern.matcher(companyHtml);
                                if (matcher.find()) {
                                    dictProp.setProvince(matcher.group(1));
                                }

                                //提取各种人信息
                                pattern = Pattern.compile("<a person_id=\"(.*?)\" class=\"turnto\" href=\"javascript:void\\(0\\)\">(.*?)</a>");
                                matcher = pattern.matcher(companyHtml);
                                int nLoopCount = 0;
                                while (matcher.find()) {
                                    if (nLoopCount == 0) {
                                        dictProp.setCeo(matcher.group(2));
                                    }
                                    if (nLoopCount == 2) {
                                        dictProp.setArtificialPerson(matcher.group(2));
                                        break;
                                    }
                                    nLoopCount++;
                                }

                                pattern = Pattern.compile("<td colspan=\"3\">(.*?)</td>",Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                                matcher = pattern.matcher(companyHtml);
                                while (matcher.find()) {
                                    if (matcher.group(1).contains("办公地址")) {
                                        pattern = Pattern.compile("<span>(.*?)</span>");
                                        matcher = pattern.matcher(matcher.group(1));
                                        if (matcher.find()) {
                                            dictProp.setAddress(matcher.group(1));
                                        }
                                    }
                                }
                            }
                            //添加其他数据
                            dictProp.setId(genIdUtil.nextId());
                            dictProp.setCode(dict.getCode());
                            dictProp.setName(dict.getName());
                            groupedDictPropList.add(dictProp);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    twoDeepDictPropList.add(groupedDictPropList);
                    latch.countDown();
                    webDriver.quit();
                    log.info("=======================线程{}已完成任务，剩余未完成线程数:{}=======================",Thread.currentThread().getName(),latch.getCount());

                    //移除当前线程
                    threadNameList.remove(Thread.currentThread().getName());
                    //如果当前线程不是最后一个完成任务的，且其他线程都在休眠，则唤醒其他线程继续执行
                    if (!threadNameList.isEmpty() && waitThreadCounter.get() == latch.getCount()) {
                        //重新初始化计数器
                        counter.getAndSet(0);
                        //还有最后一个未完成时，无需继续抓取了，直接给当前剩下的代理池给最后一个未完成的线程
                        if (latch.getCount() == 1) {
                            log.info("=======================剩余最后一个线程，将当前线程的所有未使用的代理池转移过去=======================");
                            twoDeepProxyList.get(threadNameIndexMap.get(threadNameList.get(0))).addAll(curProxyList);
                        }
                        //唤醒其他阻塞的线程
                        for (;;) {
                            log.info("=======================线程{}已重置计数器",Thread.currentThread().getName());
                            synchronized (lock) {
                                lock.notifyAll();
                            }
                            log.info("=======================线程{}已唤醒其他阻塞线程，所有线程即将重新工作",Thread.currentThread().getName());
                            break;
                        }
                    }
                });
                loopIndex++;
            }
            //等待子线程全部计算完毕
            try {
                latch.await();
                threadPool.shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            long cEndTime = System.currentTimeMillis();
            log.info("==========================抓取股票基础数据工耗时：{}秒==========================",(cEndTime - startTime) / 1000);

            //删除验证码图片目录
            File dirFile = new File(stockConfig.getImgDownadDir());
            if (dirFile.exists()) {
                try {
                    SpiderUtil.deleteFile(dirFile);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("====================删除验证码目录失败====================");
                }
            }

            List<DictProp> dbDictPropList = twoDeepDictPropList.stream().flatMap(List::stream).collect(Collectors.toList());

            //先删除之前的基本信息
            dictPropMapper.deleteAll();
            mybatisBatchHandler.batchInsertOrUpdate(dbDictPropList,DictPropMapper.class,GlobalConstant.BATCH_MODDEL_INSERT);
            long endTime = System.currentTimeMillis();
            log.info("==========================抓取股票基本数据完成，当前时间：{}==========================",SpiderUtil.getCurrentTimeStr());
            log.info("==========================共耗时{}秒==========================", (endTime - startTime) / 1000);
        }
    }

    /**
     * 抓取网易的股票统计数据
     * @param maxDate
     * @param count
     */
    public List<String> crewNetEaseStockList(LocalDate maxDate,int count) {
        List<Dict> dictList = dictMapper.selectListByType(GlobalConstant.DICT_TYPE_STOCK);
        if (dictList.size() > 0) {
            //启动多线程统计
            List<List<Dict>> twoDeepList = SpiderUtil.partitionList(dictList,GlobalConstant.MAX_THREAD_COUNT);
            ExecutorService threadPool = Executors.newFixedThreadPool(twoDeepList.size());
            //计算时间
            DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

            String startDate = maxDate.plusDays(-1 * count).format(ymdFormatter);
            String endDate = maxDate.format(ymdFormatter);
            log.info("============================本次共启动{}个线程下载{}天开始的{}个网易股票CSV文件,当前时间：{}============================",twoDeepList.size(),endDate,dictList.size(),SpiderUtil.getCurrentTimeStr());
            if (twoDeepList.size() > 0) {
                //先创建目录
                File dirFile = new File(stockConfig.getCsvDownloadDir());
                if (dirFile.exists()) {
                    SpiderUtil.deleteFile(dirFile);
                }
                dirFile.mkdir();

                long cStartTime = System.currentTimeMillis();
                CountDownLatch latch = new CountDownLatch(twoDeepList.size());
                List<List<String>> allFileList = new ArrayList<>();
                twoDeepList.forEach(innerList -> {
                    threadPool.execute(() -> {
                        List<String> tmpFileList = new ArrayList<>();
                        for (Dict dict : innerList) {
                            //下载网易excel文件
                            tmpFileList.add(downNetEaseCSVFile(dict.getCode(),startDate,endDate));
                        }
                        allFileList.add(tmpFileList);
                        latch.countDown();
                    });
                });
                //等待子线程全部计算完毕
                try {
                    latch.await();
                    threadPool.shutdown();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                long cEndTime = System.currentTimeMillis();
                log.info("==========================下载网易股票数据耗时：{}秒==========================", (cEndTime - cStartTime) / 1000);
                List<String> csvFilePathList = allFileList.stream().flatMap(List::stream).collect(Collectors.toList());
                //写入文件
                String content = String.join("\r\n", csvFilePathList);
                log.info("==================content => \r\n{}",content);
                try {
                    Files.write(Paths.get(String.format("%s%scsvList.txt", stockConfig.getCsvDownloadDir(),File.separator)),content.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return csvFilePathList;
            }
        }
        return null;
    }

    /**
     * 下载网易csv文件
     * @param code
     * @param startDate
     * @param endDate
     * @return
     */
    public String downNetEaseCSVFile(String code,String startDate,String endDate) {
        try {
            CloseableHttpClient client = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(String.format(GlobalConstant.NETEASE_EXCEL_DOWNLOAD_URL,code.replace("sh","0").replace("sz","1"),startDate,endDate));

            httpGet.setHeader("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
            httpGet.setHeader("Accept-Encoding","gzip, deflate");
            httpGet.setHeader("Accept-Language","zh-CN,zh;q=0.9");
            httpGet.setHeader("Connection","keep-alive");
            httpGet.setHeader("Cookie","_ntes_nnid=a3406e68a37fd08f213787d1f43816a6,1574132031965; _ntes_nuid=a3406e68a37fd08f213787d1f43816a6; UM_distinctid=16ea21f539cda3-0f6bcd998fa747-7711a3e-384000-16ea21f539ddad; mail_psc_fingerprint=a10a6c4409bbb17b5a131103384bfaf0; P_INFO=test111@163.com|1576743154|0|other|00&99|hub&1576720535&other#hub&420100#10#0#0|&0||test111@163.com; vinfo_n_f_l_n3=5c9514b46b4d8194.1.1.1574677991509.1574678039251.1577700690573");
            httpGet.setHeader("Host","quotes.money.163.com");
            httpGet.setHeader("Upgrade-Insecure-Requests","1");
            httpGet.setHeader("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.97 Safari/537.36");

            CloseableHttpResponse response = client.execute(httpGet);

            //下载文件
            String csvPath = String.format("%s%s%s.csv", stockConfig.getCsvDownloadDir(), File.separator, code);
            File csvFile = new File(csvPath);
            OutputStream outStream = new FileOutputStream(csvFile);
            IOUtils.copy(response.getEntity().getContent(),outStream);
            log.info("=====================下载股票 => {} CSV文件完成=====================",code);
            return csvPath;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 从CSV文件读取数据并保存
     * @param csvFilePathList
     */
    public void saveQuotationFromCSVFile(List<String> csvFilePathList) {
        if (csvFilePathList == null || csvFilePathList.size() == 0) {
            try {
                long startTime = System.currentTimeMillis();
                csvFilePathList = Files.readAllLines(Paths.get(String.format("%s%scsvList.txt", stockConfig.getCsvDownloadDir(),File.separator)));
                List<List<String>> twoDeepList = SpiderUtil.partitionList(csvFilePathList,GlobalConstant.MAX_THREAD_COUNT);
                ExecutorService threadPool = Executors.newFixedThreadPool(twoDeepList.size());
                if (twoDeepList.size() > 0) {
                    long cStartTime = System.currentTimeMillis();
                    CountDownLatch latch = new CountDownLatch(twoDeepList.size());
                    Set<List<Quotation>> allQuotationList = new HashSet<>();
                    DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    twoDeepList.forEach(innerList -> {
                        threadPool.execute(() -> {
                            try {
                               for (String csvPath : innerList) {
                                   String code = csvPath.substring(csvPath.lastIndexOf(File.separator) + 1,csvPath.indexOf("."));
                                   //读取csv文件
                                   //设定UTF-8字符集，使用带缓冲区的字符输入流BufferedReader读取文件内容
                                   BufferedReader file = new BufferedReader(new InputStreamReader(new FileInputStream(csvPath), "GBK"));
                                   file.readLine(); //跳过表头所在的行

                                   // 遍历数据行并存储在名为records的ArrayList中，每一行records中存储的对象为一个String数组
                                   List<Quotation> curQuotationList = new ArrayList<>();
                                   Quotation quotation = null;
                                   String record;
                                   String date = null;
                                   BigDecimal offSetRate = null;
                                   while ((record = file.readLine()) != null) {
                                       String[] fields = record.split(",");
                                       quotation = new Quotation();
                                       try {
                                           quotation.setId(genIdUtil.nextId());
                                           quotation.setCode(code);
                                           quotation.setName(fields[2].trim());

                                           date = fields[0].trim();
                                           if (date.contains("/")) {
                                               date = date.replaceAll("/","-");
                                           }
                                           quotation.setDate(LocalDate.parse(date,ymdFormatter));
                                           quotation.setInit(BigDecimal.valueOf(Double.parseDouble(fields[7].trim())));
                                           quotation.setOpen(BigDecimal.valueOf(Double.parseDouble(fields[6].trim())));
                                           quotation.setHigh(BigDecimal.valueOf(Double.parseDouble(fields[4].trim())));
                                           quotation.setLow(BigDecimal.valueOf(Double.parseDouble(fields[5].trim())));
                                           quotation.setClose(BigDecimal.valueOf(Double.parseDouble(fields[3].trim())));
                                           quotation.setCurrent(BigDecimal.valueOf(Double.parseDouble(fields[3].trim())));
                                           //计算涨跌幅
                                           offSetRate = BigDecimal.valueOf(((Double.parseDouble(fields[3].trim()) - Double.parseDouble(fields[7].trim())) * 100 / Double.parseDouble(fields[7].trim()))).setScale(4,BigDecimal.ROUND_HALF_UP);
                                           quotation.setOffsetRate(offSetRate);
                                           quotation.setVolume(BigDecimal.valueOf(Double.parseDouble(fields[11].trim())));
                                           quotation.setVolumeAmt(BigDecimal.valueOf(Double.parseDouble(fields[12].trim())));
                                       } catch (Exception e) {
                                           e.printStackTrace();
                                       }
                                       curQuotationList.add(quotation);
                                   }
                                   allQuotationList.add(curQuotationList);
                                   // 关闭文件
                                   file.close();
                               }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            latch.countDown();
                        });
                    });
                    //等待子线程全部计算完毕
                    try {
                        latch.await();
                        threadPool.shutdown();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    long cEndTime = System.currentTimeMillis();
                    log.info("==========================读取CSV数据耗时：{}秒==========================", (cEndTime - cStartTime) / 1000);

                    List<Quotation> newQuotationList = allQuotationList.stream().flatMap(List::stream).collect(Collectors.toList());
                    //先删除之前的
                    Map<String,List<Quotation>> dateList = newQuotationList.stream().collect(Collectors.groupingBy(x -> x.getDate().format(ymdFormatter)));
                    dateList.forEach((k,v) -> {
                        quotationMapper.deleteByDate(k);
                    });
                    mybatisBatchHandler.batchInsertOrUpdate(newQuotationList,QuotationMapper.class,GlobalConstant.BATCH_MODDEL_INSERT);
                }
                long endTime = System.currentTimeMillis();
                log.info("==========================保存数据完成，共耗时{}秒==========================",(endTime - startTime) / 1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 多线程抓取新浪均价数据
     * @param maxDate
     * @param count
     */
    private List<ProxyIp> proxyIpList = null;
    private Set<String> globalProxyIpSet = null;
    private List<List<ProxyIp>> twoDeepProxyList = null;
    private Object lock = new Object();
    public void crewHisSingAvgDataByMutiThread(LocalDate maxDate,int count) {
        long startTime = System.currentTimeMillis();
        DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        log.info("==========================开始抓取均价数据统计，当前时间：{}==========================", SpiderUtil.getCurrentTimeStr());
        List<Dict> dbDictList = dictMapper.selectListByType(GlobalConstant.DICT_TYPE_STOCK);

        //将顺序打乱
        Collections.shuffle(dbDictList);

        long cStartTime = System.currentTimeMillis();
        if (dbDictList.size() > 0) {
            //获取股票行情数据列表
            List<Quotation> dbQuotationList = quotationMapper.selectListByRangeOfNDay(maxDate.format(ymdFormatter),count);
            Map<String,Quotation> codeDateQuotationMap = dbQuotationList.stream().collect(Collectors.toMap(x -> String.format("%s%s",x.getCode(),x.getDate().format(ymdFormatter)),Function.identity(),(o,n) -> n));

            Map<String,String> headerMap = new HashMap<>();
            headerMap.put("Host","money.finance.sina.com.cn");
            headerMap.put("Connection","keep-alive");
            headerMap.put("Upgrade-Insecure-Requests","1");
            headerMap.put("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.97 Safari/537.36");
            headerMap.put("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
            headerMap.put("Accept-Encoding","gzip, deflate");
            headerMap.put("Accept-Language","zh-CN,zh;q=0.9");
            String cookieFormat = "SINAGLOBAL=%s_1574389911.226197; UOR=www.baidu.com,finance.sina.com.cn,; UM_distinctid=16ea21eb2a3244-07fbe25f59d207-7711a3e-384000-16ea21eb2a4eaf; lxlrttp=%s; SUB=_2AkMquCyZf8NxqwJRmP4RzWjiaY9-wgjEieKc5N1CJRMyHRl-yD83qm0etRB6ATgCdh44MVQ8WbBjl8wUvQPJr7oE4EzP; SUBP=0033WrSXqPxfM72-Ws9jqgMF55529P9D9WWI7U.OauA0w5WcR8BlYAW9; FINA_V_S_2=sz300253; U_TRS1=00000087.7de34ac8.5de78803.282d5be0; SR_SEL=1_511; SGUID=1575620980420_54409087; ULV=1579085113351:8:1:1::%s; Apache=171.113.253.112_1579085113.807277";

            //启动多线程统计
            List<List<Dict>> twoDeepList = SpiderUtil.partitionList(dbDictList,GlobalConstant.MAX_THREAD_COUNT);
            ExecutorService threadPool = Executors.newFixedThreadPool(twoDeepList.size());
            log.info("============================本次共启动{}个线程抓取{}个股票均价数据,当前时间：{}============================",twoDeepList.size(),dbDictList.size(),SpiderUtil.getCurrentTimeStr());
            CountDownLatch latch = new CountDownLatch(twoDeepList.size());
            List<List<AvgPrice>> allAvgPriceList = new ArrayList<>();
            AtomicInteger counter = new AtomicInteger(0);

            //定义mimvp初始化时间
            long initMimvpTime = System.currentTimeMillis() / 1000 - 20 * 60 * 1000;
            //抓取米铺次数
            int crewMimvpCount = 2;
            //先抓取米铺代理列表，必须保证大于GlobalConstant.MAX_THREAD_COUNT个才够分配
            int fetchMivipCount = 0;
            for (;;) {
                log.info("==========================开始第{}次抓取米铺代理==========================",fetchMivipCount+1);
                proxyIpList = this.crewProxyList(8,2);
                //代理ip也分给每个线程
                twoDeepProxyList = SpiderUtil.partitionList(proxyIpList,twoDeepList.size());
                if (twoDeepProxyList.size() >= twoDeepList.size()) {
                    //第一次抓取到代理ip后初始化全局ip列表
                    globalProxyIpSet = new HashSet<>();
                    globalProxyIpSet.addAll(proxyIpList.stream().map(ProxyIp::getIp).collect(Collectors.toList()));
                    break;
                }
                fetchMivipCount++;
                log.info("==========================第{}次重新抓取米铺代理列表==========================",fetchMivipCount);
                if (fetchMivipCount > 3) {
                    log.error("==========================抓取米铺代理列表出现异常，请核对==========================");
                    return;
                }
            }

            //记录重新抓取米扑次数
            AtomicInteger switchCounter = new AtomicInteger(0);
            //记录阻塞数量
            AtomicInteger waitThreadCounter = new AtomicInteger(0);
            Map<String,Integer> banIpMap = new ConcurrentHashMap<>();
            Map<String,Integer> threadNameIndexMap = new HashMap<>();
            //设置线程池中的线程名称和索引映射
            List<String> threadNameList = new CopyOnWriteArrayList<>();
            for (int i = 0 ; i < latch.getCount() ; i++) {
                threadNameList.add(String.format("crew-sina-thread-%s",(i+1)));
                threadNameIndexMap.put(threadNameList.get(i),i);
            }
            //计数使用，识别所有线程是否已经全部使用完代理
            int loopIndex = 0;
            for (List<Dict> innerList : twoDeepList) {
                //随机休眠，方便协作
                try {
                    Thread.sleep(SpiderUtil.getRandomNum(1,20));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                final int index = loopIndex;
                threadPool.execute(() -> {
                    //设置线程名
                    String threadName = threadNameList.get(index);
                    Thread.currentThread().setName(threadName);
                    //当前线程使用的代理列表
                    List<ProxyIp> curProxyList = twoDeepProxyList.get(threadNameIndexMap.get(threadName));

                    List<AvgPrice> curAvgPriceList = new ArrayList<>();
                    AvgPrice avgPrice = null;
                    JSONObject dataObj = null;
                    String key = null;
                    String day = null;

                    //从可用代理ip列表中随机获取一个使用，延长调用间隔，可一定程度降低封ip概率
                    ProxyIp proxyIp = curProxyList.size() == 1 ? curProxyList.get(0) : SpiderUtil.getRandomEntity(curProxyList);
                    //配置独立的httpclient客户端，抓取时只用重试1次，提升效率
                    HttpClientUtil clientUtil = new HttpClientUtil();
                    clientUtil.setMaxRetryCount(1);
                    clientUtil.setProxyPropArr(new String[]{proxyIp.getIp(),String.valueOf(proxyIp.getPort())});
                    headerMap.put("Cookie",String.format(cookieFormat,proxyIp.getIp(),System.currentTimeMillis(),System.currentTimeMillis()));

                    DateTimeFormatter dFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    for (Dict dict : innerList) {
                        String url = String.format(GlobalConstant.SINA_AVG_DATA_URL,dict.getCode(),count);
                        log.debug("==============sina url => {}",url);
                        ResponseEntity responseEntity = null;
                        int rCount = 0;
                        for (;;) {
                            //每个线程都还剩下最后一个时，就要重新抓取代理，并分配了
                            if (curProxyList.size() == 0) {
                                //先需要计数，再检查是否全部线程都使用完毕，最后一个完成的，需重新抓取，并唤醒所有阻塞的线程继续工作
                                counter.getAndAdd(1);
                                //每个线程的代理列表都剩下最后一个
                                if (counter.get() == latch.getCount()) {
                                    log.info("=======================线程{}最后使用完代理，即将重新抓取米铺代理",Thread.currentThread().getName());
                                    switchCounter.getAndAdd(1);
                                    log.info("==========================线程{}第{}次抓取米铺代理==========================",Thread.currentThread().getName(),switchCounter.get());
                                    //可能抓取的代理全部被新浪加入了黑名单，需要重复抓取，直到有未加入黑名单的代理ip为止
                                    int fetchCount = 0;
                                    List<ProxyIp> tmpProxyIpList = null;
                                    List<String> curInitProxyIpList = null;
                                    for (;;) {
                                        log.info("=========================抓取前代理列表数量 => {}",proxyIpList.size());
                                        //重新抓取代理ip列表，并加入代理ip池
                                        tmpProxyIpList = this.crewProxyList((int)latch.getCount(),1);
                                        curInitProxyIpList = tmpProxyIpList.stream().map(ProxyIp::getIp).collect(Collectors.toList());
                                        //提取出本次抓取的不重复的代理ip列表
                                        curInitProxyIpList.removeAll(globalProxyIpSet);
                                        globalProxyIpSet.addAll(curInitProxyIpList);
                                        List<String> curFinalProxyIpList = curInitProxyIpList;
                                        //本次只添加不重复的代理ip列表
                                        proxyIpList.addAll(tmpProxyIpList.stream().filter(x -> curFinalProxyIpList.contains(x.getIp())).collect(Collectors.toList()));
                                        log.info("=========================抓取后代理列表数量 => {}",proxyIpList.size());
                                        //过滤掉已经被拉黑的ip
                                        log.info("=========================黑名单数量 => {}",banIpMap.size());
                                        proxyIpList = proxyIpList.stream().filter(x -> !banIpMap.containsKey(x.getIp())).collect(Collectors.toList());
                                        if (proxyIpList.size() >= latch.getCount()) {
                                            break;
                                        }
                                        fetchCount++;
                                        log.info("==========================抓取的米铺代理不足以分配{}个线程使用，第{}次重新抓取==========================",latch.getCount(),fetchCount);
                                    }

                                    //重新分配代理列表
                                    twoDeepProxyList = SpiderUtil.partitionList(proxyIpList,(int)latch.getCount());
                                    log.info("====================当前线程池数量 count => {}",twoDeepProxyList.size());
                                    //重新分配线程和索引映射
                                    threadNameIndexMap.clear();
                                    for (int i = 0 ; i < latch.getCount() ; i++) {
                                        threadNameIndexMap.put(threadNameList.get(i),i);
                                    }
                                    threadNameIndexMap.forEach((k,v) -> log.info("=============线程索引映射 {} => {}",k,v));

                                    //新分配的列表重新加入
                                    curProxyList.addAll(twoDeepProxyList.get(threadNameIndexMap.get(Thread.currentThread().getName())));
                                    //如果不是最后一个未完成任务的，需要唤醒其他休眠线程继续工作
                                    if (latch.getCount() > 1) {
                                        //重新初始化计数器
                                        counter.getAndSet(0);
                                        //唤醒全部阻塞的线程继续抓取
                                        for (;;) {
                                            synchronized (lock) {
                                                lock.notifyAll();
                                            }
                                            log.info("=======================线程{}已唤醒其他阻塞线程，所有线程即将重新工作",Thread.currentThread().getName());
                                            break;
                                        }
                                    }
                                } else {
                                    //自己完成，其他线程未用完，先阻塞等其他都完成了，再重新抓取
                                    synchronized (lock) {
                                        try {
                                            log.info("=======================线程{}代理IP已用完，开始阻塞",Thread.currentThread().getName());
                                            waitThreadCounter.getAndAdd(1);
                                            lock.wait();
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    //唤醒后也重新抓取分配好了
                                    waitThreadCounter.getAndAdd(-1);
                                    log.info("=======================线程{}被唤醒，继续抓取",Thread.currentThread().getName());
                                    curProxyList.addAll(twoDeepProxyList.get(threadNameIndexMap.get(Thread.currentThread().getName())));
                                }
                            }
                            responseEntity = clientUtil.recursiveHttpRequest(GlobalConstant.HTTP_GET,0,url,null,headerMap,GlobalConstant.CHARASET_GBK);
                            if (responseEntity != null && responseEntity.getCode() == 200 && responseEntity.getContent().contains("open")) {
                                break;
                            } else {
                                if (responseEntity != null && responseEntity.getContent().contains("异常访问")) {
                                    log.error("===========================线程{}抓取异常，状态码 => {} 代理IP【{}】已被加入黑名单===========================",Thread.currentThread().getName(),responseEntity.getCode(),proxyIp.getIp());
                                } else {
                                    log.error("==========================线程{}代理【{}:{}】缓慢或异常==========================",Thread.currentThread().getName(),proxyIp.getIp(),proxyIp.getPort());
                                }
                                //不管ip被拉黑或者访问缓慢都需要加入黑名单
                                banIpMap.put(proxyIp.getIp(),0);
                            }
                            //移除废弃的代理IP
                            curProxyList.remove(proxyIp);
                            if (curProxyList.size() > 0) {
                                rCount++;
                                //切换代理
                                proxyIp = curProxyList.get(0);
                                log.info("==========================线程{}第{}次切换代理，IP【{}】==========================",Thread.currentThread().getName(),rCount,proxyIp.getIp());
                                clientUtil.setProxyPropArr(new String[]{proxyIp.getIp(),String.valueOf(proxyIp.getPort())});
                                headerMap.put("Cookie",String.format(cookieFormat,proxyIp.getIp(),System.currentTimeMillis(),System.currentTimeMillis()));
                            }
                        }
                        //抓取成功，解析抓取结果
                        log.info("======================线程{}成功抓取股票{} => {}",Thread.currentThread().getName(),dict.getCode(),dict.getName());
                        JSONArray dataArr = JSON.parseArray(responseEntity.getContent());
                        if (dataArr.size() > 0) {
                            for (int i  = 0 ; i < dataArr.size() ; i++) {
                                dataObj = dataArr.getJSONObject(i);
                                day = dataObj.getString("day");

                                avgPrice = new AvgPrice();
                                avgPrice.setId(genIdUtil.nextId());
                                avgPrice.setCode(dict.getCode());
                                avgPrice.setName(dict.getName());
                                try {
                                    avgPrice.setDate(LocalDate.parse(day,dFormat));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                key = String.format("%s%s",dict.getCode(),day);
                                if (!codeDateQuotationMap.containsKey(key)) {
                                    continue;
                                }
                                if (codeDateQuotationMap.get(key).getVolume().doubleValue() > 0d) {
                                    avgPrice.setAvg(codeDateQuotationMap.get(key).getVolumeAmt().divide(codeDateQuotationMap.get(key).getVolume(),3,BigDecimal.ROUND_HALF_UP));
                                }
                                if (dataObj.containsKey("ma_price5")) {
                                    avgPrice.setAvg5(dataObj.getBigDecimal("ma_price5"));
                                }
                                if (dataObj.containsKey("ma_price10")) {
                                    avgPrice.setAvg10(dataObj.getBigDecimal("ma_price10"));
                                }
                                if (dataObj.containsKey("ma_price20")) {
                                    avgPrice.setAvg20(dataObj.getBigDecimal("ma_price20"));
                                }
                                if (dataObj.containsKey("ma_price30")) {
                                    avgPrice.setAvg30(dataObj.getBigDecimal("ma_price30"));
                                }
                                curAvgPriceList.add(avgPrice);
                            }
                        }
                    }
                    allAvgPriceList.add(curAvgPriceList);
                    latch.countDown();
                    log.info("=======================线程{}已完成任务，剩余未完成线程数:{}=======================",Thread.currentThread().getName(),latch.getCount());

                    //移除当前线程
                    threadNameList.remove(Thread.currentThread().getName());
                    //如果当前线程不是最后一个完成任务的，且其他线程都在休眠，则唤醒其他线程继续执行
                    if (threadNameList.size() > 0 && waitThreadCounter.get() == latch.getCount()) {
                        //重新初始化计数器
                        counter.getAndSet(0);
                        //还有最后一个未完成时，无需继续抓取了，直接给当前剩下的代理池给最后一个未完成的线程
                        if (latch.getCount() == 1) {
                            log.info("=======================剩余最后一个线程，将当前线程的所有未使用的代理池转移过去=======================");
                            twoDeepProxyList.get(threadNameIndexMap.get(threadNameList.get(0))).addAll(curProxyList);
                        } else {
                            //将当前未使用完的ip分配给其中一个休眠的线程，让他可以最后调度
                            twoDeepProxyList.get(threadNameIndexMap.get(threadNameList.get(0))).add(proxyIp);
                        }
                        //唤醒其他阻塞的线程
                        for (;;) {
                            log.info("=======================线程{}已重置计数器",Thread.currentThread().getName());
                            synchronized (lock) {
                                lock.notifyAll();
                            }
                            log.info("=======================线程{}已唤醒其他阻塞线程，所有线程即将重新工作",Thread.currentThread().getName());
                            break;
                        }
                    }
                });
                loopIndex++;
            }
            //等待子线程全部计算完毕
            try {
                latch.await();
                threadPool.shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            long cEndTime = System.currentTimeMillis();
            log.info("==========================抓取均价数据耗时：{}秒==========================",(cEndTime - cStartTime) / 1000);

            //删除验证码图片目录
            File dirFile = new File(stockConfig.getImgDownadDir());
            if (dirFile.exists()) {
                try {
                    SpiderUtil.deleteFile(dirFile);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("====================删除验证码目录失败====================");
                }
            }

            List<AvgPrice> newAvgPriceList = allAvgPriceList.stream().flatMap(List::stream).collect(Collectors.toList());
            //先删除当天之前统计的涨停跌停数据
            Map<String,List<AvgPrice>> dateAvgPriceMap = newAvgPriceList.stream().collect(Collectors.groupingBy(x -> x.getDate().format(ymdFormatter)));
            dateAvgPriceMap.forEach((k,v) -> {
                avgPriceMapper.deleteByDate(k);
            });
            //批量插入均价数据
            mybatisBatchHandler.batchInsertOrUpdate(newAvgPriceList,AvgPriceMapper.class,GlobalConstant.BATCH_MODDEL_INSERT);
        }
        long endTime = System.currentTimeMillis();
        log.info("==========================抓取新浪均价数据完成，当前时间：{}==========================", SpiderUtil.getCurrentTimeStr());
        log.info("==========================共耗时{}秒==========================", (endTime - startTime) / 1000);
    }

    /**
     * 更新股票近10天的趋势
     * @param date
     */
    public void updateLast10Trend(LocalDate date) {
        long startTime = System.currentTimeMillis();
        log.info("==========================开始更新股票最近趋势数据，当前时间：{}==========================", SpiderUtil.getCurrentTimeStr());
//        if (SpiderUtil.isWeekendOfToday(date)) {
//            log.error("==========================周末时间不开盘，忽略本次任务==========================");
//            return;
//        }
        DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String curDate = date.format(ymdFormatter);
        //查询股票均价数据列表
        List<AvgPrice> dbAvgPriceList = avgPriceMapper.selectListByDate(curDate);

        //查询最近5个交易日的均价列表
        int nDay = 10;
        List<AvgPrice> last10AvgPriceList = avgPriceMapper.selectListByRangeOfNDay(curDate,nDay+1);
        //过滤掉30日均价为空的数据
        last10AvgPriceList = last10AvgPriceList.stream().filter(x -> x.getAvg30() != null && !curDate.equals(x.getDate().format(ymdFormatter))).collect(Collectors.toList());
        Map<String,List<AvgPrice>> codeAvgPriceMap = last10AvgPriceList.stream().collect(Collectors.groupingBy(AvgPrice::getCode));

        //根据线程数分配
        List<List<AvgPrice>> twoDeepList = SpiderUtil.partitionList(dbAvgPriceList,GlobalConstant.MAX_THREAD_COUNT);
        log.info("============================本次共启动{}个线程，计算【{}】日{}个股票涨跌趋势数据,当前时间：{}============================",twoDeepList.size(),curDate,dbAvgPriceList.size(),SpiderUtil.getCurrentTimeStr());
        if (twoDeepList.size() > 0) {
            ExecutorService threadPool = Executors.newFixedThreadPool(GlobalConstant.MAX_THREAD_COUNT);
            CountDownLatch latch = new CountDownLatch(twoDeepList.size());
            twoDeepList.forEach(innerList -> {
                threadPool.execute(() -> {
                    innerList.forEach(avgPrice -> {
                        try {
                            if (codeAvgPriceMap.containsKey(avgPrice.getCode())) {
                                List<AvgPrice> curAvgPriceList = codeAvgPriceMap.get(avgPrice.getCode());
                                //date从小到大排序
                                curAvgPriceList.sort(Comparator.comparing(AvgPrice::getDate));
                                if (curAvgPriceList.size() < nDay) {
                                    avgPrice.setLast10Trend(-1000);
                                    avgPrice.setLast10MonthTrend(-1000);
                                } else {
                                    //超过半数上涨，则趋势上涨
                                    int upDays = 0;
                                    int downDays = 0;
                                    int minLimitDay = (int)(nDay * 0.6);
                                    //前10日的10日线趋势
                                    //默认平盘
                                    avgPrice.setLast10Trend(0);
                                    for (int i = 0 ; i < nDay - 1 ; i++) {
                                        //前10日的10日线趋势
                                        if (curAvgPriceList.get(i+1).getAvg10().compareTo(curAvgPriceList.get(i).getAvg10()) > 0) {
                                            upDays++;
                                        }
                                        if (curAvgPriceList.get(i+1).getAvg10().compareTo(curAvgPriceList.get(i).getAvg10()) < 0) {
                                            downDays++;
                                        }
                                    }
                                    //根据上涨天数判断当前股票10日均价趋势
                                    if (upDays == nDay - 1) {
                                        avgPrice.setLast10Trend(99);
                                    } else if(upDays >= minLimitDay) {
                                        avgPrice.setLast10Trend(1);
                                    }
                                    if(downDays == nDay - 1) {
                                        avgPrice.setLast10Trend(-99);
                                    } else if(downDays >= minLimitDay) {
                                        avgPrice.setLast10Trend(-1);
                                    }

                                    //前10日的30日线趋势
                                    //默认平盘
                                    upDays = 0;
                                    downDays = 0;
                                    avgPrice.setLast10MonthTrend(0);
                                    for (int i = 0 ; i < nDay - 1 ; i++) {
                                        if (curAvgPriceList.get(i+1).getAvg30().compareTo(curAvgPriceList.get(i).getAvg30()) > 0) {
                                            upDays++;
                                        }
                                        if (curAvgPriceList.get(i+1).getAvg30().compareTo(curAvgPriceList.get(i).getAvg30()) < 0) {
                                            downDays++;
                                        }
                                    }
                                    //根据上涨天数判断当前股票30日均价趋势
                                    if (upDays == nDay - 1) {
                                        avgPrice.setLast10MonthTrend(99);
                                    } else if(upDays >= minLimitDay) {
                                        avgPrice.setLast10MonthTrend(1);
                                    }
                                    if(downDays == nDay - 1) {
                                        avgPrice.setLast10MonthTrend(-99);
                                    } else if(downDays >= minLimitDay) {
                                        avgPrice.setLast10MonthTrend(-1);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    latch.countDown();
                });
            });
            try {
                latch.await();
                threadPool.shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.info("==========================统计完成，当前时间：{}==========================", SpiderUtil.getCurrentTimeStr());
            long dbStartTime = System.currentTimeMillis();
            avgPriceMapper.deleteByDate(curDate);
            mybatisBatchHandler.batchInsertOrUpdate(dbAvgPriceList,AvgPriceMapper.class,GlobalConstant.BATCH_MODDEL_INSERT);
            long dbEndTime = System.currentTimeMillis();
            log.info("==========================入库完成，当前时间：{}==========================", SpiderUtil.getCurrentTimeStr());
            log.info("==========================共耗时{}秒==========================", (dbEndTime - dbStartTime) / 1000);
        }
        long endTime = System.currentTimeMillis();
        log.info("==========================更新股票最近趋势数据完成，当前时间：{}==========================", SpiderUtil.getCurrentTimeStr());
        log.info("==========================共耗时{}秒==========================", (endTime - startTime) / 1000);
    }

    /**
     * 根据配置类型获取不同途径的代理列表
     * @param threadCount
     * @param multiple
     * @return
     */
    public List<ProxyIp> crewProxyList(int threadCount,int multiple) {
        List<ProxyIp> proxyIpList = null;
        switch (stockConfig.getProxyType()) {
            case 1:
                proxyIpList = crewMimvpProxyList(threadCount,multiple);
                break;
            case 2:
                proxyIpList = crewXiongmaoProxyList(threadCount,multiple);
                break;
        }
        return proxyIpList;
    }

    /**
     * 抓取米扑代理列表
     * @param threadCount
     * @param multiple
     * @return
     */
    public List<ProxyIp> crewMimvpProxyList(int threadCount,int multiple) {
        List<ProxyIp> proxyIpList = new ArrayList<>();

        long initMimvpTime = System.currentTimeMillis();
        Map<String,String> headerMap = new HashMap<>();
        headerMap.put("Host","proxy.mimvp.com");
        headerMap.put("Connection","keep-alive");
        headerMap.put("Upgrade-Insecure-Requests","1");
        headerMap.put("User-Agent","Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.138 Safari/537.36");
        headerMap.put("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
        headerMap.put("Sec-Fetch-Site","none");
        headerMap.put("Sec-Fetch-Mode","navigate");
        headerMap.put("Sec-Fetch-Dest", "document");
        headerMap.put("Accept-Encoding","gzip, deflate, br");
        headerMap.put("Accept-Language","en-US,en;q=0.9");
        String cookieFormat = "MIMVPSESSID=vh839bkvcho72u8n68orgtfa8m; Hm_lvt_51e3cc975b346e7705d8c255164036b3=1589029467; Hm_lpvt_51e3cc975b346e7705d8c255164036b3=%s";
        headerMap.put("Cookie",String.format(cookieFormat,initMimvpTime,initMimvpTime / 1000 - 60 * 1000));

        //提取代理列表
        ResponseEntity responseEntity = null;
        for (int i = 0 ; i < multiple ; i++) {
            responseEntity = clientUtil.recursiveHttpRequest(GlobalConstant.HTTP_GET,0, String.format(GlobalConstant.MIMVP_FREE_PROXY_URL,Math.random()),null,headerMap,GlobalConstant.CHARASET_UTF8);
            if (responseEntity.getCode() == 200 && StringUtils.isNotEmpty(responseEntity.getContent())) {
                Document doc = Jsoup.parse(responseEntity.getContent());
                doc.select("tr:gt(0)").forEach(x -> {
                    if (x.select("td:eq(7)").attr("title").startsWith("0") && x.select("td:eq(8)").attr("title").startsWith("0")) {
                        String localImgPath = downImgToLocal(String.format("%s%s","https://proxy.mimvp.com/", x.select("td:eq(2) > img").attr("src")));
                        if (StringUtils.isNotEmpty(localImgPath)) {
                            Integer port = null;
                            try {
                                port = Integer.parseInt(MimvpOCRUtil.ocrText(localImgPath));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            if (port != null) {
                                ProxyIp proxyIp = new ProxyIp();
                                proxyIp.setId(genIdUtil.nextId());
                                proxyIp.setIp(x.select("td:eq(1)").text());
                                proxyIp.setPort(port);
                                proxyIp.setType(x.select("td:eq(3)").attr("title").toUpperCase());
                                proxyIpList.add(proxyIp);
                            }
                        }
                    }
                });
            }
            //抓取时候适当休眠
            try {
                Thread.sleep(5 * 1000,20 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (proxyIpList.size() > 0) {
            proxyIpList.forEach(proxy -> {
                log.info("{}:{}",proxy.getIp(),proxy.getPort());
            });
            //存储入库
            proxyIpMapper.deleteAll();
            mybatisBatchHandler.batchInsertOrUpdate(proxyIpList,ProxyIpMapper.class,GlobalConstant.BATCH_MODDEL_INSERT);
        }
        return proxyIpList;
    }

    /**
     * 提取熊猫代理列表
     * @param threadCount
     * @param multiple
     * @return
     */
    public List<ProxyIp> crewXiongmaoProxyList(int threadCount,int multiple) {
        List<ProxyIp> proxyIpList = new ArrayList<>();
        try {
            String url = String.format(GlobalConstant.XIONGMAO_PROXY_URL,xiongmaoConfig.getSecret(),xiongmaoConfig.getOrderNo(),threadCount * multiple);
            ResponseEntity responseEntity = clientUtil.recursiveHttpRequest(GlobalConstant.HTTP_GET,0, url,null,null,GlobalConstant.CHARASET_UTF8);
            if (responseEntity.getCode() == 200 && StringUtils.isNotEmpty(responseEntity.getContent())) {
                String[] strProxyList = responseEntity.getContent().split("\r\n");
                String []proxyArr = null;
                for (String proxy : strProxyList) {
                    proxyArr = proxy.split(":");
                    ProxyIp proxyIp = new ProxyIp();
                    proxyIp.setId(genIdUtil.nextId());
                    proxyIp.setIp(proxyArr[0]);
                    proxyIp.setPort(Integer.parseInt(proxyArr[1]));
                    proxyIp.setType("http/https");
                    proxyIpList.add(proxyIp);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (proxyIpList.size() > 0) {
            proxyIpList.forEach(proxy -> {
                log.info("{}:{}",proxy.getIp(),proxy.getPort());
            });
        }
        return proxyIpList;
    }

    /**
     * 下载图片到本地
     * @param imgUrl
     * @return
     */
    private String downImgToLocal(String imgUrl) {
        try {
            //下载图片
            CloseableHttpClient client = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(imgUrl);
            httpGet.setHeader("Host", "proxy.mimvp.com");
            httpGet.setHeader("Connection", "keep-alive");
            httpGet.setHeader("Cache-Control", "max-age=0");
            httpGet.setHeader("Upgrade-Insecure-Requests", "1");
            httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.70 Safari/537.36");
            httpGet.setHeader("Sec-Fetch-User", "?1");
            httpGet.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
            httpGet.setHeader("Sec-Fetch-Site", "none");
            httpGet.setHeader("Sec-Fetch-Mode", "navigate");
            httpGet.setHeader("Accept-Encoding", "gzip, deflate, br");
            httpGet.setHeader("Accept-Language", "zh-CN,zh;q=0.9");
            httpGet.setHeader("Cookie", "Hm_lvt_2470f08b0a4e8514a3d12a641ddcb46d=1566062072; PHPSESSID=bte6259ou8hrbkgre4mtp2vajl; Hm_lvt_51e3cc975b346e7705d8c255164036b3=1578219350,1578219363,1578219647,1578219947; Hm_lpvt_51e3cc975b346e7705d8c255164036b3=1578225593");

            CloseableHttpResponse response = client.execute(httpGet);
            BufferedImage bi = ImageIO.read(response.getEntity().getContent());
            File dirFile = new File(stockConfig.getImgDownadDir());
            if (!dirFile.exists()) {
                dirFile.mkdir();
            }
            String imgPath = String.format("%s%s%s.png", stockConfig.getImgDownadDir(), File.separator, UUID.randomUUID().toString());
            File imgFile = new File(imgPath);
            ImageIO.write(bi, "png", imgFile);
            return imgPath;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 检测同步任务是否完成
     */
    public void checkSyncTaskStatus() {
        if (SpiderUtil.isWeekendOfToday(LocalDate.now())) {
            log.info("==========================周末不开盘==========================");
            return;
        }
        List<SysDict> dictList = sysDictMapper.selectByType(GlobalConstant.SYS_DICT_TYPE_QUALITY_STOCKS);
        if (dictList != null && dictList.size() > 0) {
            //获取今天20点的时间
            Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY,20);
            c.set(Calendar.MINUTE,0);
            c.set(Calendar.SECOND,0);
            if (dictList.get(0).getCreateDate().getTime() < c.getTimeInMillis()) {
                String warnMsg = "### 警告信息列表：\r\n" + "- 同步任务失败，请核对";
                sendDingDingGroupMsg(warnMsg);
            }
        }
    }

    /**
     * 提取连续涨停股票和监控列表
     * 1.最近15天没涨停的，抓1版
     * 2.昨天涨停的。抓2版
     * 3.前几天有连版的，抓反包版
     * @param date
     */
    public void fetchCuDataList(LocalDate date) {
        DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String strDate = date.format(ymdFormatter);

        //股票属性
        List<Dict> dbDictList = dictMapper.selectListByType(GlobalConstant.DICT_TYPE_STOCK);
        Map<String,Dict> codeDictMap = dbDictList.stream().collect(Collectors.toMap(Dict::getCode,Function.identity()));

        //黑名单列表
        List<SysDict> backlistList = sysDictMapper.selectByType(GlobalConstant.SYS_DICT_TYPE_BACKLIST);
        Map<String,SysDict> codeBackListMap = backlistList.stream().collect(Collectors.toMap(SysDict::getValue,Function.identity(),(o,n) -> n));

        List<Quotation> quotationList = quotationMapper.selectListByDate(strDate);
        Map<String,Quotation> codeQuoMap = quotationList.stream().collect(Collectors.toMap(Quotation::getCode,Function.identity(),(o,n) ->n));

        //处理均价信息，计算最大最小均价
        Map<String,AvgPriceVo> codeAvgMap = new HashMap<>();
        List<AvgPrice> avgPriceList = avgPriceMapper.selectListByDate(strDate);
        if (avgPriceList != null && avgPriceList.size() > 0) {
            avgPriceList.forEach(avg -> {
                AvgPriceVo avgVo = new AvgPriceVo();
                BeanUtils.copyProperties(avg,avgVo);
                //计算最大最小均价信息
                List<BigDecimal> curAvgList = new ArrayList<>();
                curAvgList.add(avg.getAvg5());
                curAvgList.add(avg.getAvg10());
                curAvgList.add(avg.getAvg20());
                curAvgList.add(avg.getAvg30());
                curAvgList = curAvgList.stream().filter(Objects::nonNull).collect(Collectors.toList());
                if (curAvgList.size() > 0) {
                    avgVo.setAvgMin(Collections.min(curAvgList));
                    avgVo.setAvgMax(Collections.max(curAvgList));
                }
                codeAvgMap.put(avg.getCode(),avgVo);
            });
        }

        //按照当前的高地位过滤
        List<RelativePositionVo> positionList = quotationMapper.selectCurPosition(strDate);
        Map<String,RelativePositionVo> codePositionMap = positionList.stream().collect(Collectors.toMap(RelativePositionVo::getCode,Function.identity(),(o,n) -> n));

        //按照活跃度过滤
        List<String> inactiveCodeList = quotationMapper.selectInactiveQuoList();

        //最近7天涨停列表
        List<LastNUpLimitVo> last7DataList = getLastNUpLimitDataList(strDate,7,1);
        Map<String,Integer> codeUpMap = last7DataList.stream().collect(Collectors.toMap(LastNUpLimitVo::getCode,LastNUpLimitVo::getCount,(o,n) -> n));

        //最近5天涨停列表
        List<LastNUpLimitVo> last5DataList = getLastNUpLimitDataList(strDate,5,1);
        Map<String,Integer> last5CodeUpMap = last5DataList.stream().collect(Collectors.toMap(LastNUpLimitVo::getCode,LastNUpLimitVo::getCount,(o,n) -> n));

        //提取出最近5日被5日均线压制的，并过滤掉
        List<Quotation> lastHis5QuoList = quotationMapper.selectListByRangeOfNDay(strDate,5);
        List<AvgPrice> last5AvgList = avgPriceMapper.selectListByRangeOfNDay(strDate,5);
        Map<String,BigDecimal> codeAvgSumMap = new HashMap<>();
        if (last5AvgList.size() > 0) {
            Map<String,List<AvgPrice>> codeLast5AvgMap = last5AvgList.stream().collect(Collectors.groupingBy(AvgPrice::getCode));
            codeLast5AvgMap.forEach((k,v) -> {
                codeAvgSumMap.put(k,v.stream().map(AvgPrice::getAvg5).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
            });
        }
        Map<String,BigDecimal> stifleCodeQuoMap = new HashMap<>();
        if (lastHis5QuoList.size() > 0) {
            Map<String,List<Quotation>> codeLast5QuoMap = lastHis5QuoList.stream().collect(Collectors.groupingBy(Quotation::getCode));
            codeLast5QuoMap.forEach((k,v) -> {
                //过滤出被5日均线压制的股票，即sum(high5) <= sum(avg5)
                if (codeAvgSumMap.containsKey(k)) {
                    BigDecimal sum5QuoAmt = v.stream().map(Quotation::getHigh).reduce(BigDecimal.ZERO, BigDecimal::add);
                    if (v.size() < 5 || sum5QuoAmt.compareTo(codeAvgSumMap.get(k)) <= 0) {
                        stifleCodeQuoMap.put(k,sum5QuoAmt);
                    }
                }
            });
        }

        //提取公共需过滤的股票
        Map<String,String> commRemoveCodeMap = new HashMap<>();
        if (dbDictList.size() > 0) {
            String c = null;
            for (Dict dict : dbDictList) {
                c = dict.getCode();
                //过滤掉ST
                if (dict.getName().toUpperCase().contains("ST")) {
                    commRemoveCodeMap.put(c,dict.getName());
                    continue;
                }
                //过滤掉创业板和科创版
                if (c.contains("sz300") || c.contains("sh688")) {
                    commRemoveCodeMap.put(c,dict.getName());
                    continue;
                }
                //过滤掉上市不足30天的股票
                if (codeAvgMap.containsKey(c) && codeAvgMap.get(c).getAvg30() == null) {
                    commRemoveCodeMap.put(c,dict.getName());
                    continue;
                }
                //过滤掉黑名单
                if (codeBackListMap.containsKey(c)) {
                    commRemoveCodeMap.put(c,dict.getName());
                    continue;
                }
                //市值25-300E
                if (codeDictMap.get(c) == null || codeDictMap.get(c).getCirMarketValue() == null || codeDictMap.get(c).getCirMarketValue().doubleValue() < 20 || codeDictMap.get(c).getCirMarketValue().doubleValue() > 100) {
                    commRemoveCodeMap.put(c,dict.getName());
                    continue;
                }
                //过滤掉非正常数据
                if (!codeQuoMap.containsKey(c) || !codeAvgMap.containsKey(c) || !codePositionMap.containsKey(c)) {
                    commRemoveCodeMap.put(c,dict.getName());
                    continue;
                }
                //股价4-200
                if (codeQuoMap.get(c).getCurrent().doubleValue() < 2 || codeQuoMap.get(c).getCurrent().doubleValue() > 80) {
                    commRemoveCodeMap.put(c,dict.getName());
                    continue;
                }
                //成交量低于2000w的过滤掉
                if (codeQuoMap.get(c).getVolumeAmt().doubleValue() < 25000000) {
                    commRemoveCodeMap.put(c,dict.getName());
                    continue;
                }
                //多头向下的过滤掉
                if (codeAvgMap.get(c).getLast10Trend() == -99 && codeAvgMap.get(c).getLast10MonthTrend() == -99) {
                    commRemoveCodeMap.put(c,dict.getName());
                    continue;
                }
                //当日最高价低于最小均价的过滤掉
                if (codeQuoMap.get(c).getHigh().doubleValue() <= codeAvgMap.get(c).getAvgMin().doubleValue()) {
                    commRemoveCodeMap.put(c,dict.getName());
                    continue;
                }
                //当日均低于最小均价的过滤掉
                if (codeAvgMap.get(c).getAvg().doubleValue() <= codeAvgMap.get(c).getAvgMin().doubleValue()) {
                    commRemoveCodeMap.put(c,dict.getName());
                    continue;
                }
                //当日最近10天趋势是全部下降
                if (codeAvgMap.get(c).getLast10Trend().doubleValue() == -99d) {
                    commRemoveCodeMap.put(c,dict.getName());
                    continue;
                }
                //当日一阴破4线的过滤掉
                if (codeQuoMap.get(c).getOffsetRate().doubleValue() < 0 && codeQuoMap.get(c).getOpen().doubleValue() > codeAvgMap.get(c).getAvgMax().doubleValue()
                        && codeQuoMap.get(c).getClose().doubleValue() < codeAvgMap.get(c).getAvgMin().doubleValue()) {
                    commRemoveCodeMap.put(c,dict.getName());
                    continue;
                }
                //当日最近10天和最近30天趋势全是上升的过滤掉
                if (codeAvgMap.get(c).getLast10Trend().doubleValue() == 99d && codeAvgMap.get(c).getLast10MonthTrend().doubleValue() == 99d) {
                    commRemoveCodeMap.put(c,dict.getName());
                    continue;
                }
                //多头向上的过滤掉,即last10trend > 0 && close > m5 && && m5 > m10 > m20 > m30
                if (codeAvgMap.get(c).getLast10Trend().doubleValue() > 0d
                        && codeQuoMap.get(c).getClose().doubleValue() >= codeAvgMap.get(c).getAvg5().doubleValue()
                        && codeAvgMap.get(c).getAvg5().doubleValue() > codeAvgMap.get(c).getAvg10().doubleValue()
                        && codeAvgMap.get(c).getAvg10().doubleValue() > codeAvgMap.get(c).getAvg20().doubleValue()
                        && codeAvgMap.get(c).getAvg20().doubleValue() > codeAvgMap.get(c).getAvg30().doubleValue()
                ) {
                    commRemoveCodeMap.put(c,dict.getName());
                    continue;
                }
                //月线压制10日线的过滤掉
                if (codeAvgMap.get(c).getAvg30().doubleValue() > codeAvgMap.get(c).getAvg20().doubleValue() && codeAvgMap.get(c).getAvg20().doubleValue() > codeAvgMap.get(c).getAvg10().doubleValue()) {
                    commRemoveCodeMap.put(c,dict.getName());
                    continue;
                }
                //相对高地位20-80
                if (codePositionMap.get(c).getPRate() < 20 || codePositionMap.get(c).getPRate() > 80) {
                    commRemoveCodeMap.put(c,dict.getName());
                    continue;
                }
                //不活跃的过滤掉
                if (inactiveCodeList.contains(c)) {
                    commRemoveCodeMap.put(c,dict.getName());
                }
            }
        }

        List<CuMonitor> hisCuMonitorList = new ArrayList<>();
        //提取2天前有连版的
        //获取最近10天的最高价和最低价
        String minDate = date.plusDays(-20).format(ymdFormatter);
        List<Quotation> lastNQuoList = quotationMapper.selectListByDateRange(minDate,strDate);
        Map<String,List<Quotation>> initCodeQuoListMap = lastNQuoList.stream().collect(Collectors.groupingBy(Quotation::getCode));
        Map<String,LastNPriceVo> codeLastNMap = new HashMap<>();
        if (initCodeQuoListMap.size() > 0) {
            initCodeQuoListMap.forEach((k,v) -> {
                List<Quotation> lastNQList = v.stream().sorted(Comparator.comparing(Quotation::getDate).reversed()).limit(10).collect(Collectors.toList());
                LastNPriceVo lastNPriceVo = new LastNPriceVo();
                lastNPriceVo.setCode(k);
                lastNPriceVo.setMaxPrice(BigDecimal.valueOf(lastNQList.stream().mapToDouble(x -> x.getHigh().doubleValue()).max().getAsDouble()));
                lastNPriceVo.setMinPrice(BigDecimal.valueOf(lastNQList.stream().mapToDouble(x -> x.getLow().doubleValue()).min().getAsDouble()));
                codeLastNMap.put(k,lastNPriceVo);
            });
        }
        //获取上个交易日日期
        List<Quotation> last5QuoList = quotationMapper.selectLastNDateList(date.format(ymdFormatter),12,GlobalConstant.STOCK_REFER_CODE);
        //调整器5-10天
        last5QuoList = last5QuoList.stream().skip(5).limit(5).collect(Collectors.toList());
        //新股列表
        List<String> newCodeList = quotationMapper.selectNewQuoList();
        Set<String> upCodeSet = new HashSet<>();
        String jsonStr = null;
        Map<String,Object> resultMap = null;
        JSONObject jsonObject = null;
        for (Quotation q : last5QuoList) {
            //获取连版数
            resultMap = queryLimitUpDownList(q.getDate().format(ymdFormatter),4);
            jsonStr = JSON.toJSONString(resultMap);
            jsonObject = JSONObject.parseObject(jsonStr);
            //转成json
            //获取2版没4版的
            List<CodeNameVo> top2List = jsonObject.getJSONObject("top2").getJSONObject("up").getJSONArray("data").toJavaList(CodeNameVo.class);
            List<CodeNameVo> top4List = jsonObject.getJSONObject("top4").getJSONObject("up").getJSONArray("data").toJavaList(CodeNameVo.class);

            List<String> top2CodeList = top2List.stream().map(CodeNameVo::getCode).collect(Collectors.toList());
            List<String> top4CodeList = top4List.stream().map(CodeNameVo::getCode).collect(Collectors.toList());
            //过滤掉4连班的
            top2CodeList.removeAll(top4CodeList);
            upCodeSet.addAll(top2CodeList);
        }
        //过滤掉今天涨停的和新股
        newCodeList.forEach(upCodeSet::remove);

        if (upCodeSet.size() > 0) {
            for (String code : upCodeSet) {
                //根据公共过滤条件过滤
                if (commRemoveCodeMap.containsKey(code)) {
                    continue;
                }
                //最高价到现在下跌了20%
                if ((codeLastNMap.get(code).getMaxPrice().doubleValue() - codeQuoMap.get(code).getCurrent().doubleValue()) / codeLastNMap.get(code).getMaxPrice().doubleValue() > 0.2) {
                    continue;
                }
                //成交量低于1e的过滤掉
                if (codeQuoMap.get(code).getVolumeAmt().doubleValue() < 100000000) {
                    continue;
                }
                //过滤掉最近5天有涨停的
                if (last5CodeUpMap.containsKey(code) && last5CodeUpMap.get(code) > 1) {
                    continue;
                }
                hisCuMonitorList.add(new CuMonitor() {{
                    setDate(strDate);
                    setCode(code);
                    setName(codeDictMap.get(code).getName());
                    setType(GlobalConstant.MONITOR_TYPE_A4);
                    setStatus(0);
                }});
            }
        }
        List<CuMonitor> cuMonitorList = new ArrayList<>(hisCuMonitorList);
        log.info("===================反包涨停列表 => {}",JSON.toJSONString(hisCuMonitorList));

        Map<String,CuMonitor> codeCuMonMap = cuMonitorList.stream().collect(Collectors.toMap(CuMonitor::getCode,Function.identity(),(o,n) ->n));

//        //统计两连版的股票，全部加入监控
//        List<CuMonitor> top2CuMonitorList = new ArrayList<>();
//        List<String> todayUpLimitCodeList = new ArrayList<>();
//        List<String> yestUpLimitCodeList = new ArrayList<>();
//        if (StringUtils.isNotEmpty(limitUpDownList.get(0).getUpList())) {
//            todayUpLimitCodeList.addAll(Arrays.asList(limitUpDownList.get(0).getUpList().split(",")));
//        }
//        if (StringUtils.isNotEmpty(limitUpDownList.get(1).getUpList())) {
//            yestUpLimitCodeList.addAll(Arrays.asList(limitUpDownList.get(1).getUpList().split(",")));
//        }
//        //获取交集就是连续2连板的
//        todayUpLimitCodeList.retainAll(yestUpLimitCodeList);
//        for (String code : todayUpLimitCodeList) {
//            //根据公共过滤条件过滤
//            if (commRemoveCodeMap.containsKey(code)) {
//                continue;
//            }
//            //过滤掉重复的股票
//            if (codeCuMonMap.containsKey(code)) {
//                continue;
//            }
//            if (codeUpMap.get(code) == 2 && codeDictMap.containsKey(code)) {
//                top2CuMonitorList.add(new CuMonitor() {{
//                    setDate(strDate);
//                    setCode(code);
//                    setName(codeDictMap.get(code).getName());
//                    setType(2);
//                    setStatus(0);
//                }});
//            }
//        }
//        cuMonitorList.addAll(top2CuMonitorList);
//        log.debug("===================连续2版涨停列表 => {}",JSON.toJSONString(top2CuMonitorList));

        //统计今天第一次涨停的
//        codeCuMonMap = cuMonitorList.stream().collect(Collectors.toMap(CuMonitor::getCode,Function.identity(),(o,n) ->n));
//        List<CuMonitor> top1CuMonitorList = new ArrayList<>();
//        if (StringUtils.isNotEmpty(limitUpDownList.get(0).getUpList())) {
//            String []upArr = limitUpDownList.get(0).getUpList().split(",");
//            //统计今天第一次涨停的
//            for (String code : upArr) {
//                //根据公共过滤条件过滤
//                if (commRemoveCodeMap.containsKey(code)) {
//                    continue;
//                }
//                //过滤掉重复的股票
//                if (codeCuMonMap.containsKey(code)) {
//                    continue;
//                }
//                //7天内第一次涨停
//                if (codeUpMap.get(code) > 1) {
//                    continue;
//                }
//                if (codeDictMap.containsKey(code)) {
//                    top1CuMonitorList.add(new CuMonitor() {{
//                        setDate(strDate);
//                        setCode(code);
//                        setName(codeDictMap.get(code).getName());
//                        setType(1);
//                        setStatus(0);
//                    }});
//                }
//            }
//            cuMonitorList.addAll(top1CuMonitorList);
//            log.debug("===================初次涨停列表 => {}",JSON.toJSONString(top1CuMonitorList));
//        }

        //过滤首版涨停列表股票
        codeCuMonMap = cuMonitorList.stream().collect(Collectors.toMap(CuMonitor::getCode,Function.identity(),(o,n) ->n));
        if (dbDictList.size() > 0) {
            for (Dict dict : dbDictList) {
                //根据公共过滤条件过滤
                if (commRemoveCodeMap.containsKey(dict.getCode())) {
                    continue;
                }
                //过滤掉重复的股票
                if (codeCuMonMap.containsKey(dict.getCode())) {
                    continue;
                }
                //过滤掉银行
                if (dict.getName().toUpperCase().contains("银行")) {
                    commRemoveCodeMap.put(dict.getCode(),dict.getName());
                    continue;
                }
                //7天内没涨停过的
                if (codeUpMap.containsKey(dict.getCode())) {
                    continue;
                }
                //过滤掉被5日均线压制的股票
                if (stifleCodeQuoMap.containsKey(dict.getCode())) {
                    continue;
                }
                cuMonitorList.add(new CuMonitor() {{
                    setDate(strDate);
                    setCode(dict.getCode());
                    setName(codeDictMap.get(dict.getCode()).getName());
                    setType(GlobalConstant.MONITOR_TYPE_A1);
                    setStatus(0);
                }});
            }
            log.info("===================待涨停列表 => {}",JSON.toJSONString(cuMonitorList));
        }

        //最终按照type排序后入库
        cuMonitorList.sort(Comparator.comparing(CuMonitor::getType));
        cuMonitorList.forEach(cu -> cu.setId(genIdUtil.nextId()));
        log.debug("===================目标涨停列表 => {}",JSON.toJSONString(cuMonitorList));

        //存储入库
        cuMonitorMapper.deleteAll();
        mybatisBatchHandler.batchInsertOrUpdate(cuMonitorList,CuMonitorMapper.class,GlobalConstant.BATCH_MODDEL_INSERT);
    }

    /**
     * 获取从某开始的限制天数提取最小涨停次数的股票列表
     * @param strDate
     * @param days
     * @param minCount
     * @return
     */
    public List<LastNUpLimitVo> getLastNUpLimitDataList(String strDate, int days,int minCount) {
        List<LastNUpLimitVo> dataList = new ArrayList<>();
        List<String> upLimitCodeList = new ArrayList<>();
        List<LimitUpDown> limitUpDownList = limitUpDownMapper.selectLastNList(strDate,days);
        if (limitUpDownList != null && limitUpDownList.size() > 0) {
            limitUpDownList.forEach(ud -> {
                if (StringUtils.isNotEmpty(ud.getUpList())) {
                    upLimitCodeList.addAll(Arrays.asList(ud.getUpList().split(",")));
                }
            });
            Map<String,Long> codeCountMap = upLimitCodeList.stream().collect(Collectors.groupingBy(x -> x,Collectors.counting()));
            //根据最小涨停数过滤
            codeCountMap = codeCountMap.entrySet().stream().filter(x -> x.getValue() >= minCount).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            if (codeCountMap.size() > 0) {
                codeCountMap.forEach((k,v) -> {
                    dataList.add(new LastNUpLimitVo(){{
                        setCode(k);
                        setCount(v.intValue());
                    }});
                });
            }
        }
        log.debug("====================提取{}个结果 => {}",dataList.size(),JSON.toJSONString(dataList));
        return dataList;
    }

    /**
     * 提取昨天涨停今天放量暴跌的股票或者放量假阴的股票
     * @param date
     * @return
     */
    public Map<String,Integer> getWTSSourceCodeTypeMap(LocalDate date) {
        //获取最近2天的股票行情列表
        DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String strDate = date.format(ymdFormatter);
        int days = 2;
        //查询最近7天的日期,以工行行情数据为准
        List<Quotation> lastNQuoList = quotationMapper.selectLastNDateList(strDate,days,GlobalConstant.STOCK_REFER_CODE);
        List<String> lastNDateList = lastNQuoList.stream().map(x -> x.getDate().format(ymdFormatter)).sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        String yestDate = lastNDateList.get(1);

        //查询最近2天的行情数据
        List<Quotation> dbQuotationList = quotationMapper.selectListByRangeOfNDay(strDate,days);
        //根据日期分组
        Map<String,List<Quotation>> dateQuotationListMap = dbQuotationList.stream().collect(Collectors.groupingBy(x -> x.getDate().format(ymdFormatter)));
        //今天和昨天的行情数据
        List<Quotation> todayQuoList = dateQuotationListMap.get(strDate);
        List<Quotation> yestQuoList = dateQuotationListMap.get(yestDate);
        //根据code映射行情列表
        Map<String,Quotation> todayCodeQuoMap = todayQuoList.stream().collect(Collectors.toMap(Quotation::getCode,Function.identity(),(o,n) ->n));
        Map<String,Quotation> yestCodeQuoMap = yestQuoList.stream().collect(Collectors.toMap(Quotation::getCode,Function.identity(),(o,n) ->n));

        //查询昨天涨停的股票列表
        LimitUpDown yestLimitUD = limitUpDownMapper.selectByDate(yestDate);
        List<String> upLimitCodeList = new ArrayList<>(Arrays.asList(yestLimitUD.getUpList().split(",")));

        //获取4连版的股票列表
        Map<String,Object> resultMap = queryLimitUpDownList(yestDate,5);
        String jsonStr = JSON.toJSONString(resultMap);
        JSONObject jsonObject = JSONObject.parseObject(jsonStr);
        //获取4连板的的所有股票
        List<CodeNameVo> top4List = jsonObject.getJSONObject("top4").getJSONObject("up").getJSONArray("data").toJavaList(CodeNameVo.class);
        List<String> top4UpLimitCodeList = top4List.stream().map(CodeNameVo::getCode).collect(Collectors.toList());

        //昨天所有涨停的过滤掉4版以上的就是最高3连扳的股票
        if (top4UpLimitCodeList.size() > 0) {
            upLimitCodeList.removeAll(top4UpLimitCodeList);
        }

        //统计结果
        Map<String,Integer> wtsCodeTypeMap = new HashMap<>();
        for (String code : upLimitCodeList) {
            //必须放量
            if (todayCodeQuoMap.containsKey(code) && yestCodeQuoMap.containsKey(code) && todayCodeQuoMap.get(code).getVolumeAmt().doubleValue() > yestCodeQuoMap.get(code).getVolumeAmt().doubleValue() * 1.2) {
                //当天成交量低于2E的过滤掉
                if (todayCodeQuoMap.get(code).getVolumeAmt().doubleValue() < 200000000) {
                    continue;
                }
                //以最低价收盘的过滤掉
                if (todayCodeQuoMap.get(code).getClose().doubleValue() == todayCodeQuoMap.get(code).getLow().doubleValue()) {
                    continue;
                }
                //有缺口的过滤掉，前天最高点 < 昨天最低点
                if (todayCodeQuoMap.containsKey(code) && yestCodeQuoMap.containsKey(code) && yestCodeQuoMap.get(code).getHigh().doubleValue() < todayCodeQuoMap.get(code).getLow().doubleValue()) {
                    continue;
                }
                //放量暴跌的或者放量假阴的
                if (todayCodeQuoMap.get(code).getOffsetRate().doubleValue() < -2 || (todayCodeQuoMap.get(code).getClose().doubleValue() > todayCodeQuoMap.get(code).getInit().doubleValue() && todayCodeQuoMap.get(code).getClose().doubleValue() < todayCodeQuoMap.get(code).getOpen().doubleValue())) {
                    wtsCodeTypeMap.put(code,0);
                }
            }
        }

        //提取昨天炸板的股票列表
        if (todayQuoList.size() > 0) {
            for (Quotation q : todayQuoList) {
                //计算今日涨停股价
                BigDecimal upLimitVal = q.getInit().multiply(BigDecimal.valueOf(1.1)).setScale(2, RoundingMode.HALF_UP);
                if (q.getHigh().compareTo(upLimitVal) == 0 && q.getClose().compareTo(upLimitVal) < 0) {
                    wtsCodeTypeMap.put(q.getCode(),1);
                }
            }
        }

        log.info("===================放量暴跌或假阴列表 => {}",JSON.toJSONString(wtsCodeTypeMap));
        return wtsCodeTypeMap;
    }

    /**
     * 清空历史行情数据表
     */
    public void truncateHisData() {
        log.info("==========================开始清空历史实时数据，当前时间：{}==========================", SpiderUtil.getCurrentTimeStr());
        long startTime = System.currentTimeMillis();
        hisQuotationMapper.truncateHisData();
        long endTime = System.currentTimeMillis();
        log.info("==========================清空历史实时数据完成，当前时间：{}==========================", SpiderUtil.getCurrentTimeStr());
        log.info("==========================共耗时{}秒==========================", (endTime - startTime) / 1000);
    }

    /**
     * 生产股票行业概念关系表数据
     */
    public void genQuoIdpDataList() {
        log.info("==========================开始生成股票行业概念数据，当前时间：{}==========================", SpiderUtil.getCurrentTimeStr());
        long startTime = System.currentTimeMillis();
        List<QuoIdp> quoIdpList = new ArrayList<>();
        //添加行业类型和细分行业类型
        List<IdpCodesVo> idyCodesList = dictPropMapper.selectIdyCodes();
        if (idyCodesList.size() > 0) {
            idyCodesList.forEach(p -> {
                if (StringUtils.isNotEmpty(p.getCodes())) {
                    String []codeArr = p.getCodes().split(",");
                    for (String code : codeArr) {
                        quoIdpList.add(new QuoIdp(){{
                            setCode(code);
                            setIdpName(p.getIdpName());
                            setType(p.getType());
                        }});
                    }
                }
            });
        }

        //添加概念类型
        List<DictProp> propList = dictPropMapper.selectAll();
        propList = propList.stream().filter(x -> StringUtils.isNotEmpty(x.getIndustry())).collect(Collectors.toList());
        if (propList.size() > 0) {
            propList.forEach(p -> {
                if (StringUtils.isNotEmpty(p.getPlate())) {
                    String []plateArr = p.getPlate().split(",");
                    for (String plate : plateArr) {
                        //去除无用概念
                        if (StringUtils.isEmpty(plate) || "融资融券".equals(plate) || "沪股通".equals(plate) || "同花顺漂亮100".equals(plate) || "标普道琼斯A股".equals(plate) || "MSCI概念".equals(plate) || "新股与次新股".equals(plate) ) {
                            continue;
                        }
                        quoIdpList.add(new QuoIdp(){{
                            setCode(p.getCode());
                            setIdpName(plate);
                            setPIdyName(p.getIndustry());
                            setType(GlobalConstant.CODE_IDP_TYPE_P);
                        }});
                    }
                }
            });
        }

        quoIdpList.forEach(cu -> cu.setId(genIdUtil.nextId()));

        //存储入库
        quoIdpMapper.deleteAll();
        mybatisBatchHandler.batchInsertOrUpdate(quoIdpList, QuoIdpMapper.class,GlobalConstant.BATCH_MODDEL_INSERT);
        long endTime = System.currentTimeMillis();
        log.info("==========================生成股票行业概念数据完成，当前时间：{}==========================", SpiderUtil.getCurrentTimeStr());
        log.info("==========================共耗时{}秒==========================", (endTime - startTime) / 1000);
    }

    /**
     * 检测今天是否休息日，即不开盘
     * @param date
     * @return
     */
    public boolean checkIsRestDay(LocalDate date) {
        //先抓取标准工行数据
        crewStockData(GlobalConstant.CREW_STOCK_REFER,false);
        //再比对今天和上个交易日的交易量，若交易量相同则是休息日
        DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<Quotation> quotationList = quotationMapper.selectLastNDateList(date.format(ymdFormatter),2,GlobalConstant.STOCK_REFER_CODE);
        return quotationList.get(0).getVolumeAmt().doubleValue() == quotationList.get(1).getVolumeAmt().doubleValue();
    }

    /**
     * 清空无效的数据列表
     * @param date
     */
    public void clearInvalidDataList(LocalDate date) {
        DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String strDate = date.format(ymdFormatter);
        quotationMapper.deleteByDate(strDate);
        limitUpDownMapper.deleteByDate(strDate);
        upDownMapper.deleteByDate(strDate);
        avgPriceMapper.deleteByDate(strDate);
        cuMonitorMapper.deleteByDate(strDate);
        recommandMapper.deleteByDate(strDate,GlobalConstant.RECOMMAND_TYPE_VOLUME);
        recommandMapper.deleteByDate(strDate,GlobalConstant.RECOMMAND_TYPE_BACK);
        hisQuotationMapper.truncateHisData();
    }

    /**
     * 发送钉钉消息
     * @param message
     */
    public void sendDingDingGroupMsg(String message) {
        //消息组装
        Map<String,Object> contentMap = new HashMap<>();
        contentMap.put("title", "change list");
        contentMap.put("text", message);
        //格式为markdown
        Map<String,Object> pMap = new HashMap<>();
        pMap.put("msgtype","markdown");
        pMap.put("markdown",contentMap);

        Map<String,Object> paramMap = new HashMap<>();
        paramMap.put(HttpClientUtil.STRBODY,JSON.toJSONString(pMap));

        Map<String,String> headerMap = new HashMap<>();
        headerMap.put("Content-Type","application/json; charset=utf-8");

        HttpClientUtil clientUtil = new HttpClientUtil();
        clientUtil.setMaxRetryCount(2);
        ResponseEntity responseEntity = clientUtil.recursiveHttpRequest(GlobalConstant.HTTP_POST_JSON,0,dingdingConfig.getWebHook(),paramMap,headerMap,GlobalConstant.CHARASET_UTF8);
        if (responseEntity.getCode() == 200) {
            JSONObject res = JSONObject.parseObject(responseEntity.getContent());
            if (res.getIntValue("errcode") == 0) {
                log.info("=========================发送钉钉消息成功=========================");
            } else {
                log.info("=========================发送钉钉消息失败，失败原因：{}=========================",responseEntity.getContent());
            }
        } else {
            log.info("=========================发送钉钉消息失败,失败原因：{}=========================",responseEntity.getContent());
        }
    }
}
