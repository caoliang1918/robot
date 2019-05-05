package com.zhongweixian.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.zhongweixian.domain.BaseUserCache;
import com.zhongweixian.domain.request.*;
import com.zhongweixian.domain.request.component.BaseRequest;
import com.zhongweixian.domain.response.*;
import com.zhongweixian.domain.shared.*;
import com.zhongweixian.enums.AddScene;
import com.zhongweixian.enums.MessageType;
import com.zhongweixian.enums.OpLogCmdId;
import com.zhongweixian.enums.VerifyUserOPCode;
import com.zhongweixian.exception.WechatException;
import com.zhongweixian.utils.DeviceIdGenerator;
import com.zhongweixian.utils.HeaderUtils;
import com.zhongweixian.utils.RandomUtils;
import com.zhongweixian.utils.WechatUtils;
import com.zhongweixian.utils.rest.StatefullRestTemplate;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WechatHttpService {
    private Logger logger = LoggerFactory.getLogger(WechatHttpService.class);

    public static final String WECHAT_HOST = "https://wx.qq.com/";

    public static final String WECHAT_LOGIN_HOST = "https://login.weixin.qq.com/";
    public static final String WECHAT_FILE_HOST = "https://file.web.wechat.com";
    public static final String WECHAT_PUSH_HOST = "https://webpush.web.wechat.com";


    private static final String WECHAT_OPTION_URL = "%s" + "/cgi-bin/mmwebwx-bin";


    private String WECHAT_URL_UUID = WECHAT_LOGIN_HOST + "jslogin?appid=wx782c26e4c19acffb&fun=new&lang=zh_CN&_=%s";
    private String WECHAT_URL_LOGIN = WECHAT_LOGIN_HOST + "/cgi-bin/mmwebwx-bin/login?loginicon=true&uuid=%s&tip=0&r=%s&_=%s";
    private String WECHAT_URL_QRCODE = WECHAT_LOGIN_HOST + "qrcode";
    private String WECHAT_URL_STATUS_NOTIFY = WECHAT_OPTION_URL + "/webwxstatusnotify";
    private String WECHAT_URL_STATREPORT = WECHAT_OPTION_URL + "/webwxstatreport?fun=new";
    private String WECHAT_URL_INIT = WECHAT_OPTION_URL + "/webwxinit?r=%s";
    private String WECHAT_URL_SYNC_CHECK = "https://webpush.%s/cgi-bin/mmwebwx-bin/synccheck";
    private String WECHAT_URL_SYNC = WECHAT_OPTION_URL + "/webwxsync?sid=%s&skey=%s";
    private String WECHAT_URL_GET_CONTACT = WECHAT_OPTION_URL + "/webwxgetcontact?r=%s&seq=%s&skey=%s";
    private String WECHAT_URL_SEND_MSG = WECHAT_OPTION_URL + "/webwxsendmsg";
    private String WECHAT_URL_REVOKE_MSG = WECHAT_OPTION_URL + "/webwxrevokemsg";
    private String WECHAT_URL_LOGOUT = WECHAT_OPTION_URL + "/webwxlogout?redirect=1&type=1&skey=%s";
    private String WECHAT_URL_BATCH_GET_CONTACT = WECHAT_OPTION_URL + "/webwxbatchgetcontact?type=ex&r=%s";
    private String WECHAT_URL_OP_LOG = WECHAT_OPTION_URL + "/webwxoplog";
    private String WECHAT_URL_VERIFY_USER = WECHAT_OPTION_URL + "/webwxverifyuser";
    private String WECHAT_URL_CREATE_CHATROOM = WECHAT_OPTION_URL + "/webwxcreatechatroom?r=%s";
    private String WECHAT_URL_DELETE_CHATROOM_MEMBER = WECHAT_OPTION_URL + "/webwxupdatechatroom?fun=delmember";
    private String WECHAT_URL_ADD_CHATROOM_MEMBER = WECHAT_OPTION_URL + "/webwxupdatechatroom?fun=addmember";


    private RestTemplate restTemplate;
    private final HttpHeaders postHeader;
    private final HttpHeaders getHeader;
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private String BROWSER_DEFAULT_ACCEPT_LANGUAGE = "en,zh-CN;q=0.8,zh;q=0.6,ja;q=0.4,zh-TW;q=0.2";
    private String BROWSER_DEFAULT_ACCEPT_ENCODING = "gzip, deflate, br";

    @Autowired
    WechatHttpService(@Value("${User-Agent}") String BROWSER_DEFAULT_USER_AGENT) {
        this.postHeader = new HttpHeaders();
        postHeader.set(HttpHeaders.USER_AGENT, BROWSER_DEFAULT_USER_AGENT);
        postHeader.set(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
        postHeader.setAccept(Arrays.asList(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.ALL));
        postHeader.set(HttpHeaders.ACCEPT_LANGUAGE, BROWSER_DEFAULT_ACCEPT_LANGUAGE);
        postHeader.set(HttpHeaders.ACCEPT_ENCODING, BROWSER_DEFAULT_ACCEPT_ENCODING);
        this.getHeader = new HttpHeaders();
        getHeader.set(HttpHeaders.USER_AGENT, BROWSER_DEFAULT_USER_AGENT);
        getHeader.set(HttpHeaders.ACCEPT_LANGUAGE, BROWSER_DEFAULT_ACCEPT_LANGUAGE);
        getHeader.set(HttpHeaders.ACCEPT_ENCODING, BROWSER_DEFAULT_ACCEPT_ENCODING);
    }

    void logout(BaseUserCache userCache) throws IOException {
        final String url = String.format(WECHAT_URL_LOGOUT, userCache.getWxHost(), escape(userCache.getsKey()));
        userCache.getRestTemplate().exchange(url, HttpMethod.GET, new HttpEntity<>(postHeader), Object.class);
    }

    /**
     * 先打开微信首页，需要缓存cookie
     */
    void start() {
        final String url = WECHAT_HOST;
        restTemplate = createRest();
        HttpHeaders customHeader = new HttpHeaders();
        customHeader.setPragma("no-cache");
        customHeader.setCacheControl("no-cache");
        customHeader.set("Upgrade-Insecure-Requests", "1");
        customHeader.set(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        HeaderUtils.assign(customHeader, getHeader);
        restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(customHeader), String.class);
        CookieStore store = (CookieStore) ((StatefullRestTemplate) restTemplate).getHttpContext().getAttribute(HttpClientContext.COOKIE_STORE);
        Date maxDate = new Date(Long.MAX_VALUE);
        String domain = WECHAT_HOST.replaceAll("https://", "").replaceAll("/", "");
        Map<String, String> cookies = new HashMap<>(3);
        cookies.put("MM_WX_NOTIFY_STATE", "1");
        cookies.put("MM_WX_SOUND_STATE", "1");
        cookies.put("refreshTimes", "2");
        appendAdditionalCookies(store, cookies, domain, "/", maxDate);
    }

    private RestTemplate createRest() {
        CookieStore cookieStore = new BasicCookieStore();
        HttpContext httpContext = new BasicHttpContext();
        httpContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
        httpContext.setAttribute(HttpClientContext.REQUEST_CONFIG, RequestConfig.custom().setRedirectsEnabled(false).build());
        return new StatefullRestTemplate(httpContext);
    }

    /**
     * Get UUID for this session
     *
     * @return UUID
     */
    String getUUID() {
        final String regEx = "window.QRLogin.code = (\\d+); window.QRLogin.uuid = \"(\\S+?)\";";
        final String url = String.format(WECHAT_URL_UUID, System.currentTimeMillis());
        final String successCode = "200";
        HttpHeaders customHeader = new HttpHeaders();
        customHeader.setPragma("no-cache");
        customHeader.setCacheControl("no-cache");
        customHeader.setAccept(Arrays.asList(MediaType.ALL));
        customHeader.set(HttpHeaders.REFERER, WECHAT_LOGIN_HOST);
        HeaderUtils.assign(customHeader, getHeader);
        ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(customHeader), String.class);
        String body = responseEntity.getBody();
        Matcher matcher = Pattern.compile(regEx).matcher(body);
        if (matcher.find()) {
            if (successCode.equals(matcher.group(1))) {
                return matcher.group(2);
            }
        }
        throw new WechatException("uuid can't be found");
    }

    /**
     * Get QR code for scanning
     *
     * @param uuid UUID
     * @return QR code in binary
     */
    byte[] getQR(String uuid) {
        final String url = WECHAT_URL_QRCODE + "/" + uuid;
        HttpHeaders customHeader = new HttpHeaders();
        customHeader.set(HttpHeaders.ACCEPT, "image/webp,image/apng,image/*,*/*;q=0.8");
        customHeader.set(HttpHeaders.REFERER, WECHAT_HOST);
        HeaderUtils.assign(customHeader, getHeader);
        ResponseEntity<byte[]> responseEntity
                = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(customHeader), new ParameterizedTypeReference<byte[]>() {
        });
        return responseEntity.getBody();
    }

    /**
     * Get hostUrl and redirectUrl
     *
     * @param uuid
     * @return hostUrl and redirectUrl
     * @throws WechatException if the response doesn't contain code
     */
    LoginResult login(String uuid) throws WechatException {
        final Pattern pattern = Pattern.compile("window.code=(\\d+)");
        Pattern hostUrlPattern = Pattern.compile("window.redirect_uri=\\\"(.*)\\/cgi-bin");
        Pattern redirectUrlPattern = Pattern.compile("window.redirect_uri=\\\"(.*)\\\";");
        long time = System.currentTimeMillis();
        final String url = String.format(WECHAT_URL_LOGIN, uuid, RandomUtils.generateDateWithBitwiseNot(time), time);
        HttpHeaders customHeader = new HttpHeaders();
        customHeader.setAccept(Arrays.asList(MediaType.ALL));
        customHeader.set(HttpHeaders.REFERER, WECHAT_HOST);
        HeaderUtils.assign(customHeader, getHeader);
        ResponseEntity<String> responseEntity
                = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(customHeader), String.class);
        String body = responseEntity.getBody();
        Matcher matcher = pattern.matcher(body);
        LoginResult response = new LoginResult();
        if (matcher.find()) {
            response.setCode(matcher.group(1));
        } else {
            throw new WechatException("code can't be found");
        }
        Matcher hostUrlMatcher = hostUrlPattern.matcher(body);
        if (hostUrlMatcher.find()) {
            response.setHostUrl(hostUrlMatcher.group(1));
        }
        Matcher redirectUrlMatcher = redirectUrlPattern.matcher(body);
        if (redirectUrlMatcher.find()) {
            response.setRedirectUrl(redirectUrlMatcher.group(1));
        }
        return response;
    }

    /**
     * Get basic parameters for this session
     *
     * @param redirectUrl
     * @return session token
     * @throws IOException if the http response body can't be convert to {@link Token}
     */
    Token openNewloginpage(String redirectUrl, BaseUserCache userCache) throws IOException {
        HttpHeaders customHeader = new HttpHeaders();
        customHeader.set(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        customHeader.set(HttpHeaders.REFERER, WECHAT_HOST);
        customHeader.set("Upgrade-Insecure-Requests", "1");
        HeaderUtils.assign(customHeader, getHeader);
        ResponseEntity<String> responseEntity
                = restTemplate.exchange(redirectUrl, HttpMethod.GET, new HttpEntity<>(customHeader), String.class);
        String xmlString = responseEntity.getBody();
        ObjectMapper xmlMapper = new XmlMapper();
        userCache.setRestTemplate(restTemplate);
        return xmlMapper.readValue(xmlString, Token.class);
    }

    /**
     * Redirect to main page of wechat
     *
     * @param hostUrl hostUrl
     */
    void redirect(String hostUrl, BaseUserCache userCache) {
        final String url = hostUrl + "/";
        HttpHeaders customHeader = new HttpHeaders();
        customHeader.set(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        customHeader.set(HttpHeaders.REFERER, WECHAT_HOST);
        customHeader.set("Upgrade-Insecure-Requests", "1");
        HeaderUtils.assign(customHeader, getHeader);
        userCache.getRestTemplate().exchange(url, HttpMethod.GET, new HttpEntity<>(customHeader), String.class);
    }

    /**
     * Initialization
     *
     * @param hostUrl   hostUrl
     * @param userCache userCache
     * @return current user's information and contact information
     * @throws IOException if the http response body can't be convert to {@link InitResponse}
     */
    InitResponse init(String hostUrl, BaseUserCache userCache) throws IOException {
        String url = String.format(WECHAT_URL_INIT, hostUrl, RandomUtils.generateDateWithBitwiseNot());
        CookieStore store = (CookieStore) ((StatefullRestTemplate) userCache.getRestTemplate()).getHttpContext().getAttribute(HttpClientContext.COOKIE_STORE);
        Date maxDate = new Date(Long.MAX_VALUE);
        String domain = hostUrl.replaceAll("https://", "").replaceAll("/", "");
        Map<String, String> cookies = new HashMap<>(3);
        cookies.put("MM_WX_NOTIFY_STATE", "1");
        cookies.put("MM_WX_SOUND_STATE", "1");
        appendAdditionalCookies(store, cookies, domain, "/", maxDate);
        InitRequest request = new InitRequest();
        request.setBaseRequest(userCache.getBaseRequest());
        HttpHeaders customHeader = new HttpHeaders();
        customHeader.set(HttpHeaders.REFERER, hostUrl + "/");
        customHeader.setOrigin(hostUrl);
        HeaderUtils.assign(customHeader, postHeader);
        ResponseEntity<String> responseEntity
                = userCache.getRestTemplate().exchange(url, HttpMethod.POST, new HttpEntity<>(request, customHeader), String.class);
        return jsonMapper.readValue(WechatUtils.textDecode(responseEntity.getBody()), InitResponse.class);
    }

    /**
     * Notify mobile side once certain actions have been taken on web side.
     *
     * @param hostUrl     hostUrl
     * @param baseRequest baseRequest
     * @param userName    the userName of the user
     * @param code
     * @return the http response body
     * @throws IOException if the http response body can't be convert to {@link StatusNotifyResponse}
     */
    StatusNotifyResponse statusNotify(String hostUrl, BaseRequest baseRequest, String userName, int code) throws IOException {
        String rnd = String.valueOf(System.currentTimeMillis());
        final String url = String.format(WECHAT_URL_STATUS_NOTIFY, hostUrl);
        StatusNotifyRequest request = new StatusNotifyRequest();
        request.setBaseRequest(baseRequest);
        request.setFromUserName(userName);
        request.setToUserName(userName);
        request.setCode(code);
        request.setClientMsgId(rnd);
        HttpHeaders customHeader = new HttpHeaders();
        customHeader.set(HttpHeaders.REFERER, hostUrl + "/");
        customHeader.setOrigin(hostUrl);
        HeaderUtils.assign(customHeader, postHeader);
        ResponseEntity<String> responseEntity
                = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(request, customHeader), String.class);
        return jsonMapper.readValue(WechatUtils.textDecode(responseEntity.getBody()), StatusNotifyResponse.class);
    }

    /**
     * report stats to server
     */
    void statReport(BaseUserCache userCache) {
        final String url = String.format(WECHAT_URL_STATREPORT, userCache.getWxHost());
        StatReportRequest request = new StatReportRequest();
        BaseRequest baseRequest = new BaseRequest();
        baseRequest.setUin("");
        baseRequest.setSid("");
        request.setBaseRequest(baseRequest);
        request.setCount(0);
        request.setList(new StatReport[0]);
        HttpHeaders customHeader = new HttpHeaders();
        customHeader.set(HttpHeaders.REFERER, userCache.getReferer());
        customHeader.setOrigin(userCache.getOrigin());
        HeaderUtils.assign(customHeader, postHeader);
        userCache.getRestTemplate().exchange(url, HttpMethod.POST, new HttpEntity<>(request, customHeader), String.class);
    }

    /**
     * Get all the contacts. If the Seq it returns is greater than zero, it means at least one more request is required to fetch all contacts.
     *
     * @return contact information
     * @throws IOException if the http response body can't be convert to {@link ContactResponse}
     */
    ContactResponse getContact(BaseUserCache userCache) throws IOException {
        long rnd = System.currentTimeMillis();
        final String url = String.format(WECHAT_URL_GET_CONTACT, userCache.getWxHost(), rnd, 1, escape(userCache.getsKey()));
        HttpHeaders customHeader = new HttpHeaders();
        customHeader.setAccept(Arrays.asList(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.ALL));
        customHeader.set(HttpHeaders.REFERER, userCache.getWxHost() + "/");
        HeaderUtils.assign(customHeader, getHeader);
        ResponseEntity<String> responseEntity = userCache.getRestTemplate().exchange(url, HttpMethod.GET, new HttpEntity<>(customHeader), String.class);
        return jsonMapper.readValue(WechatUtils.textDecode(responseEntity.getBody()), ContactResponse.class);
    }

    /**
     * @param userCache
     * @param list
     * @return
     * @throws IOException
     */
    BatchGetContactResponse batchGetContact(BaseUserCache userCache, ChatRoomDescription[] list) throws IOException {
        long rnd = System.currentTimeMillis();
        String url = String.format(WECHAT_URL_BATCH_GET_CONTACT, userCache.getWxHost(), rnd);
        BatchGetContactRequest request = new BatchGetContactRequest();
        request.setBaseRequest(userCache.getBaseRequest());
        request.setCount(list.length);
        request.setList(list);
        HttpHeaders customHeader = createPostCustomHeader(userCache);
        HeaderUtils.assign(customHeader, postHeader);
        ResponseEntity<String> responseEntity = userCache.getRestTemplate().exchange(url, HttpMethod.POST, new HttpEntity<>(request, customHeader), String.class);
        return jsonMapper.readValue(WechatUtils.textDecode(responseEntity.getBody()), BatchGetContactResponse.class);
    }

    /**
     * 轮训消息
     *
     * @param baseUserCache
     * @return
     * @throws IOException
     * @throws URISyntaxException
     */
    SyncCheckResponse syncCheck(BaseUserCache baseUserCache) throws IOException, URISyntaxException {
        String url = baseUserCache.getWxHost().replaceAll("https://", "");
        URIBuilder builder = new URIBuilder(String.format(WECHAT_URL_SYNC_CHECK, url));
        builder.addParameter("uin", baseUserCache.getUin());
        builder.addParameter("sid", baseUserCache.getSid());
        builder.addParameter("skey", baseUserCache.getsKey());
        builder.addParameter("deviceid", DeviceIdGenerator.generate());
        builder.addParameter("synckey", baseUserCache.getSyncKey().toString());
        builder.addParameter("r", String.valueOf(System.currentTimeMillis()));
        builder.addParameter("_", String.valueOf(System.currentTimeMillis()));
        final URI uri = builder.build().toURL().toURI();
        HttpHeaders customHeader = new HttpHeaders();
        customHeader.setAccept(Arrays.asList(MediaType.ALL));
        customHeader.set(HttpHeaders.REFERER, baseUserCache.getReferer());
        HeaderUtils.assign(customHeader, getHeader);

        ResponseEntity<String> responseEntity = null;
        try {
            responseEntity = baseUserCache.getRestTemplate().exchange(uri, HttpMethod.GET, new HttpEntity<>(customHeader), String.class);
        } catch (Exception e) {
            logger.error("{}", e);
        }
        logger.debug("resttemplate : {}", ((StatefullRestTemplate) baseUserCache.getRestTemplate()).getHttpContext().getAttribute(HttpClientContext.COOKIE_STORE));
        if (responseEntity == null || !HttpStatus.OK.equals(responseEntity.getStatusCode())) {
            return null;
        }
        String body = responseEntity.getBody();
        final Pattern pattern = Pattern.compile("window.synccheck=\\{retcode:\"(\\d+)\",selector:\"(\\d+)\"\\}");
        Matcher matcher = pattern.matcher(body);
        if (!matcher.find()) {
            return null;
        } else {
            SyncCheckResponse result = new SyncCheckResponse();
            result.setRetcode(Integer.valueOf(matcher.group(1)));
            result.setSelector(Integer.valueOf(matcher.group(2)));
            return result;
        }
    }

    /**
     * Sync with server to get new messages and contacts
     *
     * @param baseUserCache
     * @return
     * @throws IOException
     */
    SyncResponse sync(BaseUserCache baseUserCache) throws IOException {
        final String url = String.format(WECHAT_URL_SYNC, baseUserCache.getWxHost(), baseUserCache.getSid(), escape(baseUserCache.getsKey()));
        SyncRequest request = new SyncRequest();
        request.setBaseRequest(baseUserCache.getBaseRequest());
        request.setRr(-System.currentTimeMillis() / 1000);
        request.setSyncKey(baseUserCache.getSyncKey());
        HttpHeaders customHeader = createPostCustomHeader(baseUserCache);
        HeaderUtils.assign(customHeader, postHeader);
        ResponseEntity<String> responseEntity
                = baseUserCache.getRestTemplate().exchange(url, HttpMethod.POST, new HttpEntity<>(request, customHeader), String.class);
        return jsonMapper.readValue(WechatUtils.textDecode(responseEntity.getBody()), SyncResponse.class);
    }

    /**
     * @param baseUserCache
     * @param verifyUsers
     * @return
     * @throws IOException
     * @throws URISyntaxException
     */
    VerifyUserResponse acceptFriend(BaseUserCache baseUserCache, VerifyUser[] verifyUsers) throws IOException, URISyntaxException {
        final int opCode = VerifyUserOPCode.VERIFYOK.getCode();
        final int[] sceneList = new int[]{AddScene.WEB.getCode()};
        final String path = String.format(WECHAT_URL_VERIFY_USER);
        VerifyUserRequest request = new VerifyUserRequest();
        request.setBaseRequest(baseUserCache.getBaseRequest());
        request.setOpcode(opCode);
        request.setSceneList(sceneList);
        request.setSceneListCount(sceneList.length);
        request.setSkey(baseUserCache.getsKey());
        request.setVerifyContent("");
        request.setVerifyUserList(verifyUsers);
        request.setVerifyUserListSize(verifyUsers.length);

        URIBuilder builder = new URIBuilder(path);
        builder.addParameter("r", String.valueOf(System.currentTimeMillis()));
        builder.addParameter("pass_ticket", baseUserCache.getPassTicket());
        final URI uri = builder.build().toURL().toURI();

        ResponseEntity<String> responseEntity
                = baseUserCache.getRestTemplate().exchange(uri, HttpMethod.POST, new HttpEntity<>(request, this.postHeader), String.class);
        return jsonMapper.readValue(WechatUtils.textDecode(responseEntity.getBody()), VerifyUserResponse.class);
    }

    SendMsgResponse sendText(BaseUserCache userCache, String content, String toUserName) throws IOException {
        final int scene = 0;
        final String rnd = String.valueOf(System.currentTimeMillis() * 10);
        final String url = String.format(WECHAT_URL_SEND_MSG, userCache.getWxHost());
        SendMsgRequest request = new SendMsgRequest();
        request.setBaseRequest(userCache.getBaseRequest());
        request.setScene(scene);
        BaseMsg msg = new BaseMsg();
        msg.setType(MessageType.TEXT.getCode());
        msg.setClientMsgId(rnd);
        msg.setContent(content);
        msg.setFromUserName(userCache.getOwner().getUserName());
        msg.setToUserName(toUserName);
        msg.setLocalID(rnd);
        request.setMsg(msg);
        HttpHeaders customHeader = createPostCustomHeader(userCache);
        HeaderUtils.assign(customHeader, postHeader);
        ResponseEntity<String> responseEntity = userCache.getRestTemplate().exchange(url, HttpMethod.POST, new HttpEntity<>(request, customHeader), String.class);
        return jsonMapper.readValue(WechatUtils.textDecode(responseEntity.getBody()), SendMsgResponse.class);
    }

    void revoke(BaseUserCache userCache, String toUserName, String messageId) throws IOException {
        final String rnd = String.valueOf(System.currentTimeMillis() * 10);
        final String url = String.format(WECHAT_URL_REVOKE_MSG, userCache.getWxHost());
        RevokeRequst request = new RevokeRequst();

        request.setToUserName(toUserName);
        request.setClientMsgId(messageId);
        request.setSvrMsgId(messageId);
        request.setBaseRequest(userCache.getBaseRequest());
        HttpHeaders customHeader = createPostCustomHeader(userCache);
        HeaderUtils.assign(customHeader, postHeader);
        ResponseEntity<String> responseEntity = userCache.getRestTemplate().exchange(url, HttpMethod.POST, new HttpEntity<>(request, customHeader), String.class);
        logger.info("撤销消息 {} , {}", messageId, responseEntity.getStatusCode());
    }

    OpLogResponse setAlias(BaseUserCache userCache, String newAlias, String userName) throws IOException {
        final int cmdId = OpLogCmdId.MODREMARKNAME.getCode();
        final String url = String.format(WECHAT_URL_OP_LOG, userCache.getWxHost());
        OpLogRequest request = new OpLogRequest();
        request.setBaseRequest(userCache.getBaseRequest());
        request.setCmdId(cmdId);
        request.setRemarkName(newAlias);
        request.setUserName(userName);
        HttpHeaders customHeader = createPostCustomHeader(userCache);
        HeaderUtils.assign(customHeader, postHeader);
        ResponseEntity<String> responseEntity
                = userCache.getRestTemplate().exchange(url, HttpMethod.POST, new HttpEntity<>(request, customHeader), String.class);
        return jsonMapper.readValue(WechatUtils.textDecode(responseEntity.getBody()), OpLogResponse.class);
    }

    CreateChatRoomResponse createChatRoom(BaseUserCache userCache, String[] userNames, String topic) throws IOException {
        String rnd = String.valueOf(System.currentTimeMillis());
        final String url = String.format(WECHAT_URL_CREATE_CHATROOM, userCache.getWxHost(), rnd);
        CreateChatRoomRequest request = new CreateChatRoomRequest();
        request.setBaseRequest(userCache.getBaseRequest());
        request.setMemberCount(userNames.length);
        ChatRoomMember[] members = new ChatRoomMember[userNames.length];
        for (int i = 0; i < userNames.length; i++) {
            members[i] = new ChatRoomMember();
            members[i].setUserName(userNames[i]);
        }
        request.setMemberList(members);
        request.setTopic(topic);
        HttpHeaders customHeader = createPostCustomHeader(userCache);
        HeaderUtils.assign(customHeader, postHeader);
        ResponseEntity<String> responseEntity
                = userCache.getRestTemplate().exchange(url, HttpMethod.POST, new HttpEntity<>(request, customHeader), String.class);
        return jsonMapper.readValue(WechatUtils.textDecode(responseEntity.getBody()), CreateChatRoomResponse.class);
    }

    DeleteChatRoomMemberResponse deleteChatRoomMember(BaseUserCache userCache, String chatRoomUserName, String userName) throws IOException {
        final String url = String.format(WECHAT_URL_DELETE_CHATROOM_MEMBER, userCache.getWxHost());
        DeleteChatRoomMemberRequest request = new DeleteChatRoomMemberRequest();
        request.setBaseRequest(userCache.getBaseRequest());
        request.setChatRoomName(chatRoomUserName);
        request.setDelMemberList(userName);
        HttpHeaders customHeader = createPostCustomHeader(userCache);
        HeaderUtils.assign(customHeader, postHeader);
        ResponseEntity<String> responseEntity
                = userCache.getRestTemplate().exchange(url, HttpMethod.POST, new HttpEntity<>(request, customHeader), String.class);
        return jsonMapper.readValue(WechatUtils.textDecode(responseEntity.getBody()), DeleteChatRoomMemberResponse.class);
    }

    AddChatRoomMemberResponse addChatRoomMember(BaseUserCache userCache, String chatRoomUserName, String userName) throws IOException {
        final String url = String.format(WECHAT_URL_ADD_CHATROOM_MEMBER, userCache.getWxHost());
        AddChatRoomMemberRequest request = new AddChatRoomMemberRequest();
        request.setBaseRequest(userCache.getBaseRequest());
        request.setChatRoomName(chatRoomUserName);
        request.setAddMemberList(userName);
        HttpHeaders customHeader = createPostCustomHeader(userCache);
        HeaderUtils.assign(customHeader, postHeader);
        ResponseEntity<String> responseEntity
                = userCache.getRestTemplate().exchange(url, HttpMethod.POST, new HttpEntity<>(request, customHeader), String.class);
        return jsonMapper.readValue(WechatUtils.textDecode(responseEntity.getBody()), AddChatRoomMemberResponse.class);
    }

    byte[] downloadImage(BaseUserCache userCache, String url) {
        HttpHeaders customHeader = new HttpHeaders();
        customHeader.set("Accept", "image/webp,image/apng,image/*,*/*;q=0.8");
        customHeader.set("Referer", userCache.getReferer());
        HeaderUtils.assign(customHeader, getHeader);
        ResponseEntity<byte[]> responseEntity
                = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(customHeader), new ParameterizedTypeReference<byte[]>() {
        });
        return responseEntity.getBody();
    }

    private String escape(String str) throws IOException {
        return URLEncoder.encode(str, StandardCharsets.UTF_8.toString());
    }

    private void appendAdditionalCookies(CookieStore store, Map<String, String> cookies, String domain, String path, Date expiryDate) {
        cookies.forEach((key, value) -> {
            BasicClientCookie cookie = new BasicClientCookie(key, value);
            cookie.setDomain(domain);
            cookie.setPath(path);
            cookie.setExpiryDate(expiryDate);
            store.addCookie(cookie);
        });
    }

    private HttpHeaders createPostCustomHeader(BaseUserCache userCache) {
        HttpHeaders customHeader = new HttpHeaders();
        customHeader.setOrigin(userCache.getOrigin());
        customHeader.set(HttpHeaders.REFERER, userCache.getReferer());
        return customHeader;
    }
}