package com.td.stock;

import com.td.common.util.SpiderUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @auther lotey
 * @date 2019/7/30 20:53
 * @desc 功能描述
 */
public class StockTest {

    public static void main(String []args) {
//        System.out.println(String.format("%6d", 500).replace(" ", "0"));

        Date date = new Date();
        Calendar calendar= Calendar.getInstance();
//        calendar.setTime(date);
//        calendar.add(Calendar.DAY_OF_MONTH,0);
//        Date minDate = calendar.getTime();
//        SimpleDateFormat ymdFormat = new SimpleDateFormat("yyyy-MM-dd");
//        System.out.println(ymdFormat.format(minDate));
//
//        System.out.println(date.getTime());
//        System.out.println(calendar.getTimeInMillis());

        System.out.println(calendar.getTimeInMillis());
        calendar.set(Calendar.HOUR_OF_DAY,9);
        calendar.set(Calendar.MINUTE,40);
        calendar.set(Calendar.SECOND,0);
        System.out.println(calendar.getTimeInMillis());

        Set<String> set = new HashSet<>();
        set.add("a");
        set.add("b");
        set.add("c");
        set.add("d");

        Set<String> copySet = SpiderUtil.deepCopyByProtobuff(set);
        copySet.remove("a");

        String s = "sh600000";
        System.out.println(s.substring(2));
        System.out.println(set);

        List<Integer> numList = new ArrayList<>();
        int init = 1;
        int max = 25;
        while (init <= max) {
            numList.add(init++);
        }
        List<List<Integer>> targetList = SpiderUtil.partitionList(numList,8);
        System.out.println(targetList);
    }
}
