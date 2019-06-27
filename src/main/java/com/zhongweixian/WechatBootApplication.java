package com.zhongweixian;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@EnableScheduling
@SpringBootApplication
public class WechatBootApplication {

    @Bean
    public ExecutorService wxExecutor() {
        ScheduledExecutorService wxExecutor = new ScheduledThreadPoolExecutor(10,
                new BasicThreadFactory.Builder().namingPattern("wx-login-schedule-pool-%d").daemon(true).build());
        return wxExecutor;
    }

    @Bean
    public ScheduledExecutorService wbExecutor() {
        ScheduledExecutorService wbExecutor = new ScheduledThreadPoolExecutor(500,
                new BasicThreadFactory.Builder().namingPattern("weibo-schedule-pool--%d").daemon(true).build());
        return wbExecutor;
    }


    @Bean
    public RestTemplate restTemplate() {
        System.setProperty("javax.net.ssl.trustStore", "src/main/resources/cert/weibo.cer");
        // 支持HTTP、HTTPS
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", SSLConnectionSocketFactory.getSocketFactory())
                .build();
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registry);
        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(50000)
                .setConnectTimeout(5000)
                .setConnectionRequestTimeout(2000)
                .build();
        HttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).setConnectionManager(connectionManager).build();
        ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        restTemplate.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return restTemplate;
    }

    public static void main(String[] args) {
        SpringApplication.run(WechatBootApplication.class, args);
    }
}
