/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.stock.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MimvpOCRUtil {

    public final static Integer[] ZERO = {0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	1,	1,	0,	0,	0,	1,	0,	0,	1,	0,	1,	0,	0,	0,	0,	1,	1,	0,	0,	0,	0,	1,	1,	0,	0,	0,	0,	1,	1,	0,	0,	0,	0,	1,	1,	0,	0,	0,	0,	1,	1,	0,	0,	0,	0,	1,	0,	1,	0,	0,	1,	0,	0,	0,	1,	1,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0};
    public final static Integer[] ONE = {0,	0,	0,	0,	0,	0,  0,	0,	0,	0,	0,  0,	0,	0,	0,	0,	0,  0,	0,	0,	0,	0,	0,  0,	0,	0,	0,	0,	0,  0,	0,	0,	0,	0,	0,	0,  0,	0,	0,	0,	0,  0,	0,	0,	0,	0,	0,  0,	0,	0,	1,	0,	0,  0,	0,	1,	1,	0,	0,  0,	1,	0,	1,	0,	0,  0,	0,	0,	1,	0,	0,  0,	0,	0,	1,	0,	0,  0,	0,	0,	1,	0,	0,  0,	0,	0,	1,	0,	0,  0,	0,	0,	1,	0,	0,  0,	0,	0,	1,	0,	0,  0,	1,	1,	1,	1,	1,  0,	0,	0,	0,	0,	0,  0,	0,	0,	0,	0,	0,  0,	0,	0,	0,	0,	0,  0,	0,	0,	0,	0,	0,  0,	0,	0,	0,	0,	0,  0,	0,	0,	0,	0,	0,  0,	0,	0,	0,	0,	0,  0};
    public final static Integer[] TWO = {0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	1,	1,	1,	1,	0,	1,	0,	0,	0,	0,	1,	1,	0,	0,	0,	0,	1,	0,	0,	0,	0,	0,	1,	0,	0,	0,	1,	1,	0,	0,	0,	1,	0,	0,	0,	0,	1,	0,	0,	0,	0,	1,	0,	0,	0,	0,	0,	1,	0,	0,	0,	0,	0,	1,	1,	1,	1,	1,	1,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0};
    public final static Integer[] THREE = {0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	1,	1,	1,	1,	0,	1,	0,	0,	0,	0,	1,	1,	0,	0,	0,	0,	1,	0,	0,	0,	0,	0,	1,	0,	0,	1,	1,	1,	0,	0,	0,	0,	0,	0,	1,	0,	0,	0,	0,	0,	1,	1,	0,	0,	0,	0,	1,	1,	0,	0,	0,	0,	1,	0,	1,	1,	1,	1,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0};
    public final static Integer[] FOUR = {0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	1,	0,	0,	0,	0,	1,	1,	0,	0,	0,	1,	0,	1,	0,	0,	1,	0,	0,	1,	0,	1,	0,	0,	0,	1,	0,	1,	0,	0,	0,	1,	0,	1,	1,	1,	1,	1,	1,	0,	0,	0,	0,	1,	0,	0,	0,	0,	0,	1,	0,	0,	0,	0,	0,	1,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0};
    public final static Integer[] FIVE = {0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	1,	1,	1,	1,	1,	1,	1,	0,	0,	0,	0,	0,	1,	0,	0,	0,	0,	0,	1,	0,	0,	0,	0,	0,	1,	1,	1,	1,	1,	0,	0,	0,	0,	0,	0,	1,	0,	0,	0,	0,	0,	1,	0,	0,	0,	0,	0,	1,	1,	0,	0,	0,	0,	1,	0,	1,	1,	1,	1,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0};
    public final static Integer[] SIX = {0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	1,	1,	1,	0,	0,	1,	0,	0,	0,	0,	1,	0,	0,	0,	0,	0,	1,	0,	0,	0,	0,	0,	1,	1,	1,	1,	1,	0,	1,	0,	0,	0,	0,	1,	1,	0,	0,	0,	0,	1,	1,	0,	0,	0,	0,	1,	1,	0,	0,	0,	0,	1,	0,	1,	1,	1,	1,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0};
    public final static Integer[] SEVEN = {0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	1,	1,	1,	1,	1,	1,	0,	0,	0,	0,	0,	1,	0,	0,	0,	0,	0,	1,	0,	0,	0,	0,	1,	0,	0,	0,	0,	0,	1,	0,	0,	0,	0,	0,	1,	0,	0,	0,	0,	1,	0,	0,	0,	0,	0,	1,	0,	0,	0,	0,	0,	1,	0,	0,	0,	0,	0,	1,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0};
    public final static Integer[] EIGHT = {0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	1,	1,	1,	1,	0,	1,	0,	0,	0,	0,	1,	1,	0,	0,	0,	0,	1,	1,	0,	0,	0,	0,	1,	0,	1,	1,	1,	1,	0,	1,	0,	0,	0,	0,	1,	1,	0,	0,	0,	0,	1,	1,	0,	0,	0,	0,	1,	1,	0,	0,	0,	0,	1,	0,	1,	1,	1,	1,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0};
    public final static Integer[] NINTH = {0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	1,	1,	1,	1,	0,	1,	0,	0,	0,	0,	1,	1,	0,	0,	0,	0,	1,	1,	0,	0,	0,	0,	1,	0,	1,	1,	1,	1,	1,	0,	0,	0,	0,	0,	1,	0,	0,	0,	0,	0,	1,	0,	0,	0,	0,	0,	1,	0,	0,	0,	0,	1,	0,	0,	1,	1,	1,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0,	0};

    public final static Integer[][] NUM = {ZERO, ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINTH};

    public static String ocrText(String imgPath) throws IOException {
        /**
         * 灰度值矩阵
         */
        File file= new File(imgPath);
        BufferedImage bi = ImageIO.read(file);
        int height = bi.getHeight();
        int width = bi.getWidth();
        Integer[][] arr = new Integer[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                String hexString = Integer.toHexString(bi.getRGB(i, j));
                if("ff000000".equals(hexString)){
                    arr[i][j] = 1;
                }else {
                    arr[i][j] = 0;
                }
            }
        }
        /**
         * 数字个数
         * 有效列位置
         */
        boolean status = false;
        int numTotal = 0;
        List<Integer> numLoc = new ArrayList<>();
        for (int i = 0; i < width; i++){
            Integer[] temp = arr[i];
            int sumRow = (int) Arrays.stream(temp).mapToDouble(x -> (double)x).sum();
            if(!status){
                if(sumRow != 0){
                    status = true;
                    numTotal++;
                    numLoc.add(i);
                }
            }else {
                if(sumRow == 0){
                    status = false;
                    numTotal++;
                    numLoc.add(i-1);
                }
            }
        }
        numTotal = numTotal/2;
        /**
         * 向量化
         */
        List<List<Integer>> lineNum = new ArrayList<>();
        for (int i = 0; i < numLoc.size(); i+=2){
            Integer start = numLoc.get(i);
            Integer end = numLoc.get(i+1);
            Integer[][] temp = new Integer[6][height];

            //数字"1"的特殊处理
            if((end -start) == 4){
                temp = Arrays.copyOfRange(arr, start, end+2);  // 包前不包后;
            }else {
                temp = Arrays.copyOfRange(arr, start, end+1);  // 包前不包后;
            }
            //reshape 重排列
            temp = transposition(temp);
            List<Integer> tempList = new ArrayList<>();
            for (Integer[] rowArr : temp){
                tempList.addAll(Arrays.asList(rowArr));
            }
            lineNum.add(tempList);
        }
        /**
         * 匹配
         */
        Integer inf = 99999;
        StringBuilder port = new StringBuilder();
        Integer[] figure = new Integer[numTotal];
        for (int i = 0; i < lineNum.size(); i++){
            inf = 99999;
            for(int j = 0; j < NUM.length; j++){
                Integer dist = distance(lineNum.get(i), Arrays.asList(NUM[j]));
                if (dist < inf){
                    inf = dist;
                    figure[i] = j;
                }
            }
        }
        for (int i = 0; i < figure.length; i++){
            port.append(figure[i].toString());
        }
        return port.toString();
    }

    /**
     * 数组(矩阵)转置
     * @param arr
     * @return
     */
    public static Integer[][] transposition(Integer[][] arr){
        int row = arr.length;
        int line = arr[0].length;
        Integer [][] MatrixC = new Integer[line][row];
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < line; j++) {
                MatrixC[j][i] = arr[i][j] ;
            }
        }
        return MatrixC ;
    }

    /**
     * 明翰距离
     * @return
     */
    public static Integer distance(List<Integer> list1, List<Integer> list2){
        int sum = 0;
        int len = list1.size();
        for (int i = 0; i< len; i++){
            sum += Math.abs(list1.get(i) - list2.get(i));
        }
        return sum;
    }

    public static void main(String[] args) {
        try {
            String portNum = ocrText("D:\\tmp\\129523bc-f159-4ebc-9b2b-1dd51f2b67ca.png");
            System.out.println(portNum);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
