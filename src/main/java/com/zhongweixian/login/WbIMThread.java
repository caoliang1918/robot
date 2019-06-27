package com.zhongweixian.login;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

public class WbIMThread implements Runnable {
    private Logger logger = LoggerFactory.getLogger(WbIMThread.class);


    private HttpHeaders httpHeaders;
    private RestTemplate restTemplate;


    @Override
    public void run() {


    }


    void listen() {
        do {

        } while (true);
    }


}
