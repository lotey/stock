package com.td.stock;

import com.td.common.common.GlobalConstant;
import com.td.common.common.ResponseEntity;
import com.td.common.util.HttpClientUtil;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @auther lotey
 * @date 2019/8/16 12:56
 * @desc 功能描述
 */
public class SpiderTest {

    public static void main(String []args) throws Exception {
//        String url = "http://stockpage.10jqka.com.cn/300340/";
//        String url = "http://q.10jqka.com.cn/thshy/detail/code/881104/";
//        String url = "https://basic.10jqka.com.cn/000651/";
        String homeUrl = "https://basic.10jqka.com.cn/601566/";
        String companyUrl = "https://basic.10jqka.com.cn/002822/company.html";

        Map<String,String> headerMap = new HashMap<>();
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


        headerMap.put("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.70 Safari/537.36");
        HttpClientUtil clientUtil = new HttpClientUtil();
//        ResponseEntity responseEntity = clientUtil.recursiveHttpRequest(GlobalConstant.HTTP_GET,0, companyUrl,null,headerMap, GlobalConstant.CHARASET_GBK);
//        clientUtil.setProxyPropArr(new String[]{"47.98.123.237","9000","proxy","proxy2020"});
//        ResponseEntity responseEntity = clientUtil.recursiveHttpRequest(GlobalConstant.HTTP_GET,0, "https://proxy.mimvp.com/freeopen.php",null,headerMap,GlobalConstant.CHARASET_UTF8);
//        ResponseEntity responseEntity = clientUtil.recursiveHttpRequest(GlobalConstant.HTTP_GET,0, "http://hq.sinajs.cn/list=sh600526,sz002286",null,headerMap,GlobalConstant.CHARASET_GBK);
//        ResponseEntity responseEntity = clientUtil.recursiveHttpRequest(GlobalConstant.HTTP_GET,0, "http://localhost:5000/rest/stock/list=sh603113,sz000632",null,headerMap);
//        System.out.println(responseEntity.getContent());

//        String homeHtml = responseEntity.getContent();
//        String companyHtml = responseEntity.getContent();

//        System.setProperty("webdriver.chrome.driver", "D:\\SeleniumDriver\\chromedriver.exe");
//        WebDriver webDriver = null;
//        try{
//            ChromeOptions options = new ChromeOptions();
//            options.addArguments("--headless"); //无浏览器模式
//            options.addArguments("--proxy-server=http://47.98.123.237:8888");
//            options.addArguments("--no-sandbox");
//            options.addArguments("--disable-gpu");
//            options.addArguments("blink-settings=imagesEnabled=false");
//            options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.70 Safari/537.36");
////            options.addExtensions(new File("D:\\SeleniumDriver\\proxy.zip"));//增加代理扩展
//            webDriver = new ChromeDriver(options);//实例化
//
//            //访问首页
//            webDriver.get(homeUrl);
//            homeHtml = webDriver.getPageSource();
//            Files.write(Paths.get("E:\\var\\home.txt"), homeHtml.getBytes());
//
//            webDriver.get(companyUrl);
//            companyHtml = webDriver.getPageSource();
//            Files.write(Paths.get("E:\\var\\company.txt"), companyHtml.getBytes());
//
//            System.out.println(webDriver.getPageSource());
//
//            Files.write(Paths.get("E:\\var\\company.txt"), companyHtml.getBytes());
//        }catch(Exception e){
//            e.printStackTrace();
//        }finally{
//            //使用完毕，关闭webDriver
//            if(webDriver != null){
//                webDriver.quit();
//            }
//        }

        BufferedReader br = null;
        String line = null;
        StringBuffer sbf;
        String html = null;
        Pattern pattern = null;
        Matcher matcher = null;
        //从数据中提取信息
        try {
//            br = new BufferedReader(new FileReader("E:\\var\\home.txt"));
//            sbf = new StringBuffer();
//            line = null;
//            while ((line = br.readLine()) != null) {
//                sbf.append(line);
//            }
//            html = sbf.toString();
//
//            //提取行业
//            pattern = Pattern.compile("<span class=\"tip f14\">(.*?)</span>");
//            Matcher matcher = pattern.matcher(html);
//            if (matcher.find()) {
//                System.out.println(matcher.group(1));
//            }
//
//            //提取板块
//            StringBuffer sBuff = new StringBuffer();
//            pattern = Pattern.compile("ifind\">(.*?)</a>");
//            matcher = pattern.matcher(html);
//            String sText = null;
//            while (matcher.find()) {
//                sText = matcher.group(1);
//                if (!sText.contains("详情")) {
//                    if (sText.contains("em")) {
//                        sBuff.append(sText.substring(0,sText.indexOf("<"))).append(",");
//                    } else {
//                        sBuff.append(sText).append(",");
//                    }
//                }
//            }
//            System.out.println(sBuff.toString());
//
//            //提取市盈率
//            pattern = Pattern.compile("id=\"jtsyl\">(.*?)</span>");
//            matcher = pattern.matcher(html);
//            if (matcher.find()) {
//                System.out.println(matcher.group(1));
//            }
//
//            //提取动态市盈率
//            pattern = Pattern.compile("id=\"sjl\">(.*?)</span>");
//            matcher = pattern.matcher(html);
//            if (matcher.find()) {
//                System.out.println(matcher.group(1));
//            }
//
//            //提取解禁信息
//            if (html.contains("解禁股份类型")) {
//                pattern = Pattern.compile("<span class=\"tip f12\">(.*?)</span>");
//                matcher = pattern.matcher(html);
//                while (matcher.find()) {
//                    if (matcher.group(1).contains("-")) {
//                        System.out.println(matcher.group(1));
//                    }
//                }
//            }
//
            //提取公司信息
            br = new BufferedReader(new FileReader("E:\\var\\company.txt"));
            line = null;
            sbf = new StringBuffer();
            while ((line = br.readLine()) != null) {
                sbf.append(line);
            }
            html = sbf.toString();

            //提取公司名称
            pattern = Pattern.compile("公司名称：</strong><span>(.*?)</span>");
            matcher = pattern.matcher(html);
            if (matcher.find()) {
                System.out.println(matcher.group(1));
            }

            //提取省份
            pattern = Pattern.compile("所属地域：</strong><span>(.*?)</span>");
            matcher = pattern.matcher(html);
            if (matcher.find()) {
                System.out.println(matcher.group(1));
            }

            //提取各种人信息
            pattern = Pattern.compile("<a person_id=\"(.*?)\" class=\"turnto\" href=\"javascript:void\\(0\\)\">(.*?)</a>");
            matcher = pattern.matcher(html);
            while (matcher.find()) {
                System.out.println(matcher.group(2));
            }

            pattern = Pattern.compile("<td colspan=\"3\">(.*?)</td>");
            matcher = pattern.matcher(html);
            while (matcher.find()) {
                if (matcher.group(1).contains("办公地址")) {
                    pattern = Pattern.compile("<span>(.*?)</span>");
                    matcher = pattern.matcher(matcher.group(1));
                    if (matcher.find()) {
                        System.out.println(matcher.group(1));
                    }
                }
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
