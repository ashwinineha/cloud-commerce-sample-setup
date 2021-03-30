package com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.cart.redis.hook.impl;

import com.sap.cxservices.rediscart.model.RedisCartMetadataModel;
import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.Document;
import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.Entity;
import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.cart.CartAttributes;
import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.cart.redis.hook.RedisCartMetadataUpdateHook;
import de.hybris.platform.core.PK;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.persistence.polyglot.model.Identity;
import de.hybris.platform.persistence.polyglot.model.Reference;
import de.hybris.platform.servicelayer.model.ModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class UserRedisCartMetadataUpdateHook implements RedisCartMetadataUpdateHook {
    private static final Logger LOG = LoggerFactory.getLogger(UserRedisCartMetadataUpdateHook.class);

    private ModelService modelService;

    @Override
    public boolean isMetadataUpdateRequired(Entity persistedCart, Entity modifiedCart) {
        Reference persistedUser = persistedCart.get(CartAttributes.user());
        Reference modifiedUser = modifiedCart.get(CartAttributes.user());
        LOG.debug("Persisted user {}, modified user {}", persistedUser, modifiedUser);
        return !Objects.equals(modifiedUser, persistedUser);
    }

    @Override
    public void updateMetadata(RedisCartMetadataModel metadata, Entity modifiedCart) {
        Reference modifiedUser = modifiedCart.get(CartAttributes.user());
        UserModel user = modelService.get(PK.fromLong(modifiedUser.getIdentity().toLongValue()));
        metadata.setUser(user);
    }

    public void setModelService(ModelService modelService) {
        this.modelService = modelService;
    }
}
