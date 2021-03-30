package com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.cart.redis.hook;

import com.sap.cxservices.rediscart.model.RedisCartMetadataModel;
import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.Entity;

public interface RedisCartMetadataUpdateHook {

    boolean isMetadataUpdateRequired(Entity modifiedCart, Entity persistedCart);

    void updateMetadata(RedisCartMetadataModel metadata, Entity modifiedCart);

}
