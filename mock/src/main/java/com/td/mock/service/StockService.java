/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.mock.service;

import com.td.common.common.GlobalConstant;
import com.td.common.common.MybatisBatchHandler;
import com.td.common.mapper.CuMonitorMapper;
import com.td.common.mapper.HisQuotationMapper;
import com.td.common.mapper.ProxyIpMapper;
import com.td.common.mapper.QuotationDetailMapper;
import com.td.common.model.CuMonitor;
import com.td.common.model.HisQuotation;
import com.td.common.model.QuotationDetail;
import com.td.common.util.HttpClientUtil;
import com.td.common.util.SnowflakeGenIdUtil;
import com.td.common.util.SpiderUtil;
import com.td.mock.config.ProxyConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

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
    private ProxyConfig proxyConfig;
    @Autowired
    private MybatisBatchHandler mybatisBatchHandler;
    @Autowired
    private QuotationDetailMapper quotationDetailMapper;
    @Autowired
    private HisQuotationMapper hisQuotationMapper;
    @Autowired
    private CuMonitorMapper cuMonitorMapper;
    @Autowired
    private ProxyIpMapper proxyIpMapper;

    //日期列表
    private List<String> dateList = null;

    /**
     * 初始化全部日期列表
     */
    @PostConstruct
    public void initDateList() {
        dateList = quotationDetailMapper.selectAviDateList();
    }

    /**
     * 构造连续涨停数据
     */
    public void makeDataList() {
        String testCode = "sh603113";
        List<QuotationDetail> detailList = quotationDetailMapper.selectListByCode(testCode);
        String strDataFormat = "金能科技,11.360,11.410,%s,11.370,10.830,10.850,10.860,11864401,130770018.000,5490,10.850,35800,10.840,54700,10.830,27800,10.820,33500,10.810,7300,10.860,13900,10.870,13500,10.880,3300,10.890,4900,10.900,2019-06-03,15:00:00,00";
        double initPrice = 11.50d;
        for (int i = 0 ; i < detailList.size() ; i++) {
            detailList.get(i).setData4(String.format(strDataFormat,initPrice * (i + 1)));
        }
        quotationDetailMapper.deleteByCode(testCode);
        mybatisBatchHandler.batchInsertOrUpdate(detailList, QuotationDetailMapper.class,GlobalConstant.BATCH_MODDEL_INSERT);
    }

    private double initPrice = 6.810;
    private int initIndex = 0;

    /**
     * 获取模拟连续上涨股票
     * @param num
     * @param isRestart
     * @return
     */
    public String getMockDataList(int num,int isRestart) {
        if (isRestart == 1) {
            initPrice = 6.2;
        }
        StringBuilder builder = new StringBuilder();
        String strData1Format = "var hq_str_sz000639=\"西王食品,6.200,6.190,%s,6.810,6.150,6.810,0.000,37495012,247030628.470,6943578,6.810,14300,6.800,11900,6.790,7800,6.780,30200,6.770,0,0.000,0,0.000,0,0.000,0,0.000,0,0.000,2020-12-30,15:00:03,00\";";
        String strData2Format = "var hq_str_sz002167=\"东方锆业,5.630,5.650,%s,6.220,5.620,6.220,0.000,35986982,217762142.930,2180698,6.220,22600,6.210,86500,6.200,15000,6.190,25400,6.180,0,0.000,0,0.000,0,0.000,0,0.000,0,0.000,2020-12-30,15:00:03,00\";";
        builder.append(String.format(strData1Format,initPrice + (++initIndex * 0.05)));
        builder.append("\r\n");
        builder.append(String.format(strData2Format,6.220 + (SpiderUtil.getRandomNum(0,5) * 0.05)));
        return builder.toString();
    }

    /**
     * 获取模拟的行情数据列表
     * @param codeStrList
     * @param count
     * @return
     */
    public String getMockDataList(String codeStrList,int count) {
        log.info("============================第{}次抓取模拟行情数据============================",count);
        String strDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
//        strDate = "2021-01-22";
        //解析要抓取的股票编码
        String codes = Arrays.stream(codeStrList.split(",")).map(x -> String.format("'%s'",x)).collect(Collectors.joining(","));
        List<HisQuotation> quotationList = hisQuotationMapper.selectListByDateAndCodes(strDate,count,codes);
        StringBuilder sbBuilder = new StringBuilder();
        quotationList.forEach(line -> {
            sbBuilder.append(line.getSourceData()).append("\n");
        });
        return sbBuilder.toString();
    }

    /**
     * 设置请求次数
     * @param date
     */
    public void setCrewCount(String date) {
        long cStartTime = System.currentTimeMillis();
        List<CuMonitor> dbCuMonitorList = cuMonitorMapper.selectNewestList();
        if (!dbCuMonitorList.isEmpty()) {
            //启动多线程设置
            List<List<CuMonitor>> twoDeepList = SpiderUtil.partitionList(dbCuMonitorList,GlobalConstant.MAX_THREAD_COUNT);
            ExecutorService threadPool = Executors.newFixedThreadPool(twoDeepList.size());
            log.info("============================本次共启动{}个线程设置{}个股票次数数据,当前时间：{}============================",twoDeepList.size(),dbCuMonitorList.size(),SpiderUtil.getCurrentTimeStr());
            CountDownLatch latch = new CountDownLatch(twoDeepList.size());
            List<List<HisQuotation>> newCuMonitorList = new ArrayList<>();
            twoDeepList.forEach(innerList -> {
                threadPool.execute(() -> {
                    //随机休眠，方便协作
                    try {
                        Thread.sleep(SpiderUtil.getRandomNum(1,200));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    try {
                        List<HisQuotation> curHisQuotationList = new ArrayList<>();
                        int cCount = 0;
                        for (CuMonitor cuMonitor : innerList) {
                            List<HisQuotation> dataList = hisQuotationMapper.selectListByDateAndCode(date,cuMonitor.getCode());
                            log.info("============================线程{}查询{}历史行情数据完成============================",Thread.currentThread().getName(),cuMonitor.getCode());
                            for (int i = 0 ; i < dataList.size() ; i++) {
                                dataList.get(i).setCount(i + 1);
                            }
                            //直接存储入库
                            hisQuotationMapper.deleteByDateAndCode(date,cuMonitor.getCode());
                            mybatisBatchHandler.batchInsertOrUpdate(dataList, HisQuotationMapper.class,GlobalConstant.BATCH_MODDEL_INSERT);
                            log.info("============================线程{}的处理{}完成，共处理{}个股票数据============================",Thread.currentThread().getName(),cuMonitor.getCode(),++cCount);
                        }
                        newCuMonitorList.add(curHisQuotationList);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                    log.info("============================线程{}完成任务============================",Thread.currentThread().getName());
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
            log.info("==========================设置次数耗时：{}秒==========================",(cEndTime - cStartTime) / 1000);

//            //先删除当天之前统计的涨停跌停数据
//            hisQuotationMapper.deleteAll();
//            List<HisQuotation> newDataList = newCuMonitorList.stream().filter(x -> x != null && x.size() > 0).flatMap(List::stream).collect(Collectors.toList());
//            mybatisBatchHandler.batchInsertOrUpdate(newDataList, HisQuotationMapper.class,GlobalConstant.BATCH_MODDEL_INSERT);
            long fEndTime = System.currentTimeMillis();
            log.info("==========================总耗时：{}秒==========================",(fEndTime - cStartTime) / 1000);
        }

        List<HisQuotation> dataList = hisQuotationMapper.selectListByDate(date);
        Map<String,List<HisQuotation>> codeQuoListMap = dataList.stream().collect(Collectors.groupingBy(HisQuotation::getCode));
        List<HisQuotation> newDataList = new ArrayList<>();
        codeQuoListMap.forEach((k,v) -> {
            v.sort(Comparator.comparing(HisQuotation::getCreateTime));
            for (int i = 0 ; i < v.size() ; i++) {
                v.get(i).setCount(i + 1);
                newDataList.add(v.get(i));
            }
        });
        //重新设置次数
        hisQuotationMapper.deleteAll();
        mybatisBatchHandler.batchInsertOrUpdate(newDataList, HisQuotationMapper.class,GlobalConstant.BATCH_MODDEL_INSERT);
    }

    /**
     * 清空代理列表
     */
    public void clearProxyList() {
        proxyIpMapper.deleteAll();
    }

//    /**
//     * 抓取免费的米扑代理
//     * @param count
//     */
//    public List<ProxyEntity> crewMimvpFreeProxyList(int count) {
//        Map<String,String> headerMap = new HashMap<>();
//        headerMap.put("Host","proxy.mimvp.com");
//        headerMap.put("Connection","keep-alive");
//        headerMap.put("Pragma","no-cache");
//        headerMap.put("Cache-Control","no-cache");
//        headerMap.put("Upgrade-Insecure-Requests","1");
//        headerMap.put("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.70 Safari/537.36");
//        headerMap.put("Sec-Fetch-User","?1");
//        headerMap.put("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
//        headerMap.put("Sec-Fetch-Site","none");
//        headerMap.put("Sec-Fetch-Mode","navigate");
//        headerMap.put("Accept-Encoding","gzip, deflate, br");
//        headerMap.put("Accept-Language","zh-CN,zh;q=0.9");
//        headerMap.put("Cookie","Hm_lvt_2470f08b0a4e8514a3d12a641ddcb46d=1566062072; PHPSESSID=bte6259ou8hrbkgre4mtp2vajl; Hm_lvt_51e3cc975b346e7705d8c255164036b3=1578219350,1578219363,1578219647,1578219947; Hm_lpvt_51e3cc975b346e7705d8c255164036b3=1578220407");
//
//        PortNumUtil portNumUtil = null;
//        try {
//            portNumUtil = new PortNumUtil();
//        } catch (MWException e) {
//            e.printStackTrace();
//        }
//        PortNumUtil fPortNumUtil = portNumUtil;
//
//        List<ProxyEntity> proxyList = new ArrayList<>();
//        ResponseEntity responseEntity = null;
//        for (int i = 0 ; i < count ; i++) {
//            responseEntity = clientUtil.recursiveHttpRequest(GlobalConstant.HTTP_GET,0, String.format(GlobalConstant.MIMVP_PROXY_URL,Math.random()),null,headerMap);
//            if (responseEntity.getCode() == 200 && StringUtils.isNotEmpty(responseEntity.getContent())) {
//                Document doc = Jsoup.parse(responseEntity.getContent());
//                doc.select("tr:gt(0)").forEach(x -> {
//                    if (x.select("td:eq(7)").attr("title").startsWith("0") && x.select("td:eq(8)").attr("title").startsWith("0")) {
//                        String localImgPath = downImgToLocal(String.format("%s%s","https://proxy.mimvp.com/", x.select("td:eq(2) > img").attr("src")));
//                        if (StringUtils.isNotEmpty(localImgPath)) {
//                            Object []port = null;
//                            try {
//                                if (fPortNumUtil == null) {
//                                    log.error("========================验证码识别工具类初始化异常========================");
//                                    return;
//                                }
//                                port = fPortNumUtil.getPort(1,localImgPath);
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                            }
//                            if (port != null) {
//                                MWNumericArray arr = (MWNumericArray)port[0];
//                                List<Integer> list = Arrays.stream( arr.getIntData() ).boxed().collect(Collectors.toList());
//                                String portNum = list.stream().map(Objects::toString).collect(Collectors.joining());
//                                //构建对象
//                                ProxyEntity entity = new ProxyEntity();
//                                entity.setIp(x.select("td:eq(1)").text());
//                                entity.setType(x.select("td:eq(3)").attr("title").toUpperCase());
//                                entity.setPort(Integer.parseInt(portNum));
//                                proxyList.add(entity);
//                            }
//                        }
//                    }
//                });
//            }
//        }
//        return proxyList;
//    }

    /**
     * 下载图片到本地
     *
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
            File dirFile = new File(proxyConfig.getImgDownadDir());
            if (!dirFile.exists()) {
                dirFile.mkdir();
            }
            String imgPath = String.format("%s%s%s.png", proxyConfig.getImgDownadDir(), File.separator, UUID.randomUUID().toString());
            File imgFile = new File(imgPath);
            ImageIO.write(bi, "png", imgFile);
            return imgPath;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
