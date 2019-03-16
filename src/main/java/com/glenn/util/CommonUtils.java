package com.glenn.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class CommonUtils {

    public static String convertObjectToJSONString(Object object) {
        ObjectMapper mapper = new ObjectMapper();
        String jsonObject = null;
        try {
            jsonObject = mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public static <T> T convertJsonStringToObject(String jsonString, Class<T> object) {
        ObjectMapper mapper = new ObjectMapper();
        T jsonObject = null;
        try {
            jsonObject = mapper.readValue(jsonString, object);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

}
