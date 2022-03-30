/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.stock.backtrace;

import com.alibaba.fastjson.JSON;
import com.td.common.common.GlobalConstant;
import com.td.common.mapper.QuotationMapper;
import com.td.common.model.Quotation;
import com.td.common.util.SpiderUtil;
import com.td.stock.config.StockConfig;
import com.td.stock.vo.BackTraceParamVo;
import com.td.stock.vo.BackTraceVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @auther lotey
 * @date 6/1/20 8:53 PM
 * @desc 回溯基类
 */
@Component
@Slf4j
public abstract class BaseBackTrace {

    @Autowired
    private QuotationMapper quotationMapper;
    @Autowired
    private StockConfig stockConfig;

    /**
     * 获取提取的股票编码列表
     *
     * @param date
     * @param days
     * @return
     */
    public abstract List<BackTraceParamVo> fetchTraceParamList(Date date,int days);

    /**
     * 回溯流程
     * @param date
     * @param days
     * @throws Exception
     */
    public void process(Date date,int days) throws Exception {
        SimpleDateFormat ymdFormat = new SimpleDateFormat("yyyy-MM-dd");
        String strDate = ymdFormat.format(date);
        List<BackTraceParamVo> traceParamVoList = fetchTraceParamList(date,days);
        if (traceParamVoList != null && traceParamVoList.size() > 0) {
            List<List<BackTraceVo>> twoDeepRecordVoList = new ArrayList<>();
            //剔除当前天的，当前天还没有下一天
            traceParamVoList = traceParamVoList.stream().filter(x -> !strDate.equals(x.getDate())).collect(Collectors.toList());
            //启动多线程统计
            List<List<BackTraceParamVo>> twoDeepList = SpiderUtil.partitionList(traceParamVoList, GlobalConstant.MAX_THREAD_COUNT);
            ExecutorService threadPool = Executors.newFixedThreadPool(twoDeepList.size());
            log.info("============================本次共启动{}个线程回溯{}天的股票行情,当前时间：{}============================",twoDeepList.size(),traceParamVoList.stream().collect(Collectors.groupingBy(x -> x.getDate())).size(),SpiderUtil.getCurrentTimeStr());
            CountDownLatch latch = new CountDownLatch(twoDeepList.size());
            int nDay = 4;
            twoDeepList.forEach(innerList -> {
                threadPool.execute(() ->{
                    try {
                        List<BackTraceVo> curRecordVoList = new ArrayList<>();
                        SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd");
                        innerList.forEach(traceVo -> {
                            //查询当前股票当天和下一天的行情数据
                            String curDateStr = traceVo.getDate();

                            List<Quotation> quotationList = quotationMapper.selectSingleAfterNDayList(traceVo.getCode(),curDateStr,nDay);
                            Map<String,Quotation> dateQuotationMap = quotationList.stream().collect(Collectors.toMap(x -> formater.format(x.getDate()), Function.identity(),(o, n) -> n));
                            List<String> dateKeyList = new ArrayList<>(dateQuotationMap.keySet());
                            if (dateKeyList.size() == nDay) {
                                try {
                                    Collections.sort(dateKeyList);
                                    //下个交易日
                                    String nextDate = dateKeyList.get(1);
                                    String next2Date = dateKeyList.get(2);
                                    String next3Date = dateKeyList.get(3);
                                    BackTraceVo recordVo = new BackTraceVo();
                                    recordVo.setDate(curDateStr);
                                    recordVo.setCode(traceVo.getCode());
                                    recordVo.setName(traceVo.getName());
                                    if (!dateQuotationMap.containsKey(curDateStr)) {
                                        return;
                                    }
                                    recordVo.setClosePrice(dateQuotationMap.get(curDateStr).getClose());
                                    recordVo.setNextDayMinPrice(dateQuotationMap.get(nextDate).getLow());
                                    recordVo.setNextDayMaxPrice(dateQuotationMap.get(nextDate).getHigh());
                                    recordVo.setWinRate((dateQuotationMap.get(nextDate).getHigh().subtract(dateQuotationMap.get(curDateStr).getClose()).multiply(new BigDecimal(100)).divide(dateQuotationMap.get(curDateStr).getClose(),2, BigDecimal.ROUND_HALF_UP)));
//                                    recordVo.setLostRate((dateQuotationMap.get(nextDate).getLow().subtract(dateQuotationMap.get(curDateStr).getClose()).multiply(new BigDecimal(100)).divide(dateQuotationMap.get(curDateStr).getClose(),2, BigDecimal.ROUND_HALF_UP)));
                                    recordVo.setLostRate((dateQuotationMap.get(nextDate).getClose().subtract(dateQuotationMap.get(curDateStr).getClose()).multiply(new BigDecimal(100)).divide(dateQuotationMap.get(curDateStr).getClose(),2, BigDecimal.ROUND_HALF_UP)));
                                    recordVo.setDownRate((dateQuotationMap.get(curDateStr).getHigh().subtract(dateQuotationMap.get(curDateStr).getClose())).multiply(new BigDecimal(100)).divide(dateQuotationMap.get(curDateStr).getClose(),2, BigDecimal.ROUND_HALF_UP));

                                    //统计后三日最大涨幅
                                    List<BigDecimal> upPriceList = new ArrayList<>();
                                    upPriceList.add(dateQuotationMap.get(nextDate).getHigh());
                                    upPriceList.add(dateQuotationMap.get(next2Date).getHigh());
                                    upPriceList.add(dateQuotationMap.get(next3Date).getHigh());
                                    upPriceList.sort(Comparator.reverseOrder());
                                    recordVo.setNext3DayMaxWinRate((upPriceList.get(0).subtract(dateQuotationMap.get(curDateStr).getClose())).multiply(new BigDecimal(100)).divide(dateQuotationMap.get(curDateStr).getClose(),2, BigDecimal.ROUND_HALF_UP));
    //
    //                                    //统计后三日最大跌幅
    //                                    List<BigDecimal> downPriceList = new ArrayList<>();
    //                                    downPriceList.add(dateQuotationMap.get(nextDate).getLow());
    //                                    downPriceList.add(dateQuotationMap.get(next2Date).getLow());
    //                                    downPriceList.add(dateQuotationMap.get(next3Date).getLow());
    //                                    downPriceList.sort(Comparator.naturalOrder());
    //                                    recordVo.setNext3DayMaxLostRate((downPriceList.get(0).subtract(dateQuotationMap.get(curDateStr).getClose())).multiply(new BigDecimal(100)).divide(dateQuotationMap.get(curDateStr).getClose(),2, BigDecimal.ROUND_HALF_UP));

                                    curRecordVoList.add(recordVo);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        twoDeepRecordVoList.add(curRecordVoList);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    latch.countDown();
                });
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            threadPool.shutdown();
            List<BackTraceVo> recordVoList = twoDeepRecordVoList.stream().flatMap(List::stream).collect(Collectors.toList());
            log.info("==========================抓取股票数据完成，总条数{}，当前时间：{}==========================", recordVoList.size(), SpiderUtil.getCurrentTimeStr());

            //统计，结果写入文件
            //先创建目录
            File dirFile = new File(stockConfig.getBackTraceLogDir());
            if (!dirFile.exists()) {
                dirFile.mkdir();
            }

            boolean isExistsOldResult = false;
            //先查看是否存在上次的提取结果,如果存在,上次结果重命名为result_code_old.txt
            File newFile = new File(String.format("%s%sresult_code_new.txt", stockConfig.getBackTraceLogDir(), File.separator));
            File oldFile = new File(String.format("%s%sresult_code_old.txt", stockConfig.getBackTraceLogDir(), File.separator));
            if (newFile.exists()) {
                isExistsOldResult = true;
                BufferedWriter bwOld = new BufferedWriter(new FileWriter(oldFile));
                BufferedReader brOld = new BufferedReader(new FileReader(newFile));
                String line = null;
                while ((line = brOld.readLine()) != null) {
                    try {
                        bwOld.write(line+"\r\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                bwOld.flush();
                bwOld.close();
                brOld.close();
            }

            //将本次提取结果写入文件
            BufferedWriter bwNew = new BufferedWriter(new FileWriter(newFile));
            recordVoList.forEach(r -> {
                try {
                    bwNew.write(JSON.toJSONString(r)+"\r\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            bwNew.flush();
            bwNew.close();

            SimpleDateFormat fFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            BufferedWriter bw = new BufferedWriter(new FileWriter(String.format("%s%sstock_back_trace_%s.txt", stockConfig.getBackTraceLogDir(), File.separator,fFormat.format(new Date()))));
            //提取盈利的
            List<BackTraceVo> winRecordVoList = recordVoList.stream().filter(x -> x.getWinRate().doubleValue() > 0).collect(Collectors.toList());
            bw.write("==================盈利列表==================\r\n");
            winRecordVoList.sort(Comparator.comparing(BackTraceVo::getDate).reversed());
            System.out.println("date              code              name              winRate        loseRate");
            bw.write("date              code              name              winRate              loseRate\r\n");
            winRecordVoList.forEach(vo -> {
                System.out.println(String.format("%s              %s              %s              %s              %s",vo.getDate(),vo.getCode(),vo.getName(),vo.getWinRate(),vo.getLostRate()));
                try {
                    bw.write(String.format("%s              %s              %s              %s              %s\r\n",vo.getDate(),vo.getCode(),vo.getName(),vo.getWinRate(),vo.getLostRate()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            log.info("==================华丽的分隔符==================");
            bw.write("\r\n");
            bw.write("\r\n");
            bw.write("\r\n");
            bw.write("==================华丽的分隔符==================");
            bw.write("\r\n");
            bw.write("\r\n");
            bw.write("\r\n");

            //提取亏损的
            List<BackTraceVo> failRecordVoList = recordVoList.stream().filter(x -> x.getWinRate().doubleValue() <= 0).collect(Collectors.toList());
            bw.write("==================亏损列表==================\r\n");
            failRecordVoList.sort(Comparator.comparing(BackTraceVo::getDate));
            System.out.println("date              code              name              loseRate");
            failRecordVoList.forEach(vo -> {
                System.out.println(String.format("%s              %s              %s              %s",vo.getDate(),vo.getCode(),vo.getName(),vo.getLostRate()));
                try {
                    bw.write(String.format("%s              %s              %s              %s\r\n",vo.getDate(),vo.getCode(),vo.getName(),vo.getLostRate()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            //提取盈利率>1%的
            List<BackTraceVo> win1PersentRecordVoList = recordVoList.stream().filter(x -> x.getWinRate().doubleValue() >= 1).collect(Collectors.toList());

            //提取盈利率>2%的
            List<BackTraceVo> win2PersentRecordVoList = recordVoList.stream().filter(x -> x.getWinRate().doubleValue() >= 2).collect(Collectors.toList());

            //提取盈利率>5%的
            List<BackTraceVo> win5PersentRecordVoList = recordVoList.stream().filter(x -> x.getWinRate().doubleValue() >= 5).collect(Collectors.toList());

            //后三天盈利率>5%的
            List<BackTraceVo> nxtt3DayWin5PersentRecordVoList = recordVoList.stream().filter(x -> x.getNext3DayMaxWinRate().doubleValue() >= 3).collect(Collectors.toList());
            List<BackTraceVo> nxtt3DayWin10PersentRecordVoList = recordVoList.stream().filter(x -> x.getNext3DayMaxWinRate().doubleValue() >= 10).collect(Collectors.toList());
            log.info("==================总提取数:{} 盈利数:{} 盈利率为:{}",recordVoList.size(),winRecordVoList.size(),(double)winRecordVoList.size() / recordVoList.size());
            log.info("==================总提取数:{} %1的盈利数:{} 1%的盈利率为:{}",recordVoList.size(),win1PersentRecordVoList.size(),(double)win1PersentRecordVoList.size() / recordVoList.size());
            log.info("==================总提取数:{} %2的盈利数:{} 2%的盈利率为:{}",recordVoList.size(),win2PersentRecordVoList.size(),(double)win2PersentRecordVoList.size() / recordVoList.size());
            log.info("==================总提取数:{} %5的盈利数:{} 5%的盈利率为:{}",recordVoList.size(),win5PersentRecordVoList.size(),(double)win5PersentRecordVoList.size() / recordVoList.size());
            log.info("==================总提取数:{} 后三天3%的盈利数:{} 后三天3%的盈利率为:{}",recordVoList.size(),nxtt3DayWin5PersentRecordVoList.size(),(double)nxtt3DayWin5PersentRecordVoList.size() / recordVoList.size());
            log.info("==================总提取数:{} 后三天10%的盈利数:{} 后三天10%的盈利率为:{}",recordVoList.size(),nxtt3DayWin10PersentRecordVoList.size(),(double)nxtt3DayWin10PersentRecordVoList.size() / recordVoList.size());

            bw.write("\r\n");
            bw.write("\r\n");
            bw.write("\r\n");
            bw.write("==================华丽的分隔符==================");
            bw.write("\r\n");
            bw.write("\r\n");
            bw.write("\r\n");

            bw.write(String.format("==================总提取数:%d 盈利数:%d 盈利率为:%f\r\n",recordVoList.size(),winRecordVoList.size(),(double)winRecordVoList.size() / recordVoList.size()));
            bw.write(String.format("==================总提取数:%d %%2的盈利数:%d 2%%的盈利率为:%f\r\n",recordVoList.size(),win2PersentRecordVoList.size(),(double)win2PersentRecordVoList.size() / recordVoList.size()));
            bw.write(String.format("==================总提取数:%d %%5的盈利数:%d 5%%的盈利率为:%f\r\n",recordVoList.size(),win5PersentRecordVoList.size(),(double)win5PersentRecordVoList.size() / recordVoList.size()));

            bw.flush();
            bw.close();

            //提取出本次和上次的差异部分,方便比对
            if (isExistsOldResult) {
                List<String> oldTextList = Files.readAllLines(Paths.get(String.format("%s%sresult_code_old.txt", stockConfig.getBackTraceLogDir(), File.separator)));
                List<String> newTextList = Files.readAllLines(Paths.get(String.format("%s%sresult_code_new.txt", stockConfig.getBackTraceLogDir(), File.separator)));
                List<BackTraceVo> oldVList = new ArrayList<>();
                if (oldTextList.size() > 0) {
                    oldTextList.forEach(s -> {
                        if (StringUtils.isNotEmpty(s)) {
                            oldVList.add(JSON.parseObject(s,BackTraceVo.class));
                        }
                    });
                }
                List<BackTraceVo> newVList = new ArrayList<>();
                if (newTextList.size() > 0) {
                    newTextList.forEach(s -> {
                        newVList.add(JSON.parseObject(s,BackTraceVo.class));
                    });
                }
                List<String> oldCodeList = oldVList.stream().map(BackTraceVo::getCode).collect(Collectors.toList());
                List<String> newCodeList = newVList.stream().map(BackTraceVo::getCode).collect(Collectors.toList());
                //比较差异
                List<String> copyOldCodeList = SpiderUtil.deepCopyByProtobuff(oldCodeList);
                List<String> copyNewCodeList = SpiderUtil.deepCopyByProtobuff(newCodeList);
                //本次新增的
                newCodeList.removeAll(copyOldCodeList);
                //本次移除的
                oldCodeList.removeAll(copyNewCodeList);
                List<BackTraceVo> curNewRecordVoList = recordVoList.stream().filter(x -> newCodeList.contains(x.getCode())).collect(Collectors.toList());
                System.out.println("=====================本次新增列表=====================");
                System.out.println("date              code              name              winRate        loseRate");
                curNewRecordVoList.forEach(vo -> {
                    System.out.println(String.format("%s              %s              %s              %s              %s",vo.getDate(),vo.getCode(),vo.getName(),vo.getWinRate(),vo.getLostRate()));
                });
                List<BackTraceVo> curRemovedRecordVoList = oldVList.stream().filter(x -> oldCodeList.contains(x.getCode())).collect(Collectors.toList());
                System.out.println("=====================本次移除列表=====================");
                System.out.println("date              code              name              winRate        loseRate");
                curRemovedRecordVoList.forEach(vo -> {
                    System.out.println(String.format("%s              %s              %s              %s              %s",vo.getDate(),vo.getCode(),vo.getName(),vo.getWinRate(),vo.getLostRate()));
                });
            }
        }
    }
}
