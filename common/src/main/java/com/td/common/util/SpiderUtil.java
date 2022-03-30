/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.common.util;

import com.google.common.collect.Lists;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @auther lotey
 * @date 2019/5/5 17:53
 * @desc 工具类
 */
public class SpiderUtil {
    /**
     * Map转String工具
     * @param map
     * @param separator 分隔符
     * @param kvSplice  键值拼接符
     * @return
     */
    public static String mapToString(Map<?, ?> map, String separator, String kvSplice) {
        StringBuffer sbf = new StringBuffer();
        map.forEach((k,v) -> {
            sbf.append(k).append(kvSplice).append(v).append(separator);
        });
        String result = sbf.toString();
        result = result.substring(0,result.lastIndexOf(separator));
        return result;
    }

    /**
     * 获取URL参数的MAP
     * @return
     */
    public static Map<String,Object> getUrlParamMap(String url) {
        Map<String,Object> resultMap = new HashMap<>();
        url = url.substring(url.indexOf("?") + 1);
        //用&截取再=截取
        String []paramArr = url.split("&");
        Arrays.stream(paramArr).forEach(x -> {
            if (String.format("%s=",x.split("=")[0]).equals(x)) {
                resultMap.put(x.split("=")[0].trim(), "");
            } else {
                resultMap.put(x.split("=")[0].trim(), x.split("=")[1].trim());
            }
        });
        return resultMap;
    }

    /***
     * 获取指定范围的随机数
     * @param min
     * @param max
     * @return
     */
    public static int getRandomNum(int min, int max){
        return new Random().nextInt(max) % (max - min + 1) + min;
    }

    /**
     * 从列表中获取随机的实体
     * @param sourceList
     * @param <T>
     * @return
     */
    public static <T> T getRandomEntity(List<T> sourceList){
        return sourceList.get(getRandomNum(0,sourceList.size()));
    }

    /**
     * 比较两个list差异部分
     * @param allOpenidList
     * @param dbOpenidList
     * @return
     */
    public static List<String> compareListDiff(List<String> allOpenidList, List<String> dbOpenidList) {
        if (dbOpenidList != null && !dbOpenidList.isEmpty()) {
            Map<String, String> dataMap = new HashMap<>();
            for (String id : dbOpenidList) {
                dataMap.put(id, id);
            }

            List<String> newList = new ArrayList<>();
            for (String id : allOpenidList) {
                if (!dataMap.containsKey(id)) {
                    newList.add(id);
                }
            }
            return newList;
        } else {
            return allOpenidList;
        }
    }

    /**
     * 根据线程数分配二维列表，内部使用guava实现
     * @param sourceList
     * @param threadCount
     * @param <T>
     * @return
     */
    public static <T> List<List<T>> partitionList(List<T> sourceList, int threadCount) {
        List<List<T>> twoDeepList = new ArrayList<>();
        if  (sourceList != null && sourceList.size() > 0) {
            //待分配的列表数量<线程数，多少个列表分成多少个线程
            if (sourceList.size() < threadCount) {
                sourceList.forEach(t -> {
                    twoDeepList.add(new ArrayList<T>(){{
                        add(t);
                    }});
                });
                return twoDeepList;
            }

            //先去除不能分配部分，并按照最小的分
            int matchSize = sourceList.size() / threadCount * threadCount;
            //够分配列表
            List<T> minList = sourceList.stream().limit(matchSize).collect(Collectors.toList());
            //先按照可以分配的列表分配
            List<List<T>> partitionList = Lists.partition(minList,sourceList.size() / threadCount);
            //guava分配的列表不能直接使用，需要重新转换成正常的list
            for (List<T> innerList : partitionList) {
                twoDeepList.add(new ArrayList<>(innerList));
            }
            //剩余部分均分到列表
            List<T> restList = sourceList.stream().skip(matchSize).collect(Collectors.toList());
            //如果有剩余列表，再追加到后面
            if (restList.size() > 0) {
                for (int i = 0 ; i < restList.size() ; i++) {
                    twoDeepList.get(i).add(restList.get(i));
                }
            }
        }
        return twoDeepList;
    }

    /**
     * 将一纬List转成二纬
     * //TODO 此算法有bug,18分7时有问题
     * @param sourceList
     * @param threadCount
     * @param <T>
     * @return
     */
    public static <T> List<List<T>> makeListToTwoDeep(List<T> sourceList, int threadCount) {
        List<List<T>> targetList = new ArrayList<>();
        List<T> innerList = new ArrayList<>();
        if (sourceList.isEmpty()) {
            return targetList;
        } else {
            if (sourceList.size() <= threadCount) {
                for (int i = 0 ; i < sourceList.size() ; i ++) {
                    innerList.add(sourceList.get(i));
                    targetList.add(innerList);
                    innerList = new ArrayList<>();
                }
            } else {
                int avgCount = sourceList.size() / threadCount;
                int mod = sourceList.size() % threadCount;
                /**
                 * 根据是否能除尽，分2种情况
                 * a:可以均分
                 *   avgCount * threadCount = sourceList.size()，直接均分
                 * b.不可均分，根据avgCount和mod大小对比，又分2种情况
                 *   b1:avgCount > mod，此时前N-1个分配sourceList.size() / threadCount个，最后一个分配剩余部分
                 *   b2:avgCount <= mod，此时前N-1个分配sourceList.size() / threadCount + 1，最后一个分配剩余部分
                 */
                if (avgCount * threadCount == sourceList.size()) {
                    //全部均分
                    for (int i = 0 ; i < sourceList.size() ; i ++) {
                        innerList.add(sourceList.get(i));
                        //第一次放行直接添加,倒数第二次匹配需放行，最后一次需添加
                        if (i == avgCount - 1 || (i > avgCount - 1 && i % avgCount == avgCount - 1) || (i == sourceList.size() - 1)) {
                            targetList.add(innerList);
                            innerList = new ArrayList<>();
                        }
                    }
                } else {//不等时，差异在于avgCount取值
                    if (avgCount > 1 && mod > avgCount) {
                        avgCount = sourceList.size() / threadCount + 1;
                    } else {
                        avgCount = sourceList.size() / threadCount;
                    }
                    for (int i = 0 ; i < sourceList.size() ; i ++) {
                        innerList.add(sourceList.get(i));
                        //第一次放行直接添加,倒数第二次匹配需放行，最后一次需添加
                        if (i == avgCount - 1 || (i > avgCount - 1 && i % avgCount == avgCount - 1 && i < avgCount * (threadCount - 1)) || (i == sourceList.size() - 1)) {
                            targetList.add(innerList);
                            innerList = new ArrayList<>();
                        }
                    }
                }
            }
        }
        return targetList;
    }

    /**
     * 使用JDK序列化的方式对List进行深拷贝
     * T必须实现序列化接口
     * @param src
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> deepCopyByJDK(List<T> src) {
        List<T> dest = null;
        try {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(byteOut);
            out.writeObject(src);

            ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
            ObjectInputStream in = new ObjectInputStream(byteIn);
            dest = (List<T>) in.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dest;
    }

    /**
     * 使用protobuff序列化方式进行深拷贝对象
     * T不需要实现序列化接口
     * @param obj
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T deepCopyByProtobuff(T obj){
        T cloneObj = null;
        byte[] clonebytes = ProtostuffUtils.serialize(obj);
        cloneObj = (T)ProtostuffUtils.deserialize(clonebytes,obj.getClass());
        return cloneObj;
    }

    /**
     * 判断今天是否周末
     * @return
     */
    public static boolean isWeekendOfToday(LocalDate date) {
        DayOfWeek week = date.getDayOfWeek();
        return week == DayOfWeek.SATURDAY || week == DayOfWeek.SUNDAY;
    }

    /**
     * 判断今天是否周末
     * @return
     */
    public static DayOfWeek getWeekDayOfToday(LocalDate date) {
        return date.getDayOfWeek();
    }

    /**
     * 获取当前时间字符串
     * @return
     */
    public static String getCurrentTimeStr() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    /**
     * 递归删除目录
     *
     * @param file
     */
    public static void deleteFile(File file) {
        if (!file.isFile()) {
            String[] childFilePaths = file.list();//得到当前的路径
            for (String childFilePath : childFilePaths) {
                File childFile = new File(file.getAbsolutePath() + File.separator + childFilePath);
                deleteFile(childFile);
            }
        }
        file.delete();
    }

    /**
     * 计算最大回撤
     *
     * @param dataList
     * @return
     */
    public static double getMaxDrawdown(List<Double> dataList) {
        double maxSoFar = dataList.get(0);
        double drawdown;
        double maxDrawdown = 0;
        for (int i = 0 ; i < dataList.size() ; i++) {
            if (dataList.get(i) > maxSoFar) {
                drawdown = 0;
                maxSoFar = dataList.get(i);
            } else {
                drawdown = maxSoFar - dataList.get(i);
            }
            if (drawdown > maxDrawdown) {
                maxDrawdown = drawdown;
            }
        }
        return maxDrawdown;
    }

    public static void main(String[] args) {
        int maxNum = 100;
        IntStream.range(0,maxNum).forEach(x -> System.out.print(getRandomNum(0,4) + "\t"));

        Map<String, Object> map = new HashMap<>();
        map.put("name", "hjz");
        map.put("value", 25);
        System.out.println(SpiderUtil.mapToString(map, "&", "="));

        int max = 223;
        int count = 8;

        List<Integer> list = new ArrayList<>();
        for (int i = 0 ;i < max ;i++) {
            list.add(i);
        }
        System.out.println("元素个数："+max+"  线程数："+count);
        List<List<Integer>> targetList = makeListToTwoDeep(list,count);
        for (int i = 0;i < targetList.size();i++) {
            System.out.print("第"+(i+1)+"组：");
            for (int j = 0;j < targetList.get(i).size();j++) {
                System.out.print(targetList.get(i).get(j) + "\t");
            }
            System.out.println();
        }

        Double []nums = {6.920,6.920,6.900,6.900,6.900,6.900,6.900,6.900,6.900,6.880,6.900,6.880,6.880,6.880,6.900,6.860,6.860,6.860,6.860,6.850,6.850,6.850,6.850,6.860,6.860,6.860,6.800,6.830,6.830,6.800,6.800,6.810,6.810,6.810,6.830,6.830,6.830,6.850,6.860,6.850,6.850,6.860,6.820,6.820,6.820,6.840,6.830,6.830,6.830,6.830,6.820,6.830,6.830,6.790,6.800,6.790,6.790,6.800,6.780,6.770,6.770,6.800,6.810,6.800,6.800,6.820,6.830,6.760,6.760,6.840,6.830,6.810,6.810,6.800,6.800,6.820,6.820,6.820,6.820,6.840,6.840,6.830,6.840,6.840,6.840,6.840,6.860,6.860,6.860,6.880,6.880,6.880,6.880,6.900,6.900,6.950,6.950,6.910,6.940,6.910,6.910,6.910,6.910,6.910,6.910,6.910,6.910,6.900,6.910,6.910,6.910,6.910,6.920,6.920,6.920,6.950,6.950,6.950,6.970,7.020,7.050,7.060,7.060,7.090,7.100,7.110,7.110,7.120,7.120,7.070,7.070,7.070,7.120,7.110,7.110,7.120,7.100,7.100,7.100,7.100,7.110,7.100,7.100,7.080,7.090,7.080,7.080,7.080,7.080,7.050,7.070,7.070,7.030,7.020,7.030,7.030,7.030,7.010,7.040,7.040,7.040,7.030,7.020,7.020,7.050,7.040,7.070,7.070,7.050,7.070,7.070,7.070,7.070,7.070,7.070,7.070,7.080,7.080,7.080,7.080,7.080,7.080,7.080,7.080,7.070,7.070,7.080,7.080,7.070,7.080,7.080,7.080,7.080,7.080,7.100,7.110,7.110,7.120,7.120,7.120,7.130,7.130,7.150,7.150,7.120,7.120,7.150,7.120,7.140,7.140,7.120,7.120,7.140,7.140,7.120,7.110,7.120,7.120,7.120,7.110,7.110,7.110,7.110,7.100,7.110,7.110,7.120,7.150,7.130,7.140,7.140,7.150,7.150,7.160,7.160,7.160,7.170,7.160,7.160,7.170,7.170,7.170,7.170,7.160,7.150,7.150,7.150,7.150,7.140,7.130,7.130,7.130,7.140,7.130,7.130,7.120,7.140,7.130,7.120,7.120,7.150,7.150,7.180,7.180,7.160,7.170,7.160,7.160,7.160,7.190,7.180,7.180,7.180,7.180,7.200,7.200,7.210,7.220,7.210,7.210,7.210,7.220,7.220,7.220,7.220,7.220,7.220,7.240,7.240,7.220,7.240,7.230,7.230};
        List<Double> dataList = new LinkedList<>(Arrays.asList(nums));
        double maxDrawdown = getMaxDrawdown(dataList);
        System.out.println("最大回撤 => " + maxDrawdown);
    }
}