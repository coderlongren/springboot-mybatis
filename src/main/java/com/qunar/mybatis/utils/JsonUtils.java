package com.qunar.mybatis.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qunar.flight.userproduct.gaea.log4track.LoggerFactoryProxy;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import qunar.api.json.JsonFeature;
import qunar.api.json.JsonMapper;
import qunar.api.json.MapperBuilder;

/**
 * Created by zhangbin on 2017/11/2.
 *
 * @author kevin.zhang
 */
public class JsonUtils {

    private static Logger logger = LoggerFactoryProxy.getLogger(JsonUtils.class);

    private static JsonMapper jsonMapper = MapperBuilder.create().configure(JsonFeature.INCLUSION_NOT_NULL, true).build();

    /**
     * jsonStr -> object
     *
     * @param json
     * @param type
     * @param <T>
     * @return
     */
    public static <T> T jsonToObject(String json, Class<T> type) {
        if (StringUtils.isEmpty(json)){
            return null;
        }
        try {
            return jsonMapper.readValue(json, type);
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
        }
        return null;
    }

    public static <T> T jsonToObject(String jsonString, TypeReference<T> type) {
        if (StringUtils.isEmpty(jsonString)) {
            return null;
        } else {
            try {
                return jsonMapper.readValue(jsonString, type);
            } catch (Exception e) {
                logger.error(e.getMessage(),e);
                return null;
            }
        }
    }

    /**
     * 对象 -> jsonStr
     *
     * @param obj
     * @return
     */
    public static String objectToJson(Object obj) {
        if (obj == null) {
            return "";
        }
        return jsonMapper.writeValueAsString(obj);
    }

    public static boolean isJson(String theString) {
        if (StringUtils.isBlank(theString)) {
            return false;
        }
        String trim = theString.trim();
        if ((trim.charAt(0) == '{' && trim.charAt(trim.length() - 1) == '}')) {

            return true;
        }
        return false;
    }

    public static boolean isJsonArray(String theString) {
        if(StringUtils.isBlank(theString)){
            return false;
        }
        String trim = theString.trim();
        if((trim.charAt(0)=='[' && theString.charAt(trim.length()-1)==']')){

            return true;
        }
        return false;
    }

}
