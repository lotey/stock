/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.common.util;

import com.td.common.common.GlobalConstant;
import com.td.common.common.ResponseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.HttpRequestRetryStrategy;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.NoHttpResponseException;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.util.Args;
import org.apache.hc.core5.util.TimeValue;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author lotey
 * @date 2016年7月14日 下午12:12:38
 * @desc HTTP请求工具类
 */
//@Component(value = "clientUtil")
//@Scope("prototype")
@Slf4j
public class HttpClientUtil {

    public static final String STRBODY = "STRBODY";

    //最大重试次数
    @Setter
    @Getter
    private int maxRetryCount;

    //代理属性数组
    @Setter
    @Getter
    private String []proxyPropArr;

    /**
     * 获取带ssl的httpclient对象
     * 忽略服务器证书，采用信任机制
     * @return
     */
    public CloseableHttpClient getSSLHttpClient(String[] proxyPropArr) throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        // 实现一个X509TrustManager接口，用于绕过验证，不用修改里面的方法
        X509TrustManager x509TrustManager = new X509TrustManager() {

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

            }
        };
        // 初始化SSL上下文
        sslContext.init(null, new TrustManager[] { x509TrustManager }, null);
        // SSL套接字连接工厂,NoopHostnameVerifier为信任所有服务器
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext,new String[]{"SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2"}, null, NoopHostnameVerifier.INSTANCE);
        // 注册http套接字工厂和https套接字工厂
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory> create()
                .register("http", PlainConnectionSocketFactory.INSTANCE)
                .register("https", sslsf).build();

        // 连接池管理器
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(registry);
        connManager.setMaxTotal(1000);// 连接池最大连接数
        connManager.setDefaultMaxPerRoute(100);// 每个路由最大连接数

        // 配置超时回调机制
        HttpRequestRetryStrategy retryHandler = new HttpRequestRetryStrategy() {
            @Override
            public boolean retryRequest(HttpRequest httpRequest, IOException e, int i, HttpContext httpContext) {
                if (i >= 3) {
                    return false;
                }
                if (e instanceof NoHttpResponseException) {// 如果服务器丢掉了连接，那么就重试
                    return true;
                }
                if (e instanceof SSLHandshakeException) {// 不要重试SSL握手异常
                    return false;
                }
                if (e instanceof InterruptedIOException) {// 超时
                    return true;
                }
                if (e instanceof UnknownHostException) {// 目标服务器不可达
                    return false;
                }
                if (e instanceof SSLException) {// ssl握手异常
                    return false;
                }
//                HttpClientContext clientContext = HttpClientContext.adapt(httpContext);
//                HttpRequest request = clientContext.getRequest();
//                // 如果请求是幂等的，就再次尝试
//                if (!(request instanceof HttpEntityEnclosingRequest)) {
//                    return true;
//                }
                return false;
            }

            @Override
            public boolean retryRequest(HttpResponse httpResponse, int i, HttpContext httpContext) {
                return false;
            }

            @Override
            public TimeValue getRetryInterval(HttpResponse httpResponse, int i, HttpContext httpContext) {
                return null;
            }
        };

        // 配置请求参数
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(4000, TimeUnit.MILLISECONDS)
                .setConnectTimeout(8000,TimeUnit.MILLISECONDS)
                .setResponseTimeout(10000,TimeUnit.MILLISECONDS)
                .build();

        // 构建https客户端
        if (this.getProxyPropArr() == null) {
            return HttpClients.custom()
                    .setConnectionManager(connManager)
                    .setDefaultRequestConfig(requestConfig)
                    .setRetryStrategy(retryHandler)
                    //禁用重定向
                    .disableRedirectHandling()
                    .build();
        } else if (this.getProxyPropArr().length == 2) {
            return HttpClients.custom()
                    .setConnectionManager(connManager)
                    .setDefaultRequestConfig(requestConfig)
                    .setRetryStrategy(retryHandler)
                    .setProxy(new HttpHost(this.getProxyPropArr()[0], Integer.parseInt(this.getProxyPropArr()[1])))
                    //禁用重定向
                    .disableRedirectHandling()
                    .build();
        } else {
            HttpHost proxy = new HttpHost(this.getProxyPropArr()[0], Integer.parseInt(this.getProxyPropArr()[1]));
            // 设置认证
            BasicCredentialsProvider provider = null;
            if (proxyPropArr.length == 4) {
                provider = new BasicCredentialsProvider();
                provider.setCredentials(new AuthScope(proxy), new UsernamePasswordCredentials(proxyPropArr[2], proxyPropArr[3].toCharArray()));
            }

            // 构建https客户端
            return HttpClients.custom()
                    .setConnectionManager(connManager)
                    .setDefaultRequestConfig(requestConfig)
                    .setRetryStrategy(retryHandler)
                    .setDefaultCredentialsProvider(provider)
                    //禁用重定向
                    .disableRedirectHandling()
                    .build();
        }
    }

    /**
     * 递归调用HTTP请求
     * 若未获取到结果，重试至MAX_RETRY_COUNT次
     * @param method
     * @param baseUrl
     * @param paramMap
     * @param headerMap
     * @return
     */
    public ResponseEntity recursiveHttpRequest(String method, int count, String baseUrl, Map<String, Object> paramMap, Map<String, String> headerMap,String charset) {
        if (count > maxRetryCount) {
            return null;
        } else {
            if (count > 0) {
                log.info("请求失败，重试第{}次",count);
            }
            ResponseEntity responseEntity = null;
            if (GlobalConstant.HTTP_GET.equals(method)) {
                responseEntity = getRequest(baseUrl,paramMap,headerMap,charset);
            } else if (GlobalConstant.HTTP_POST.equals(method)) {
                responseEntity = postRequest(baseUrl,paramMap,headerMap,charset);
            } else {
                responseEntity = postStringRequest(baseUrl,paramMap,headerMap,charset);
            }
            count++;
            if (responseEntity == null) {
                return recursiveHttpRequest(method, count,baseUrl,paramMap,headerMap, charset);
            } else {
                return responseEntity;
            }
        }
    }

    /**
     * HTTP GET请求
     * @param baseUrl 请求基本路径，即不带参数的路径
     * @param paramMap 参数集合
     * @param headerMap header集合
     * @param charset 源目标输出编码
     * @return
     */
    private ResponseEntity getRequest(String baseUrl, Map<String, Object> paramMap, Map<String, String> headerMap,String charset) {
        //创建httpclient实例，用于发送请求
        CloseableHttpClient client = null;
        try {
            client = getSSLHttpClient(proxyPropArr);
        }catch (Exception e) {
            client = HttpClients.createDefault();
            e.printStackTrace();
        }
        HttpGet httpGet = new HttpGet(baseUrl);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(4000,TimeUnit.MILLISECONDS)
                .setConnectTimeout(8000,TimeUnit.MILLISECONDS)
                .setResponseTimeout(10000,TimeUnit.MILLISECONDS)
                .setRedirectsEnabled(false)
                .build();
        if  (proxyPropArr != null && proxyPropArr.length > 0) {
            //设置代理IP、端口、协议（请分别替换）
            HttpHost proxy = new HttpHost(proxyPropArr[0], Integer.parseInt(proxyPropArr[1]));
            // 设置认证
            BasicCredentialsProvider provider = null;
            if (proxyPropArr.length == 4) {
                provider = new BasicCredentialsProvider();
                provider.setCredentials(new AuthScope(proxy), new UsernamePasswordCredentials(proxyPropArr[2], proxyPropArr[3].toCharArray()));
            }
            //设置超时时间
            requestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout(4000,TimeUnit.MILLISECONDS)
                    .setConnectTimeout(8000,TimeUnit.MILLISECONDS)
                    .setResponseTimeout(10000,TimeUnit.MILLISECONDS)
                    .setRedirectsEnabled(false)
                    .setProxy(proxy)
                    .build();
        }
        httpGet.setConfig(requestConfig);

        CloseableHttpResponse response = null;
        try {
            //封装参数列表
            if(paramMap != null && paramMap.size() > 0) {
                String scheme = baseUrl.split(":")[0];
                String host = baseUrl.substring(baseUrl.indexOf("//") + 2,baseUrl.lastIndexOf("/"));
                String path = baseUrl.substring(baseUrl.lastIndexOf("/"));
                log.debug("==========================scheme = {} host = {} path = {}==========================",scheme,host,path);
                //用URIBuilder构造请求
                URIBuilder builder = new URIBuilder().setScheme(scheme).setHost(host).setPath(path);
                List<NameValuePair> paramList = new ArrayList<>();
                for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
                    paramList.add(new BasicNameValuePair(entry.getKey(), String.valueOf(entry.getValue())));
                }
                //追加参数
                builder.addParameters(paramList);
                //设置GET请求URL
                httpGet.setUri(builder.build());
            }
            //设置header
            if(headerMap != null && headerMap.size() > 0) {
                for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                    httpGet.setHeader(entry.getKey(), entry.getValue());
                }
            }
            //执行请求返回结果集
            response = client.execute(httpGet);
            // 服务器返回码
            int status_code = response.getCode();
            log.debug("==========================调用状态码：{}==========================",status_code);
            // 服务器返回内容
            String respStr = null;
            HttpEntity responseEntity = response.getEntity();
            if(responseEntity != null) {
                //根据目标编码转码
                Args.check(responseEntity.getContentLength() <= 2147483647L, "HTTP entity too large to be buffered in memory");
                int capacity = (int)responseEntity.getContentLength();
                if (capacity < 0) {
                    capacity = 4096;
                }
                try (
                        InputStream in = responseEntity.getContent();
                        ByteArrayOutputStream os = new ByteArrayOutputStream(capacity);
                ) {
                    byte[] temp = new byte[1024];
                    int size;
                    while ((size = in.read(temp)) != -1) {
                        os.write(temp, 0, size);
                    }
//                    respStr = EntityUtils.toString(responseEntity, Consts.UTF_8);
                    respStr = os.toString(charset);
                }
            }

            //获取全部cookie，过滤掉gzip，防止压缩解码
            Header[] headers = response.getHeaders();
            Map<String,String> cookieMap = new HashMap<>();
            //此处存储时统一将key修改为小写，避免调用时候大小写问题
            String headerValue = null;
            for (Header header : headers) {
                headerValue = header.getValue().split(";")[0];
                if (cookieMap.containsKey(header.getName().toLowerCase())) {
                    cookieMap.put(header.getName().toLowerCase(), cookieMap.get(header.getName().toLowerCase()) + ";" + headerValue);
                } else {
                    cookieMap.put(header.getName().toLowerCase(),headerValue);
                }
                log.debug("{} -> {}",header.getName().toLowerCase(),header.getValue());
                log.debug("{} -> {}",header.getName().toLowerCase(),headerValue);
            }

            // 释放资源
            EntityUtils.consume(responseEntity);
            return new ResponseEntity(status_code,cookieMap,respStr);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * HTTP POST请求
     * @param baseUrl 请求基本路径，即不带参数的路径
     * @param paramMap 参数集合
     * @param headerMap header集合
     * @param charset 源目标输出编码
     * @return
     */
    private ResponseEntity postRequest(String baseUrl,Map<String, Object> paramMap,Map<String, String> headerMap,String charset) {
        //创建httpclient实例，用于发送请求
        CloseableHttpClient client = null;
        try {
            client = getSSLHttpClient(proxyPropArr);
        } catch (Exception e) {
            client = HttpClients.createDefault();
            e.printStackTrace();
        }
        //创建post请求
        HttpPost httpPost = new HttpPost(baseUrl);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(4000,TimeUnit.MILLISECONDS)
                .setConnectTimeout(8000,TimeUnit.MILLISECONDS)
                .setResponseTimeout(10000,TimeUnit.MILLISECONDS)
                .setRedirectsEnabled(false)
                .build();
        if  (proxyPropArr != null && proxyPropArr.length > 0) {
            //设置代理IP、端口、协议（请分别替换）
            HttpHost proxy = new HttpHost(proxyPropArr[0], Integer.parseInt(proxyPropArr[1]));
            //设置超时时间
            requestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout(4000,TimeUnit.MILLISECONDS)
                    .setConnectTimeout(8000,TimeUnit.MILLISECONDS)
                    .setResponseTimeout(10000,TimeUnit.MILLISECONDS)
                    .setRedirectsEnabled(false)
                    .setProxy(proxy)
                    .build();
        }
        httpPost.setConfig(requestConfig);

        CloseableHttpResponse response = null;
        try {
            //封装参数列表
            if(paramMap != null && paramMap.size() > 0) {
                List<NameValuePair> paramList = new ArrayList<>();
                for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
                    paramList.add(new BasicNameValuePair(entry.getKey(), String.valueOf(entry.getValue())));
                }
                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(paramList, StandardCharsets.UTF_8);
                httpPost.setEntity(entity);
            }

            //设置header
            if(headerMap != null && headerMap.size() > 0) {
                for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                    httpPost.setHeader(entry.getKey(), entry.getValue());
                }
            }
            //执行请求返回结果集
            response = client.execute(httpPost);
            // 服务器返回码
            int status_code = response.getCode();
            log.debug("==========================调用状态码：{}==========================",status_code);
            //服务器返回内容
            String respStr = null;
            HttpEntity responseEntity = response.getEntity();
            if(responseEntity != null) {
                //根据目标编码转码
                Args.check(responseEntity.getContentLength() <= 2147483647L, "HTTP entity too large to be buffered in memory");
                int capacity = (int)responseEntity.getContentLength();
                if (capacity < 0) {
                    capacity = 4096;
                }
                try (
                        InputStream is = responseEntity.getContent();
                        ByteArrayOutputStream os = new ByteArrayOutputStream(capacity);
                ) {
                    byte[] temp = new byte[1024];
                    int size;
                    while ((size = is.read(temp)) != -1) {
                        os.write(temp, 0, size);
                    }
//                    respStr = EntityUtils.toString(responseEntity, Consts.UTF_8);
                    respStr = os.toString(charset);
                }
            }
            //获取全部cookie，过滤掉gzip，防止压缩解码
            Header[] headers = response.getHeaders();
            Map<String,String> cookieMap = new HashMap<>();
            //此处存储时统一将key修改为小写，避免调用时候大小写问题
            String headerValue = null;
            for (Header header : headers) {
                headerValue = header.getValue().split(";")[0];
                if (cookieMap.containsKey(header.getName().toLowerCase())) {
                    cookieMap.put(header.getName().toLowerCase(), cookieMap.get(header.getName().toLowerCase()) + ";" + headerValue);
                } else {
                    cookieMap.put(header.getName().toLowerCase(),headerValue);
                }
                log.debug("{} -> {}",header.getName().toLowerCase(),header.getValue());
                log.debug("{} -> {}",header.getName().toLowerCase(),headerValue);
            }

            // 释放资源
            EntityUtils.consume(responseEntity);
            return new ResponseEntity(status_code,cookieMap,respStr);
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * HTTP POST请求体请求
     * @param baseUrl
     * @param paramMap
     * @param headerMap
     * @param charset
     * @return
     */
    private ResponseEntity postStringRequest(String baseUrl,Map<String, Object> paramMap,Map<String, String> headerMap, String charset) {
        //创建httpclient实例，用于发送请求
        CloseableHttpClient client = null;
        try {
            client = getSSLHttpClient(proxyPropArr);
        } catch (Exception e) {
            client = HttpClients.createDefault();
            e.printStackTrace();
        }
        //创建post请求
        HttpPost httpPost = new HttpPost(baseUrl);

        CloseableHttpResponse response = null;
        try {
            //封装参数列表
            if(paramMap != null && paramMap.size() > 0) {
                for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
                    httpPost.addHeader(entry.getKey(), String.valueOf(entry.getValue()));
                }

                //获取json参数直接提交到服务器
                String jsonParamStr = (String)paramMap.get(STRBODY);
                StringEntity entity = null;
                if (StringUtils.isNotEmpty(jsonParamStr)) {
                    entity = new StringEntity(jsonParamStr, ContentType.create("application/json",StandardCharsets.UTF_8));
                    httpPost.setEntity(entity);
                }
            }

            //设置header
            if(headerMap != null && headerMap.size() > 0) {
                for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                    httpPost.setHeader(entry.getKey(), entry.getValue());
                }
            }
            //执行请求返回结果集
            response = client.execute(httpPost);
            // 服务器返回码
            int status_code = response.getCode();
            log.debug("==========================调用状态码：{}==========================",status_code);
            //服务器返回内容
            String respStr = null;
            HttpEntity responseEntity = response.getEntity();
            if(responseEntity != null) {
                //根据目标编码转码
                Args.check(responseEntity.getContentLength() <= 2147483647L, "HTTP entity too large to be buffered in memory");
                int capacity = (int)responseEntity.getContentLength();
                if (capacity < 0) {
                    capacity = 4096;
                }
                try (
                        InputStream is = responseEntity.getContent();
                        ByteArrayOutputStream os = new ByteArrayOutputStream(capacity);
                ) {
                    byte[] temp = new byte[1024];
                    int size;
                    while ((size = is.read(temp)) != -1) {
                        os.write(temp, 0, size);
                    }
//                    respStr = EntityUtils.toString(responseEntity, Consts.UTF_8);
                    respStr = os.toString(charset);
                }
            }
            //获取全部cookie，过滤掉gzip，防止压缩解码
            Header[] headers = response.getHeaders();
            Map<String,String> cookieMap = new HashMap<>();
            //此处存储时统一将key修改为小写，避免调用时候大小写问题
            String headerValue = null;
            for (Header header : headers) {
                headerValue = header.getValue().split(";")[0];
                if (cookieMap.containsKey(header.getName().toLowerCase())) {
                    cookieMap.put(header.getName().toLowerCase(), cookieMap.get(header.getName().toLowerCase()) + ";" + headerValue);
                } else {
                    cookieMap.put(header.getName().toLowerCase(),headerValue);
                }
                log.debug("{} -> {}",header.getName().toLowerCase(),header.getValue());
                log.debug("{} -> {}",header.getName().toLowerCase(),headerValue);
            }

            // 释放资源
            EntityUtils.consume(responseEntity);
            return new ResponseEntity(status_code,cookieMap,respStr);
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public static void main(String[] args) {
//		Map<String, Object> paramMap = new HashMap<>();
//		paramMap.put("account", "xiaoping3");
//		paramMap.put("password", "123456");
//
//		String result = "";
//		try {
//			result = getRequest2("https://kfc2001.com/member/period?lottery=BJPK10", paramMap,null);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		System.out.println(result);
    }
}