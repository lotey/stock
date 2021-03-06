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
 * @desc ???????????????
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
     * ??????????????????????????????
     */
    public void crewDictData() {
        long startTime = System.currentTimeMillis();
        log.info("==========================????????????????????????????????????????????????{}==========================", SpiderUtil.getCurrentTimeStr());
        //????????????????????????
        int batchSize = 50;
        //?????????????????????
        int threadCount = 3;
        ExecutorService threadPool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        log.info("============================???????????????{}?????????,???????????????{}============================",threadCount,SpiderUtil.getCurrentTimeStr());
        List<List<Dict>> twoDeepDictList = new ArrayList<>();
        //?????????????????????
        threadPool.execute(() -> {
            try {
                long shStartTime = System.currentTimeMillis();
                log.info("============================????????????????????????????????????????????????{}============================",SpiderUtil.getCurrentTimeStr());
                //600000, 605999
                List<String> shCodeRangeList = IntStream.rangeClosed(600000,605999).mapToObj(x -> String.format("%s%s","sh",x)).collect(Collectors.toList());
                //??????google????????????
                List<List<String>> groupCodeList =  Lists.partition(shCodeRangeList, batchSize);
                List<Dict> tmpDictList = new ArrayList<>();
                groupCodeList.forEach(codeList -> {
                    String url = String.format(GlobalConstant.SINA_STOCK_QUOTATION_URL, String.join(",",codeList));
                    ResponseEntity responseEntity = clientUtil.recursiveHttpRequest(GlobalConstant.HTTP_GET, 0, url, null, GlobalConstant.DEFAULTSINAHEADER,GlobalConstant.CHARASET_GBK);
                    if (responseEntity.getCode() == 200) {
                        //?????????????????????????????????
                        String[] dataArr = responseEntity.getContent().split("\n");
                        if (dataArr.length > 0) {
                            Dict dict = null;
                            String code = null;
                            String name = null;
                            String[] splitArr = null;
                            String data = null;
                            for (String rData : dataArr) {
                                //??????????????????
                                splitArr = rData.split("=");
                                //????????????????????????????????????
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
                log.info("============================????????????????????????????????????????????????{}============================",SpiderUtil.getCurrentTimeStr());
                log.info("============================?????????{}???============================", (shEndTime - shStartTime) / 1000);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        //?????????????????????
        threadPool.execute(() -> {
            try {
                long szStartTime = System.currentTimeMillis();
                log.info("============================????????????????????????????????????????????????{}============================",SpiderUtil.getCurrentTimeStr());
                //0, 40000
                List<String> shCodeRangeList = IntStream.rangeClosed(0,4000).mapToObj(x -> String.format("%s%s","sz",String.format("%6d", x).replace(" ", "0"))).collect(Collectors.toList());
                //??????google????????????
                List<List<String>> groupCodeList =  Lists.partition(shCodeRangeList, batchSize);
                List<Dict> tmpDictList = new ArrayList<>();
                groupCodeList.forEach(codeList -> {
                    String url = String.format(GlobalConstant.SINA_STOCK_QUOTATION_URL, String.join(",",codeList));
                    ResponseEntity responseEntity = clientUtil.recursiveHttpRequest(GlobalConstant.HTTP_GET, 0, url, null, GlobalConstant.DEFAULTSINAHEADER,GlobalConstant.CHARASET_GBK);
                    if (responseEntity.getCode() == 200) {
                        //?????????????????????????????????
                        String[] dataArr = responseEntity.getContent().split("\n");
                        if (dataArr.length > 0) {
                            Dict dict = null;
                            String code = null;
                            String name = null;
                            String[] splitArr = null;
                            String data = null;
                            for (String rData : dataArr) {
                                //??????????????????
                                splitArr = rData.split("=");
                                //????????????????????????????????????
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
                log.info("============================????????????????????????????????????????????????{}============================",SpiderUtil.getCurrentTimeStr());
                log.info("============================?????????{}???============================", (szEndTime - szStartTime) / 1000);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        //?????????????????????
        threadPool.execute(() -> {
            try {
                long szStartTime = System.currentTimeMillis();
                log.info("============================???????????????????????????????????????????????????{}============================",SpiderUtil.getCurrentTimeStr());
                //300001, 300999
                List<String> shCodeRangeList = IntStream.rangeClosed(300001,300999).mapToObj(x -> String.format("%s%s","sz",x)).collect(Collectors.toList());
                //??????google????????????
                List<List<String>> groupCodeList =  Lists.partition(shCodeRangeList, batchSize);
                List<Dict> tmpDictList = new ArrayList<>();
                groupCodeList.forEach(codeList -> {
                    String url = String.format(GlobalConstant.SINA_STOCK_QUOTATION_URL, String.join(",",codeList));
                    ResponseEntity responseEntity = clientUtil.recursiveHttpRequest(GlobalConstant.HTTP_GET, 0, url, null, GlobalConstant.DEFAULTSINAHEADER,GlobalConstant.CHARASET_GBK);
                    if (responseEntity.getCode() == 200) {
                        //?????????????????????????????????
                        String[] dataArr = responseEntity.getContent().split("\n");
                        if (dataArr.length > 0) {
                            Dict dict = null;
                            String code = null;
                            String name = null;
                            String[] splitArr = null;
                            String data = null;
                            for (String rData : dataArr) {
                                //??????????????????
                                splitArr = rData.split("=");
                                //????????????????????????????????????
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
                log.info("============================???????????????????????????????????????????????????{}============================",SpiderUtil.getCurrentTimeStr());
                log.info("============================?????????{}???============================", (szEndTime - szStartTime) / 1000);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

//        //?????????????????????
//        threadPool.execute(() -> {
//            try {
//                long szStartTime = System.currentTimeMillis();
//                log.info("============================???????????????????????????????????????????????????{}============================",SpiderUtil.getCurrentTimeStr());
//                //688001, 689001
//                List<String> shCodeRangeList = IntStream.rangeClosed(688001,689001).mapToObj(x -> String.format("%s%s","sh",x)).collect(Collectors.toList());
//                //??????google????????????
//                List<List<String>> groupCodeList =  Lists.partition(shCodeRangeList, batchSize);
//                List<Dict> tmpDictList = new ArrayList<>();
//                groupCodeList.forEach(codeList -> {
//                    String url = String.format(GlobalConstant.SINA_STOCK_QUOTATION_URL, String.join(",",codeList));
//                    ResponseEntity responseEntity = clientUtil.recursiveHttpRequest(GlobalConstant.HTTP_GET, 0, url, null, GlobalConstant.DEFAULTSINAHEADER,GlobalConstant.CHARASET_GBK);
//                    if (responseEntity.getCode() == 200) {
//                        //?????????????????????????????????
//                        String[] dataArr = responseEntity.getContent().split("\n");
//                        if (dataArr.length > 0) {
//                            Dict dict = null;
//                            String code = null;
//                            String name = null;
//                            String[] splitArr = null;
//                            String data = null;
//                            for (String rData : dataArr) {
//                                //??????????????????
//                                splitArr = rData.split("=");
//                                //????????????????????????????????????
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
//                log.info("============================???????????????????????????????????????????????????{}============================",SpiderUtil.getCurrentTimeStr());
//                log.info("============================?????????{}???============================", (szEndTime - szStartTime) / 1000);
//            } catch (Exception e) {
//                e.printStackTrace();
//            } finally {
//                latch.countDown();
//            }
//        });

        //???????????????????????????????????????????????????
        try {
            latch.await();
            threadPool.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long cEndTime = System.currentTimeMillis();
        log.info("==========================????????????????????????????????????????????????{}==========================", SpiderUtil.getCurrentTimeStr());
        log.info("==========================?????????{}???==========================", (cEndTime - startTime) / 1000);

        //?????????????????????????????????????????????DB????????????
        List<Dict> dictList = twoDeepDictList.stream().flatMap(List::stream).collect(Collectors.toList());
        //?????????ST??????????????????
        dictList = dictList.stream().filter(x -> !x.getName().endsWith("???") && !x.getName().toUpperCase().contains("ST")).collect(Collectors.toList());

        long dbStartTime = System.currentTimeMillis();
        log.info("==========================??????????????????????????????{}==========================", SpiderUtil.getCurrentTimeStr());
        //????????????????????????
        dictMapper.deleteByType(GlobalConstant.DICT_TYPE_STOCK);
        //??????????????????????????????
        dictList.forEach(x -> {
            x.setId(genIdUtil.nextId());
            x.setType(GlobalConstant.DICT_TYPE_STOCK);
        });
        mybatisBatchHandler.batchInsertOrUpdate(dictList,DictMapper.class,GlobalConstant.BATCH_MODDEL_INSERT);
        long dbEndTime = System.currentTimeMillis();
        log.info("==========================??????????????????????????????{}==========================", SpiderUtil.getCurrentTimeStr());
        log.info("==========================?????????{}???==========================", (dbEndTime - dbStartTime) / 1000);

        long endTime = System.currentTimeMillis();
        log.info("==========================????????????????????????????????????????????????{}==========================", SpiderUtil.getCurrentTimeStr());
        log.info("==========================?????????{}???==========================", (endTime - startTime) / 1000);
    }

    /**
     * ??????????????????????????????
     * @param date
     */
    public void updateCirMarketValue(LocalDate date) {
        long startTime = System.currentTimeMillis();
        log.info("==========================????????????????????????????????????????????????{}==========================", SpiderUtil.getCurrentTimeStr());
//        if (SpiderUtil.isWeekendOfToday(date)) {
//            log.error("==========================??????????????????????????????????????????==========================");
//            return;
//        }
        //????????????????????????
        List<Dict> dbDictList = dictMapper.selectListByType(GlobalConstant.DICT_TYPE_STOCK);
        Map<String,Dict> codeDictMap = dbDictList.stream().collect(Collectors.toMap(Dict::getCode,Function.identity()));
        //??????????????????????????????
        DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<Quotation> dbQuotationList = quotationMapper.selectListByDate(date.format(ymdFormatter));
        Map<String,Double> codePriceMap = dbQuotationList.stream().collect(Collectors.toMap(Quotation::getCode, x -> x.getCurrent().doubleValue()));
        //????????????????????????
        int batchSize = 50;
        //?????????????????????
        List<List<Dict>> twoDeepList = SpiderUtil.partitionList(dbDictList,GlobalConstant.MAX_THREAD_COUNT);
        log.info("============================???????????????{}??????????????????{}???????????????,???????????????{}============================",twoDeepList.size(),dbDictList.size(),SpiderUtil.getCurrentTimeStr());
        if (twoDeepList.size() > 0) {
            ExecutorService threadPool = Executors.newFixedThreadPool(GlobalConstant.MAX_THREAD_COUNT);
            CountDownLatch latch = new CountDownLatch(twoDeepList.size());
            List<List<Dict>> dictResultList = new ArrayList<>();
            twoDeepList.forEach(innerList -> {
                threadPool.execute(() -> {
                    //????????????????????????dict??????
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
                            //?????????????????????????????????
                            String[] dataArr = responseEntity.getContent().split("\n");
                            if (dataArr.length > 0) {
                                String []itemArr = null;
                                Dict curDict = null;
                                String []infArr = null;
                                BigDecimal mValue = null;
                                for (String dictI : dataArr) {
                                    itemArr = dictI.split("=")[0].split("_");
                                    if (codeDictMap.containsKey(itemArr[2])) {
                                        //????????????
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
            log.info("==========================??????????????????????????????{}==========================", SpiderUtil.getCurrentTimeStr());
            log.info("==========================?????????{}???==========================", (dbEndTime - dbStartTime) / 1000);
        }
        long endTime = System.currentTimeMillis();
        log.info("==========================????????????????????????????????????????????????{}==========================", SpiderUtil.getCurrentTimeStr());
        log.info("==========================?????????{}???==========================", (endTime - startTime) / 1000);
    }

    /**
     * ????????????????????????????????????
     * @param type ????????????
     * @param isMarketClosed ????????????
     */
    public void crewStockData(int type,boolean isMarketClosed) {
        long startTime = System.currentTimeMillis();
        log.info("==========================??????????????????????????????????????????{}==========================", SpiderUtil.getCurrentTimeStr());
        LocalDate today = LocalDate.now();

        //??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        if (isMarketClosed) {
            //??????????????????????????????????????????
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
        //????????????????????????
        List<Dict> dictList = new ArrayList<>();
        List<CuMonitor> cuMonitorList = cuMonitorMapper.selectNewestList();
        if (GlobalConstant.CREW_STOCK_POOL == type) {//????????????????????????????????????????????????
            if (cuMonitorList.size() > 0) {
                for (CuMonitor m : cuMonitorList) {
                    Dict dict = new Dict();
                    BeanUtils.copyProperties(m,dict);
                    dictList.add(dict);
                }
            }
        } else if (GlobalConstant.CREW_STOCK_REFER == type){//?????????????????????????????????????????????????????????
            Dict referDict = new Dict();
            referDict.setCode(GlobalConstant.STOCK_REFER_CODE);
            dictList.add(referDict);
        } else if (GlobalConstant.CREW_STOCK_MAIN_BOARD == type) {//??????????????????????????????????????????
            dictList = dictMapper.selectListByType(GlobalConstant.DICT_TYPE_STOCK);
            dictList = dictList.stream().filter(x -> !x.getCode().contains("sz300")).collect(Collectors.toList());
        } else {//???????????????????????????????????????
            dictList = dictMapper.selectListByType(GlobalConstant.DICT_TYPE_STOCK);
        }
        //????????????????????????
        int batchSize = 50;
        //?????????????????????
        List<List<Dict>> twoDeepList = SpiderUtil.partitionList(dictList,GlobalConstant.MAX_THREAD_COUNT);
        ExecutorService threadPool = Executors.newFixedThreadPool(twoDeepList.size());
        log.info("============================???????????????{}??????????????????{}???????????????,???????????????{}============================",twoDeepList.size(),dictList.size(),SpiderUtil.getCurrentTimeStr());
        if (twoDeepList.size() > 0) {
            Set<List<Quotation>> crewResultSet = new HashSet<>();
            List<List<String>> upCodeList = new ArrayList<>();
            List<List<String>> downCodeList = new ArrayList<>();
            Set<List<String>> upLimitCodeList = new HashSet<>();
            Set<List<String>> downLimitCodeList = new HashSet<>();
            CountDownLatch latch = new CountDownLatch(twoDeepList.size());
            twoDeepList.forEach(innerList -> {
                threadPool.execute(() -> {
                    //??????google????????????
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
                                //?????????????????????????????????
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
                                        //??????????????????
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

                                        //????????????????????????
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
                                        //?????????????????????????????????????????????=?????????
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
            //???????????????????????????????????????????????????
            try {
                latch.await();
                threadPool.shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //?????????????????????????????????????????????DB????????????
            List<Quotation> quotationList = crewResultSet.stream().flatMap(List::stream).collect(Collectors.toList());
            log.info("==========================????????????????????????????????????{} ??????????????????????????????{}==========================", quotationList.size(), SpiderUtil.getCurrentTimeStr());
            long startDBTime = System.currentTimeMillis();
            //??????????????????
            quotationMapper.deleteByDate(todayStr);
            //??????????????????????????????
            mybatisBatchHandler.batchInsertOrUpdate(quotationList, QuotationMapper.class, GlobalConstant.BATCH_MODDEL_INSERT);
            long endTime = System.currentTimeMillis();
            log.info("==========================????????????????????????????????????????????????{} DB???????????????{}???==========================", SpiderUtil.getCurrentTimeStr(),(endTime - startDBTime) / 1000);

            //?????????????????????????????????????????????????????????????????????
            //????????????????????????????????????????????????,???????????????
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

            //????????????
            //????????????????????????????????????????????????
            limitUpDownMapper.deleteByDate(todayStr);
            //????????????????????????
            //????????????????????????????????????????????????????????????
            String upLimitCodeListStr = upLimitCodeList.stream().flatMap(List::stream).collect(Collectors.joining(","));
            String downLimitCodeListStr = downLimitCodeList.stream().flatMap(List::stream).collect(Collectors.joining(","));
            LimitUpDown limitUpDown = new LimitUpDown();
            limitUpDown.setId(genIdUtil.nextId());
            limitUpDown.setDate(today);
            limitUpDown.setUpList(upLimitCodeListStr);
            limitUpDown.setDownList(downLimitCodeListStr);
            limitUpDownMapper.insertSelective(limitUpDown);
            log.info("==========================??????????????????????????????????????????????????????{}==========================", SpiderUtil.getCurrentTimeStr());

            //????????????
            //????????????????????????????????????????????????
            upDownMapper.deleteByDate(todayStr);
            //????????????????????????
            //????????????????????????????????????????????????????????????
            String upCodeListStr = upCodeList.stream().flatMap(List::stream).collect(Collectors.joining(","));
            String downCodeListStr = downCodeList.stream().flatMap(List::stream).collect(Collectors.joining(","));
            UpDown upDown = new UpDown();
            upDown.setId(genIdUtil.nextId());
            upDown.setDate(today);
            upDown.setUpList(upCodeListStr);
            upDown.setDownList(downCodeListStr);
            upDownMapper.insertSelective(upDown);
            log.info("==========================??????????????????????????????????????????????????????{}==========================", SpiderUtil.getCurrentTimeStr());

            log.info("==========================?????????{}???==========================", (endTime - startTime) / 1000);
        }
    }

    /**
     * ?????????????????????????????????5??????10??????20??????30?????????
     * @param date
     */
    public void updateAvgPrice(LocalDate date) {
        long startTime = System.currentTimeMillis();
        SimpleDateFormat ymdFormat = new SimpleDateFormat("yyyy-MM-dd");
        String strDate = ymdFormat.format(date);
        log.info("==========================????????????{}????????????????????????{}==========================",strDate, SpiderUtil.getCurrentTimeStr());
//        if (SpiderUtil.isWeekendOfToday(date)) {
//            log.error("==========================??????????????????????????????????????????==========================");
//            return;
//        }
        List<Quotation> dbQuotationList = quotationMapper.selectListByDate(strDate);
        if (dbQuotationList.size() == 0) {
            log.info("=========================??????????????????????????????????????????=========================");
            return;
        }
        long cStartTime = System.currentTimeMillis();
        if (dbQuotationList.size() > 0) {
            //?????????????????????
            List<List<Quotation>> twoDeepList = SpiderUtil.partitionList(dbQuotationList, GlobalConstant.MAX_THREAD_COUNT);
            //=======================================================????????????????????????=======================================================
            ExecutorService threadPool = Executors.newFixedThreadPool(twoDeepList.size());
            log.info("============================???????????????{}???????????????{}???{}???????????????,???????????????{}============================", twoDeepList.size(), strDate, dbQuotationList.size(), SpiderUtil.getCurrentTimeStr());
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
            //?????????????????????????????????
            try {
                latch.await();
                threadPool.shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            List<AvgPrice> insertAvgPriceList = twoDeepPriceList.stream().flatMap(List::stream).collect(Collectors.toList());
            Map<String,AvgPrice> codeAvgPriceMap = insertAvgPriceList.stream().collect(Collectors.toMap(AvgPrice::getCode,Function.identity(),(o,n) -> n));
            long cEndTime = System.currentTimeMillis();
            log.info("==========================???????????????????????????{}???==========================", (cEndTime - cStartTime) / 1000);
            //=======================================================????????????????????????=======================================================

            //=======================================================??????N???????????????=======================================================
            //??????60??????????????????
            LocalDate minDate = date.plusDays(-60);
            //??????????????????60?????????????????????
            List<Quotation> dbHisQuotationList = quotationMapper.selectListByDateRange(ymdFormat.format(minDate),strDate);
            //????????????????????????????????????
            Map<String,List<Quotation>> codeQuotationMap = dbHisQuotationList.stream().collect(Collectors.groupingBy(Quotation::getCode));
            cStartTime = System.currentTimeMillis();
            //??????????????????????????????
            List<Quotation> curDateQuotationList = dbQuotationList.stream().filter(x -> ymdFormat.format(x.getDate()).equals(strDate)).collect(Collectors.toList());
            //?????????????????????
            twoDeepList = SpiderUtil.partitionList(curDateQuotationList,GlobalConstant.MAX_THREAD_COUNT);
            ExecutorService threadPool2 = Executors.newFixedThreadPool(twoDeepList.size());
            log.info("============================???????????????{}???????????????{}???{}?????????????????????,???????????????{}============================",twoDeepList.size(),strDate,curDateQuotationList.size(),SpiderUtil.getCurrentTimeStr());
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
                        //???????????????????????????????????????
                        List<Quotation> hisQuotationList = codeQuotationMap.get(quotation.getCode());
                        if (hisQuotationList != null && hisQuotationList.size() > 5) {
                            //????????????????????????????????????????????????30?????????
                            hisQuotationList.sort(Comparator.comparing(Quotation::getDate).reversed());

                            //??????4?????????
                            //5?????????
                            recent5QuotationList = hisQuotationList.stream().limit(5).collect(Collectors.toList());
                            volumeAmt = recent5QuotationList.stream().collect(Collectors.summarizingDouble(x -> x.getVolumeAmt().doubleValue())).getSum();
                            volume = recent5QuotationList.stream().collect(Collectors.summarizingDouble(x -> x.getVolume().doubleValue())).getSum();
                            if (volume < 1) {
                                continue;
                            }
                            avg5Price = BigDecimal.valueOf(volumeAmt).divide(BigDecimal.valueOf(volume),2,BigDecimal.ROUND_HALF_UP);
                            //10?????????
                            recent10QuotationList = hisQuotationList.stream().limit(10).collect(Collectors.toList());
                            if (recent10QuotationList.size() >= 10) {
                                volumeAmt = recent10QuotationList.stream().collect(Collectors.summarizingDouble(x -> x.getVolumeAmt().doubleValue())).getSum();
                                volume = recent10QuotationList.stream().collect(Collectors.summarizingDouble(x -> x.getVolume().doubleValue())).getSum();
                                avg10Price = BigDecimal.valueOf(volumeAmt).divide(BigDecimal.valueOf(volume),2,BigDecimal.ROUND_HALF_UP);
                            }
                            //20?????????
                            recent20QuotationList = hisQuotationList.stream().limit(20).collect(Collectors.toList());
                            if (recent20QuotationList.size() >= 20) {
                                volumeAmt = recent20QuotationList.stream().collect(Collectors.summarizingDouble(x -> x.getVolumeAmt().doubleValue())).getSum();
                                volume = recent20QuotationList.stream().collect(Collectors.summarizingDouble(x -> x.getVolume().doubleValue())).getSum();
                                avg20Price = BigDecimal.valueOf(volumeAmt).divide(BigDecimal.valueOf(volume),2,BigDecimal.ROUND_HALF_UP);
                            }
                            //30?????????
                            recent30QuotationList = hisQuotationList.stream().limit(30).collect(Collectors.toList());
                            if (recent30QuotationList.size() >= 30) {
                                volumeAmt = recent30QuotationList.stream().collect(Collectors.summarizingDouble(x -> x.getVolumeAmt().doubleValue())).getSum();
                                volume = recent30QuotationList.stream().collect(Collectors.summarizingDouble(x -> x.getVolume().doubleValue())).getSum();
                                avg30Price = BigDecimal.valueOf(volumeAmt).divide(BigDecimal.valueOf(volume),2,BigDecimal.ROUND_HALF_UP);
                            }

                            //??????????????????
                            if (codeAvgPriceMap.containsKey(quotation.getCode())) {
                                avgPrice = codeAvgPriceMap.get(quotation.getCode());
                                avgPrice.setAvg5(avg5Price);
                                avgPrice.setAvg10(avg10Price);
                                avgPrice.setAvg20(avg20Price);
                                avgPrice.setAvg30(avg30Price);
                            }
                        } else {
                            log.info("======================?????????{} -> {}???????????????5????????????????????????======================",quotation.getCode(),quotation.getName());
                        }
                    }
                    latch2.countDown();
                });
            });
            //?????????????????????????????????
            try {
                latch2.await();
                threadPool2.shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            cEndTime = System.currentTimeMillis();
            log.info("==========================???????????????????????????????????????:{}==========================", SpiderUtil.getCurrentTimeStr());
            log.info("==========================???????????????????????????{}???==========================", (cEndTime - cStartTime) / 1000);
            //=======================================================??????N???????????????=======================================================

            //??????????????????????????????????????????????????????????????????
            avgPriceMapper.deleteByDate(strDate);
            mybatisBatchHandler.batchInsertOrUpdate(insertAvgPriceList,AvgPriceMapper.class,GlobalConstant.BATCH_MODDEL_INSERT);
        }
        long endTime = System.currentTimeMillis();
        log.info("==========================??????{}??????????????????????????????{}==========================",strDate, SpiderUtil.getCurrentTimeStr());
        log.info("==========================?????????{}???==========================", (endTime - startTime) / 1000);
    }

    /**
     * ?????????????????????????????????
     * ?????????????????????
     * @param date
     */
    public void calcLimitUpDownList(LocalDate date) {
        long startTime = System.currentTimeMillis();
        DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String strDate = date.format(ymdFormatter);
        log.info("==========================????????????{}????????????????????????????????????{}==========================",strDate, SpiderUtil.getCurrentTimeStr());
//        if (SpiderUtil.isWeekendOfToday(date)) {
//            log.error("==========================??????????????????????????????????????????==========================");
//            return;
//        }
        List<Quotation> dbQuotationList = quotationMapper.selectListByDate(strDate);
        long cStartTime = System.currentTimeMillis();
        if (dbQuotationList.size() > 0) {
            //?????????????????????
            List<List<Quotation>> twoDeepList = SpiderUtil.partitionList(dbQuotationList,GlobalConstant.MAX_THREAD_COUNT);
            ExecutorService threadPool = Executors.newFixedThreadPool(twoDeepList.size());
            log.info("============================???????????????{}???????????????{}???{}???????????????????????????,???????????????{}============================",twoDeepList.size(),strDate,dbQuotationList.size(),SpiderUtil.getCurrentTimeStr());
            CountDownLatch latch = new CountDownLatch(twoDeepList.size());
            List<List<String>> upCodeList = new ArrayList<>();
            List<List<String>> downCodeList = new ArrayList<>();
            twoDeepList.forEach(innerList -> {
                threadPool.execute(() -> {
                    List<String> tmpUpCodeList = new ArrayList<>();
                    List<String> tmpDownCodeList = new ArrayList<>();
                    for (Quotation quotation : innerList) {
                        //?????????????????????
                        if (quotation.getCurrent().compareTo(BigDecimal.valueOf(0.5)) < 0) {
                            continue;
                        }
                        //???????????????
                        if (quotation.getOffsetRate().doubleValue() >= 9.6) {
                            tmpUpCodeList.add(quotation.getCode());
                        }
                        //???????????????
                        if (quotation.getOffsetRate().doubleValue() <= -9.6) {
                            tmpDownCodeList.add(quotation.getCode());
                        }
                    }
                    upCodeList.add(tmpUpCodeList);
                    downCodeList.add(tmpDownCodeList);
                    latch.countDown();
                });
            });
            //?????????????????????????????????
            try {
                latch.await();
                threadPool.shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            long cEndTime = System.currentTimeMillis();
            log.info("==========================???????????????????????????{}???==========================",(cEndTime - cStartTime) / 1000);

            //????????????????????????????????????????????????
            limitUpDownMapper.deleteByDate(strDate);
            //????????????????????????
            //????????????????????????????????????????????????????????????
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
        log.info("==========================??????{}??????????????????????????????????????????{}==========================",strDate, SpiderUtil.getCurrentTimeStr());
        log.info("==========================?????????{}???==========================", (endTime - startTime) / 1000);
    }

    /**
     * ???????????????????????????
     * ?????????????????????
     * @param date
     */
    public void calcUpDownList(LocalDate date) {
        long startTime = System.currentTimeMillis();
        DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String strDate = date.format(ymdFormatter);
        log.info("==========================????????????{}??????????????????????????????{}==========================",strDate, SpiderUtil.getCurrentTimeStr());
//        if (SpiderUtil.isWeekendOfToday(date)) {
//            log.error("==========================??????????????????????????????????????????==========================");
//            return;
//        }
        List<Quotation> dbQuotationList = quotationMapper.selectListByDate(strDate);
        long cStartTime = System.currentTimeMillis();
        if (dbQuotationList.size() > 0) {
            //?????????????????????
            List<List<Quotation>> twoDeepList = SpiderUtil.partitionList(dbQuotationList,GlobalConstant.MAX_THREAD_COUNT);
            ExecutorService threadPool = Executors.newFixedThreadPool(twoDeepList.size());
            log.info("============================???????????????{}???????????????{}???{}?????????????????????,???????????????{}============================",twoDeepList.size(),strDate,dbQuotationList.size(),SpiderUtil.getCurrentTimeStr());
            CountDownLatch latch = new CountDownLatch(twoDeepList.size());
            List<List<String>> upCodeList = new ArrayList<>();
            List<List<String>> downCodeList = new ArrayList<>();
            twoDeepList.forEach(innerList -> {
                threadPool.execute(() -> {
                    List<String> tmpUpCodeList = new ArrayList<>();
                    List<String> tmpDownCodeList = new ArrayList<>();
                    for (Quotation quotation : innerList) {
                        //?????????????????????
                        if (quotation.getCurrent().compareTo(BigDecimal.valueOf(0.5)) < 0) {
                            continue;
                        }
                        //???????????????????????? ??? ????????????????????????????????????
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
            //?????????????????????????????????
            try {
                latch.await();
                threadPool.shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            long cEndTime = System.currentTimeMillis();
            log.info("==========================?????????????????????{}???==========================",(cEndTime - cStartTime) / 1000);

            //????????????????????????????????????????????????
            upDownMapper.deleteByDate(strDate);
            //????????????????????????
            //????????????????????????????????????????????????????????????
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
        log.info("==========================??????{}????????????????????????????????????{}==========================",strDate, SpiderUtil.getCurrentTimeStr());
        log.info("==========================?????????{}???==========================", (endTime - startTime) / 1000);
    }

    /**
     * ???????????????????????????
     * @param date
     */
    public void calcVolumeList(LocalDate date) {
        long startTime = System.currentTimeMillis();
        DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String strDate = date.format(ymdFormatter);
        log.info("==========================????????????{}??????????????????????????????{}==========================",strDate, SpiderUtil.getCurrentTimeStr());
        int days = 7;
        //?????????????????????
        List<Dict> dictList = dictMapper.selectListByType(GlobalConstant.DICT_TYPE_STOCK);
        //?????????????????????????????????ST
        List<String> normalCodeList = dictList.stream().map(Dict::getCode).filter(code -> !code.contains("sz300")).collect(Collectors.toList());

        //????????????8????????????????????????2????????????
        List<UpLimitCountVo> upLimitCountVoList = quotationMapper.selectUpLimitGtN(60,date,3);
        Map<String,UpLimitCountVo> codeLimitCountMap = upLimitCountVoList.stream().collect(Collectors.toMap(UpLimitCountVo::getCode,Function.identity(),(o,n) -> n));

        //???????????????????????????????????????
        List<Quotation> dbQuotationList = quotationMapper.selectListByRangeOfNDay(strDate,days);
        dbQuotationList = dbQuotationList.stream().filter(x -> normalCodeList.contains(x.getCode()) && !codeLimitCountMap.containsKey(x.getCode())).collect(Collectors.toList());

//        //???????????????????????????????????????
//        List<Quotation> dbQuotationList = quotationMapper.selectListByRangeOfNDay(strDate,days);
//        dbQuotationList = dbQuotationList.stream().filter(x -> normalCodeList.contains(x.getCode())).collect(Collectors.toList());

        //??????????????????
        Map<String,List<Quotation>> dateQuotationListMap = dbQuotationList.stream().collect(Collectors.groupingBy(x -> x.getDate().format(ymdFormatter)));

        if (!dateQuotationListMap.containsKey(strDate)) {
            log.info("=======================?????????{}??????????????????????????????????????????=======================",strDate);
            return;
        }

        //??????????????????
        List<DictProp> propList = dictPropMapper.selectAll();
        Map<String,DictProp> codePropMap = propList.stream().collect(Collectors.toMap(DictProp::getCode,Function.identity(),(o,n) -> n));

        //????????????7????????????,???????????????????????????
        List<Quotation> lastNQuoList = quotationMapper.selectLastNDateList(strDate,days,GlobalConstant.STOCK_REFER_CODE);
        List<String> lastNDateList = lastNQuoList.stream().map(x -> x.getDate().format(ymdFormatter)).collect(Collectors.toList());

        //?????????????????????????????????
        List<LastNPriceVo> lastNPriceList = quotationMapper.selectLastNMaxPrice(strDate,30);
        Map<String,LastNPriceVo> lastNPriceMap = lastNPriceList.stream().collect(Collectors.toMap(LastNPriceVo::getCode,Function.identity(),(o,n) -> n));

        long cStartTime = System.currentTimeMillis();
        if (dateQuotationListMap.size() == days) {
            //????????????
            lastNDateList.sort(Comparator.reverseOrder());
            String last1Date = lastNDateList.get(1);
            String last2Date = lastNDateList.get(2);
            String last3Date = lastNDateList.get(3);
            String last4Date = lastNDateList.get(4);
            String last5Date = lastNDateList.get(5);
            String last6Date = lastNDateList.get(6);

            //??????N???????????????
            Map<String,Quotation> last1CodeQuotationMap = dateQuotationListMap.get(last1Date).stream().collect(Collectors.toMap(Quotation::getCode,Function.identity(),(o,n) -> n));
            Map<String,Quotation> last2CodeQuotationMap = dateQuotationListMap.get(last2Date).stream().collect(Collectors.toMap(Quotation::getCode,Function.identity(),(o,n) -> n));
            Map<String,Quotation> last3CodeQuotationMap = dateQuotationListMap.get(last3Date).stream().collect(Collectors.toMap(Quotation::getCode,Function.identity(),(o,n) -> n));
            Map<String,Quotation> last4CodeQuotationMap = dateQuotationListMap.get(last4Date).stream().collect(Collectors.toMap(Quotation::getCode,Function.identity(),(o,n) -> n));
            Map<String,Quotation> last5CodeQuotationMap = dateQuotationListMap.get(last5Date).stream().collect(Collectors.toMap(Quotation::getCode,Function.identity(),(o,n) -> n));
            Map<String,Quotation> last6CodeQuotationMap = dateQuotationListMap.get(last6Date).stream().collect(Collectors.toMap(Quotation::getCode,Function.identity(),(o,n) -> n));

            //??????????????????????????????
            List<AvgPrice> lastNAvgPriceList = avgPriceMapper.selectListByRangeOfNDay(strDate,4);
            Map<String,List<AvgPrice>> dateAvgPriceListMap = lastNAvgPriceList.stream().collect(Collectors.groupingBy(x -> x.getDate().format(ymdFormatter)));

            //??????????????????
            Map<String,AvgPrice> codeAvgPriceMap = dateAvgPriceListMap.get(strDate).stream().collect(Collectors.toMap(AvgPrice::getCode,Function.identity(),(o,n) -> n));
            Map<String,AvgPrice> last1AvgPriceMap = dateAvgPriceListMap.get(last1Date).stream().collect(Collectors.toMap(AvgPrice::getCode,Function.identity(),(o,n) -> n));
            Map<String,AvgPrice> last2AvgPriceMap = dateAvgPriceListMap.get(last2Date).stream().collect(Collectors.toMap(AvgPrice::getCode,Function.identity(),(o,n) -> n));
            Map<String,AvgPrice> last3AvgPriceMap = dateAvgPriceListMap.get(last3Date).stream().collect(Collectors.toMap(AvgPrice::getCode,Function.identity(),(o,n) -> n));

            //?????????????????????
            List<SysDict> backlistList = sysDictMapper.selectByType(GlobalConstant.SYS_DICT_TYPE_BACKLIST);
            Map<String,SysDict> codeBackListMap = backlistList.stream().collect(Collectors.toMap(SysDict::getValue,Function.identity(),(o,n) -> n));

            //????????????????????????
            Map<String,Dict> codeDictMap = dictList.stream().collect(Collectors.toMap(Dict::getCode,Function.identity(),(o,n) -> n));

            //??????????????????????????????
            List<RelativePositionVo> positionList = quotationMapper.selectCurPosition(strDate);
            Map<String,RelativePositionVo> codePositionMap = positionList.stream().collect(Collectors.toMap(RelativePositionVo::getCode,Function.identity(),(o,n) -> n));

            //?????????????????????
            List<List<Quotation>> twoDeepList = SpiderUtil.partitionList(dateQuotationListMap.get(strDate),GlobalConstant.MAX_THREAD_COUNT);
            ExecutorService threadPool = Executors.newFixedThreadPool(twoDeepList.size());
            log.info("============================???????????????{}???????????????{}???{}?????????????????????,???????????????{}============================",twoDeepList.size(),strDate,dateQuotationListMap.get(strDate).size(),SpiderUtil.getCurrentTimeStr());
            if (twoDeepList.size() > 0) {
                //????????????????????????
                SimpleDateFormat vFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String vTime = vFormat.format(new Date());

                CountDownLatch latch = new CountDownLatch(twoDeepList.size());
                List<List<VolumeVo>> twoDeepVolumeList = new ArrayList<>();
                //??????????????????
                BigDecimal minMultiple = BigDecimal.valueOf(2);
                BigDecimal maxMultiple = BigDecimal.valueOf(4);
                twoDeepList.forEach(innerList -> {
                    threadPool.execute(() -> {
                        List<VolumeVo> tmpVolumeList = new ArrayList<>();
                        VolumeVo volumeVo = null;
                        for (Quotation quotation : innerList) {
                            try {
                                if (last2CodeQuotationMap.containsKey(quotation.getCode()) && last1CodeQuotationMap.containsKey(quotation.getCode()) && last3CodeQuotationMap.containsKey(quotation.getCode())) {
                                    //?????????????????????????????????2?????????????????????
                                    if (quotation.getVolumeAmt() != null && last1CodeQuotationMap.get(quotation.getCode()).getVolumeAmt() != null && last2CodeQuotationMap.get(quotation.getCode()).getVolumeAmt() != null && last3CodeQuotationMap.get(quotation.getCode()).getVolumeAmt() != null &&
                                            quotation.getVolumeAmt().doubleValue() > last1CodeQuotationMap.get(quotation.getCode()).getVolumeAmt().doubleValue() * minMultiple.doubleValue() &&
                                            quotation.getVolumeAmt().doubleValue() > last2CodeQuotationMap.get(quotation.getCode()).getVolumeAmt().doubleValue() * minMultiple.doubleValue() &&
                                            quotation.getVolumeAmt().doubleValue() > last3CodeQuotationMap.get(quotation.getCode()).getVolumeAmt().doubleValue() * minMultiple.doubleValue()) {
                                        //????????????????????????8000w????????????
                                        if (quotation.getVolumeAmt().compareTo(BigDecimal.valueOf(80000000)) < 0) {
                                            continue;
                                        }
                                        //???????????????
                                        if (quotation.getName().contains("??????")) {
                                            continue;
                                        }
                                        //????????????????????????
                                        if (quotation.getOffsetRate().compareTo(BigDecimal.valueOf(0)) < 0) {
                                            continue;
                                        }
                                        //????????????3??????????????????
                                        if (quotation.getOffsetRate().doubleValue() < 4.8) {
                                            continue;
                                        }
                                        //?????????????????????4??????
                                        if (quotation.getLow().doubleValue() < 4 || quotation.getLow().doubleValue() > 80) {
                                            continue;
                                        }
                                        //?????????????????????
                                        if (codeBackListMap.containsKey(quotation.getCode())) {
                                            continue;
                                        }
                                        //??????????????????
                                        if (!codeDictMap.containsKey(quotation.getCode()) || codeDictMap.get(quotation.getCode()) == null || codeDictMap.get(quotation.getCode()).getCirMarketValue() == null || codeDictMap.get(quotation.getCode()).getCirMarketValue().doubleValue() < 20 ||  codeDictMap.get(quotation.getCode()).getCirMarketValue().doubleValue() > 800) {
                                            continue;
                                        }
                                        //?????????????????????,????????????????????????????????????40
                                        if (!codePropMap.containsKey(quotation.getCode()) || codePropMap.get(quotation.getCode()) == null || codePropMap.get(quotation.getCode()).getLyr() == null || codePropMap.get(quotation.getCode()).getLyr().compareTo(BigDecimal.valueOf(-1)) == 0 || (codePropMap.get(quotation.getCode()).getLyr().doubleValue() < 40 && codePropMap.get(quotation.getCode()).getTtm() != null && codePropMap.get(quotation.getCode()).getTtm().doubleValue() > 0 && codePropMap.get(quotation.getCode()).getTtm().doubleValue() < 40)) {
                                            continue;
                                        }
                                        //?????????????????????????????????
                                        if (codeAvgPriceMap.get(quotation.getCode()) == null || codeAvgPriceMap.get(quotation.getCode()).getAvg5() == null || codeAvgPriceMap.get(quotation.getCode()).getAvg10() == null || codeAvgPriceMap.get(quotation.getCode()).getAvg20() == null || codeAvgPriceMap.get(quotation.getCode()).getAvg30() == null) {
                                            continue;
                                        }
                                        //??????10????????????,??????????????????????????????????????????20%????????????
                                        double lastNMaxPrice = lastNPriceMap.get(quotation.getCode()).getMaxPrice().doubleValue();
                                        double lastNMinPrice = lastNPriceMap.get(quotation.getCode()).getMinPrice().doubleValue();
                                        if (codeAvgPriceMap.get(quotation.getCode()).getLast10Trend() < 0 && (lastNMaxPrice - lastNMinPrice) / lastNMaxPrice > 0.15) {
                                            continue;
                                        }
                                        //??????3???????????????????????????
                                        if (!codePositionMap.containsKey(quotation.getCode()) || codePositionMap.get(quotation.getCode()) == null || codePositionMap.get(quotation.getCode()).getPRate() > 80) {
                                            continue;
                                        }
                                        //????????????70?????????5????????????????????????
                                        if (codePositionMap.get(quotation.getCode()).getPRate() > 70 && quotation.getLow().doubleValue() > codeAvgPriceMap.get(quotation.getCode()).getAvg5().doubleValue()) {
                                            continue;
                                        }
                                        //????????????70?????????5????????????????????????
                                        if (codePositionMap.get(quotation.getCode()).getPRate() > 70 && codeAvgPriceMap.get(quotation.getCode()).getLast10Trend() > 0 && codeAvgPriceMap.get(quotation.getCode()).getLast10MonthTrend() > 0 && quotation.getLow().doubleValue() > codeAvgPriceMap.get(quotation.getCode()).getAvg10().doubleValue()) {
                                            continue;
                                        }
                                        //??????????????????????????????
                                        if (last2CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() > 9.3 || last1CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() > 9.3) {
                                            continue;
                                        }
                                        //????????????????????????????????????
                                        if (last2CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() > 0 && last1CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() > 0) {
                                            continue;
                                        }
                                        //??????????????????????????????3%?????????
                                        if (last2CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() < -4 || last1CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() < -4) {
                                            continue;
                                        }
                                        //??????????????????-2.5~2.5??????
                                        if (last1CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() < -2.5 || last1CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() > 2.5) {
                                            continue;
                                        }
                                        //??????????????????4%?????????
                                        if ((quotation.getHigh().doubleValue() - quotation.getInit().doubleValue()) * 100 / quotation.getInit().doubleValue() - quotation.getOffsetRate().doubleValue() > 4) {
                                            continue;
                                        }
                                        //????????????,???????????????????????????????????????
                                        if (quotation.getOpen().doubleValue() > last1CodeQuotationMap.get(quotation.getCode()).getHigh().doubleValue()) {
                                            continue;
                                        }
                                        //??????????????????????????????????????????????????????3??????
                                        if (quotation.getOpen().doubleValue() > quotation.getInit().doubleValue() && quotation.getVolumeAmt().doubleValue() > last1CodeQuotationMap.get(quotation.getCode()).getVolumeAmt().doubleValue() * maxMultiple.doubleValue()) {
                                            continue;
                                        }
                                        //???3??????????????????5??????????????????5??????????????????
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
                                        //??????????????????
                                        List<Double> hisAvgList = new ArrayList<>();
                                        hisAvgList.add(codeAvgPriceMap.get(quotation.getCode()).getAvg5().doubleValue());
                                        hisAvgList.add(codeAvgPriceMap.get(quotation.getCode()).getAvg10().doubleValue());
                                        hisAvgList.add(codeAvgPriceMap.get(quotation.getCode()).getAvg20().doubleValue());
                                        hisAvgList.add(codeAvgPriceMap.get(quotation.getCode()).getAvg30().doubleValue());
                                        hisAvgList = hisAvgList.stream().sorted().collect(Collectors.toList());
                                        //????????????,??????????????????4???????????????????????????
                                        if (codeAvgPriceMap.get(quotation.getCode()).getLast10Trend() >= 0 && codeAvgPriceMap.get(quotation.getCode()).getLast10MonthTrend() == 99 && (hisAvgList.get(3) - hisAvgList.get(0)) / hisAvgList.get(3) <= 0.06) {
                                            continue;
                                        }
                                        //????????????,10????????????20??????,??????????????????20??????????????????,????????????3???
                                        if (codeAvgPriceMap.get(quotation.getCode()).getLast10Trend() > 0 && codeAvgPriceMap.get(quotation.getCode()).getLast10MonthTrend() > 0 &&
                                                codeAvgPriceMap.get(quotation.getCode()).getAvg10().doubleValue() > codeAvgPriceMap.get(quotation.getCode()).getAvg20().doubleValue() &&
                                                last1CodeQuotationMap.get(quotation.getCode()).getLow().doubleValue() >= last1AvgPriceMap.get(quotation.getCode()).getAvg20().doubleValue() &&
                                                last1CodeQuotationMap.get(quotation.getCode()).getHigh().doubleValue() <= last1AvgPriceMap.get(quotation.getCode()).getAvg10().doubleValue()
                                        ) {
                                            continue;
                                        }
                                        //????????????,??????????????????4???????????????????????????
                                        if (codeAvgPriceMap.get(quotation.getCode()).getLast10Trend() == -1 && codeAvgPriceMap.get(quotation.getCode()).getLast10MonthTrend() == 99 && (hisAvgList.get(3) - hisAvgList.get(0)) / hisAvgList.get(3) <= 0.05 &&
                                            quotation.getOpen().doubleValue() < hisAvgList.get(0) && quotation.getCurrent().doubleValue() > hisAvgList.get(3)
                                        ) {
                                            continue;
                                        }
                                        //??????????????????n????????????,???????????????12????????????
                                        if ((last6CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() < 0 && last5CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() < 0 && last4CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() < 0 &&
                                           (last6CodeQuotationMap.get(quotation.getCode()).getInit().doubleValue() - last4CodeQuotationMap.get(quotation.getCode()).getClose().doubleValue()) / last6CodeQuotationMap.get(quotation.getCode()).getInit().doubleValue() > 0.12) ||
                                           (last5CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() < 0 && last4CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() < 0 && last3CodeQuotationMap.get(quotation.getCode()).getOffsetRate().doubleValue() < 0 &&
                                           (last5CodeQuotationMap.get(quotation.getCode()).getInit().doubleValue() - last3CodeQuotationMap.get(quotation.getCode()).getClose().doubleValue()) / last5CodeQuotationMap.get(quotation.getCode()).getInit().doubleValue() > 0.12)) {
                                            continue;
                                        }
                                        //??????????????????????????????<3?????????N???????????????
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
                                            //1????????????????????????????????????????????? >= ???????????????
                                            //2?????????????????????
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
                //?????????????????????????????????
                try {
                    latch.await();
                    threadPool.shutdown();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                long cEndTime = System.currentTimeMillis();
                log.info("==========================???????????????????????????{}???==========================",(cEndTime - cStartTime) / 1000);

                try {
                    //??????????????????????????????
                    recommandMapper.deleteByDate(strDate,GlobalConstant.RECOMMAND_TYPE_VOLUME);
                    //????????????????????????????????????????????????????????????????????????
                    List<VolumeVo> curVolumeList = twoDeepVolumeList.stream().flatMap(List::stream).collect(Collectors.toList());
                    if (curVolumeList.size() > 0) {
                        //????????????????????????
                        Recommand newRecommand = new Recommand();
                        newRecommand.setId(genIdUtil.nextId());
                        newRecommand.setDate(date);
                        newRecommand.setType(GlobalConstant.RECOMMAND_TYPE_VOLUME);
                        newRecommand.setDataList(JSON.toJSONString(curVolumeList));
                        recommandMapper.insertSelective(newRecommand);
                    }
                } catch (Exception e) {
                    log.error("=======================???????????????????????????????????????=======================");
                    e.printStackTrace();
                }
            }
        }
        long endTime = System.currentTimeMillis();
        log.info("==========================??????{}????????????????????????????????????{}==========================",strDate, SpiderUtil.getCurrentTimeStr());
        log.info("==========================?????????{}???==========================", (endTime - startTime) / 1000);
    }

    /**
     * ????????????????????????????????????
     */
    public void clearVolumeData(String type) {
        log.info("====================????????????????????????====================");
        List<Recommand> recommandList = recommandMapper.selectAll(type);
        if (recommandList != null && recommandList.size() > 0) {
            recommandList.forEach(vo -> {
                recommandMapper.deleteById(vo.getId());
            });
        }
        log.info("====================????????????????????????====================");
    }

    /**
     * ??????N??????????????????????????????
     * @param startDate
     * @param maxDays
     * @return
     */
    public Map<String, Object> queryLimitUpDownList(String startDate, int maxDays) {
        //???????????????????????????
        List<Dict> dictList = dictMapper.selectListByType(GlobalConstant.DICT_TYPE_STOCK);
        Map<String,Dict> dictMap = dictList.stream().collect(Collectors.toMap(Dict::getCode, Function.identity()));
        DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate sDate = LocalDate.parse(startDate,ymdFormatter);
        sDate = sDate.plusDays(-maxDays * 3L);
        List<LimitUpDown> dbLimitUpDownList = limitUpDownMapper.selectListByDateRange(sDate.format(ymdFormatter),startDate);
        Map<String,Object> resultMap = new LinkedHashMap<>();
        if (dbLimitUpDownList.size() >= maxDays) {
            //????????????????????????????????????maxDays???
            List<LimitUpDown> usedLimitUpDownList = dbLimitUpDownList.stream().sorted(Comparator.comparing(LimitUpDown::getDate).reversed()).limit(maxDays).collect(Collectors.toList());
            Set<String> upCodeSet = new HashSet<>();
            Set<String> downCodeSet = new HashSet<>();

            //??????????????????
            List<DictProp> propList = dictPropMapper.selectAll();
            //??????code->prop??????
            Map<String,DictProp> codePropMap = propList.stream().collect(Collectors.toMap(DictProp::getCode,Function.identity(),(oldP,newP) -> newP));

            Map<String,Object> map = null;
            Map<String,Object> upMap = null;
            Map<String,Object> downMap = null;

            List<Map<String,String>> codeNameList = null;
            Map<String,String> codeNameMap = null;
            int loopIndex = 0;
            for (LimitUpDown limitUpDown : usedLimitUpDownList) {
                map = new HashMap<>();

                //??????????????????
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
                    String plate = "??????";
                    if (codePropMap.containsKey(code)) {
                        StringBuilder plateBuffer = new StringBuilder();
                        if (StringUtils.isNotEmpty(codePropMap.get(code).getIndustry())) {
                            plateBuffer.append(codePropMap.get(code).getIndustry()).append(" -> ");
                        }
                        if (StringUtils.isNotEmpty(codePropMap.get(code).getPlate())){
                            plateBuffer.append(codePropMap.get(code).getPlate());
                        }
                        plate = plateBuffer.toString();
                        plate = StringUtils.isEmpty(plate) ? "??????" : plate;
                    }
                    codeNameMap.put("plate",plate);
                    codeNameList.add(codeNameMap);
                }
                upMap.put("data",codeNameList);
                upMap.put("count",codeNameList.size());
                map.put("up",upMap);

                //??????????????????
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
                    String plate = "??????";
                    if (codePropMap.containsKey(code)) {
                        StringBuilder plateBuffer = new StringBuilder();
                        if (StringUtils.isNotEmpty(codePropMap.get(code).getIndustry())) {
                            plateBuffer.append(codePropMap.get(code).getIndustry()).append(" -> ");
                        }
                        if (StringUtils.isNotEmpty(codePropMap.get(code).getPlate())){
                            plateBuffer.append(codePropMap.get(code).getPlate());
                        }
                        plate = plateBuffer.toString();
                        plate = StringUtils.isEmpty(plate) ? "??????" : plate;
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
            log.error("============================??????????????????????????????{}???============================",maxDays);
        }
        return resultMap;
    }

    /**
     * ??????N??????????????????????????????
     * @param startDate
     * @param maxDays
     * @param isBest
     * @return
     */
    public Map<String,Object> queryUpDownList(String startDate,int maxDays,int isBest) {
        //???????????????????????????
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
            //????????????????????????????????????maxDays???
            List<UpDown> usedUpDownList = dbLimitUpDownList.stream().sorted(Comparator.comparing(UpDown::getDate).reversed()).limit(maxDays).collect(Collectors.toList());
            Set<String> upCodeSet = new HashSet<>();
            Set<String> downCodeSet = new HashSet<>();

            Map<String,Object> map = null;
            Map<String,Object> upMap = null;
            Map<String,Object> downMap = null;

            //??????????????????
            List<DictProp> propList = dictPropMapper.selectAll();
            //??????code->prop??????
            Map<String,DictProp> codePropMap = propList.stream().collect(Collectors.toMap(DictProp::getCode,Function.identity(),(oldP,newP) -> newP));

            List<Map<String,String>> codeNameList = null;
            Map<String,String> codeNameMap = null;
            int loopIndex = 0;
            for (UpDown upDown : usedUpDownList) {
                map = new HashMap<>();

                //??????????????????
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
                    String plate = "??????";
                    if (codePropMap.containsKey(code)) {
                        StringBuilder plateBuffer = new StringBuilder();
                        if (StringUtils.isNotEmpty(codePropMap.get(code).getIndustry())) {
                            plateBuffer.append(codePropMap.get(code).getIndustry()).append(" -> ");
                        }
                        if (StringUtils.isNotEmpty(codePropMap.get(code).getPlate())){
                            plateBuffer.append(codePropMap.get(code).getPlate());
                        }
                        plate = plateBuffer.toString();
                        plate = StringUtils.isEmpty(plate) ? "??????" : plate;
                    }
                    codeNameMap.put("plate",plate);
                    codeNameList.add(codeNameMap);
                }
                upMap.put("data",codeNameList);
                upMap.put("count",codeNameList.size());
                map.put("up",upMap);

                //??????????????????
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
                    String plate = "??????";
                    if (codePropMap.containsKey(code)) {
                        StringBuilder plateBuffer = new StringBuilder();
                        if (StringUtils.isNotEmpty(codePropMap.get(code).getIndustry())) {
                            plateBuffer.append(codePropMap.get(code).getIndustry()).append(" -> ");
                        }
                        if (StringUtils.isNotEmpty(codePropMap.get(code).getPlate())){
                            plateBuffer.append(codePropMap.get(code).getPlate());
                        }
                        plate = plateBuffer.toString();
                        plate = StringUtils.isEmpty(plate) ? "??????" : plate;
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
            log.error("============================??????????????????????????????{}???============================",maxDays);
        }
        return resultMap;
    }

    /**
     * ?????????????????????????????????
     * 1.5????????????
     * 2.???????????????
     * 3.??????????????????4????????????
     * 4.??????????????????????????? > 2%
     * @param date
     * @return
     */
    public List<Quotation> queryRecommandList(LocalDate date) {
        SimpleDateFormat ymdFormat = new SimpleDateFormat("yyyy-MM-dd");
        String strDate = ymdFormat.format(date);
        //????????????8????????????????????????2????????????
        List<UpLimitCountVo> upLimitCountVoList = quotationMapper.selectUpLimitGtN(30,date,3);
        Map<String,UpLimitCountVo> codeUpLimitVoMap = upLimitCountVoList.stream().collect(Collectors.toMap(UpLimitCountVo::getCode,Function.identity(),(o,n) -> n));
        //??????????????????
        List<Quotation> quotationList = quotationMapper.selectRecommandListByRangeOfNDay(strDate,6);
        quotationList = quotationList.stream().filter(x -> !x.getCode().contains("sz300") && !codeUpLimitVoMap.containsKey(x.getCode())).collect(Collectors.toList());
        //??????????????????
        Map<String,List<Quotation>> dateQuotationListMap = quotationList.stream().collect(Collectors.groupingBy(x -> ymdFormat.format(x.getDate())));
        List<String> sortDateList = dateQuotationListMap.keySet().stream().sorted(Comparator.comparing(x -> Integer.parseInt(x.replaceAll("-","")))).collect(Collectors.toList());
        //??????????????????
        Map<String,Quotation> back5QuotationMap = dateQuotationListMap.get(sortDateList.get(0)).stream().collect(Collectors.toMap(Quotation::getCode,Function.identity(),(o,n) -> n));
        Map<String,Quotation> back4QuotationMap = dateQuotationListMap.get(sortDateList.get(1)).stream().collect(Collectors.toMap(Quotation::getCode,Function.identity(),(o,n) -> n));
        Map<String,Quotation> back3QuotationMap = dateQuotationListMap.get(sortDateList.get(2)).stream().collect(Collectors.toMap(Quotation::getCode,Function.identity(),(o,n) -> n));
        Map<String,Quotation> back2QuotationMap = dateQuotationListMap.get(sortDateList.get(3)).stream().collect(Collectors.toMap(Quotation::getCode,Function.identity(),(o,n) -> n));
        Map<String,Quotation> back1QuotationMap = dateQuotationListMap.get(sortDateList.get(4)).stream().collect(Collectors.toMap(Quotation::getCode,Function.identity(),(o,n) -> n));

        //??????????????????
        List<AvgPrice> avgPriceList = avgPriceMapper.selectListByRangeOfNDay(strDate,6);
        avgPriceList = avgPriceList.stream().filter(x -> x.getAvg30() != null).collect(Collectors.toList());
        //??????????????????
        Map<String,List<AvgPrice>> dateAvgListMap = avgPriceList.stream().collect(Collectors.groupingBy(x -> ymdFormat.format(x.getDate())));
        List<String> sortAvgDateList = dateAvgListMap.keySet().stream().sorted(Comparator.comparing(x -> Integer.parseInt(x.replaceAll("-","")))).collect(Collectors.toList());
        //??????????????????
        Map<String,AvgPrice> back6AvgMap = dateAvgListMap.get(sortAvgDateList.get(0)).stream().collect(Collectors.toMap(AvgPrice::getCode,Function.identity(),(o,n) -> n));
        Map<String,AvgPrice> back5AvgMap = dateAvgListMap.get(sortAvgDateList.get(1)).stream().collect(Collectors.toMap(AvgPrice::getCode,Function.identity(),(o,n) -> n));
        Map<String,AvgPrice> back4AvgMap = dateAvgListMap.get(sortAvgDateList.get(2)).stream().collect(Collectors.toMap(AvgPrice::getCode,Function.identity(),(o,n) -> n));
        Map<String,AvgPrice> back3AvgMap = dateAvgListMap.get(sortAvgDateList.get(3)).stream().collect(Collectors.toMap(AvgPrice::getCode,Function.identity(),(o,n) -> n));
        Map<String,AvgPrice> back2AvgMap = dateAvgListMap.get(sortAvgDateList.get(4)).stream().collect(Collectors.toMap(AvgPrice::getCode,Function.identity(),(o,n) -> n));
        Map<String,AvgPrice> back1AvgMap = dateAvgListMap.get(sortAvgDateList.get(5)).stream().collect(Collectors.toMap(AvgPrice::getCode,Function.identity(),(o,n) -> n));
        //????????????????????????????????????????????????
        List<Quotation> resultList = new ArrayList<>();
        for (Quotation q : dateQuotationListMap.get(sortDateList.get(5))) {
//            if ("sz300406".equals(q.getCode())) {
//                log.info("===========================??????===========================");
//            }
            //???????????????5~30
            if (q.getCurrent().doubleValue() < 4 || q.getCurrent().doubleValue() > 100) {
                continue;
            }
            //???????????????5000w????????????
            if (q.getVolumeAmt().doubleValue() < 80000000) {
                continue;
            }
            //?????????????????????1~8??????
            if (q.getOffsetRate().doubleValue() < 3 && q.getOffsetRate().doubleValue() > 7) {
                continue;
            }
            //?????????????????????????????????2??????????????????
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
                //5???????????????????????????2??????????????????
                if (downCount >= 2) {
                    //?????????3???????????????
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
                    //?????????2???????????????
                    if (dCount >= 2) {
                        //??????3????????????????????????
                        if (back3QuotationMap.get(q.getCode()).getOffsetRate().doubleValue() >= -0.5 && back3QuotationMap.get(q.getCode()).getOffsetRate().doubleValue() <= 0 ||
                                back2QuotationMap.get(q.getCode()).getOffsetRate().doubleValue() >= -0.5 && back2QuotationMap.get(q.getCode()).getOffsetRate().doubleValue() <= 0 ||
                                back1QuotationMap.get(q.getCode()).getOffsetRate().doubleValue() >= -0.5 && back1QuotationMap.get(q.getCode()).getOffsetRate().doubleValue() <= 0) {
                            //?????????2??????????????????
                            int cCount = 0;
                            //?????????????????????N????????????
                            //????????????????????????????????????????????????
                            List<Double> hisAvgList = new ArrayList<>();
                            hisAvgList.add(back6AvgMap.get(q.getCode()).getAvg5().doubleValue());
                            hisAvgList.add(back6AvgMap.get(q.getCode()).getAvg10().doubleValue());
                            hisAvgList.add(back6AvgMap.get(q.getCode()).getAvg20().doubleValue());
                            hisAvgList.add(back6AvgMap.get(q.getCode()).getAvg30().doubleValue());
                            hisAvgList = hisAvgList.stream().sorted().collect(Collectors.toList());
                            //????????????????????????
                            if (back5QuotationMap.get(q.getCode()).getHigh().doubleValue() <= hisAvgList.get(0)) {
                                continue;
                            }
                            //????????????????????????4???????????????????????????002556???2020-07-16???
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
                            //??????3????????????????????????????????????????????????????????????????????????????????????????????????????????????+0.05
                            if (back3QuotationMap.get(q.getCode()).getLow().doubleValue() > back2QuotationMap.get(q.getCode()).getLow().doubleValue() && back2QuotationMap.get(q.getCode()).getLow().doubleValue() > back1QuotationMap.get(q.getCode()).getLow().doubleValue() &&
                                    back3QuotationMap.get(q.getCode()).getClose().doubleValue() + 0.05 >= back2QuotationMap.get(q.getCode()).getOpen().doubleValue()
                            ) {
                                continue;
                            }
                            //??????????????????4%?????????
                            if (back1QuotationMap.get(q.getCode()).getOffsetRate().doubleValue() <= -4) {
                                continue;
                            }
                            if (back2AvgMap.get(q.getCode()).getAvg().doubleValue() >= hisAvgList.get(0) * 1.01 && back2AvgMap.get(q.getCode()).getAvg().doubleValue() <= hisAvgList.get(3) * 1.01) {
                                ++cCount;
                            }
                            if (cCount >= 2) {
                                //??????????????????2?????????
                                hisAvgList = new ArrayList<>();
                                hisAvgList.add(back1AvgMap.get(q.getCode()).getAvg5().doubleValue());
                                hisAvgList.add(back1AvgMap.get(q.getCode()).getAvg10().doubleValue());
                                hisAvgList.add(back1AvgMap.get(q.getCode()).getAvg20().doubleValue());
                                hisAvgList.add(back1AvgMap.get(q.getCode()).getAvg30().doubleValue());
                                hisAvgList = hisAvgList.stream().sorted().collect(Collectors.toList());
                                //??????????????????2?????????
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
        //????????????
        try {
            //??????????????????????????????
            recommandMapper.deleteByDate(strDate,GlobalConstant.RECOMMAND_TYPE_BACK);
            List<CodeNameVo> dataList = new ArrayList<>();
            resultList.forEach(q -> {
                dataList.add(new CodeNameVo(q.getCode(),q.getName()));
            });
            if (resultList.size() > 0) {
                //????????????????????????
                Recommand newRecommand = new Recommand();
                newRecommand.setId(genIdUtil.nextId());
                newRecommand.setDate(date);
                newRecommand.setType(GlobalConstant.RECOMMAND_TYPE_BACK);
                newRecommand.setDataList(JSON.toJSONString(dataList));
                recommandMapper.insertSelective(newRecommand);
            }
        } catch (Exception e) {
            log.error("=======================???????????????????????????????????????=======================");
            e.printStackTrace();
        }
        return resultList;
    }

    /**
     * ??????N??????????????????????????????
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
        //??????????????????
        List<Quotation> dbQuotationList = quotationMapper.selectListByDateRange(ymdFormat.format(calendar.getTime()),startDate);
        //??????code??????
        Map<String,List<Quotation>> codeQuotationListMap = dbQuotationList.stream().collect(Collectors.groupingBy(Quotation::getCode));
        //??????????????????
        List<UpDown> dbLimitUpDownList = upDownMapper.selectListByDateRange(ymdFormat.format(calendar.getTime()),startDate);
        //??????startDate????????????
        List<AvgPrice> avgPriceList = avgPriceMapper.selectListByDateRange(ymdFormat.format(calendar.getTime()),startDate);

        //???????????????????????????
        List<Dict> dictList = dictMapper.selectListByType(GlobalConstant.DICT_TYPE_STOCK);
        Map<String,Dict> codeDictMap = dictList.stream().collect(Collectors.toMap(Dict::getCode, Function.identity()));

        Map<String,Object> fetchResultMap = null;
        Map<String,Object> resultMap = new LinkedHashMap<>();
        if (dbLimitUpDownList.size() > maxDays) {
            //????????????????????????????????????maxDays???
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
                            //?????????????????????????????????????????????????????????????????????
                            continue;
                        }
                    } else {
                        downCodeSet.addAll(Arrays.asList(upDown.getDownList().split(",")));
                    }
                    downDays++;
                }
                loopIndex++;
            }

            //============================????????????????????????????????????============================
            //????????????????????????????????????
            List<String> r1CodeList = new ArrayList<>(upCodeSet);
            r1CodeList.retainAll(downCodeSet);

            //============================??????5???????????????============================

            List<AvgPrice> curAvgPriceList = avgPriceList.stream().filter(x -> ymdFormat.format(x.getDate()).equals(startDate)).collect(Collectors.toList());
            Map<String,AvgPrice> codeAvgMap = curAvgPriceList.stream().collect(Collectors.toMap(AvgPrice::getCode, Function.identity()));

            //??????startDate????????????
            List<Quotation> quotationList = dbQuotationList.stream().filter(x -> ymdFormat.format(x.getDate()).equals(startDate)).collect(Collectors.toList());
            Map<String,Quotation> codeQuotationMap = quotationList.stream().collect(Collectors.toMap(Quotation::getCode, Function.identity()));

            List<String> r2CodeList = r1CodeList.stream().filter(x -> codeQuotationMap.get(x) != null && codeAvgMap.get(x) != null && codeAvgMap.get(x).getAvg() != null && codeAvgMap.get(x).getAvg5() != null && codeAvgMap.get(x).getAvg().compareTo(codeAvgMap.get(x).getAvg5()) > 0).collect(Collectors.toList());

            //============================??????????????????????????????,5?????? ??? 10???????????????????????????============================
            List<String> r3CodeList = r2CodeList.stream().filter(x -> codeAvgMap.get(x).getAvg5() != null && codeAvgMap.get(x).getAvg10() != null && codeAvgMap.get(x).getAvg5().compareTo(codeAvgMap.get(x).getAvg10()) > 0).collect(Collectors.toList());

            //??????????????????
            Map<String,Object> rMap = null;
            List<Map<String,String>> rDataList = null;
            Map<String,String> tmpMap = null;

            //?????????????????????????????????
            if (r1CodeList.size() > 0) {
                rMap = new HashMap<>();
                rDataList = new ArrayList<>();
                for (String code : r1CodeList) {
                    tmpMap = new HashMap<>();
                    tmpMap.put("code",code);
                    if (codeDictMap.containsKey(code)) {
                        tmpMap.put("name",codeDictMap.get(code).getName());
                    } else {
                        tmpMap.put("name","??????");
                    }
                    rDataList.add(tmpMap);
                }
                rMap.put("data",rDataList);
                rMap.put("count",rDataList.size());
                resultMap.put("init",rMap);
            }

            //????????????????????????????????????
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

            //????????????????????????????????????
            //???????????????????????????
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

            //???????????????????????????????????????
            fetchResultMap = this.fetchDownDoji(maxDays,dbLimitUpDownList,codeQuotationListMap,codeDictMap);
            if (fetchResultMap != null) {
                resultMap.put("????????????",fetchResultMap);
            }

            //?????????????????????????????????
            fetchResultMap = this.fetchMorningDoji(r1CodeList,codeQuotationListMap,codeDictMap);
            if (fetchResultMap != null) {
                resultMap.put("???????????????",fetchResultMap);
            }

            //????????????3???5?????????10??????????????????
            fetchResultMap = this.fetchCrossWith510(maxDays,avgPriceList,codeQuotationListMap,codeDictMap);
            if (fetchResultMap != null) {
                resultMap.put("??????",fetchResultMap);
            }
        } else {
            log.error("============================??????????????????????????????{}???============================",maxDays);
        }

        fetchResultMap = this.fetchCross(avgPriceList,codeQuotationListMap,codeDictMap);
        if (fetchResultMap != null) {
            resultMap.put("????????????",fetchResultMap);
        }
        return resultMap;
    }

    /**
     * ???????????????????????????
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
     * ??????N?????????????????????????????????????????????
     * ??????????????????????????????(?????????-????????????) / ????????? >= -0.6
     * @return
     */
    public Map<String,Object> fetchCross(List<AvgPrice> AvgPriceList,Map<String,List<Quotation>> codeQuotationMap,Map<String,Dict> codeDictMap) {
        //?????????code??????
        Map<String,List<AvgPrice>> codeAvgListMap = AvgPriceList.stream().collect(Collectors.groupingBy(AvgPrice::getCode));
        List<String> targetCodeList = new ArrayList<>();
        int nDays = 4;
        codeAvgListMap.forEach((code,hisAvgList) -> {
            if (codeQuotationMap.containsKey(code)) {
                //???????????????????????????5???
                List<AvgPrice> curHisAvgList = hisAvgList.stream().filter(x -> x.getAvg30() != null).sorted(Comparator.comparing(AvgPrice::getDate).reversed()).limit(nDays).collect(Collectors.toList());
                if (curHisAvgList.size() == nDays) {
                    //5????????????3???????????????????????????
//                curHisAvgList = hisAvgList.stream().sorted(Comparator.comparing(AvgPrice::getDate)).collect(Collectors.toList());
                    //5????????????10????????????
                    for (int i = 2; i < curHisAvgList.size(); i++) {
                        if (curHisAvgList.get(i - 2).getAvg5().compareTo(curHisAvgList.get(i - 2).getAvg10()) < 0 && curHisAvgList.get(i - 1).getAvg5().compareTo(curHisAvgList.get(i - 1).getAvg10()) > 0 &&
                                //???????????????
                                (((curHisAvgList.get(i - 2).getAvg5().compareTo(curHisAvgList.get(i - 2).getAvg20()) < 0 && curHisAvgList.get(i - 1).getAvg5().compareTo(curHisAvgList.get(i - 1).getAvg20()) > 0 ||
                                        curHisAvgList.get(i - 2).getAvg10().compareTo(curHisAvgList.get(i - 2).getAvg20()) < 0 && curHisAvgList.get(i - 1).getAvg10().compareTo(curHisAvgList.get(i - 1).getAvg20()) > 0)) ||
                                        //???????????????
                                        ((curHisAvgList.get(i - 1).getAvg5().compareTo(curHisAvgList.get(i - 1).getAvg20()) < 0 && curHisAvgList.get(i).getAvg5().compareTo(curHisAvgList.get(i).getAvg20()) > 0 ||
                                                curHisAvgList.get(i - 1).getAvg10().compareTo(curHisAvgList.get(i - 1).getAvg20()) < 0 && curHisAvgList.get(i).getAvg10().compareTo(curHisAvgList.get(i).getAvg20()) > 0)) &&
                                                //????????????????????????????????????
                                                //5????????????
                                                ((curHisAvgList.get(i - 2).getAvg5().compareTo(curHisAvgList.get(i - 1).getAvg5()) < 0 && curHisAvgList.get(i - 1).getAvg5().compareTo(curHisAvgList.get(i).getAvg5()) < 0) ||
                                                //????????????
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

        //??????????????????
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
     * ??????N?????????????????????????????????????????????
     * ??????????????????????????????(?????????-????????????) / ????????? >= -0.6
     * @return
     */
    public Map<String,Object> fetchCrossWith510(int maxDays,List<AvgPrice> avgPriceList,Map<String,List<Quotation>> codeQuotationMap,Map<String,Dict> codeDictMap) {
        //?????????code??????
        Map<String,List<AvgPrice>> codeAvgListMap = avgPriceList.stream().collect(Collectors.groupingBy(AvgPrice::getCode));
        List<String> targetCodeList = new ArrayList<>();
        codeAvgListMap.forEach((code,hisAvgList) -> {
            //???????????????????????????5???
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
                //?????????10???????????????????????????1%??????
                targetCodeList.add(curAvgList.get(0).getCode());
            }
        });

        //TODO 5?????????????????????????????????2%??????

        //?????????????????????
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
                //?????????????????????60%
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

        //??????????????????
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
     * ????????????????????????????????????
     * ????????????https://cj.sina.com.cn/article/detail/5909404541/406694
     * @return
     */
    public Map<String,Object> fetchMorningDoji(List<String> codeList,Map<String,List<Quotation>> codeQuotationMap,Map<String,Dict> codeDictMap) {
        //?????????code???????????????
        List<Quotation> quotationList = null;

        Quotation lastQuotation = null;
        Quotation midQuotation = null;
        Quotation curQuotation = null;
        List<String> targetCodeList = new ArrayList<>();
        for (String code : codeList) {
            quotationList = codeQuotationMap.get(code);
            //???????????????3?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
            quotationList = quotationList.stream().sorted(Comparator.comparing(Quotation::getDate).reversed()).limit(3).collect(Collectors.toList());
            if (quotationList.size() < 3) {
                continue;
            }
            //??????N??????????????????
           lastQuotation = quotationList.get(2);
           midQuotation = quotationList.get(1);
           curQuotation = quotationList.get(0);

            if (lastQuotation.getClose().doubleValue() < lastQuotation.getOpen().doubleValue()) {
                //???????????????????????????????????????????????? < ????????? < ????????? && ????????????0.6??????
                log.debug("?????????:{} ?????????:{} ?????????:{}",midQuotation.getCurrent().doubleValue(),midQuotation.getInit().doubleValue(),midQuotation.getCurrent().doubleValue() - lastQuotation.getInit().doubleValue());
                log.debug("?????????:{}",(midQuotation.getCurrent().doubleValue() - midQuotation.getInit().doubleValue()) / midQuotation.getInit().doubleValue());
                if (midQuotation.getCurrent().doubleValue() < midQuotation.getOpen().doubleValue() &&
                        midQuotation.getHigh().doubleValue() < midQuotation.getCurrent().doubleValue() &&
                        (midQuotation.getCurrent().doubleValue() - midQuotation.getOpen().doubleValue()) / midQuotation.getOpen().doubleValue() <= GlobalConstant.MAX_DOJI_LIMIT_NUM ||//????????????
                        (midQuotation.getCurrent().doubleValue() - midQuotation.getOpen().doubleValue()) / midQuotation.getOpen().doubleValue() >= -GlobalConstant.MAX_DOJI_LIMIT_NUM) {//????????????
                    //?????????????????????????????????????????????????????????????????????????????????
                    if (curQuotation.getCurrent().doubleValue() > curQuotation.getOpen().doubleValue() && curQuotation.getOpen().doubleValue() >= lastQuotation.getClose().doubleValue() && curQuotation.getCurrent().doubleValue() <= lastQuotation.getOpen().doubleValue() && (int)curQuotation.getVolumeAmt().doubleValue() > GlobalConstant.MIN_TRADE_AMOUNT) {
                        targetCodeList.add(code);
                    }
                }
            }
        }
        //??????????????????
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
     * ??????N?????????????????????????????????????????????
     * ??????????????????????????????(?????????-????????????) / ????????? >= -0.6
     * @return
     */
    public Map<String,Object> fetchDownDoji(int maxDays,List<UpDown> upDownList,Map<String,List<Quotation>> codeQuotationMap,Map<String,Dict> codeDictMap) {
        Set<String> downCodeSet = new HashSet<>();
        int limitDay = 1;
        double downRate = 0.6d;
        //?????????????????? = ???????????? * ?????????
        int minDownDay = (int)((maxDays - limitDay) * downRate);
        int downDays = 0;
        for (UpDown upDown : upDownList) {
            if (downCodeSet.size() > 0) {
                if (downDays < minDownDay) {
                    downCodeSet.retainAll(Arrays.asList(upDown.getDownList().split(",")));
                } else {
                    //?????????????????????????????????????????????????????????????????????
                    continue;
                }
            } else {
                downCodeSet.addAll(Arrays.asList(upDown.getDownList().split(",")));
            }
            downDays++;
        }

        //??????????????????????????????????????????????????????????????????????????????
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
//                log.info("?????????:{} ?????????:{} ?????????:{}",Double.parseDouble(curQuotationDataArr[3]),Double.parseDouble(curQuotationDataArr[1]),Double.parseDouble(curQuotationDataArr[3]) - Double.parseDouble(curQuotationDataArr[1]));
//                log.info("?????????:{}",(Double.parseDouble(curQuotationDataArr[3]) - Double.parseDouble(curQuotationDataArr[1])) / Double.parseDouble(curQuotationDataArr[1]));
//            }

            if (quotation.getCurrent().doubleValue() <= quotation.getOpen().doubleValue() &&
                    quotation.getLow().doubleValue() < quotation.getCurrent().doubleValue() &&
                    (quotation.getCurrent().doubleValue() - quotation.getOpen().doubleValue()) / quotation.getOpen().doubleValue() >= -GlobalConstant.DOJI_LIMIT_NUM) {//???????????????
                //???????????????????????????????????????3???
                if (quotation.getCurrent().doubleValue() >= GlobalConstant.MIN_STOCK_PRICE && (int)quotation.getVolumeAmt().doubleValue() > GlobalConstant.MIN_TRADE_AMOUNT) {
                    targetCodeList.add(code);
                }
            }
        }
        //??????????????????
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
     * ???????????????????????????
     * @param maxDate
     * @param rRate
     */
    public void fetchQualityStockList(LocalDate maxDate,int limitDays,double rRate) {
        log.info("==========================?????????????????????????????????????????????{}==========================", SpiderUtil.getCurrentTimeStr());
        long startTime = System.currentTimeMillis();
        //????????????????????????
        List<SysDict> backListDictList = sysDictMapper.selectByType(GlobalConstant.SYS_DICT_TYPE_BACKLIST);
        List<String> backListCodeList = backListDictList.stream().map(SysDict::getValue).collect(Collectors.toList());

        //?????????????????????
        List<Dict> dictList = dictMapper.selectListByType(GlobalConstant.DICT_TYPE_STOCK);
        List<Dict> r1DictList = dictList.stream().filter(x -> x.getCirMarketValue() != null && x.getCirMarketValue().doubleValue() >= 20 && x.getCirMarketValue().doubleValue() <= 1000
                && !backListCodeList.contains(x.getCode())).collect(Collectors.toList());

        //??????????????????
        List<DictProp> propList = dictPropMapper.selectAll();
        Map<String,DictProp> codePropMap = propList.stream().collect(Collectors.toMap(DictProp::getCode,Function.identity(),(o,n) -> n));

        DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String strDate = maxDate.format(ymdFormatter);


        //??????
        List<AvgPrice> avgPriceList = avgPriceMapper.selectListByRangeOfNDay(strDate,limitDays);
        Map<String,List<AvgPrice>> codeHisMap = avgPriceList.stream().collect(Collectors.groupingBy(AvgPrice::getCode));

        //??????
        List<Quotation> quotationList = quotationMapper.selectListByRangeOfNDay(strDate,limitDays);
        Map<String,List<Quotation>> codeQuoMap = quotationList.stream().collect(Collectors.groupingBy(Quotation::getCode));

        //????????????????????????
        Set<String> r2Set = new HashSet<>();

        int minArrDays = (int) (limitDays * rRate);
        for (Dict x : r1DictList) {
//            if ("sz002166".equals(x.getCode())) {
//                System.out.println(x.getCode());
//            }
            //????????????????????????????????????????????????
            if (!codePropMap.containsKey(x.getCode()) || StringUtils.isEmpty(codePropMap.get(x.getCode()).getProvince()) || codePropMap.get(x.getCode()).getProvince().contains("?????????") ||
                    codePropMap.get(x.getCode()).getProvince().contains("?????????") || codePropMap.get(x.getCode()).getProvince().contains("??????") || codePropMap.get(x.getCode()).getProvince().contains("??????") ||
                    codePropMap.get(x.getCode()).getProvince().contains("?????????") || codePropMap.get(x.getCode()).getProvince().contains("????????????") || codePropMap.get(x.getCode()).getProvince().contains("?????????") ||
                    codePropMap.get(x.getCode()).getProvince().contains("??????") || codePropMap.get(x.getCode()).getProvince().contains("?????????")) {
                continue;
            }
            //????????????????????????
            if (codePropMap.get(x.getCode()).getLyr() == null || codePropMap.get(x.getCode()).getLyr().doubleValue() < 0 || codePropMap.get(x.getCode()).getLyr().doubleValue() > 200) {
                continue;
            }
            if (codeHisMap.containsKey(x.getCode()) && codeQuoMap.containsKey(x.getCode())) {
                List<AvgPrice> curHisList = codeHisMap.get(x.getCode());
                List<Quotation> curQuoList = codeQuoMap.get(x.getCode());
                if (curHisList.get(curHisList.size() - 1).getAvg30() != null && curHisList.get(curHisList.size() - 1).getAvg30().doubleValue() < 5 || curHisList.get(curHisList.size() - 1).getAvg30() != null && curHisList.get(curHisList.size() - 1).getAvg30().doubleValue() > 50) {
                    continue;
                }
                //??????????????????
                List<Quotation> curLimitQuoList = SpiderUtil.deepCopyByProtobuff(curQuoList).stream().sorted(Comparator.comparing(Quotation::getDate).reversed()).limit(limitDays).collect(Collectors.toList());
                curLimitQuoList = curLimitQuoList.stream().sorted(Comparator.comparing(Quotation::getDate)).collect(Collectors.toList());
                List<AvgPrice> curLimitHisList = SpiderUtil.deepCopyByProtobuff(curHisList).stream().sorted(Comparator.comparing(AvgPrice::getDate).reversed()).limit(limitDays).collect(Collectors.toList());
                curLimitHisList = curLimitHisList.stream().sorted(Comparator.comparing(AvgPrice::getDate)).collect(Collectors.toList());

                boolean isOK = false;

//                //?????????????????????2000w?????????????????????
//                Iterator<Quotation> quoIte = curLimitQuoList.iterator();
//                Quotation cQuotation = null;
//                while (quoIte.hasNext()) {
//                    cQuotation = quoIte.next();
//                    if (cQuotation.getVolumeAmt().doubleValue() < 20000000) {
//                        quoIte.remove();
//                        break;
//                    }
//                }

                //5????????????
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

                //???????????????5??????
                if (!isOK) {
                    if (curLimitQuoList.size() == limitDays) {
                        int arrCount = 0;
                        Map<String,AvgPrice> dateHisMap = curLimitHisList.stream().collect(Collectors.toMap(h -> h.getDate().format(ymdFormatter), Function.identity(),(oldV,newV) ->{
                            System.out.println(String.format("?????????%s -> %s",newV.getCode(),newV.getDate().format(ymdFormatter)));
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

                //???????????????
                if (!isOK) {
                    Map<String, AvgPrice> dateHisMap = curLimitHisList.stream().collect(Collectors.toMap(h -> h.getDate().format(ymdFormatter), Function.identity(), (oldV, newV) -> {
                        log.info(String.format("?????????%s -> %s", newV.getCode(), newV.getDate().format(ymdFormatter)));
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

        //???????????????????????????3???????????????????????????
        List<RelativePositionVo> relativePositionVoList = quotationMapper.selectCurPosition(strDate);
        Map<String,RelativePositionVo> codePositionMap = relativePositionVoList.stream().collect(Collectors.toMap(RelativePositionVo::getCode,Function.identity(),(o,n) -> n));
        //??????????????????????????????????????????????????????80????????????????????????
        r2Set = r2Set.stream().filter(x -> codePositionMap.containsKey(x) && codePositionMap.get(x).getPRate() < 80).collect(Collectors.toSet());

        //????????????8????????????????????????2????????????
        List<UpLimitCountVo> upLimitCountVoList = quotationMapper.selectUpLimitGtN(250,maxDate,0);
        Map<String,UpLimitCountVo> codeLimitCountMap = upLimitCountVoList.stream().collect(Collectors.toMap(UpLimitCountVo::getCode,Function.identity(),(o,n) -> n));
        //???????????????????????????2??????
        r2Set = r2Set.stream().filter(codeLimitCountMap::containsKey).collect(Collectors.toSet());

        //??????????????????
        Map<String,Dict> codeNameMap = dictList.stream().collect(Collectors.toMap(Dict::getCode,Function.identity()));
        List<CodeNameVo> resultList = r2Set.stream().map(x -> new CodeNameVo(x,codeNameMap.get(x).getName())).collect(Collectors.toList());

        log.info("==============================??????????????????\r\n");
        resultList.forEach(x -> log.info("{} -> {}",x.getCode(),x.getName()));
        log.info("==============================???{}???==============================\r\n",resultList.size());

        //??????????????????????????????
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
        //??????????????????
        mybatisBatchHandler.batchInsertOrUpdate(sysDictList, SysDictMapper.class,GlobalConstant.BATCH_MODDEL_INSERT);
        long endTime = System.currentTimeMillis();
        log.info("==========================???????????????????????????????????????????????????{}==========================", SpiderUtil.getCurrentTimeStr());
        log.info("==========================????????????{}???==========================", (endTime - startTime) / 1000);
    }

    /**
     * ???????????????
     * @param codeList
     */
    public void addBackList(String codeList) {
        List<Dict> dictList = dictMapper.selectAll();
        Map<String,String> codeNameMap = dictList.stream().collect(Collectors.toMap(Dict::getCode,Dict::getName));

        List<SysDict> sysDictList = sysDictMapper.selectByType(GlobalConstant.SYS_DICT_TYPE_BACKLIST);
        List<String> backListCodeList = sysDictList.stream().map(SysDict::getValue).collect(Collectors.toList());

        //?????????????????????
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
     * ????????????????????????
     * ??????????????????
     */
    public void crewDictProp() {
        long startTime = System.currentTimeMillis();
        log.info("==========================????????????????????????????????????????????????{}==========================",SpiderUtil.getCurrentTimeStr());
        List<Dict> dictList = dictMapper.selectListByType(GlobalConstant.DICT_TYPE_STOCK);
        //??????ST???????????????????????????
        dictList = dictList.stream().filter(x -> !x.getName().toUpperCase().contains("ST") && !x.getCode().contains("sz300") && !x.getCode().contains("sh688")).collect(Collectors.toList());
        if (dictList.size() > 0) {
            //?????????????????????
            Collections.shuffle(dictList);

//            //TODO ??????
//            List<String> codeList = Arrays.asList("sh600497","sh601566","sz300285","sz002837","sh603936","sz002380","sh600183","sz002622","sz000533","sz002116");
//            List<String> codeList = Arrays.asList("sz002256","sz002873");
//            dictList = dictList.stream().filter(x -> codeList.contains(x.getCode())).collect(Collectors.toList());

            //????????????????????????
            List<List<Dict>> twoDeepList = SpiderUtil.partitionList(dictList,GlobalConstant.MAX_THREAD_COUNT);
            SimpleDateFormat ymdFormat = new SimpleDateFormat("yyyy-MM-dd");

            log.info("============================???????????????{}???????????????{}?????????????????????,???????????????{}============================",twoDeepList.size(),dictList.size(),SpiderUtil.getCurrentTimeStr());
            ExecutorService threadPool = Executors.newFixedThreadPool(twoDeepList.size());
            CountDownLatch latch = new CountDownLatch(twoDeepList.size());
            List<List<DictProp>> twoDeepDictPropList = new ArrayList<>();

            //?????????????????????
            proxyIpList = this.crewProxyList(twoDeepList.size(),1);
            //??????ip?????????????????????
            twoDeepProxyList = SpiderUtil.partitionList(proxyIpList,twoDeepList.size());
            if (twoDeepProxyList.size() < twoDeepList.size()) {
                log.error("============================??????IP??????????????????============================");
                return;
            }
            int loopIndex = 0;
            //??????????????????????????????????????????????????????????????????
            AtomicInteger counter = new AtomicInteger(0);
            //??????????????????
            AtomicInteger waitThreadCounter = new AtomicInteger(0);
            //??????????????????????????????
            AtomicInteger switchCounter = new AtomicInteger(0);
            Map<String,Integer> banIpMap = new ConcurrentHashMap<>();
            Map<String,Integer> threadNameIndexMap = new HashMap<>();
            //????????????????????????????????????????????????
            List<String> threadNameList = new CopyOnWriteArrayList<>();
            for (int i = 0 ; i < latch.getCount() ; i++) {
                threadNameList.add(String.format("crew-jqka-thread-%s",(i+1)));
                threadNameIndexMap.put(threadNameList.get(i),i);
            }

            //??????header
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
                    //???????????????
                    Thread.currentThread().setName(threadNameList.get(index));
                    //?????????????????????????????????
                    List<ProxyIp> curProxyList = twoDeepProxyList.get(threadNameIndexMap.get(Thread.currentThread().getName()));
                    ProxyIp proxyIp = curProxyList.get(0);

                    //???????????????httpclient?????????????????????????????????1??????????????????
                    HttpClientUtil clientUtil = new HttpClientUtil();
                    clientUtil.setMaxRetryCount(1);
                    clientUtil.setProxyPropArr(new String[]{proxyIp.getIp(),String.valueOf(proxyIp.getPort())});

                    //????????????????????????
                    ResponseEntity responseEntity = null;

                    String homeHtml = null;
                    String companyHtml = null;
                    List<DictProp> groupedDictPropList = new ArrayList<>();
                    for (Dict dict : innerList) {
                        try {
                            DictProp dictProp = new DictProp();

                            //????????????
                            boolean isFetchSuccess = false;
                            int rCount = 0;
                            for (;;) {
                                //?????????????????????????????????????????????????????????????????????????????????
                                if (curProxyList.size() == 0) {
                                    //???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                                    counter.getAndAdd(1);
                                    //??????????????????????????????????????????
                                    if (counter.get() == latch.getCount()) {
                                        log.info("=======================??????{}??????????????????????????????????????????????????????",Thread.currentThread().getName());
                                        switchCounter.getAndAdd(1);
                                        log.info("==========================??????{}???{}?????????????????????==========================",Thread.currentThread().getName(),switchCounter.get());
                                        //??????????????????????????????????????????????????????????????????????????????????????????????????????????????????ip??????
                                        int fetchCount = 0;
                                        for (;;) {
                                            //??????????????????ip????????????????????????ip???
                                            proxyIpList.addAll(this.crewProxyList((int)latch.getCount(),1));
                                            //???????????????????????????ip
                                            proxyIpList = proxyIpList.stream().filter(x -> !banIpMap.containsKey(x.getIp())).collect(Collectors.toList());
                                            if (proxyIpList.size() >= latch.getCount()) {
                                                break;
                                            }
                                            fetchCount++;
                                            log.info("==========================??????????????????????????????????????????{}???????????????==========================",fetchCount);
                                        }
                                        //????????????????????????
                                        twoDeepProxyList = SpiderUtil.partitionList(proxyIpList,(int)latch.getCount());
                                        //?????????????????????????????????
                                        threadNameIndexMap.clear();
                                        for (int i = 0 ; i < latch.getCount() ; i++) {
                                            threadNameIndexMap.put(threadNameList.get(i),i);
                                        }
                                        threadNameIndexMap.forEach((k,v) -> log.info("=============?????????????????? {} => {}",k,v));
                                        //??????????????????????????????
                                        curProxyList.addAll(twoDeepProxyList.get(threadNameIndexMap.get(Thread.currentThread().getName())));
                                        if (latch.getCount() > 1) {
                                            //????????????????????????
                                            counter.getAndSet(0);
                                            //???????????????????????????????????????
                                            for (;;) {
                                                synchronized (lock) {
                                                    lock.notifyAll();
                                                }
                                                log.info("=======================??????{}????????????????????????????????????????????????????????????",Thread.currentThread().getName());
                                                break;
                                            }
                                        }
                                    } else {
                                        //???????????????????????????????????????????????????????????????????????????????????????
                                        synchronized (lock) {
                                            try {
                                                log.info("=======================??????{}??????IP????????????????????????",Thread.currentThread().getName());
                                                waitThreadCounter.getAndAdd(1);
                                                lock.wait();
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                        //????????????????????????????????????
                                        waitThreadCounter.getAndAdd(-1);
                                        log.info("=======================??????{}????????????????????????",Thread.currentThread().getName());
                                        curProxyList.addAll(twoDeepProxyList.get(threadNameIndexMap.get(Thread.currentThread().getName())));
                                    }
                                }
                                try {
                                    responseEntity = clientUtil.recursiveHttpRequest(GlobalConstant.HTTP_GET,0, String.format(GlobalConstant.JQKA_STOCK_BASIC_URL,dict.getCode().substring(2)),null,headerMap, GlobalConstant.CHARASET_GBK);
                                    homeHtml = responseEntity.getContent();
                                } catch (Exception e) {
                                    log.info("=========================??????????????????????????????????????????????????????????????????=========================");
                                    homeHtml = null;
                                }
                                //?????????????????????????????????????????????
                                if (StringUtils.isNotEmpty(homeHtml) && homeHtml.contains("????????????")) {
                                    isFetchSuccess = true;
                                    break;
                                }

                                //??????????????????ip???????????????
                                banIpMap.put(proxyIp.getIp(),0);
                                //????????????????????????
                                curProxyList.remove(proxyIp);
                                //????????????????????????????????????????????????????????????
                                if (curProxyList.size() > 0) {
                                    rCount++;
                                    //????????????
                                    proxyIp = curProxyList.get(0);
                                    log.info("==========================??????{}???{}??????????????????IP???{}???==========================",Thread.currentThread().getName(),rCount,proxyIp.getIp());
                                    //????????????
                                    clientUtil.setProxyPropArr(new String[]{proxyIp.getIp(),String.valueOf(proxyIp.getPort())});
                                }
                            }
                            if (isFetchSuccess) {
                                //????????????
                                StringBuilder sBuff = new StringBuilder();
                                Pattern pattern = Pattern.compile("ifind\">(.*?)</a>");
                                Matcher matcher = pattern.matcher(homeHtml);
                                String sText = null;
                                while (matcher.find()) {
                                    sText = matcher.group(1);
                                    if (!sText.contains("??????")) {
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

                                //??????????????????
                                pattern = Pattern.compile("id=\"jtsyl\">(.*?)</span>");
                                matcher = pattern.matcher(homeHtml);
                                if (matcher.find()) {
                                    if ("??????".equals(matcher.group(1))) {
                                        dictProp.setLyr(new BigDecimal(-1));
                                    } else {
                                        try {
                                            dictProp.setLyr(new BigDecimal(matcher.group(1)));
                                        } catch (Exception e) {
                                            log.error("==================??????{}????????????????????????==================",dict.getName());
                                            e.printStackTrace();
                                        }
                                    }
                                }

                                //?????????????????????
                                pattern = Pattern.compile("id=\"dtsyl\">(.*?)</span>");
                                matcher = pattern.matcher(homeHtml);
                                if (matcher.find()) {
                                    if ("??????".equals(matcher.group(1))) {
                                        dictProp.setTtm(new BigDecimal(-1));
                                    } else {
                                        try {
                                            dictProp.setTtm(new BigDecimal(matcher.group(1)));
                                        } catch (Exception e) {
                                            log.error("==================??????{}????????????????????????==================",dict.getName());
                                            e.printStackTrace();
                                        }
                                    }
                                }

                                //??????????????????
                                if (homeHtml.contains("??????????????????")) {
                                    pattern = Pattern.compile("<span class=\"tip f12\">(.*?)</span>");
                                    matcher = pattern.matcher(homeHtml);
                                    while (matcher.find()) {
                                        if (matcher.group(1).contains("-") && !matcher.group(1).contains(".")) {
                                            dictProp.setLatestLiftBan(ymdFormat.parse(matcher.group(1)));
                                        }
                                    }
                                }
                            }

                            //??????????????????
                            for (;;) {
                                //?????????????????????????????????????????????????????????????????????????????????
                                if (curProxyList.size() == 0) {
                                    //???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                                    counter.getAndAdd(1);
                                    //??????????????????????????????????????????
                                    if (counter.get() == latch.getCount()) {
                                        log.info("=======================??????{}??????????????????????????????????????????????????????",Thread.currentThread().getName());
                                        switchCounter.getAndAdd(1);
                                        log.info("==========================??????{}???{}?????????????????????==========================",Thread.currentThread().getName(),switchCounter.get());
                                        //??????????????????????????????????????????????????????????????????????????????????????????????????????????????????ip??????
                                        int fetchCount = 0;
                                        for (;;) {
                                            //??????????????????ip????????????????????????ip???
                                            proxyIpList.addAll(this.crewProxyList((int)latch.getCount(),1));
                                            //???????????????????????????ip
                                            proxyIpList = proxyIpList.stream().filter(x -> !banIpMap.containsKey(x.getIp())).collect(Collectors.toList());
                                            if (proxyIpList.size() >= latch.getCount()) {
                                                break;
                                            }
                                            fetchCount++;
                                            log.info("==========================????????????????????????????????????????????????{}???????????????==========================",fetchCount);
                                        }
                                        //????????????????????????
                                        twoDeepProxyList = SpiderUtil.partitionList(proxyIpList,(int)latch.getCount());
                                        //?????????????????????????????????
                                        threadNameIndexMap.clear();
                                        for (int i = 0 ; i < latch.getCount() ; i++) {
                                            threadNameIndexMap.put(threadNameList.get(i),i);
                                        }
                                        threadNameIndexMap.forEach((k,v) -> log.info("=============?????????????????? {} => {}",k,v));
                                        //??????????????????????????????
                                        curProxyList.addAll(twoDeepProxyList.get(threadNameIndexMap.get(Thread.currentThread().getName())));
                                        //???????????????????????????????????????????????????????????????????????????????????????
                                        if (latch.getCount() > 1) {
                                            //????????????????????????
                                            counter.getAndSet(0);
                                            //???????????????????????????????????????
                                            for (;;) {
                                                synchronized (lock) {
                                                    lock.notifyAll();
                                                }
                                                log.info("=======================??????{}????????????????????????????????????????????????????????????",Thread.currentThread().getName());
                                                break;
                                            }
                                        }
                                    } else {
                                        //???????????????????????????????????????????????????????????????????????????????????????
                                        synchronized (lock) {
                                            try {
                                                log.info("=======================??????{}??????IP????????????????????????",Thread.currentThread().getName());
                                                waitThreadCounter.getAndAdd(1);
                                                lock.wait();
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                        //????????????????????????????????????
                                        waitThreadCounter.getAndAdd(-1);
                                        log.info("=======================??????{}????????????????????????",Thread.currentThread().getName());
                                        curProxyList.addAll(twoDeepProxyList.get(threadNameIndexMap.get(Thread.currentThread().getName())));
                                    }
                                }
                                try {
                                    responseEntity = clientUtil.recursiveHttpRequest(GlobalConstant.HTTP_GET,0, String.format(GlobalConstant.JQKA_STOCK_COMPANY_URL,dict.getCode().substring(2)),null,headerMap,GlobalConstant.CHARASET_GBK);
                                    companyHtml = responseEntity.getContent();
                                } catch (Exception e) {
                                    log.error("=========================??????????????????????????????????????????????????????????????????=========================");
                                    companyHtml = null;
                                }
                                //?????????????????????????????????????????????????????????
                                if (StringUtils.isNotEmpty(companyHtml) && companyHtml.contains("????????????")) {
                                    isFetchSuccess = true;
                                    break;
                                }

                                //??????????????????ip???????????????
                                banIpMap.put(proxyIp.getIp(),0);
                                //????????????????????????
                                curProxyList.remove(proxyIp);
                                //????????????????????????????????????????????????????????????
                                if (curProxyList.size() > 0) {
                                    rCount++;
                                    //????????????
                                    proxyIp = curProxyList.get(0);
                                    log.info("==========================??????{}???{}??????????????????IP???{}???==========================",Thread.currentThread().getName(),rCount,proxyIp.getIp());
                                    //????????????
                                    clientUtil.setProxyPropArr(new String[]{proxyIp.getIp(),String.valueOf(proxyIp.getPort())});
                                }
                            }
                            if (isFetchSuccess) {
                                companyHtml = responseEntity.getContent();

                                //??????????????????
                                Pattern pattern = Pattern.compile("???????????????</strong><span>(.*?)</span>");
                                Matcher matcher = pattern.matcher(companyHtml);
                                if (matcher.find()) {
                                    dictProp.setCompanyName(matcher.group(1));
                                }

                                //???????????????????????????
                                pattern = Pattern.compile("?????????????????????</strong><span>(.*?)</span>");
                                matcher = pattern.matcher(companyHtml);
                                if (matcher.find()) {
                                    String matchText = matcher.group(1);
                                    if (StringUtils.isNotEmpty(matchText)) {
                                        if (matchText.contains("???")) {
                                            matchText = matchText.replaceAll(" ","");
                                            String []matchArr = matchText.split("???");
                                            dictProp.setIndustry(matchArr[0]);
                                            dictProp.setIdySegment(matchArr[1]);
                                        } else {
                                            dictProp.setIndustry(matchText);
                                        }
                                    }
                                }

                                //????????????
                                pattern = Pattern.compile("???????????????</strong><span>(.*?)</span>");
                                matcher = pattern.matcher(companyHtml);
                                if (matcher.find()) {
                                    dictProp.setProvince(matcher.group(1));
                                }

                                //?????????????????????
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
                                            log.error("==============================={} -> {}????????????????????????===============================",dict.getCode(),String.format(GlobalConstant.JQKA_STOCK_COMPANY_URL,dict.getCode().substring(2)));
                                        }
                                        break;
                                    }
                                    nLoopCount++;
                                }

                                pattern = Pattern.compile("<td colspan=\"3\">(.*?)</td>",Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                                matcher = pattern.matcher(companyHtml);
                                while (matcher.find()) {
                                    if (matcher.group(1).contains("????????????")) {
                                        pattern = Pattern.compile("<span>(.*?)</span>");
                                        matcher = pattern.matcher(matcher.group(1));
                                        if (matcher.find()) {
                                            dictProp.setAddress(matcher.group(1));
                                        }
                                    }
                                }
                            }
                            //??????????????????
                            dictProp.setId(genIdUtil.nextId());
                            dictProp.setCode(dict.getCode());
                            dictProp.setName(dict.getName());
                            groupedDictPropList.add(dictProp);
                            log.info("====================================?????????????????? => {}",dict.getName());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    twoDeepDictPropList.add(groupedDictPropList);
                    latch.countDown();
                    log.info("=======================??????{}??????????????????????????????????????????:{}=======================",Thread.currentThread().getName(),latch.getCount());

                    //??????????????????
                    threadNameList.remove(Thread.currentThread().getName());
                    //?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                    if (!threadNameList.isEmpty() && waitThreadCounter.get() == latch.getCount()) {
                        //????????????????????????
                        counter.getAndSet(0);
                        //???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                        if (latch.getCount() == 1) {
                            log.info("=======================????????????????????????????????????????????????????????????????????????????????????=======================");
                            twoDeepProxyList.get(threadNameIndexMap.get(threadNameList.get(0))).addAll(curProxyList);
                        }
                        //???????????????????????????
                        for (;;) {
                            log.info("=======================??????{}??????????????????",Thread.currentThread().getName());
                            synchronized (lock) {
                                lock.notifyAll();
                            }
                            log.info("=======================??????{}????????????????????????????????????????????????????????????",Thread.currentThread().getName());
                            break;
                        }
                    }
                });
                loopIndex++;
            }
            //?????????????????????????????????
            try {
                latch.await();
                threadPool.shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            long cEndTime = System.currentTimeMillis();
            log.info("==========================????????????????????????????????????{}???==========================",(cEndTime - startTime) / 1000);

            //???????????????????????????
            File dirFile = new File(stockConfig.getImgDownadDir());
            if (dirFile.exists()) {
                try {
                    SpiderUtil.deleteFile(dirFile);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("====================???????????????????????????====================");
                }
            }

            List<DictProp> dbDictPropList = twoDeepDictPropList.stream().flatMap(List::stream).collect(Collectors.toList());
            //??????????????????????????????
            dictPropMapper.deleteAll();
            mybatisBatchHandler.batchInsertOrUpdate(dbDictPropList,DictPropMapper.class,GlobalConstant.BATCH_MODDEL_INSERT);
            long endTime = System.currentTimeMillis();
            log.info("==========================????????????????????????????????????????????????{}==========================",SpiderUtil.getCurrentTimeStr());

            //??????????????????-??????-??????????????????
            genQuoIdpDataList();
            log.info("==========================?????????{}???==========================", (endTime - startTime) / 1000);
        }
    }

    /**
     * ????????????????????????
     * ??????Selenium???????????????????????????
     */
    public void crewDictPropWithSelenium() {
        long startTime = System.currentTimeMillis();
        log.info("==========================????????????????????????????????????????????????{}==========================",SpiderUtil.getCurrentTimeStr());
        List<Dict> dictList = dictMapper.selectListByType(GlobalConstant.DICT_TYPE_STOCK);
        //??????ST???????????????????????????
        dictList = dictList.stream().filter(x -> !x.getName().toUpperCase().contains("ST") && !x.getCode().contains("sz300") && !x.getCode().contains("sh688")).collect(Collectors.toList());
        if (dictList.size() > 0) {
            //?????????????????????
            Collections.shuffle(dictList);

            //TODO ??????
//            List<String> codeList = Arrays.asList("sh600497","sh601566","sz300285","sz002837","sh603936","sz002380","sh600183","sz002622","sz000533","sz002116");
//            List<String> codeList = Arrays.asList("sz000777");
//            dictList = dictList.stream().filter(x -> codeList.contains(x.getCode())).collect(Collectors.toList());

            //????????????????????????
            int crewMimvpCount = 2;
            List<List<Dict>> twoDeepList = SpiderUtil.partitionList(dictList,GlobalConstant.MAX_THREAD_COUNT);
            SimpleDateFormat ymdFormat = new SimpleDateFormat("yyyy-MM-dd");

            log.info("============================???????????????{}???????????????{}?????????????????????,???????????????{}============================",twoDeepList.size(),dictList.size(),SpiderUtil.getCurrentTimeStr());
            ExecutorService threadPool = Executors.newFixedThreadPool(twoDeepList.size());
            CountDownLatch latch = new CountDownLatch(twoDeepList.size());
            int timeoutMs = 10;
            List<List<DictProp>> twoDeepDictPropList = new ArrayList<>();

            long initMimvpTime = System.currentTimeMillis() / 1000 - 20 * 60 * 1000;
            //?????????????????????
            proxyIpList = this.crewProxyList(8,1);
            //??????ip?????????????????????
            twoDeepProxyList = SpiderUtil.partitionList(proxyIpList,twoDeepList.size());
            if (twoDeepProxyList.size() < twoDeepList.size()) {
                log.error("============================??????IP??????????????????============================");
                return;
            }
            int loopIndex = 0;
            //??????????????????????????????????????????????????????????????????
            AtomicInteger counter = new AtomicInteger(0);
            //??????????????????
            AtomicInteger waitThreadCounter = new AtomicInteger(0);
            //??????????????????????????????
            AtomicInteger switchCounter = new AtomicInteger(0);
            Map<String,Integer> banIpMap = new ConcurrentHashMap<>();
            Map<String,Integer> threadNameIndexMap = new HashMap<>();
            //????????????????????????????????????????????????
            List<String> threadNameList = new CopyOnWriteArrayList<>();
            for (int i = 0 ; i < latch.getCount() ; i++) {
                threadNameList.add(String.format("crew-jqka-thread-%s",(i+1)));
                threadNameIndexMap.put(threadNameList.get(i),i);
            }
            for (List<Dict> innerList : twoDeepList) {
                //???????????????????????????
                try {
                    Thread.sleep(SpiderUtil.getRandomNum(1,20));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                final int index = loopIndex;
                threadPool.execute(() -> {
                    //???????????????
                    Thread.currentThread().setName(threadNameList.get(index));
                    //?????????????????????????????????
                    List<ProxyIp> curProxyList = twoDeepProxyList.get(threadNameIndexMap.get(Thread.currentThread().getName()));

                    //??????????????????????????????
                    ChromeOptions options = new ChromeOptions();
                    //options.addArguments("--headless"); //??????????????????
                    options.addArguments("--no-sandbox");
                    options.addArguments("--disable-gpu");
                    options.addArguments("blink-settings=imagesEnabled=false");
                    options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.70 Safari/537.36");
                    ProxyIp proxyIp = curProxyList.get(0);
                    options.addArguments("--proxy-server=http://" + String.format("%s:%s",proxyIp.getIp(),proxyIp.getPort()));
                    //options.addExtensions(new File(proxyTool.getRandomZipProxy()));//??????????????????
                    WebDriver webDriver = new ChromeDriver(options);//?????????
                    //?????????????????????3S
                    webDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(timeoutMs));

                    String homeHtml = null;
                    String companyHtml = null;
                    List<DictProp> groupedDictPropList = new ArrayList<>();
                    for (Dict dict : innerList) {
                        try {
                            DictProp dictProp = new DictProp();

                            //????????????
                            boolean isFetchSuccess = false;
                            String tmpHomeHtml = null;
                            int rCount = 0;
                            for (;;) {
                                //?????????????????????????????????????????????????????????????????????????????????
                                if (curProxyList.size() == 0) {
                                    //???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                                    counter.getAndAdd(1);
                                    //??????????????????????????????????????????
                                    if (counter.get() == latch.getCount()) {
                                        log.info("=======================??????{}??????????????????????????????????????????????????????",Thread.currentThread().getName());
                                        switchCounter.getAndAdd(1);
                                        log.info("==========================??????{}???{}?????????????????????==========================",Thread.currentThread().getName(),switchCounter.get());
                                        //??????????????????????????????????????????????????????????????????????????????????????????????????????????????????ip??????
                                        int fetchCount = 0;
                                        for (;;) {
                                            //??????????????????ip????????????????????????ip???
                                            proxyIpList.addAll(this.crewProxyList((int)latch.getCount(),1));
                                            //???????????????????????????ip
                                            proxyIpList = proxyIpList.stream().filter(x -> !banIpMap.containsKey(x.getIp())).collect(Collectors.toList());
                                            if (proxyIpList.size() >= latch.getCount()) {
                                                break;
                                            }
                                            fetchCount++;
                                            log.info("==========================????????????????????????????????????????????????{}???????????????==========================",fetchCount);
                                        }
                                        //????????????????????????
                                        twoDeepProxyList = SpiderUtil.partitionList(proxyIpList,(int)latch.getCount());
                                        //?????????????????????????????????
                                        threadNameIndexMap.clear();
                                        for (int i = 0 ; i < latch.getCount() ; i++) {
                                            threadNameIndexMap.put(threadNameList.get(i),i);
                                        }
                                        threadNameIndexMap.forEach((k,v) -> log.info("=============?????????????????? {} => {}",k,v));
                                        //??????????????????????????????
                                        curProxyList.addAll(twoDeepProxyList.get(threadNameIndexMap.get(Thread.currentThread().getName())));
                                        if (latch.getCount() > 1) {
                                            //????????????????????????
                                            counter.getAndSet(0);
                                            //???????????????????????????????????????
                                            for (;;) {
                                                synchronized (lock) {
                                                    lock.notifyAll();
                                                }
                                                log.info("=======================??????{}????????????????????????????????????????????????????????????",Thread.currentThread().getName());
                                                break;
                                            }
                                        }
                                    } else {
                                        //???????????????????????????????????????????????????????????????????????????????????????
                                        synchronized (lock) {
                                            try {
                                                log.info("=======================??????{}??????IP????????????????????????",Thread.currentThread().getName());
                                                waitThreadCounter.getAndAdd(1);
                                                lock.wait();
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                        //????????????????????????????????????
                                        waitThreadCounter.getAndAdd(-1);
                                        log.info("=======================??????{}????????????????????????",Thread.currentThread().getName());
                                        curProxyList.addAll(twoDeepProxyList.get(threadNameIndexMap.get(Thread.currentThread().getName())));
                                    }
                                }
                                try {
                                    webDriver.get(String.format(GlobalConstant.JQKA_STOCK_BASIC_URL,dict.getCode().substring(2)));
                                    tmpHomeHtml = webDriver.getPageSource();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    log.info("=========================??????????????????????????????????????????????????????????????????=========================");
                                    tmpHomeHtml = null;
                                }
                                //?????????????????????????????????????????????
                                if (StringUtils.isNotEmpty(tmpHomeHtml) && tmpHomeHtml.contains("????????????")) {
                                    isFetchSuccess = true;
                                    break;
                                }

                                //??????????????????ip???????????????
                                banIpMap.put(proxyIp.getIp(),0);
                                //????????????????????????
                                curProxyList.remove(proxyIp);
                                //????????????????????????????????????????????????????????????
                                if (curProxyList.size() > 0) {
                                    rCount++;
                                    //????????????
                                    proxyIp = curProxyList.get(0);
                                    log.info("==========================??????{}???{}??????????????????IP???{}???==========================",Thread.currentThread().getName(),rCount,proxyIp.getIp());
                                    //??????????????????????????????
                                    webDriver.quit();
                                    options = new ChromeOptions();
                                    //options.addArguments("--headless"); //??????????????????
                                    options.addArguments("--no-sandbox");
                                    options.addArguments("--disable-gpu");
                                    options.addArguments("blink-settings=imagesEnabled=false");
                                    options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.70 Safari/537.36");
                                    proxyIp = curProxyList.get(0);
                                    options.addArguments("--proxy-server=http://" + String.format("%s:%s",proxyIp.getIp(),proxyIp.getPort()));
                                    //options.addExtensions(new File(proxyTool.getRandomZipProxy()));//??????????????????
                                    webDriver = new ChromeDriver(options);//?????????
                                    //?????????????????????3S
                                    webDriver.manage().timeouts().pageLoadTimeout(timeoutMs, TimeUnit.SECONDS);
                                }
                            }
                            if (isFetchSuccess) {
                                log.info("====================================?????????????????????????????? => {}",dict.getName());
                                homeHtml = webDriver.getPageSource();

                                //????????????
                                Pattern pattern = Pattern.compile("<span class=\"tip f14\">(.*?)</span>");
                                Matcher matcher = pattern.matcher(homeHtml);
                                if (matcher.find()) {
                                    dictProp.setIndustry(matcher.group(1));
                                }

                                //????????????
                                StringBuilder sBuff = new StringBuilder();
                                pattern = Pattern.compile("ifind\">(.*?)</a>");
                                matcher = pattern.matcher(homeHtml);
                                String sText = null;
                                while (matcher.find()) {
                                    sText = matcher.group(1);
                                    if (!sText.contains("??????")) {
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

                                //??????????????????
                                pattern = Pattern.compile("id=\"jtsyl\">(.*?)</span>");
                                matcher = pattern.matcher(homeHtml);
                                if (matcher.find()) {
                                    if ("??????".equals(matcher.group(1))) {
                                        dictProp.setLyr(new BigDecimal(-1));
                                    } else {
                                        try {
                                            dictProp.setLyr(new BigDecimal(matcher.group(1)));
                                        } catch (Exception e) {
                                            log.error("==================??????{}????????????????????????==================",dict.getName());
                                            e.printStackTrace();
                                        }
                                    }
                                }

                                //?????????????????????
                                pattern = Pattern.compile("id=\"dtsyl\">(.*?)</span>");
                                matcher = pattern.matcher(homeHtml);
                                if (matcher.find()) {
                                    if ("??????".equals(matcher.group(1))) {
                                        dictProp.setTtm(new BigDecimal(-1));
                                    } else {
                                        try {
                                            dictProp.setTtm(new BigDecimal(matcher.group(1)));
                                        } catch (Exception e) {
                                            log.error("==================??????{}????????????????????????==================",dict.getName());
                                            e.printStackTrace();
                                        }
                                    }
                                }

                                //??????????????????
                                if (homeHtml.contains("??????????????????")) {
                                    pattern = Pattern.compile("<span class=\"tip f12\">(.*?)</span>");
                                    matcher = pattern.matcher(homeHtml);
                                    while (matcher.find()) {
                                        if (matcher.group(1).contains("-") && !matcher.group(1).contains(".")) {
                                            dictProp.setLatestLiftBan(ymdFormat.parse(matcher.group(1)));
                                        }
                                    }
                                }
                            }

                            //??????????????????
                            String tmpCompanyHtml = null;
                            for (;;) {
                                //?????????????????????????????????????????????????????????????????????????????????
                                if (curProxyList.size() == 0) {
                                    //???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                                    counter.getAndAdd(1);
                                    //??????????????????????????????????????????
                                    if (counter.get() == latch.getCount()) {
                                        log.info("=======================??????{}??????????????????????????????????????????????????????",Thread.currentThread().getName());
                                        switchCounter.getAndAdd(1);
                                        log.info("==========================??????{}???{}?????????????????????==========================",Thread.currentThread().getName(),switchCounter.get());
                                        //??????????????????????????????????????????????????????????????????????????????????????????????????????????????????ip??????
                                        int fetchCount = 0;
                                        for (;;) {
                                            //??????????????????ip????????????????????????ip???
                                            proxyIpList.addAll(this.crewProxyList((int)latch.getCount(),1));
                                            //???????????????????????????ip
                                            proxyIpList = proxyIpList.stream().filter(x -> !banIpMap.containsKey(x.getIp())).collect(Collectors.toList());
                                            if (proxyIpList.size() >= latch.getCount()) {
                                                break;
                                            }
                                            fetchCount++;
                                            log.info("==========================????????????????????????????????????????????????{}???????????????==========================",fetchCount);
                                        }
                                        //????????????????????????
                                        twoDeepProxyList = SpiderUtil.partitionList(proxyIpList,(int)latch.getCount());
                                        //?????????????????????????????????
                                        threadNameIndexMap.clear();
                                        for (int i = 0 ; i < latch.getCount() ; i++) {
                                            threadNameIndexMap.put(threadNameList.get(i),i);
                                        }
                                        threadNameIndexMap.forEach((k,v) -> log.info("=============?????????????????? {} => {}",k,v));
                                        //??????????????????????????????
                                        curProxyList.addAll(twoDeepProxyList.get(threadNameIndexMap.get(Thread.currentThread().getName())));
                                        //???????????????????????????????????????????????????????????????????????????????????????
                                        if (latch.getCount() > 1) {
                                            //????????????????????????
                                            counter.getAndSet(0);
                                            //???????????????????????????????????????
                                            for (;;) {
                                                synchronized (lock) {
                                                    lock.notifyAll();
                                                }
                                                log.info("=======================??????{}????????????????????????????????????????????????????????????",Thread.currentThread().getName());
                                                break;
                                            }
                                        }
                                    } else {
                                        //???????????????????????????????????????????????????????????????????????????????????????
                                        synchronized (lock) {
                                            try {
                                                log.info("=======================??????{}??????IP????????????????????????",Thread.currentThread().getName());
                                                waitThreadCounter.getAndAdd(1);
                                                lock.wait();
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                        //????????????????????????????????????
                                        waitThreadCounter.getAndAdd(-1);
                                        log.info("=======================??????{}????????????????????????",Thread.currentThread().getName());
                                        curProxyList.addAll(twoDeepProxyList.get(threadNameIndexMap.get(Thread.currentThread().getName())));
                                    }
                                }
                                try {
                                    webDriver.get(String.format(GlobalConstant.JQKA_STOCK_COMPANY_URL,dict.getCode().substring(2)));
                                    tmpCompanyHtml = webDriver.getPageSource();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    log.error("=========================??????????????????????????????????????????????????????????????????=========================");
                                    tmpCompanyHtml = null;
                                }
                                //?????????????????????????????????????????????????????????
                                if (StringUtils.isNotEmpty(tmpCompanyHtml) && tmpCompanyHtml.contains("????????????")) {
                                    isFetchSuccess = true;
                                    break;
                                }

                                //??????????????????ip???????????????
                                banIpMap.put(proxyIp.getIp(),0);
                                //????????????????????????
                                curProxyList.remove(proxyIp);
                                //????????????????????????????????????????????????????????????
                                if (curProxyList.size() > 0) {
                                    rCount++;
                                    //????????????
                                    proxyIp = curProxyList.get(0);
                                    log.info("==========================??????{}???{}??????????????????IP???{}???==========================",Thread.currentThread().getName(),rCount,proxyIp.getIp());
                                    //??????????????????????????????
                                    webDriver.quit();
                                    options = new ChromeOptions();
                                    //options.addArguments("--headless"); //??????????????????
                                    options.addArguments("--no-sandbox");
                                    options.addArguments("--disable-gpu");
                                    options.addArguments("blink-settings=imagesEnabled=false");
                                    options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.70 Safari/537.36");
                                    proxyIp = curProxyList.get(0);
                                    options.addArguments("--proxy-server=http://" + String.format("%s:%s",proxyIp.getIp(),proxyIp.getPort()));
                                    //options.addExtensions(new File(proxyTool.getRandomZipProxy()));//??????????????????
                                    webDriver = new ChromeDriver(options);//?????????
                                    //?????????????????????3S
                                    webDriver.manage().timeouts().pageLoadTimeout(timeoutMs, TimeUnit.SECONDS);
                                    //????????????????????????
                                    curProxyList.remove(proxyIp);
                                }
                            }
                            if (isFetchSuccess) {
                                log.info("====================================?????????????????????????????? => {}",dict.getName());
                                companyHtml = webDriver.getPageSource();

                                //??????????????????
                                Pattern pattern = Pattern.compile("???????????????</strong><span>(.*?)</span>");
                                Matcher matcher = pattern.matcher(companyHtml);
                                if (matcher.find()) {
                                    dictProp.setCompanyName(matcher.group(1));
                                }

                                //????????????
                                pattern = Pattern.compile("???????????????</strong><span>(.*?)</span>");
                                matcher = pattern.matcher(companyHtml);
                                if (matcher.find()) {
                                    dictProp.setProvince(matcher.group(1));
                                }

                                //?????????????????????
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
                                    if (matcher.group(1).contains("????????????")) {
                                        pattern = Pattern.compile("<span>(.*?)</span>");
                                        matcher = pattern.matcher(matcher.group(1));
                                        if (matcher.find()) {
                                            dictProp.setAddress(matcher.group(1));
                                        }
                                    }
                                }
                            }
                            //??????????????????
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
                    log.info("=======================??????{}??????????????????????????????????????????:{}=======================",Thread.currentThread().getName(),latch.getCount());

                    //??????????????????
                    threadNameList.remove(Thread.currentThread().getName());
                    //?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                    if (!threadNameList.isEmpty() && waitThreadCounter.get() == latch.getCount()) {
                        //????????????????????????
                        counter.getAndSet(0);
                        //???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                        if (latch.getCount() == 1) {
                            log.info("=======================????????????????????????????????????????????????????????????????????????????????????=======================");
                            twoDeepProxyList.get(threadNameIndexMap.get(threadNameList.get(0))).addAll(curProxyList);
                        }
                        //???????????????????????????
                        for (;;) {
                            log.info("=======================??????{}??????????????????",Thread.currentThread().getName());
                            synchronized (lock) {
                                lock.notifyAll();
                            }
                            log.info("=======================??????{}????????????????????????????????????????????????????????????",Thread.currentThread().getName());
                            break;
                        }
                    }
                });
                loopIndex++;
            }
            //?????????????????????????????????
            try {
                latch.await();
                threadPool.shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            long cEndTime = System.currentTimeMillis();
            log.info("==========================????????????????????????????????????{}???==========================",(cEndTime - startTime) / 1000);

            //???????????????????????????
            File dirFile = new File(stockConfig.getImgDownadDir());
            if (dirFile.exists()) {
                try {
                    SpiderUtil.deleteFile(dirFile);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("====================???????????????????????????====================");
                }
            }

            List<DictProp> dbDictPropList = twoDeepDictPropList.stream().flatMap(List::stream).collect(Collectors.toList());

            //??????????????????????????????
            dictPropMapper.deleteAll();
            mybatisBatchHandler.batchInsertOrUpdate(dbDictPropList,DictPropMapper.class,GlobalConstant.BATCH_MODDEL_INSERT);
            long endTime = System.currentTimeMillis();
            log.info("==========================????????????????????????????????????????????????{}==========================",SpiderUtil.getCurrentTimeStr());
            log.info("==========================?????????{}???==========================", (endTime - startTime) / 1000);
        }
    }

    /**
     * ?????????????????????????????????
     * @param maxDate
     * @param count
     */
    public List<String> crewNetEaseStockList(LocalDate maxDate,int count) {
        List<Dict> dictList = dictMapper.selectListByType(GlobalConstant.DICT_TYPE_STOCK);
        if (dictList.size() > 0) {
            //?????????????????????
            List<List<Dict>> twoDeepList = SpiderUtil.partitionList(dictList,GlobalConstant.MAX_THREAD_COUNT);
            ExecutorService threadPool = Executors.newFixedThreadPool(twoDeepList.size());
            //????????????
            DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

            String startDate = maxDate.plusDays(-1 * count).format(ymdFormatter);
            String endDate = maxDate.format(ymdFormatter);
            log.info("============================???????????????{}???????????????{}????????????{}???????????????CSV??????,???????????????{}============================",twoDeepList.size(),endDate,dictList.size(),SpiderUtil.getCurrentTimeStr());
            if (twoDeepList.size() > 0) {
                //???????????????
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
                            //????????????excel??????
                            tmpFileList.add(downNetEaseCSVFile(dict.getCode(),startDate,endDate));
                        }
                        allFileList.add(tmpFileList);
                        latch.countDown();
                    });
                });
                //?????????????????????????????????
                try {
                    latch.await();
                    threadPool.shutdown();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                long cEndTime = System.currentTimeMillis();
                log.info("==========================?????????????????????????????????{}???==========================", (cEndTime - cStartTime) / 1000);
                List<String> csvFilePathList = allFileList.stream().flatMap(List::stream).collect(Collectors.toList());
                //????????????
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
     * ????????????csv??????
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

            //????????????
            String csvPath = String.format("%s%s%s.csv", stockConfig.getCsvDownloadDir(), File.separator, code);
            File csvFile = new File(csvPath);
            OutputStream outStream = new FileOutputStream(csvFile);
            IOUtils.copy(response.getEntity().getContent(),outStream);
            log.info("=====================???????????? => {} CSV????????????=====================",code);
            return csvPath;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * ???CSV???????????????????????????
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
                                   //??????csv??????
                                   //??????UTF-8????????????????????????????????????????????????BufferedReader??????????????????
                                   BufferedReader file = new BufferedReader(new InputStreamReader(new FileInputStream(csvPath), "GBK"));
                                   file.readLine(); //????????????????????????

                                   // ?????????????????????????????????records???ArrayList???????????????records???????????????????????????String??????
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
                                           //???????????????
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
                                   // ????????????
                                   file.close();
                               }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            latch.countDown();
                        });
                    });
                    //?????????????????????????????????
                    try {
                        latch.await();
                        threadPool.shutdown();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    long cEndTime = System.currentTimeMillis();
                    log.info("==========================??????CSV???????????????{}???==========================", (cEndTime - cStartTime) / 1000);

                    List<Quotation> newQuotationList = allQuotationList.stream().flatMap(List::stream).collect(Collectors.toList());
                    //??????????????????
                    Map<String,List<Quotation>> dateList = newQuotationList.stream().collect(Collectors.groupingBy(x -> x.getDate().format(ymdFormatter)));
                    dateList.forEach((k,v) -> {
                        quotationMapper.deleteByDate(k);
                    });
                    mybatisBatchHandler.batchInsertOrUpdate(newQuotationList,QuotationMapper.class,GlobalConstant.BATCH_MODDEL_INSERT);
                }
                long endTime = System.currentTimeMillis();
                log.info("==========================??????????????????????????????{}???==========================",(endTime - startTime) / 1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * ?????????????????????????????????
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
        log.info("==========================????????????????????????????????????????????????{}==========================", SpiderUtil.getCurrentTimeStr());
        List<Dict> dbDictList = dictMapper.selectListByType(GlobalConstant.DICT_TYPE_STOCK);

        //???????????????
        Collections.shuffle(dbDictList);

        long cStartTime = System.currentTimeMillis();
        if (dbDictList.size() > 0) {
            //??????????????????????????????
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

            //?????????????????????
            List<List<Dict>> twoDeepList = SpiderUtil.partitionList(dbDictList,GlobalConstant.MAX_THREAD_COUNT);
            ExecutorService threadPool = Executors.newFixedThreadPool(twoDeepList.size());
            log.info("============================???????????????{}???????????????{}?????????????????????,???????????????{}============================",twoDeepList.size(),dbDictList.size(),SpiderUtil.getCurrentTimeStr());
            CountDownLatch latch = new CountDownLatch(twoDeepList.size());
            List<List<AvgPrice>> allAvgPriceList = new ArrayList<>();
            AtomicInteger counter = new AtomicInteger(0);

            //??????mimvp???????????????
            long initMimvpTime = System.currentTimeMillis() / 1000 - 20 * 60 * 1000;
            //??????????????????
            int crewMimvpCount = 2;
            //????????????????????????????????????????????????GlobalConstant.MAX_THREAD_COUNT???????????????
            int fetchMivipCount = 0;
            for (;;) {
                log.info("==========================?????????{}?????????????????????==========================",fetchMivipCount+1);
                proxyIpList = this.crewProxyList(8,2);
                //??????ip?????????????????????
                twoDeepProxyList = SpiderUtil.partitionList(proxyIpList,twoDeepList.size());
                if (twoDeepProxyList.size() >= twoDeepList.size()) {
                    //????????????????????????ip??????????????????ip??????
                    globalProxyIpSet = new HashSet<>();
                    globalProxyIpSet.addAll(proxyIpList.stream().map(ProxyIp::getIp).collect(Collectors.toList()));
                    break;
                }
                fetchMivipCount++;
                log.info("==========================???{}?????????????????????????????????==========================",fetchMivipCount);
                if (fetchMivipCount > 3) {
                    log.error("==========================????????????????????????????????????????????????==========================");
                    return;
                }
            }

            //??????????????????????????????
            AtomicInteger switchCounter = new AtomicInteger(0);
            //??????????????????
            AtomicInteger waitThreadCounter = new AtomicInteger(0);
            Map<String,Integer> banIpMap = new ConcurrentHashMap<>();
            Map<String,Integer> threadNameIndexMap = new HashMap<>();
            //????????????????????????????????????????????????
            List<String> threadNameList = new CopyOnWriteArrayList<>();
            for (int i = 0 ; i < latch.getCount() ; i++) {
                threadNameList.add(String.format("crew-sina-thread-%s",(i+1)));
                threadNameIndexMap.put(threadNameList.get(i),i);
            }
            //??????????????????????????????????????????????????????????????????
            int loopIndex = 0;
            for (List<Dict> innerList : twoDeepList) {
                //???????????????????????????
                try {
                    Thread.sleep(SpiderUtil.getRandomNum(1,20));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                final int index = loopIndex;
                threadPool.execute(() -> {
                    //???????????????
                    String threadName = threadNameList.get(index);
                    Thread.currentThread().setName(threadName);
                    //?????????????????????????????????
                    List<ProxyIp> curProxyList = twoDeepProxyList.get(threadNameIndexMap.get(threadName));

                    List<AvgPrice> curAvgPriceList = new ArrayList<>();
                    AvgPrice avgPrice = null;
                    JSONObject dataObj = null;
                    String key = null;
                    String day = null;

                    //???????????????ip?????????????????????????????????????????????????????????????????????????????????ip??????
                    ProxyIp proxyIp = curProxyList.size() == 1 ? curProxyList.get(0) : SpiderUtil.getRandomEntity(curProxyList);
                    //???????????????httpclient?????????????????????????????????1??????????????????
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
                            //?????????????????????????????????????????????????????????????????????????????????
                            if (curProxyList.size() == 0) {
                                //???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                                counter.getAndAdd(1);
                                //????????????????????????????????????????????????
                                if (counter.get() == latch.getCount()) {
                                    log.info("=======================??????{}??????????????????????????????????????????????????????",Thread.currentThread().getName());
                                    switchCounter.getAndAdd(1);
                                    log.info("==========================??????{}???{}?????????????????????==========================",Thread.currentThread().getName(),switchCounter.get());
                                    //??????????????????????????????????????????????????????????????????????????????????????????????????????????????????ip??????
                                    int fetchCount = 0;
                                    List<ProxyIp> tmpProxyIpList = null;
                                    List<String> curInitProxyIpList = null;
                                    for (;;) {
                                        log.info("=========================??????????????????????????? => {}",proxyIpList.size());
                                        //??????????????????ip????????????????????????ip???
                                        tmpProxyIpList = this.crewProxyList((int)latch.getCount(),1);
                                        curInitProxyIpList = tmpProxyIpList.stream().map(ProxyIp::getIp).collect(Collectors.toList());
                                        //??????????????????????????????????????????ip??????
                                        curInitProxyIpList.removeAll(globalProxyIpSet);
                                        globalProxyIpSet.addAll(curInitProxyIpList);
                                        List<String> curFinalProxyIpList = curInitProxyIpList;
                                        //?????????????????????????????????ip??????
                                        proxyIpList.addAll(tmpProxyIpList.stream().filter(x -> curFinalProxyIpList.contains(x.getIp())).collect(Collectors.toList()));
                                        log.info("=========================??????????????????????????? => {}",proxyIpList.size());
                                        //???????????????????????????ip
                                        log.info("=========================??????????????? => {}",banIpMap.size());
                                        proxyIpList = proxyIpList.stream().filter(x -> !banIpMap.containsKey(x.getIp())).collect(Collectors.toList());
                                        if (proxyIpList.size() >= latch.getCount()) {
                                            break;
                                        }
                                        fetchCount++;
                                        log.info("==========================????????????????????????????????????{}?????????????????????{}???????????????==========================",latch.getCount(),fetchCount);
                                    }

                                    //????????????????????????
                                    twoDeepProxyList = SpiderUtil.partitionList(proxyIpList,(int)latch.getCount());
                                    log.info("====================????????????????????? count => {}",twoDeepProxyList.size());
                                    //?????????????????????????????????
                                    threadNameIndexMap.clear();
                                    for (int i = 0 ; i < latch.getCount() ; i++) {
                                        threadNameIndexMap.put(threadNameList.get(i),i);
                                    }
                                    threadNameIndexMap.forEach((k,v) -> log.info("=============?????????????????? {} => {}",k,v));

                                    //??????????????????????????????
                                    curProxyList.addAll(twoDeepProxyList.get(threadNameIndexMap.get(Thread.currentThread().getName())));
                                    //???????????????????????????????????????????????????????????????????????????????????????
                                    if (latch.getCount() > 1) {
                                        //????????????????????????
                                        counter.getAndSet(0);
                                        //???????????????????????????????????????
                                        for (;;) {
                                            synchronized (lock) {
                                                lock.notifyAll();
                                            }
                                            log.info("=======================??????{}????????????????????????????????????????????????????????????",Thread.currentThread().getName());
                                            break;
                                        }
                                    }
                                } else {
                                    //???????????????????????????????????????????????????????????????????????????????????????
                                    synchronized (lock) {
                                        try {
                                            log.info("=======================??????{}??????IP????????????????????????",Thread.currentThread().getName());
                                            waitThreadCounter.getAndAdd(1);
                                            lock.wait();
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    //????????????????????????????????????
                                    waitThreadCounter.getAndAdd(-1);
                                    log.info("=======================??????{}????????????????????????",Thread.currentThread().getName());
                                    curProxyList.addAll(twoDeepProxyList.get(threadNameIndexMap.get(Thread.currentThread().getName())));
                                }
                            }
                            responseEntity = clientUtil.recursiveHttpRequest(GlobalConstant.HTTP_GET,0,url,null,headerMap,GlobalConstant.CHARASET_GBK);
                            if (responseEntity != null && responseEntity.getCode() == 200 && responseEntity.getContent().contains("open")) {
                                break;
                            } else {
                                if (responseEntity != null && responseEntity.getContent().contains("????????????")) {
                                    log.error("===========================??????{}???????????????????????? => {} ??????IP???{}????????????????????????===========================",Thread.currentThread().getName(),responseEntity.getCode(),proxyIp.getIp());
                                } else {
                                    log.error("==========================??????{}?????????{}:{}??????????????????==========================",Thread.currentThread().getName(),proxyIp.getIp(),proxyIp.getPort());
                                }
                                //??????ip???????????????????????????????????????????????????
                                banIpMap.put(proxyIp.getIp(),0);
                            }
                            //?????????????????????IP
                            curProxyList.remove(proxyIp);
                            if (curProxyList.size() > 0) {
                                rCount++;
                                //????????????
                                proxyIp = curProxyList.get(0);
                                log.info("==========================??????{}???{}??????????????????IP???{}???==========================",Thread.currentThread().getName(),rCount,proxyIp.getIp());
                                clientUtil.setProxyPropArr(new String[]{proxyIp.getIp(),String.valueOf(proxyIp.getPort())});
                                headerMap.put("Cookie",String.format(cookieFormat,proxyIp.getIp(),System.currentTimeMillis(),System.currentTimeMillis()));
                            }
                        }
                        //?????????????????????????????????
                        log.info("======================??????{}??????????????????{} => {}",Thread.currentThread().getName(),dict.getCode(),dict.getName());
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
                    log.info("=======================??????{}??????????????????????????????????????????:{}=======================",Thread.currentThread().getName(),latch.getCount());

                    //??????????????????
                    threadNameList.remove(Thread.currentThread().getName());
                    //?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                    if (threadNameList.size() > 0 && waitThreadCounter.get() == latch.getCount()) {
                        //????????????????????????
                        counter.getAndSet(0);
                        //???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                        if (latch.getCount() == 1) {
                            log.info("=======================????????????????????????????????????????????????????????????????????????????????????=======================");
                            twoDeepProxyList.get(threadNameIndexMap.get(threadNameList.get(0))).addAll(curProxyList);
                        } else {
                            //????????????????????????ip???????????????????????????????????????????????????????????????
                            twoDeepProxyList.get(threadNameIndexMap.get(threadNameList.get(0))).add(proxyIp);
                        }
                        //???????????????????????????
                        for (;;) {
                            log.info("=======================??????{}??????????????????",Thread.currentThread().getName());
                            synchronized (lock) {
                                lock.notifyAll();
                            }
                            log.info("=======================??????{}????????????????????????????????????????????????????????????",Thread.currentThread().getName());
                            break;
                        }
                    }
                });
                loopIndex++;
            }
            //?????????????????????????????????
            try {
                latch.await();
                threadPool.shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            long cEndTime = System.currentTimeMillis();
            log.info("==========================???????????????????????????{}???==========================",(cEndTime - cStartTime) / 1000);

            //???????????????????????????
            File dirFile = new File(stockConfig.getImgDownadDir());
            if (dirFile.exists()) {
                try {
                    SpiderUtil.deleteFile(dirFile);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("====================???????????????????????????====================");
                }
            }

            List<AvgPrice> newAvgPriceList = allAvgPriceList.stream().flatMap(List::stream).collect(Collectors.toList());
            //????????????????????????????????????????????????
            Map<String,List<AvgPrice>> dateAvgPriceMap = newAvgPriceList.stream().collect(Collectors.groupingBy(x -> x.getDate().format(ymdFormatter)));
            dateAvgPriceMap.forEach((k,v) -> {
                avgPriceMapper.deleteByDate(k);
            });
            //????????????????????????
            mybatisBatchHandler.batchInsertOrUpdate(newAvgPriceList,AvgPriceMapper.class,GlobalConstant.BATCH_MODDEL_INSERT);
        }
        long endTime = System.currentTimeMillis();
        log.info("==========================????????????????????????????????????????????????{}==========================", SpiderUtil.getCurrentTimeStr());
        log.info("==========================?????????{}???==========================", (endTime - startTime) / 1000);
    }

    /**
     * ???????????????10????????????
     * @param date
     */
    public void updateLast10Trend(LocalDate date) {
        long startTime = System.currentTimeMillis();
        log.info("==========================??????????????????????????????????????????????????????{}==========================", SpiderUtil.getCurrentTimeStr());
//        if (SpiderUtil.isWeekendOfToday(date)) {
//            log.error("==========================??????????????????????????????????????????==========================");
//            return;
//        }
        DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String curDate = date.format(ymdFormatter);
        //??????????????????????????????
        List<AvgPrice> dbAvgPriceList = avgPriceMapper.selectListByDate(curDate);

        //????????????5???????????????????????????
        int nDay = 10;
        List<AvgPrice> last10AvgPriceList = avgPriceMapper.selectListByRangeOfNDay(curDate,nDay+1);
        //?????????30????????????????????????
        last10AvgPriceList = last10AvgPriceList.stream().filter(x -> x.getAvg30() != null && !curDate.equals(x.getDate().format(ymdFormatter))).collect(Collectors.toList());
        Map<String,List<AvgPrice>> codeAvgPriceMap = last10AvgPriceList.stream().collect(Collectors.groupingBy(AvgPrice::getCode));

        //?????????????????????
        List<List<AvgPrice>> twoDeepList = SpiderUtil.partitionList(dbAvgPriceList,GlobalConstant.MAX_THREAD_COUNT);
        log.info("============================???????????????{}?????????????????????{}??????{}???????????????????????????,???????????????{}============================",twoDeepList.size(),curDate,dbAvgPriceList.size(),SpiderUtil.getCurrentTimeStr());
        if (twoDeepList.size() > 0) {
            ExecutorService threadPool = Executors.newFixedThreadPool(GlobalConstant.MAX_THREAD_COUNT);
            CountDownLatch latch = new CountDownLatch(twoDeepList.size());
            twoDeepList.forEach(innerList -> {
                threadPool.execute(() -> {
                    innerList.forEach(avgPrice -> {
                        try {
                            if (codeAvgPriceMap.containsKey(avgPrice.getCode())) {
                                List<AvgPrice> curAvgPriceList = codeAvgPriceMap.get(avgPrice.getCode());
                                //date??????????????????
                                curAvgPriceList.sort(Comparator.comparing(AvgPrice::getDate));
                                if (curAvgPriceList.size() < nDay) {
                                    avgPrice.setLast10Trend(-1000);
                                    avgPrice.setLast10MonthTrend(-1000);
                                } else {
                                    //????????????????????????????????????
                                    int upDays = 0;
                                    int downDays = 0;
                                    int minLimitDay = (int)(nDay * 0.6);
                                    //???10??????10????????????
                                    //????????????
                                    avgPrice.setLast10Trend(0);
                                    for (int i = 0 ; i < nDay - 1 ; i++) {
                                        //???10??????10????????????
                                        if (curAvgPriceList.get(i+1).getAvg10().compareTo(curAvgPriceList.get(i).getAvg10()) > 0) {
                                            upDays++;
                                        }
                                        if (curAvgPriceList.get(i+1).getAvg10().compareTo(curAvgPriceList.get(i).getAvg10()) < 0) {
                                            downDays++;
                                        }
                                    }
                                    //????????????????????????????????????10???????????????
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

                                    //???10??????30????????????
                                    //????????????
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
                                    //????????????????????????????????????30???????????????
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
            log.info("==========================??????????????????????????????{}==========================", SpiderUtil.getCurrentTimeStr());
            long dbStartTime = System.currentTimeMillis();
            avgPriceMapper.deleteByDate(curDate);
            mybatisBatchHandler.batchInsertOrUpdate(dbAvgPriceList,AvgPriceMapper.class,GlobalConstant.BATCH_MODDEL_INSERT);
            long dbEndTime = System.currentTimeMillis();
            log.info("==========================??????????????????????????????{}==========================", SpiderUtil.getCurrentTimeStr());
            log.info("==========================?????????{}???==========================", (dbEndTime - dbStartTime) / 1000);
        }
        long endTime = System.currentTimeMillis();
        log.info("==========================??????????????????????????????????????????????????????{}==========================", SpiderUtil.getCurrentTimeStr());
        log.info("==========================?????????{}???==========================", (endTime - startTime) / 1000);
    }

    /**
     * ???????????????????????????????????????????????????
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
     * ????????????????????????
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

        //??????????????????
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
            //????????????????????????
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
            //????????????
            proxyIpMapper.deleteAll();
            mybatisBatchHandler.batchInsertOrUpdate(proxyIpList,ProxyIpMapper.class,GlobalConstant.BATCH_MODDEL_INSERT);
        }
        return proxyIpList;
    }

    /**
     * ????????????????????????
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
     * ?????????????????????
     * @param imgUrl
     * @return
     */
    private String downImgToLocal(String imgUrl) {
        try {
            //????????????
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
     * ??????????????????????????????
     */
    public void checkSyncTaskStatus() {
        if (SpiderUtil.isWeekendOfToday(LocalDate.now())) {
            log.info("==========================???????????????==========================");
            return;
        }
        List<SysDict> dictList = sysDictMapper.selectByType(GlobalConstant.SYS_DICT_TYPE_QUALITY_STOCKS);
        if (dictList != null && dictList.size() > 0) {
            //????????????20????????????
            Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY,20);
            c.set(Calendar.MINUTE,0);
            c.set(Calendar.SECOND,0);
            if (dictList.get(0).getCreateDate().getTime() < c.getTimeInMillis()) {
                String warnMsg = "### ?????????????????????\r\n" + "- ??????????????????????????????";
                sendDingDingGroupMsg(warnMsg);
            }
        }
    }

    /**
     * ???????????????????????????????????????
     * 1.??????15?????????????????????1???
     * 2.?????????????????????2???
     * 3.????????????????????????????????????
     * @param date
     */
    public void fetchCuDataList(LocalDate date) {
        DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String strDate = date.format(ymdFormatter);

        //????????????
        List<Dict> dbDictList = dictMapper.selectListByType(GlobalConstant.DICT_TYPE_STOCK);
        Map<String,Dict> codeDictMap = dbDictList.stream().collect(Collectors.toMap(Dict::getCode,Function.identity()));

        //???????????????
        List<SysDict> backlistList = sysDictMapper.selectByType(GlobalConstant.SYS_DICT_TYPE_BACKLIST);
        Map<String,SysDict> codeBackListMap = backlistList.stream().collect(Collectors.toMap(SysDict::getValue,Function.identity(),(o,n) -> n));

        List<Quotation> quotationList = quotationMapper.selectListByDate(strDate);
        Map<String,Quotation> codeQuoMap = quotationList.stream().collect(Collectors.toMap(Quotation::getCode,Function.identity(),(o,n) ->n));

        //?????????????????????????????????????????????
        Map<String,AvgPriceVo> codeAvgMap = new HashMap<>();
        List<AvgPrice> avgPriceList = avgPriceMapper.selectListByDate(strDate);
        if (avgPriceList != null && avgPriceList.size() > 0) {
            avgPriceList.forEach(avg -> {
                AvgPriceVo avgVo = new AvgPriceVo();
                BeanUtils.copyProperties(avg,avgVo);
                //??????????????????????????????
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

        //??????????????????????????????
        List<RelativePositionVo> positionList = quotationMapper.selectCurPosition(strDate);
        Map<String,RelativePositionVo> codePositionMap = positionList.stream().collect(Collectors.toMap(RelativePositionVo::getCode,Function.identity(),(o,n) -> n));

        //?????????????????????
        List<String> inactiveCodeList = quotationMapper.selectInactiveQuoList();

        //??????7???????????????
        List<LastNUpLimitVo> last7DataList = getLastNUpLimitDataList(strDate,7,1);
        Map<String,Integer> codeUpMap = last7DataList.stream().collect(Collectors.toMap(LastNUpLimitVo::getCode,LastNUpLimitVo::getCount,(o,n) -> n));

        //??????5???????????????
        List<LastNUpLimitVo> last5DataList = getLastNUpLimitDataList(strDate,5,1);
        Map<String,Integer> last5CodeUpMap = last5DataList.stream().collect(Collectors.toMap(LastNUpLimitVo::getCode,LastNUpLimitVo::getCount,(o,n) -> n));

        //???????????????5??????5?????????????????????????????????
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
                //????????????5??????????????????????????????sum(high5) <= sum(avg5)
                if (codeAvgSumMap.containsKey(k)) {
                    BigDecimal sum5QuoAmt = v.stream().map(Quotation::getHigh).reduce(BigDecimal.ZERO, BigDecimal::add);
                    if (v.size() < 5 || sum5QuoAmt.compareTo(codeAvgSumMap.get(k)) <= 0) {
                        stifleCodeQuoMap.put(k,sum5QuoAmt);
                    }
                }
            });
        }

        //??????????????????????????????
        Map<String,String> commRemoveCodeMap = new HashMap<>();
        if (dbDictList.size() > 0) {
            String c = null;
            for (Dict dict : dbDictList) {
                c = dict.getCode();
                //?????????ST
                if (dict.getName().toUpperCase().contains("ST")) {
                    commRemoveCodeMap.put(c,dict.getName());
                    continue;
                }
                //??????????????????????????????
                if (c.contains("sz300") || c.contains("sh688")) {
                    commRemoveCodeMap.put(c,dict.getName());
                    continue;
                }
                //?????????????????????30????????????
                if (codeAvgMap.containsKey(c) && codeAvgMap.get(c).getAvg30() == null) {
                    commRemoveCodeMap.put(c,dict.getName());
                    continue;
                }
                //??????????????????
                if (codeBackListMap.containsKey(c)) {
                    commRemoveCodeMap.put(c,dict.getName());
                    continue;
                }
                //??????25-300E
                if (codeDictMap.get(c) == null || codeDictMap.get(c).getCirMarketValue() == null || codeDictMap.get(c).getCirMarketValue().doubleValue() < 20 || codeDictMap.get(c).getCirMarketValue().doubleValue() > 100) {
                    commRemoveCodeMap.put(c,dict.getName());
                    continue;
                }
                //????????????????????????
                if (!codeQuoMap.containsKey(c) || !codeAvgMap.containsKey(c) || !codePositionMap.containsKey(c)) {
                    commRemoveCodeMap.put(c,dict.getName());
                    continue;
                }
                //??????4-200
                if (codeQuoMap.get(c).getCurrent().doubleValue() < 2 || codeQuoMap.get(c).getCurrent().doubleValue() > 80) {
                    commRemoveCodeMap.put(c,dict.getName());
                    continue;
                }
                //???????????????2000w????????????
                if (codeQuoMap.get(c).getVolumeAmt().doubleValue() < 25000000) {
                    commRemoveCodeMap.put(c,dict.getName());
                    continue;
                }
                //????????????????????????
                if (codeAvgMap.get(c).getLast10Trend() == -99 && codeAvgMap.get(c).getLast10MonthTrend() == -99) {
                    commRemoveCodeMap.put(c,dict.getName());
                    continue;
                }
                //?????????????????????????????????????????????
                if (codeQuoMap.get(c).getHigh().doubleValue() <= codeAvgMap.get(c).getAvgMin().doubleValue()) {
                    commRemoveCodeMap.put(c,dict.getName());
                    continue;
                }
                //???????????????????????????????????????
                if (codeAvgMap.get(c).getAvg().doubleValue() <= codeAvgMap.get(c).getAvgMin().doubleValue()) {
                    commRemoveCodeMap.put(c,dict.getName());
                    continue;
                }
                //????????????10????????????????????????
                if (codeAvgMap.get(c).getLast10Trend().doubleValue() == -99d) {
                    commRemoveCodeMap.put(c,dict.getName());
                    continue;
                }
                //???????????????4???????????????
                if (codeQuoMap.get(c).getOffsetRate().doubleValue() < 0 && codeQuoMap.get(c).getOpen().doubleValue() > codeAvgMap.get(c).getAvgMax().doubleValue()
                        && codeQuoMap.get(c).getClose().doubleValue() < codeAvgMap.get(c).getAvgMin().doubleValue()) {
                    commRemoveCodeMap.put(c,dict.getName());
                    continue;
                }
                //????????????10????????????30?????????????????????????????????
                if (codeAvgMap.get(c).getLast10Trend().doubleValue() == 99d && codeAvgMap.get(c).getLast10MonthTrend().doubleValue() == 99d) {
                    commRemoveCodeMap.put(c,dict.getName());
                    continue;
                }
                //????????????????????????,???last10trend > 0 && close > m5 && && m5 > m10 > m20 > m30
                if (codeAvgMap.get(c).getLast10Trend().doubleValue() > 0d
                        && codeQuoMap.get(c).getClose().doubleValue() >= codeAvgMap.get(c).getAvg5().doubleValue()
                        && codeAvgMap.get(c).getAvg5().doubleValue() > codeAvgMap.get(c).getAvg10().doubleValue()
                        && codeAvgMap.get(c).getAvg10().doubleValue() > codeAvgMap.get(c).getAvg20().doubleValue()
                        && codeAvgMap.get(c).getAvg20().doubleValue() > codeAvgMap.get(c).getAvg30().doubleValue()
                ) {
                    commRemoveCodeMap.put(c,dict.getName());
                    continue;
                }
                //????????????10??????????????????
                if (codeAvgMap.get(c).getAvg30().doubleValue() > codeAvgMap.get(c).getAvg20().doubleValue() && codeAvgMap.get(c).getAvg20().doubleValue() > codeAvgMap.get(c).getAvg10().doubleValue()) {
                    commRemoveCodeMap.put(c,dict.getName());
                    continue;
                }
                //???????????????20-80
                if (codePositionMap.get(c).getPRate() < 20 || codePositionMap.get(c).getPRate() > 80) {
                    commRemoveCodeMap.put(c,dict.getName());
                    continue;
                }
                //?????????????????????
                if (inactiveCodeList.contains(c)) {
                    commRemoveCodeMap.put(c,dict.getName());
                }
            }
        }

        List<CuMonitor> hisCuMonitorList = new ArrayList<>();
        //??????2??????????????????
        //????????????10???????????????????????????
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
        //???????????????????????????
        List<Quotation> last5QuoList = quotationMapper.selectLastNDateList(date.format(ymdFormatter),12,GlobalConstant.STOCK_REFER_CODE);
        //?????????5-10???
        last5QuoList = last5QuoList.stream().skip(5).limit(5).collect(Collectors.toList());
        //????????????
        List<String> newCodeList = quotationMapper.selectNewQuoList();
        Set<String> upCodeSet = new HashSet<>();
        String jsonStr = null;
        Map<String,Object> resultMap = null;
        JSONObject jsonObject = null;
        for (Quotation q : last5QuoList) {
            //???????????????
            resultMap = queryLimitUpDownList(q.getDate().format(ymdFormatter),4);
            jsonStr = JSON.toJSONString(resultMap);
            jsonObject = JSONObject.parseObject(jsonStr);
            //??????json
            //??????2??????4??????
            List<CodeNameVo> top2List = jsonObject.getJSONObject("top2").getJSONObject("up").getJSONArray("data").toJavaList(CodeNameVo.class);
            List<CodeNameVo> top4List = jsonObject.getJSONObject("top4").getJSONObject("up").getJSONArray("data").toJavaList(CodeNameVo.class);

            List<String> top2CodeList = top2List.stream().map(CodeNameVo::getCode).collect(Collectors.toList());
            List<String> top4CodeList = top4List.stream().map(CodeNameVo::getCode).collect(Collectors.toList());
            //?????????4?????????
            top2CodeList.removeAll(top4CodeList);
            upCodeSet.addAll(top2CodeList);
        }
        //?????????????????????????????????
        newCodeList.forEach(upCodeSet::remove);

        if (upCodeSet.size() > 0) {
            for (String code : upCodeSet) {
                //??????????????????????????????
                if (commRemoveCodeMap.containsKey(code)) {
                    continue;
                }
                //???????????????????????????20%
                if ((codeLastNMap.get(code).getMaxPrice().doubleValue() - codeQuoMap.get(code).getCurrent().doubleValue()) / codeLastNMap.get(code).getMaxPrice().doubleValue() > 0.2) {
                    continue;
                }
                //???????????????1e????????????
                if (codeQuoMap.get(code).getVolumeAmt().doubleValue() < 100000000) {
                    continue;
                }
                //???????????????5???????????????
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
        log.info("===================?????????????????? => {}",JSON.toJSONString(hisCuMonitorList));

        Map<String,CuMonitor> codeCuMonMap = cuMonitorList.stream().collect(Collectors.toMap(CuMonitor::getCode,Function.identity(),(o,n) ->n));

//        //?????????????????????????????????????????????
//        List<CuMonitor> top2CuMonitorList = new ArrayList<>();
//        List<String> todayUpLimitCodeList = new ArrayList<>();
//        List<String> yestUpLimitCodeList = new ArrayList<>();
//        if (StringUtils.isNotEmpty(limitUpDownList.get(0).getUpList())) {
//            todayUpLimitCodeList.addAll(Arrays.asList(limitUpDownList.get(0).getUpList().split(",")));
//        }
//        if (StringUtils.isNotEmpty(limitUpDownList.get(1).getUpList())) {
//            yestUpLimitCodeList.addAll(Arrays.asList(limitUpDownList.get(1).getUpList().split(",")));
//        }
//        //????????????????????????2?????????
//        todayUpLimitCodeList.retainAll(yestUpLimitCodeList);
//        for (String code : todayUpLimitCodeList) {
//            //??????????????????????????????
//            if (commRemoveCodeMap.containsKey(code)) {
//                continue;
//            }
//            //????????????????????????
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
//        log.debug("===================??????2??????????????? => {}",JSON.toJSONString(top2CuMonitorList));

        //??????????????????????????????
//        codeCuMonMap = cuMonitorList.stream().collect(Collectors.toMap(CuMonitor::getCode,Function.identity(),(o,n) ->n));
//        List<CuMonitor> top1CuMonitorList = new ArrayList<>();
//        if (StringUtils.isNotEmpty(limitUpDownList.get(0).getUpList())) {
//            String []upArr = limitUpDownList.get(0).getUpList().split(",");
//            //??????????????????????????????
//            for (String code : upArr) {
//                //??????????????????????????????
//                if (commRemoveCodeMap.containsKey(code)) {
//                    continue;
//                }
//                //????????????????????????
//                if (codeCuMonMap.containsKey(code)) {
//                    continue;
//                }
//                //7?????????????????????
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
//            log.debug("===================?????????????????? => {}",JSON.toJSONString(top1CuMonitorList));
//        }

        //??????????????????????????????
        codeCuMonMap = cuMonitorList.stream().collect(Collectors.toMap(CuMonitor::getCode,Function.identity(),(o,n) ->n));
        if (dbDictList.size() > 0) {
            for (Dict dict : dbDictList) {
                //??????????????????????????????
                if (commRemoveCodeMap.containsKey(dict.getCode())) {
                    continue;
                }
                //????????????????????????
                if (codeCuMonMap.containsKey(dict.getCode())) {
                    continue;
                }
                //???????????????
                if (dict.getName().toUpperCase().contains("??????")) {
                    commRemoveCodeMap.put(dict.getCode(),dict.getName());
                    continue;
                }
                //7?????????????????????
                if (codeUpMap.containsKey(dict.getCode())) {
                    continue;
                }
                //????????????5????????????????????????
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
            log.info("===================??????????????? => {}",JSON.toJSONString(cuMonitorList));
        }

        //????????????type???????????????
        cuMonitorList.sort(Comparator.comparing(CuMonitor::getType));
        cuMonitorList.forEach(cu -> cu.setId(genIdUtil.nextId()));
        log.debug("===================?????????????????? => {}",JSON.toJSONString(cuMonitorList));

        //????????????
        cuMonitorMapper.deleteAll();
        mybatisBatchHandler.batchInsertOrUpdate(cuMonitorList,CuMonitorMapper.class,GlobalConstant.BATCH_MODDEL_INSERT);
    }

    /**
     * ????????????????????????????????????????????????????????????????????????
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
            //???????????????????????????
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
        log.debug("====================??????{}????????? => {}",dataList.size(),JSON.toJSONString(dataList));
        return dataList;
    }

    /**
     * ????????????????????????????????????????????????????????????????????????
     * @param date
     * @return
     */
    public Map<String,Integer> getWTSSourceCodeTypeMap(LocalDate date) {
        //????????????2????????????????????????
        DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String strDate = date.format(ymdFormatter);
        int days = 2;
        //????????????7????????????,???????????????????????????
        List<Quotation> lastNQuoList = quotationMapper.selectLastNDateList(strDate,days,GlobalConstant.STOCK_REFER_CODE);
        List<String> lastNDateList = lastNQuoList.stream().map(x -> x.getDate().format(ymdFormatter)).sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        String yestDate = lastNDateList.get(1);

        //????????????2??????????????????
        List<Quotation> dbQuotationList = quotationMapper.selectListByRangeOfNDay(strDate,days);
        //??????????????????
        Map<String,List<Quotation>> dateQuotationListMap = dbQuotationList.stream().collect(Collectors.groupingBy(x -> x.getDate().format(ymdFormatter)));
        //??????????????????????????????
        List<Quotation> todayQuoList = dateQuotationListMap.get(strDate);
        List<Quotation> yestQuoList = dateQuotationListMap.get(yestDate);
        //??????code??????????????????
        Map<String,Quotation> todayCodeQuoMap = todayQuoList.stream().collect(Collectors.toMap(Quotation::getCode,Function.identity(),(o,n) ->n));
        Map<String,Quotation> yestCodeQuoMap = yestQuoList.stream().collect(Collectors.toMap(Quotation::getCode,Function.identity(),(o,n) ->n));

        //?????????????????????????????????
        LimitUpDown yestLimitUD = limitUpDownMapper.selectByDate(yestDate);
        List<String> upLimitCodeList = new ArrayList<>(Arrays.asList(yestLimitUD.getUpList().split(",")));

        //??????4?????????????????????
        Map<String,Object> resultMap = queryLimitUpDownList(yestDate,5);
        String jsonStr = JSON.toJSONString(resultMap);
        JSONObject jsonObject = JSONObject.parseObject(jsonStr);
        //??????4????????????????????????
        List<CodeNameVo> top4List = jsonObject.getJSONObject("top4").getJSONObject("up").getJSONArray("data").toJavaList(CodeNameVo.class);
        List<String> top4UpLimitCodeList = top4List.stream().map(CodeNameVo::getCode).collect(Collectors.toList());

        //??????????????????????????????4????????????????????????3???????????????
        if (top4UpLimitCodeList.size() > 0) {
            upLimitCodeList.removeAll(top4UpLimitCodeList);
        }

        //????????????
        Map<String,Integer> wtsCodeTypeMap = new HashMap<>();
        for (String code : upLimitCodeList) {
            //????????????
            if (todayCodeQuoMap.containsKey(code) && yestCodeQuoMap.containsKey(code) && todayCodeQuoMap.get(code).getVolumeAmt().doubleValue() > yestCodeQuoMap.get(code).getVolumeAmt().doubleValue() * 1.2) {
                //?????????????????????2E????????????
                if (todayCodeQuoMap.get(code).getVolumeAmt().doubleValue() < 200000000) {
                    continue;
                }
                //??????????????????????????????
                if (todayCodeQuoMap.get(code).getClose().doubleValue() == todayCodeQuoMap.get(code).getLow().doubleValue()) {
                    continue;
                }
                //??????????????????????????????????????? < ???????????????
                if (todayCodeQuoMap.containsKey(code) && yestCodeQuoMap.containsKey(code) && yestCodeQuoMap.get(code).getHigh().doubleValue() < todayCodeQuoMap.get(code).getLow().doubleValue()) {
                    continue;
                }
                //????????????????????????????????????
                if (todayCodeQuoMap.get(code).getOffsetRate().doubleValue() < -2 || (todayCodeQuoMap.get(code).getClose().doubleValue() > todayCodeQuoMap.get(code).getInit().doubleValue() && todayCodeQuoMap.get(code).getClose().doubleValue() < todayCodeQuoMap.get(code).getOpen().doubleValue())) {
                    wtsCodeTypeMap.put(code,0);
                }
            }
        }

        //?????????????????????????????????
        if (todayQuoList.size() > 0) {
            for (Quotation q : todayQuoList) {
                //????????????????????????
                BigDecimal upLimitVal = q.getInit().multiply(BigDecimal.valueOf(1.1)).setScale(2, RoundingMode.HALF_UP);
                if (q.getHigh().compareTo(upLimitVal) == 0 && q.getClose().compareTo(upLimitVal) < 0) {
                    wtsCodeTypeMap.put(q.getCode(),1);
                }
            }
        }

        log.info("===================??????????????????????????? => {}",JSON.toJSONString(wtsCodeTypeMap));
        return wtsCodeTypeMap;
    }

    /**
     * ???????????????????????????
     */
    public void truncateHisData() {
        log.info("==========================????????????????????????????????????????????????{}==========================", SpiderUtil.getCurrentTimeStr());
        long startTime = System.currentTimeMillis();
        hisQuotationMapper.truncateHisData();
        long endTime = System.currentTimeMillis();
        log.info("==========================????????????????????????????????????????????????{}==========================", SpiderUtil.getCurrentTimeStr());
        log.info("==========================?????????{}???==========================", (endTime - startTime) / 1000);
    }

    /**
     * ???????????????????????????????????????
     */
    public void genQuoIdpDataList() {
        log.info("==========================??????????????????????????????????????????????????????{}==========================", SpiderUtil.getCurrentTimeStr());
        long startTime = System.currentTimeMillis();
        List<QuoIdp> quoIdpList = new ArrayList<>();
        //???????????????????????????????????????
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

        //??????????????????
        List<DictProp> propList = dictPropMapper.selectAll();
        propList = propList.stream().filter(x -> StringUtils.isNotEmpty(x.getIndustry())).collect(Collectors.toList());
        if (propList.size() > 0) {
            propList.forEach(p -> {
                if (StringUtils.isNotEmpty(p.getPlate())) {
                    String []plateArr = p.getPlate().split(",");
                    for (String plate : plateArr) {
                        //??????????????????
                        if (StringUtils.isEmpty(plate) || "????????????".equals(plate) || "?????????".equals(plate) || "???????????????100".equals(plate) || "???????????????A???".equals(plate) || "MSCI??????".equals(plate) || "??????????????????".equals(plate) ) {
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

        //????????????
        quoIdpMapper.deleteAll();
        mybatisBatchHandler.batchInsertOrUpdate(quoIdpList, QuoIdpMapper.class,GlobalConstant.BATCH_MODDEL_INSERT);
        long endTime = System.currentTimeMillis();
        log.info("==========================??????????????????????????????????????????????????????{}==========================", SpiderUtil.getCurrentTimeStr());
        log.info("==========================?????????{}???==========================", (endTime - startTime) / 1000);
    }

    /**
     * ??????????????????????????????????????????
     * @param date
     * @return
     */
    public boolean checkIsRestDay(LocalDate date) {
        //???????????????????????????
        crewStockData(GlobalConstant.CREW_STOCK_REFER,false);
        //?????????????????????????????????????????????????????????????????????????????????
        DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<Quotation> quotationList = quotationMapper.selectLastNDateList(date.format(ymdFormatter),2,GlobalConstant.STOCK_REFER_CODE);
        return quotationList.get(0).getVolumeAmt().doubleValue() == quotationList.get(1).getVolumeAmt().doubleValue();
    }

    /**
     * ???????????????????????????
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
     * ??????????????????
     * @param message
     */
    public void sendDingDingGroupMsg(String message) {
        //????????????
        Map<String,Object> contentMap = new HashMap<>();
        contentMap.put("title", "change list");
        contentMap.put("text", message);
        //?????????markdown
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
                log.info("=========================????????????????????????=========================");
            } else {
                log.info("=========================??????????????????????????????????????????{}=========================",responseEntity.getContent());
            }
        } else {
            log.info("=========================????????????????????????,???????????????{}=========================",responseEntity.getContent());
        }
    }
}
