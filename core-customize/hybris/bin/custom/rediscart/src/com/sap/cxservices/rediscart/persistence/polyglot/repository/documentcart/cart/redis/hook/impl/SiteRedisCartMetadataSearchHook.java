package com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.cart.redis.hook.impl;

import com.sap.cxservices.rediscart.model.RedisCartMetadataModel;
import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.cart.redis.hook.RedisCartMetadataSearchHook;
import de.hybris.platform.basecommerce.model.site.BaseSiteModel;
import de.hybris.platform.core.PK;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.persistence.polyglot.model.Identity;
import de.hybris.platform.servicelayer.model.ModelService;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class SiteRedisCartMetadataSearchHook implements RedisCartMetadataSearchHook {

    private ModelService modelService;

    @Override
    public void addParameter(Map<String, Object> parameters, String composedTypeCode, Identity identity) {
        if (StringUtils.equalsIgnoreCase(BaseSiteModel._TYPECODE, composedTypeCode)) {
            BaseSiteModel site = modelService.get(PK.fromLong(identity.toLongValue()));
            parameters.put(RedisCartMetadataModel.SITE, site);
        }
    }

    public void setModelService(ModelService modelService) {
        this.modelService = modelService;
    }
}
