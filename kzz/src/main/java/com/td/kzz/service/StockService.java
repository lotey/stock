/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.kzz.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.td.common.common.GlobalConstant;
import com.td.common.common.MybatisBatchHandler;
import com.td.common.common.ResponseEntity;
import com.td.common.mapper.DictMapper;
import com.td.common.mapper.KzzQuotationMapper;
import com.td.common.mapper.SysDictMapper;
import com.td.common.model.Dict;
import com.td.common.model.KzzQuotation;
import com.td.common.model.SysDict;
import com.td.common.util.HttpClientUtil;
import com.td.common.util.SnowflakeGenIdUtil;
import com.td.common.util.SpiderUtil;
import com.td.kzz.config.DingdingConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
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
    private SysDictMapper sysDictMapper;
    @Autowired
    private KzzQuotationMapper kzzQuotationMapper;
    @Autowired
    private MybatisBatchHandler mybatisBatchHandler;
    @Autowired
    private DingdingConfig dingdingConfig;

    /**
     * 抓取可转债股票字典列表
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
                //110000, 113999
                List<String> shCodeRangeList = IntStream.rangeClosed(110000,113999).mapToObj(x -> String.format("%s%s","sh",x)).collect(Collectors.toList());
                //调用google算法分配
                List<List<String>> groupCodeList =  Lists.partition(shCodeRangeList, batchSize);
                List<Dict> tmpDictList = new ArrayList<>();
                groupCodeList.forEach(codeList -> {
                    String url = String.format(GlobalConstant.SINA_STOCK_QUOTATION_URL, String.join(",",codeList));
                    ResponseEntity responseEntity = clientUtil.recursiveHttpRequest(GlobalConstant.HTTP_GET, 0, url, null, GlobalConstant.DEFAULTHEADERMAP,GlobalConstant.CHARASET_GBK);
                    if (responseEntity != null && responseEntity.getCode() == 200 && responseEntity.getContent().contains("转债")) {
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

        //抓取深圳创业板的股票
        threadPool.execute(() -> {
            try {
                long szStartTime = System.currentTimeMillis();
                log.info("============================开始抓取深圳股票数据，当前时间：{}============================",SpiderUtil.getCurrentTimeStr());
                //123000, 123999
                List<String> shCodeRangeList = IntStream.rangeClosed(123000,123999).mapToObj(x -> String.format("%s%s","sz",x)).collect(Collectors.toList());
                //调用google算法分配
                List<List<String>> groupCodeList =  Lists.partition(shCodeRangeList, batchSize);
                List<Dict> tmpDictList = new ArrayList<>();
                groupCodeList.forEach(codeList -> {
                    String url = String.format(GlobalConstant.SINA_STOCK_QUOTATION_URL, String.join(",",codeList));
                    ResponseEntity responseEntity = clientUtil.recursiveHttpRequest(GlobalConstant.HTTP_GET, 0, url, null, GlobalConstant.DEFAULTHEADERMAP,GlobalConstant.CHARASET_GBK);
                    if (responseEntity != null && responseEntity.getCode() == 200 && responseEntity.getContent().contains("转债")) {
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

        //抓取深圳中小板和主板的股票
        threadPool.execute(() -> {
            try {
                long szStartTime = System.currentTimeMillis();
                log.info("============================开始抓取深圳股票数据，当前时间：{}============================",SpiderUtil.getCurrentTimeStr());
                //128000, 128999
                List<String> shCodeRangeList = IntStream.rangeClosed(128000,128999).mapToObj(x -> String.format("%s%s","sz",x)).collect(Collectors.toList());
                //调用google算法分配
                List<List<String>> groupCodeList =  Lists.partition(shCodeRangeList, batchSize);
                List<Dict> tmpDictList = new ArrayList<>();
                groupCodeList.forEach(codeList -> {
                    String url = String.format(GlobalConstant.SINA_STOCK_QUOTATION_URL, String.join(",",codeList));
                    ResponseEntity responseEntity = clientUtil.recursiveHttpRequest(GlobalConstant.HTTP_GET, 0, url, null, GlobalConstant.DEFAULTHEADERMAP,GlobalConstant.CHARASET_GBK);
                    if (responseEntity != null && responseEntity.getCode() == 200 && responseEntity.getContent().contains("转债")) {
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
        dictMapper.deleteByType(GlobalConstant.DICT_TYPE_KZZ);
        //批量更新股票行情数据
        dictList.forEach(x -> {
            x.setId(genIdUtil.nextId());
            x.setCirMarketValue(BigDecimal.valueOf(-1));
            x.setType(GlobalConstant.DICT_TYPE_KZZ);
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
     * 抓取股票数据，并提取入库
     */
    public void crewStockData() {
        long startTime = System.currentTimeMillis();
        log.info("==========================开始抓取股票数据，当前时间：{}==========================", SpiderUtil.getCurrentTimeStr());
        Date today = new Date();
//        if (SpiderUtil.isWeekendOfToday(today)) {
//            log.error("==========================周末时间不开盘，忽略本次任务==========================");
//            return;
//        }
        SimpleDateFormat ymdFormat = new SimpleDateFormat("yyyy-MM-dd");
        String todayStr = ymdFormat.format(today);
        //只抓取关注的小盘的列表
        List<Dict> dictList = dictMapper.selectQualityKzzList();
        //单次批量抓取数量
        int batchSize = 50;
        //启动多线程抓取
        List<List<Dict>> twoDeepList = SpiderUtil.partitionList(dictList,GlobalConstant.MAX_THREAD_COUNT);
        ExecutorService threadPool = Executors.newFixedThreadPool(twoDeepList.size());
        log.info("============================本次共启动{}个线程，抓取{}个股票数据,当前时间：{}============================",twoDeepList.size(),dictList.size(),SpiderUtil.getCurrentTimeStr());
        if (twoDeepList.size() > 0) {
            List<List<KzzQuotation>> crewResultList = new ArrayList<>();
            CountDownLatch latch = new CountDownLatch(twoDeepList.size());
            twoDeepList.forEach(innerList -> {
                threadPool.execute(() -> {
                    //调用google算法分配
                    List<List<Dict>> groupList =  Lists.partition(innerList, batchSize);
                    List<String> crewCodeList = groupList.stream().map(x -> x.stream().map(Dict::getCode).collect(Collectors.joining(","))).collect(Collectors.toList());
                    if (crewCodeList.size() > 0) {
                        List<KzzQuotation> quotationList = new ArrayList<>();
                        crewCodeList.forEach(codeList -> {
                            String url = String.format(GlobalConstant.SINA_STOCK_QUOTATION_URL,codeList);
                            ResponseEntity responseEntity = clientUtil.recursiveHttpRequest(GlobalConstant.HTTP_GET,0,url,null,GlobalConstant.DEFAULTHEADERMAP,GlobalConstant.CHARASET_GBK);
                            if (responseEntity.getCode() == 200) {
                                //根据换行符分组，提取值
                                String []dataArr = responseEntity.getContent().split("\n");
                                if (dataArr.length > 0) {
                                    KzzQuotation quotation = null;
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
                                        quotation = new KzzQuotation();
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

                                        quotationList.add(quotation);
                                    }
                                }
                            }
                        });
                        crewResultList.add(quotationList);
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
            List<KzzQuotation> quotationList = crewResultList.stream().flatMap(List::stream).collect(Collectors.toList());
            log.info("==========================抓取股票数据完成，总条数{} 开始入库，当前时间：{}==========================", quotationList.size(), SpiderUtil.getCurrentTimeStr());
            long startDBTime = System.currentTimeMillis();
            //全部重新写入
            kzzQuotationMapper.deleteByDate(todayStr);
            //批量添加股票行情数据
            mybatisBatchHandler.batchInsertOrUpdate(quotationList, KzzQuotationMapper.class, GlobalConstant.BATCH_MODDEL_INSERT);
            long endTime = System.currentTimeMillis();
            log.info("==========================股票行情数据入库完成，当前时间：{} DB入库共耗时{}秒==========================", SpiderUtil.getCurrentTimeStr(),(endTime - startDBTime) / 1000);
            log.info("==========================共耗时{}秒==========================", (endTime - startTime) / 1000);
        }
    }

    /**
     * 更新可转债剩余规模
     */
    public void updateCirMarketValue() {
        String url = String.format(GlobalConstant.KZZ_JISILU_ISS_URL,System.currentTimeMillis());
        HttpClientUtil clientUtil = new HttpClientUtil();
        clientUtil.setMaxRetryCount(1);

        //请求头
        Map<String,String> headerMap = new HashMap<>();
        headerMap.put("Accept","application/json, text/javascript, */*; q=0.01");
        headerMap.put("Accept-Encoding","gzip, deflate, br");
        headerMap.put("Accept-Language","zh-CN,zh;q=0.9");
        headerMap.put("Connection","keep-alive");
        headerMap.put("Content-Type","application/x-www-form-urlencoded; charset=UTF-8");
        headerMap.put("Cookie","kbzw__Session=tsp9bgpkq09pcl990hbvel8qo6; Hm_lvt_164fe01b1433a19b507595a43bf58262=1604230590; kbz_newcookie=1; Hm_lpvt_164fe01b1433a19b507595a43bf58262=1604234106");
        headerMap.put("Host","www.jisilu.cn");
        headerMap.put("Origin","https://www.jisilu.cn");
        headerMap.put("Referer","https://www.jisilu.cn/data/cbnew/");
        headerMap.put("Sec-Fetch-Dest","empty");
        headerMap.put("Sec-Fetch-Mode","cors");
        headerMap.put("Sec-Fetch-Site","same-origin");
        headerMap.put("User-Agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36");
        headerMap.put("X-Requested-With","XMLHttpRequestFÏ");

        //参数
        Map<String,Object> paramMap = new HashMap<>();
        paramMap.put("fprice","110");
        paramMap.put("tprice","400");
        paramMap.put("curr_iss_amt","5.5");
        paramMap.put("volume","");
        paramMap.put("svolume","");
        paramMap.put("premium_rt","200");
        paramMap.put("ytm_rt","");
        paramMap.put("rating_cd","");
        paramMap.put("is_search","Y");
        paramMap.put("btype","C");
        paramMap.put("listed","Y");
        paramMap.put("sw_cd","");
        paramMap.put("bond_ids","");
        paramMap.put("rp","50");
        ResponseEntity responseEntity = clientUtil.recursiveHttpRequest(GlobalConstant.HTTP_POST, 0, url, paramMap, headerMap,GlobalConstant.CHARASET_UTF8);
        if (responseEntity != null && responseEntity.getCode() == 200) {
            //查询全部可转债，更新小盘股的余额
            List<Dict> dictList = dictMapper.selectListByType(GlobalConstant.DICT_TYPE_KZZ);
            Map<String,Dict> codeDictMap = dictList.stream().collect(Collectors.toMap(Dict::getCode, Function.identity(),(o,n) ->n));
            JSONObject jsonObject = JSON.parseObject(responseEntity.getContent());
            JSONArray dataArr = jsonObject.getJSONArray("rows");
            JSONObject dataObject = null;
            String key = null;
            String keyPrefix = null;
            for (Object obj : dataArr) {
                dataObject = (JSONObject) obj;
                if (dataObject.getString("id").startsWith("11")) {
                    keyPrefix = "sh";
                } else {
                    keyPrefix = "sz";
                }
                key = String.format("%s%s",keyPrefix,dataObject.getString("id"));
                if (codeDictMap.containsKey(key)) {
                    codeDictMap.get(key).setCirMarketValue(dataObject.getJSONObject("cell").getBigDecimal("curr_iss_amt"));
                }
            }
            //清空全部股票数据
            dictMapper.deleteByType(GlobalConstant.DICT_TYPE_KZZ);
            //批量更新股票行情数据
            mybatisBatchHandler.batchInsertOrUpdate(dictList,DictMapper.class,GlobalConstant.BATCH_MODDEL_INSERT);
        }
        log.info("======================更新可转债余额结束======================");
    }

    /**
     * 提取优质可转债
     * 模拟实现，先全部加入优质列表，后期根据需求优化
     */
    public void fetchQualityStockList() {
        log.info("==========================开始提取优质股任务，当前时间：{}==========================", SpiderUtil.getCurrentTimeStr());
        long startTime = System.currentTimeMillis();

        //根据可转债剩余金额过滤
        List<Dict> dictList = dictMapper.selectByLeftAmt(BigDecimal.valueOf(1),BigDecimal.valueOf(5.5));

        //过滤掉黑名单的
        List<SysDict> backlistList = sysDictMapper.selectByType(GlobalConstant.SYS_DICT_TYPE_BACKLIST);
        Map<String,SysDict> codeBackListMap = backlistList.stream().collect(Collectors.toMap(SysDict::getValue,Function.identity(),(o,n) -> n));
        dictList = dictList.stream().filter(x -> !codeBackListMap.containsKey(x.getCode())).collect(Collectors.toList());

        //先清空优质股字典列表
        sysDictMapper.deleteByType(GlobalConstant.SYS_DICT_TYPE_QUALITY_KZZ);
        List<SysDict> sysDictList = new ArrayList<>();
        SysDict sysDict = null;
        for (Dict dict : dictList) {
            sysDict = new SysDict();
            sysDict.setId(genIdUtil.nextId());
            sysDict.setLabel(dict.getName());
            sysDict.setValue(dict.getCode());
            sysDict.setType(GlobalConstant.SYS_DICT_TYPE_QUALITY_KZZ);
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
        paramMap.put(HttpClientUtil.STRBODY, JSON.toJSONString(pMap));

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

    /**
     * 查询某天的行情列表
     * @param date
     * @return
     */
    public List<KzzQuotation> selectListByDate(String date) {
        return kzzQuotationMapper.selectListByDate(date);
    }

    /**
     * 根据股票类型查询股票字典列表
     * @return
     */
    public List<Dict> selectDictListByType(int type) {
        return dictMapper.selectListByType(type);
    }

    /**
     * 查询全部的字典配置列表
     * @return
     */
    public List<SysDict> selectSysDictByType(String type) {
        return sysDictMapper.selectByType(type);
    }
}
