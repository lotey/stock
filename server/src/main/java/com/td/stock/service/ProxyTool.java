/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.stock.service;

import com.td.common.common.GlobalConstant;
import com.td.common.common.MybatisBatchHandler;
import com.td.common.common.ResponseEntity;
import com.td.common.mapper.ProxyIpMapper;
import com.td.common.model.ProxyIp;
import com.td.common.util.HttpClientUtil;
import com.td.common.util.SnowflakeGenIdUtil;
import com.td.common.util.SpiderUtil;
import com.td.stock.config.StockConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.logging.log4j.util.Strings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @auther lotey
 * @date 2020/1/4 16:47
 * @desc 代理工具类
 */
@Component
@Slf4j
public class ProxyTool {

    @Autowired
    private HttpClientUtil clientUtil;
    @Autowired
    private BaiduAPI baiduAPI;
    @Autowired
    private SnowflakeGenIdUtil genIdUtil;
    @Autowired
    private ProxyIpMapper proxyIpMapper;
    @Autowired
    private StockConfig stockConfig;
    @Autowired
    private MybatisBatchHandler mybatisBatchHandler;

    /**
     * 读取代理池生成choome使用的代理zip包
     */
    public void genProxyZipPackage() {
        List<ProxyIp> ipList = proxyIpMapper.getHttpsProxyIpList(stockConfig.getCount());
        if (ipList != null && ipList.size() > 0) {
            //读取模版文件
            String templateFilePath = ProxyTool.class.getResource("/file").getPath();
            templateFilePath = templateFilePath.startsWith("/") ? templateFilePath.substring(1) : templateFilePath;
            File mainfestFile = new File(String.format("%s%s%s",templateFilePath,File.separator,"manifest.json"));
            try {
                //读取模版文件
                List<String> templateContentList = Files.readAllLines(Paths.get(String.format("%s%s%s",templateFilePath,File.separator,"background.js")),StandardCharsets.UTF_8);

                ProxyIp proxy = null;
                List<File> zipFileList = null;
                for (int i = 0 ; i < ipList.size() ; i++) {
                    proxy = ipList.get(i);
                    //将内容写入新文件
                    File backgroundFile = new File(String.format("%s%sbackground.js", stockConfig.getSeleniumPath(),File.separator));
                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(backgroundFile), StandardCharsets.UTF_8));
                    String strLine = null;
                    for (String line : templateContentList) {
                        strLine = line;
                        if (line.contains("{host}")) {
                            strLine = line.replaceAll("\\{host}",proxy.getIp());
                        }
                        if (line.contains("{port}")) {
                            strLine = line.replaceAll("\\{port}",String.valueOf(proxy.getPort()));
                        }
                        if (line.contains("{userName}")) {
                            strLine = line.replaceAll("\\{userName}",proxy.getUserName());
                        }
                        if (line.contains("{password}")) {
                            strLine = line.replaceAll("\\{password}",proxy.getPassword());
                        }
                        bw.write(strLine + "\r\n");
                    }
                    bw.flush();
                    bw.close();

                    zipFileList = new ArrayList<>();
                    zipFileList.add(mainfestFile);
                    zipFileList.add(backgroundFile);
                    zipFile(zipFileList,String.format("%s%sproxy_%s.zip", stockConfig.getSeleniumPath(),File.separator,i+1));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 压缩zip文件
     * @param fileList
     * @param zipName
     */
    private void zipFile(List<File> fileList,String zipName) {
        try {
            BufferedInputStream origin = null;
            FileOutputStream dest = new FileOutputStream(zipName);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
            int buffer = 5 * 1024;
            byte data[] = new byte[buffer];
            ZipEntry entry = null;
            for (File file : fileList) {
                FileInputStream fis = new FileInputStream(file);
                origin = new BufferedInputStream(fis, buffer);
                entry = new ZipEntry(file.getName());
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, buffer)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();
            }
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取随zip包
     * @return
     */
    public String getRandomZipProxy() {
        return String.format("%s%sproxy_%s.zip", stockConfig.getSeleniumPath(),File.separator, SpiderUtil.getRandomNum(1, stockConfig.getCount()));
    }

    /**
     * 检测代理ip是否可用
     */
    public void checkAvaProxyIpList() {
        List<ProxyIp> ipList = proxyIpMapper.selectAll();
        Map<String,String> headerMap = new HashMap<>();
        headerMap.put("Host","basic.10jqka.com.cn");
        headerMap.put("Connection","keep-alive");
        headerMap.put("Upgrade-Insecure-Requests","1");
        headerMap.put("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.87 Safari/537.36");
        headerMap.put("Sec-Fetch-Mode","navigate");
        headerMap.put("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
        headerMap.put("Sec-Fetch-Site","none");
        headerMap.put("Accept-Encoding","gzip, deflate, br");
        headerMap.put("Accept-Language","zh-CN,zh;q=0.9");

        int threadCount = 10;
        List<List<ProxyIp>> twoDeepList = SpiderUtil.makeListToTwoDeep(ipList,threadCount);
        ExecutorService threadPool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        List<List<Long>> allIdList = new ArrayList<>();
        twoDeepList.forEach(proxyList -> {
            threadPool.execute(() -> {
                List<Long> idList = new ArrayList<>();
                proxyList.forEach(proxy -> {
                    HttpClientUtil clientUtil = new HttpClientUtil();
                    clientUtil.setProxyPropArr(new String[]{proxy.getIp(),String.valueOf(proxy.getPort())});
                    try {
                        ResponseEntity responseEntity = clientUtil.recursiveHttpRequest(GlobalConstant.HTTP_GET,0, "https://www.baidu.com",null,headerMap,GlobalConstant.CHARASET_UTF8);
                        if (responseEntity.getCode() == 200 && responseEntity.getContent().contains("百度一下")) {
                            idList.add(proxy.getId());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                allIdList.add(idList);
                latch.countDown();
            });
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String idListStr = Strings.join(allIdList.stream().flatMap(List::stream).collect(Collectors.toList()),',');
        System.out.println("可用Ip列表ID为："+idListStr);
    }

    /**
     * 抓取免费的米扑代理
     */
    public void crewMimvpFreeProxyList() {
        Map<String,String> headerMap = new HashMap<>();
        headerMap.put("Host","proxy.mimvp.com");
        headerMap.put("Connection","keep-alive");
        headerMap.put("Pragma","no-cache");
        headerMap.put("Cache-Control","no-cache");
        headerMap.put("Upgrade-Insecure-Requests","1");
        headerMap.put("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.70 Safari/537.36");
        headerMap.put("Sec-Fetch-User","?1");
        headerMap.put("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
        headerMap.put("Sec-Fetch-Site","none");
        headerMap.put("Sec-Fetch-Mode","navigate");
        headerMap.put("Accept-Encoding","gzip, deflate, br");
        headerMap.put("Accept-Language","zh-CN,zh;q=0.9");
        headerMap.put("Cookie","Hm_lvt_2470f08b0a4e8514a3d12a641ddcb46d=1566062072; PHPSESSID=bte6259ou8hrbkgre4mtp2vajl; Hm_lvt_51e3cc975b346e7705d8c255164036b3=1578219350,1578219363,1578219647,1578219947; Hm_lpvt_51e3cc975b346e7705d8c255164036b3=1578220407");

        //获取百度token
        String accessToken = baiduAPI.getAccessToken();

//        PortNumUtil portNumUtil = null;
//        try {
//            portNumUtil = new PortNumUtil();
//        } catch (MWException e) {
//            e.printStackTrace();
//        }
//        PortNumUtil fPortNumUtil = portNumUtil;

        ResponseEntity responseEntity = clientUtil.recursiveHttpRequest(GlobalConstant.HTTP_GET,0, "https://proxy.mimvp.com/freesecret",null,headerMap,GlobalConstant.CHARASET_UTF8);
        if (responseEntity.getCode() == 200 && StringUtils.isNotEmpty(responseEntity.getContent())) {
            List<ProxyIp> proxyIpList = new ArrayList<>();
            Document doc = Jsoup.parse(responseEntity.getContent());
            doc.select("tr:gt(0)").forEach(x -> {
                if (x.select("td:eq(7)").attr("title").startsWith("0") && x.select("td:eq(8)").attr("title").startsWith("0")) {
                    ProxyIp proxyIp = new ProxyIp();
                    proxyIp.setId(genIdUtil.nextId());
                    proxyIp.setIp(x.select("td:eq(1)").text());
                    //Integer port = downImgAndOCR(String.format("%s%s","https://proxy.mimvp.com/", x.select("td:eq(2) > img").attr("src")));
                    String localImgPath = downImgToLocal(String.format("%s%s","https://proxy.mimvp.com/", x.select("td:eq(2) > img").attr("src")));
                    if (StringUtils.isNotEmpty(localImgPath)) {
                        String port = baiduAPI.getOCRImgText(accessToken,localImgPath);
//                        Object port = null;
//                        try {
//                            if (fPortNumUtil == null) {
//                                log.error("========================验证码识别工具类初始化异常========================");
//                                return;
//                            }
//                            port = fPortNumUtil.getPort(1,localImgPath);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
                        if (port != null) {
                            proxyIp.setPort(Integer.parseInt(port.toString()));
                            proxyIp.setType(x.select("td:eq(3)").attr("title").toUpperCase());
                            proxyIpList.add(proxyIp);
                        }
                    }
                }
            });
            //保存入库
//            proxyIpMapper.deleteAll();
            mybatisBatchHandler.batchInsertOrUpdate(proxyIpList, ProxyIpMapper.class,GlobalConstant.BATCH_MODDEL_INSERT);

            //检测可用性
        }
    }

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

//    /**
//     * 下载图片并用OCR识别
//     * @return
//     */
//    private Integer downImgAndOCR(String imgUrl) {
//        try {
//            //下载图片
//            CloseableHttpClient client = HttpClients.createDefault();
//            HttpGet httpGet = new HttpGet(imgUrl);
//            httpGet.setHeader("Host","proxy.mimvp.com");
//            httpGet.setHeader("Connection","keep-alive");
//            httpGet.setHeader("Cache-Control","max-age=0");
//            httpGet.setHeader("Upgrade-Insecure-Requests","1");
//            httpGet.setHeader("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.70 Safari/537.36");
//            httpGet.setHeader("Sec-Fetch-User","?1");
//            httpGet.setHeader("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
//            httpGet.setHeader("Sec-Fetch-Site","none");
//            httpGet.setHeader("Sec-Fetch-Mode","navigate");
//            httpGet.setHeader("Accept-Encoding","gzip, deflate, br");
//            httpGet.setHeader("Accept-Language","zh-CN,zh;q=0.9");
//            httpGet.setHeader("Cookie","Hm_lvt_2470f08b0a4e8514a3d12a641ddcb46d=1566062072; PHPSESSID=bte6259ou8hrbkgre4mtp2vajl; Hm_lvt_51e3cc975b346e7705d8c255164036b3=1578219350,1578219363,1578219647,1578219947; Hm_lpvt_51e3cc975b346e7705d8c255164036b3=1578225593");
//
//            CloseableHttpResponse response = client.execute(httpGet);
//            BufferedImage bi = ImageIO.read(response.getEntity().getContent());
//            File dirFile = new File(proxyConfig.getImgDownadDir());
//            if (!dirFile.exists()) {
//                dirFile.mkdir();
//            }
//            String imgPath = String.format("%s%s%s.png", proxyConfig.getImgDownadDir(),File.separator , UUID.randomUUID().toString());
//            File imgFile = new File(imgPath);
//            ImageIO.write(bi, "png", imgFile);
//
//            //调用OCR识别图片
//            ITesseract instance = new Tesseract();
//            instance.setDatapath(proxyConfig.getTessdataDir());//支持绝对目录
//            instance.setLanguage("eng");//选择字库文件（只需要文件名，不需要后缀名）
//
//            String result = instance.doOCR(imgFile);//开始识别
//            System.out.println(result);//打印图片内容
//            return Integer.parseInt(result.trim().replaceAll("\n","").replaceAll("\"",""));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
}
