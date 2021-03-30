package com.sap.cxservices.rediscart.servicelayer.order.dao.impl;

import com.sap.cxservices.rediscart.model.RedisCartMetadataModel;
import de.hybris.platform.commerceservices.order.dao.impl.DefaultCommerceCartDao;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.servicelayer.config.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

public class RedisCommerceCartDao extends DefaultCommerceCartDao {

    private static final String CART_FOR_PK = SELECTCLAUSE + "WHERE {" + CartModel.PK + "}=?pk";

    @Autowired
    private ConfigurationService configurationService;

    @Override
    protected <T> List<T> doSearch(String query, Map<String, Object> params, Class<T> resultClass) {

        if(isRedisCartDisabled()) {
            return super.doSearch(query, params, resultClass);
        }else{
            query = query.replaceAll(CartModel._TYPECODE, RedisCartMetadataModel._TYPECODE);
            List<RedisCartMetadataModel> metadataList = super.doSearch(query, params, RedisCartMetadataModel.class);
            return getCartsForMetadataList(metadataList, resultClass);
        }
    }

    protected <T> List<T> getCartsForMetadataList(List<RedisCartMetadataModel> metadataList, Class<T> resultClass){
        List<T> carts = new ArrayList<>();
        for(RedisCartMetadataModel metadata: metadataList){
            carts.addAll(getCartsForMetadata(metadata, resultClass));
        }
        return carts;
    }

    private <T> List<T> getCartsForMetadata(RedisCartMetadataModel metadata, Class<T> resultClass){
        if(null!=metadata){
            final Map<String, Object> params = new HashMap<>();
            params.put("pk", metadata.getCartPk());
            return super.doSearch(CART_FOR_PK, params, resultClass);
        }
        return Collections.emptyList();
    }

    @Override
    protected <T> List<T> doSearch(String query, Map<String, Object> params, Class<T> resultClass, int count) {
        if(isRedisCartDisabled()) {
            return super.doSearch(query, params, resultClass, count);
        }else{
            query = query.replaceAll(CartModel._TYPECODE, RedisCartMetadataModel._TYPECODE);
            List<RedisCartMetadataModel> metadataList = super.doSearch(query, params, RedisCartMetadataModel.class, count);
            return getCartsForMetadataList(metadataList, resultClass);
        }
    }

    protected boolean isRedisCartDisabled() {
        return !configurationService.getConfiguration().getBoolean("rediscart.enabled", true);
    }

}
