package com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.cart.redis.hook;

import de.hybris.platform.persistence.polyglot.model.Identity;

import java.util.Map;

public interface RedisCartMetadataSearchHook {

    void addParameter(Map<String, Object> parameters, String composedTypeCode, Identity identity);

}
