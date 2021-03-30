package com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RedisCartRecordUtils {

    private static final Logger LOG = LoggerFactory.getLogger(RedisCartRecordUtils.class);

    public static String addValue(String json, String value) {
        List<String> entities = new ArrayList<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            if (null == json) {
                entities.add(value);
            } else {
                entities = mapper.readValue(json, ArrayList.class);
                if (entities.contains(value)) {
                    return json;
                } else {
                    entities.add(value);
                }
            }
            json = mapper.writeValueAsString(entities);
        } catch (JsonProcessingException e) {
            LOG.debug("Cannot process the JSON conversion.", e);
        }
        return json;
    }

    public static String removeValue(String json, String value) {
        List<String> entities;
        try {
            ObjectMapper mapper = new ObjectMapper();
            if (null == json) {
                return null;
            } else {
                entities = mapper.readValue(json, ArrayList.class);
                if (entities.contains(value)) {
                    entities.remove(value);
                } else {
                    return json;
                }
            }
            json = mapper.writeValueAsString(entities);
        } catch (JsonProcessingException e) {
            LOG.debug("Cannot process the JSON conversion.", e);
        }
        return json;
    }

    public static List<String> getValuesFromJSON(String json){
        if(null!=json){
            try {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(json, ArrayList.class);
            }catch (JsonProcessingException e) {
                LOG.debug("Cannot process the JSON conversion.", e);
            }
        }
        return Collections.emptyList();
    }

}
