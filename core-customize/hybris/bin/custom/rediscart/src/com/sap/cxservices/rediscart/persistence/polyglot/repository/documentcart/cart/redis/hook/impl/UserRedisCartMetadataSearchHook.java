package com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.cart.redis.hook.impl;

import com.sap.cxservices.rediscart.model.RedisCartMetadataModel;
import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.cart.redis.hook.RedisCartMetadataSearchHook;
import de.hybris.platform.core.PK;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.persistence.polyglot.model.Identity;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.user.UserService;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class UserRedisCartMetadataSearchHook implements RedisCartMetadataSearchHook {

    private ModelService modelService;

    @Override
    public void addParameter(Map<String, Object> parameters, String composedTypeCode, Identity identity) {
        if (StringUtils.equalsIgnoreCase(CustomerModel._TYPECODE, composedTypeCode) || StringUtils.equalsIgnoreCase(UserModel._TYPECODE, composedTypeCode)) {
            UserModel user = modelService.get(PK.fromLong(identity.toLongValue()));
            parameters.put(RedisCartMetadataModel.USER, user);
        }
    }

    public void setModelService(ModelService modelService) {
        this.modelService = modelService;
    }
}
