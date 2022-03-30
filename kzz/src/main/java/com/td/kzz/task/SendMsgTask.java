/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.kzz.task;

import com.td.common.common.GlobalConstant;
import com.td.common.model.Dict;
import com.td.common.model.KzzQuotation;
import com.td.common.model.SysDict;
import com.td.kzz.service.StockService;
import com.td.kzz.vo.CodeNameVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    @Autowired
    private StockService stockService;

    //记录上一次行情列表
    private List<KzzQuotation> lastQuotationList = null;
    //记录连续5次都上涨的股票代码
    private List<Set<String>> rencent5UpCodeList = new LinkedList<>();
    //记录连续5次都下跌的股票代码
    private List<Set<String>> rencent5DownCodeList = new LinkedList<>();
    //标记轮训次数
    private int pushIndex = -1;

    /**
     * 根据当天监控数据的变化，发送钉钉通知消息
     * @param isInitMonitorData
     * @param qualityStockList
     */
    public void sendDDMsg(boolean isInitMonitorData, List<SysDict> qualityStockList) {
        Date date = new Date();
        SimpleDateFormat ymdFormat = new SimpleDateFormat("yyyy-MM-dd");
        String strDate = ymdFormat.format(date);

        //查询当前行情列表
        List<KzzQuotation> quotationList = stockService.selectListByDate(strDate);

        if (isInitMonitorData) {
            log.info("=======================推送数据初始化开始=======================");
            //初始化行情数据
            lastQuotationList = quotationList;
            //初始化推送轮训次数
            pushIndex = -1;
            log.info("=======================推送数据初始化结束=======================");
        } else {
            //提取本次快速上涨和快速下跌的数据，并发送钉钉消息
            //先获取两次的交集，再比对，以本次抓取的最新数据为准
            //创建映射，并比对结果
            Map<String, KzzQuotation> curCodeQuotationMap = quotationList.stream().collect(Collectors.toMap(KzzQuotation::getCode, Function.identity()));
            Map<String, KzzQuotation> lastCodeQuotationMap = lastQuotationList.stream().collect(Collectors.toMap(KzzQuotation::getCode, Function.identity()));

            //过滤出code列表，再取交集
            List<String> curCodeList = quotationList.stream().map(KzzQuotation::getCode).collect(Collectors.toList());
            List<String> lastCodeList = lastQuotationList.stream().map(KzzQuotation::getCode).collect(Collectors.toList());
            //取交集
            curCodeList.retainAll(lastCodeList);
            //映射出交集
            List<KzzQuotation> unionQuotationList = curCodeList.stream().map(curCodeQuotationMap::get).collect(Collectors.toList());
            //快速上涨 ==> 涨幅 >= 2%
            //快速下跌 ==> 跌幅 >= 2%
            List<CodeNameVo> fastUpList = new ArrayList<>();
            List<CodeNameVo> fastDownList = new ArrayList<>();
            Set<String> upCodeSet = new HashSet<>();
            Set<String> downCodeSet = new HashSet<>();
            for (KzzQuotation q : unionQuotationList) {
                Double initPrice = q.getOpen().doubleValue();
                Double curPrice = q.getCurrent().doubleValue();
                Double lastPrice = lastCodeQuotationMap.get(q.getCode()).getCurrent().doubleValue();
                if (curPrice / initPrice - lastPrice / initPrice >= GlobalConstant.MIN_CHANGE_LIMIT_PRICE) {
                    fastUpList.add(new CodeNameVo(q.getCode(), q.getName()));
                    //添加到监控列表
                    upCodeSet.add(q.getCode());
                } else if (lastPrice / initPrice - curPrice / initPrice >= GlobalConstant.MIN_CHANGE_LIMIT_PRICE) {
                    fastDownList.add(new CodeNameVo(q.getCode(), q.getName()));
                    //添加到监控列表
                    downCodeSet.add(q.getCode());
                }
            }

            //将本次上涨的数据添加进队列
            Set<String> continuedUpCodeSet = new HashSet<>();
            //先将当前股票上涨的股票编码加入连续列表
            rencent5UpCodeList.add(upCodeSet);
            if (rencent5UpCodeList.size() == 6) {
                //查看5次中同一个股票出现3次上涨，则视为连续上涨，加入推送列表，重点关注
                Map<String, Integer> codeUpCountMap = new HashMap<>();
                rencent5UpCodeList.forEach(codeSet -> {
                    codeSet.forEach(code -> {
                        if (codeUpCountMap.containsKey(code)) {
                            codeUpCountMap.put(code, codeUpCountMap.get(code) + 1);
                            //过滤出出现3次的股票，加入推荐列表
                            if (codeUpCountMap.get(code) >= 3) {
                                continuedUpCodeSet.add(code);
                            }
                        } else {
                            codeUpCountMap.put(code, 1);
                        }
                    });
                });
                //移除最前面一次加入的数据，当前的数据加入监控，第一次一定是空，故需要移除2个
                rencent5UpCodeList.remove(0);
            }

            //将本次下跌的数据添加进队列
            Set<String> continuedDownCodeSet = new HashSet<>();
            //先将当前股票上涨的股票编码加入连续列表
            rencent5DownCodeList.add(downCodeSet);
            if (rencent5DownCodeList.size() == 6) {
                //查看5次中同一个股票出现3次上涨，则视为连续上涨，加入推送列表，重点关注
                Map<String, Integer> codeDownCountMap = new HashMap<>();
                rencent5DownCodeList.forEach(codeSet -> {
                    codeSet.forEach(code -> {
                        if (codeDownCountMap.containsKey(code)) {
                            codeDownCountMap.put(code, codeDownCountMap.get(code) + 1);
                            //过滤出出现3次的股票，加入推荐列表
                            if (codeDownCountMap.get(code) >= 3) {
                                continuedDownCodeSet.add(code);
                            }
                        } else {
                            codeDownCountMap.put(code, 1);
                        }
                    });
                });
                //移除最前面一次加入的数据，当前的数据加入监控，第一次一定是空，故需要移除2个
                rencent5DownCodeList.remove(0);
            }

            //映射code -> dict
            List<Dict> dictList = stockService.selectDictListByType(GlobalConstant.DICT_TYPE_KZZ);
            Map<String, Dict> codeDictMap = dictList.stream().collect(Collectors.toMap(Dict::getCode, Function.identity()));

            //统计连续上涨列表，并推送
            List<CodeNameVo> continuedUpList = new ArrayList<>();
            if (continuedUpCodeSet.size() > 0) {
                for (String x : continuedUpCodeSet) {
                    if (codeDictMap.containsKey(x)) {
                        continuedUpList.add(new CodeNameVo(x, codeDictMap.get(x).getName()));
                    }
                }
            }
            //统计连续下跌列表，并推送
            List<CodeNameVo> continuedDownList = new ArrayList<>();
            if (continuedDownCodeSet.size() > 0) {
                for (String x : continuedDownCodeSet) {
                    if (codeDictMap.containsKey(x)) {
                        continuedDownList.add(new CodeNameVo(x, codeDictMap.get(x).getName()));
                    }
                }
            }

            //构造最终推送字符串
            StringBuffer sendBuff = new StringBuffer();
            //只监控价值股，其他的过滤掉
            List<String> qualityStockCodeList = qualityStockList.stream().map(SysDict::getValue).collect(Collectors.toList());
            fastUpList = fastUpList.stream().filter(x -> qualityStockCodeList.contains(x.getCode())).collect(Collectors.toList());
            fastDownList = fastDownList.stream().filter(x -> qualityStockCodeList.contains(x.getCode())).collect(Collectors.toList());

            if (fastUpList.size() > 0) {
                sendBuff.append("### 快速上涨列表:\r\n");
                fastUpList.forEach(x -> {
                    sendBuff.append(String.format("- %s -> %s \r\n", x.getCode(), x.getName()));
                });
            }
            if (fastDownList.size() > 0) {
                sendBuff.append("### 快速下跌列表:\r\n");
                fastDownList.forEach(x -> {
                    sendBuff.append(String.format("- %s -> %s\r\n", x.getCode(), x.getName()));
                });
            }
            sendBuff.append("===============华丽的分隔符===============\r\n");
            //重点关注，连续上涨列表
            StringBuilder conUpBuilder = new StringBuilder();
            continuedUpList = continuedUpList.stream().filter(x -> qualityStockCodeList.contains(x.getCode())).collect(Collectors.toList());
            if (continuedUpList.size() > 0) {
                sendBuff.append("### ****************【连续拉升，重点关注】****************\r\n");
                sendBuff.append("#### 连续拉升列表:\r\n");
                conUpBuilder.append("#### 连续拉升列表:\r\n");
                continuedUpList.forEach(x -> {
                    sendBuff.append(String.format("- %s -> %s \r\n", x.getCode(), x.getName()));
                    conUpBuilder.append(String.format("- %s -> %s \r\n", x.getCode(), x.getName()));
                });
                sendBuff.append("****************【连续拉升，重点关注】****************\r\n");
            }
            //重点关注，连续下跌列表
            StringBuilder conDownBuilder = new StringBuilder();
            continuedDownList = continuedDownList.stream().filter(x -> qualityStockCodeList.contains(x.getCode())).collect(Collectors.toList());
            if (continuedDownList.size() > 0) {
                sendBuff.append("### ****************【连续下跌，重点关注】****************\r\n");
                sendBuff.append("#### 连续下跌列表:\r\n");
                conDownBuilder.append("#### 连续下跌列表:\r\n");
                continuedDownList.forEach(x -> {
                    sendBuff.append(String.format("- %s -> %s \r\n", x.getCode(), x.getName()));
                    conDownBuilder.append(String.format("- %s -> %s \r\n", x.getCode(), x.getName()));
                });
                sendBuff.append("****************【连续下跌，重点关注】****************\r\n");
            }

            log.info("===============推送消息开始===============\r\n");
            log.info(sendBuff.toString());
            ExecutorService pool = Executors.newFixedThreadPool(1);
            pool.execute(() -> {
                //钉钉发送连续上涨列表
                String conUpMsg = conUpBuilder.toString();
                if (StringUtils.isNotEmpty(conUpMsg)) {
                    stockService.sendDingDingGroupMsg(conUpMsg);
                }
                //钉钉发送连续下跌列表
                String conDownMsg = conDownBuilder.toString();
                if (StringUtils.isNotEmpty(conDownMsg)) {
                    stockService.sendDingDingGroupMsg(conDownMsg);
                }
            });
            pool.shutdown();

            log.info("===============推送消息结束===============");
            //推送轮训次数累计
            pushIndex++;
            //将当前最新的数据赋值给监控数据
            lastQuotationList = quotationList;
        }
    }
}
