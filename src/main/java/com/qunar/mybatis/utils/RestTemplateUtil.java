package com.qunar.mybatis.utils;

import com.google.common.collect.Sets;
import com.qunar.flight.qmonitor.QMonitor;
import com.qunar.flight.userproduct.gaea.log4track.LoggerFactoryProxy;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.http.cookie.Cookie;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Component
public class RestTemplateUtil {

    private Logger logger = LoggerFactoryProxy.getLogger(RestTemplateUtil.class);

    @Autowired
    private RestTemplate restTemplate;

    public <R> R get(Class<R> clazz, final String url, final String cactiUrlTitle) {
        int status = -1;
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        R body = null;
        try {
            ResponseEntity<R> resp = restTemplate.getForEntity(url, clazz);
            status = resp.getStatusCodeValue();
            body = resp.getBody();
        } catch (RestClientException e) {
            logger.error("{}####{}",url, status,  e);
            throw e;
        } finally {
            stopWatch.stop();
            if (status != 200) {
                QMonitor.recordOne(cactiUrlTitle + "_Fail");
            } else {
                QMonitor.recordQuantile(cactiUrlTitle, stopWatch.getTime());
            }
            if (status == -1) {
                QMonitor.recordOne(cactiUrlTitle + "_Timeout");
            }
            logger.info("{}####{}####{}####{}",url, status, tranBodyToString(body), stopWatch.getTime());
        }
        return body;
    }

    public <R> R post(Class<R> clazz, final String url, LinkedMultiValueMap<String,String> params, List<Cookie> cookies, final String cactiUrlTitle) {
        HttpHeaders headers = getDefaultHeader(cookies, false, false);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return doPost(clazz, url, request, cactiUrlTitle);
    }

    public <R> R postBean(Class<R> clazz, final String url, @Nullable Object param, List<Cookie> cookies, final String cactiUrlTitle) {
        return post(clazz, url, convertToHttpParams(param), cookies, cactiUrlTitle);
    }

    public <R> R postJson(Class<R> clazz, final String url, String jsonParam, List<Cookie> cookies, final String cactiUrlTitle) {
        HttpHeaders headers = getDefaultHeader(cookies, false, true);
        HttpEntity<String> request = new HttpEntity<>(jsonParam, headers);
        return doPost(clazz, url, request, cactiUrlTitle);
    }

    /**
     * 目前发现只有车车起步价接口必须指定accept才能避免乱码，其他接口正常。
     * 因此，一般接口不要使用该方法。
     * @param clazz
     * @param url
     * @param params
     * @param cookies
     * @param cactiUrlTitle
     * @param <R>
     * @return
     */
    public <R> R postWithAccept(Class<R> clazz, final String url, LinkedMultiValueMap<String,String> params, List<Cookie> cookies, final String cactiUrlTitle) {
        HttpHeaders headers = getDefaultHeader(cookies,true, false);
        //设置header
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return doPost(clazz, url, request, cactiUrlTitle);
    }

    private <R> R doPost(Class<R> clazz, final String url, HttpEntity request, final String cactiUrlTitle) {
        int status = -1;
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        String body = null;
        try {
            ResponseEntity<String> resp = restTemplate.postForEntity(url, request, String.class);
            status = resp.getStatusCodeValue();
            body = resp.getBody();
        } catch (RestClientException e) {
            logger.error("{}####{}####{}", url, parsePostParams(request), status, e);
            throw e;
        } finally {
            stopWatch.stop();
            if (status != 200) {
                QMonitor.recordOne(cactiUrlTitle + "_Fail");
            } else {
                QMonitor.recordQuantile(cactiUrlTitle, stopWatch.getTime());
            }
            if (status == -1) {
                QMonitor.recordOne(cactiUrlTitle + "_Timeout");
            }
            logger.info("{}####{}####{}####{}####{}",url, parsePostParams(request), status, body, stopWatch.getTime());
        }
        if (clazz == String.class) {
            return (R)body;
        }
        return JsonUtils.jsonToObject(body,clazz);
    }

    private String parsePostParams(HttpEntity entity) {
        if (entity == null) {
            return "{}";
        }
        Object body = entity.getBody();
        if (body instanceof String) {
            return (String)body;
        }
        return JsonUtils.objectToJson(body);
    }

    private HttpHeaders getDefaultHeader(List<Cookie> cookies, boolean needAccept, boolean needContentType) {
        HttpHeaders headers = new HttpHeaders();
        //设置cookie
        if (CollectionUtils.isNotEmpty(cookies)) {
            headers.add(HttpHeaders.COOKIE, buildCookieStr(cookies));
        }
        if (needAccept) {
            headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_UTF8_VALUE);
        }
        if (needContentType) {
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE);
        }
        return headers;
    }

    private String buildCookieStr(List<Cookie> cookies) {
        StringBuilder cookieStr = new StringBuilder();
        if (cookies != null && cookies.size()!=0) {
            for (Cookie cookie : cookies) {
                cookieStr.append(cookie.getName()).append("=").append(cookie.getValue()).append(";");
            }
            cookieStr.deleteCharAt(cookieStr.length()-1);
        }
        return cookieStr.toString();
    }

    private String tranBodyToString(Object body) {
        if (body == null) {
            return "";
        }
        if (body instanceof String) {
            return (String)body;
        }
        return JsonUtils.objectToJson(body);
    }

    public LinkedMultiValueMap<String,String> convertToHttpParams(Object object) {
        if (object == null) {
            return null;
        }
        if (object instanceof LinkedMultiValueMap) {
            return (LinkedMultiValueMap<String,String>)object;
        }
        LinkedMultiValueMap<String,String> params = new LinkedMultiValueMap<>();
        addParam(params,object);
        return params;
    }

    /**
     * object中的字段类型有限制：必须是字符串，原始类型，容器类或者普通POJO。而普通POJO内的字段也需要遵从该限制。
     * @param params
     * @param object
     */
    private void addParam(LinkedMultiValueMap<String,String> params, Object object) {
        try {
            Field[] fields = object.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(object);
                if (value!=null) {
                    if (value.getClass().isPrimitive()) {
                        //原始类型
                        params.add(field.getName(),String.valueOf(value));
                    } else if (value instanceof String) {
                        //字符串
                        params.add(field.getName(),(String)value);
                    } else if (primitiveWrapperClass.contains(value.getClass())) {
                        //原始类型包装类
                        params.add(field.getName(),String.valueOf(value));
                    } else if (value instanceof Collection) {
                        //容器类
                        for (Object o : ((Collection) value)) {
                            addParam(params,o);
                        }
                    } else {
                        //普通对象
                        addParam(params,value);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            logger.error(e.getMessage(),e);
        }
    }

    private static final Set<Class> primitiveWrapperClass = Sets.newHashSet(Byte.class,Short.class,Integer.class,Long.class,Float.class,Double.class,Boolean.class,Character.class);
}
