package com.td.stock;

import com.td.common.common.GlobalConstant;
import com.td.common.common.ResponseEntity;
import com.td.common.util.HttpClientUtil;
import java.util.HashMap;
import java.util.Map;

/**
 * @author lotey
 * @date 2020/5/9 15:29
 * @desc 米铺抓取测试
 */
public class MimvpTest {

    public static void main(String []args) {
        HttpClientUtil clientUtil = new HttpClientUtil();
        clientUtil.setMaxRetryCount(1);

        Map<String,String> headerMap = new HashMap<>();
        headerMap.put(":authority","proxy.mimvp.com");
        headerMap.put(":method","GET");
        headerMap.put(":path","/freesecret");
        headerMap.put(":scheme","https");
        headerMap.put("accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
        headerMap.put("accept-encoding","gzip, deflate, br");
        headerMap.put("accept-language","zh-CN,zh;q=0.9");
        headerMap.put("cache-control","max-age=0");
        headerMap.put("referer","https://proxy.mimvp.com/price");
        headerMap.put("sec-fetch-mode","navigate");
        headerMap.put("sec-fetch-site","same-origin");
        headerMap.put("sec-fetch-user","?1");
        headerMap.put("upgrade-insecure-requests","1");
        headerMap.put("user-agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.97 Safari/537.36");

        String cookieFormat = "Hm_lvt_2470f08b0a4e8514a3d12a641ddcb46d=1579167801,1579402674; MIMVPSESSID=kskd49qc2re1isf6p8h8lanac8; Hm_lvt_51e3cc975b346e7705d8c255164036b3=%s; Hm_lpvt_51e3cc975b346e7705d8c255164036b3=%s";
        long initTime = System.currentTimeMillis() / 1000 - 20 * 60 * 1000;
        headerMap.put("cookie",String.format(cookieFormat,initTime,System.currentTimeMillis() / 1000 - 60 * 1000));

        int init = 0;
        int max = 100;
        while (init < max) {
            headerMap.put("cookie",String.format(cookieFormat,initTime,System.currentTimeMillis() / 1000 - 60 * 1000));
            ResponseEntity responseEntity = clientUtil.recursiveHttpRequest(GlobalConstant.HTTP_GET,0, String.format(GlobalConstant.MIMVP_PROXY_URL,Math.random()),null,headerMap,GlobalConstant.CHARASET_UTF8);
            System.out.println(responseEntity);
            init++;
        }
    }
}
