package com.td.stock;

import com.td.common.common.GlobalConstant;
import com.td.common.common.ResponseEntity;
import com.td.common.util.HttpClientUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * @auther lotey
 * @date 2022/2/23 14:30
 * @desc 功能描述
 */
public class ProxyTest {

    public static void main(String []args) {
        HttpClientUtil clientUtil = new HttpClientUtil();
        clientUtil.setMaxRetryCount(1);
//        clientUtil.setProxyPropArr(new String[]{"127.0.0.1","1080","test","test"});
        clientUtil.setProxyPropArr(new String[]{"tps773.kdlapi.com","15818","t14567087617857","83h4t443"});

        Map<String,String> headerMap = new HashMap<>();
//        headerMap.put("Connection","keep-alive");
//        headerMap.put("Upgrade-Insecure-Requests","1");
//        headerMap.put("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
//        headerMap.put("Accept-Encoding","gzip, deflate");
//        headerMap.put("Accept-Language","zh-CN,zh;q=0.9");
//        headerMap.put("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/97.0.4692.71 Safari/537.36");

//        String url = "http://httpbin.org/get";
        String url = "https://proxy.mimvp.com/test_proxy2";
//        String url = "http://www.baidu.com";
        ResponseEntity responseEntity = clientUtil.recursiveHttpRequest(GlobalConstant.HTTP_GET,0,url,null,headerMap,GlobalConstant.CHARASET_UTF8);
        System.out.println(responseEntity.getContent());
    }
}
