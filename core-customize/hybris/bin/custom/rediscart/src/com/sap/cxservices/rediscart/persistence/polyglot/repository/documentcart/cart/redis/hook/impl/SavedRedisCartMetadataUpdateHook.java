package com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.cart.redis.hook.impl;

import com.sap.cxservices.rediscart.model.RedisCartMetadataModel;
import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.Entity;
import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.cart.CartAttributes;
import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.cart.redis.hook.RedisCartMetadataUpdateHook;
import de.hybris.platform.core.PK;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.persistence.polyglot.PolyglotPersistence;
import de.hybris.platform.persistence.polyglot.model.Identity;
import de.hybris.platform.persistence.polyglot.model.Reference;
import de.hybris.platform.persistence.polyglot.model.SingleAttributeKey;
import de.hybris.platform.servicelayer.exceptions.ModelNotFoundException;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.user.UserService;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Objects;

public class SavedRedisCartMetadataUpdateHook implements RedisCartMetadataUpdateHook {

    private static final Logger LOG = LoggerFactory.getLogger(SavedRedisCartMetadataUpdateHook.class);

    private static final SingleAttributeKey SAVE_TIME_KEY = PolyglotPersistence.getNonlocalizedKey("saveTime");
    private static final SingleAttributeKey SAVED_BY_KEY = PolyglotPersistence.getNonlocalizedKey("savedBy");
    private static final SingleAttributeKey EXPIRATION_TIME_KEY = PolyglotPersistence.getNonlocalizedKey("expirationTime");
    private static final SingleAttributeKey VISIBLE_KEY = PolyglotPersistence.getNonlocalizedKey("visible");

    private ModelService modelService;

    @Override
    public boolean isMetadataUpdateRequired(Entity persistedCart, Entity modifiedCart) {
        Reference persistedSavedByUser = persistedCart.get(SAVED_BY_KEY);
        Reference modifiedSavedByUser = modifiedCart.get(SAVED_BY_KEY);
        LOG.debug("Persisted savedBy key {}, modified savedBy key{}", persistedSavedByUser, modifiedSavedByUser);
        Date persistedSaveTime = persistedCart.get(SAVE_TIME_KEY);
        Date modifiedSaveTime = modifiedCart.get(SAVE_TIME_KEY);
        LOG.debug("Persisted saveTime {}, modified saveTime {}}", persistedSaveTime, modifiedSaveTime);
        Date persistedExpirationTime = persistedCart.get(EXPIRATION_TIME_KEY);
        Date modifiedExpirationTime = modifiedCart.get(EXPIRATION_TIME_KEY);
        LOG.debug("Persisted expirationTime {}, modified expirationTime {}}", persistedExpirationTime, modifiedExpirationTime);
        Boolean persistedVisible = persistedCart.get(VISIBLE_KEY);
        Boolean modifiedVisible = modifiedCart.get(VISIBLE_KEY);
        LOG.debug("Persisted visible {}, modified visible {}}", persistedVisible, modifiedVisible);
        return !Objects.equals(persistedSavedByUser, modifiedSavedByUser)
                || !Objects.equals(persistedSaveTime, modifiedSaveTime)
                || !Objects.equals(persistedExpirationTime, modifiedExpirationTime)
                || !Objects.equals(persistedVisible, modifiedVisible);
    }

    @Override
    public void updateMetadata(RedisCartMetadataModel metadata, Entity modifiedCart) {
        Reference modifiedSavedByUserPk = modifiedCart.get(SAVED_BY_KEY);
        if (null == modifiedSavedByUserPk) {
            LOG.debug("Modified saved By is null for cart");
            metadata.setSavedBy(null);
        } else {
            try {
                metadata.setSavedBy(modelService.get(PK.fromLong(modifiedSavedByUserPk.getIdentity().toLongValue())));
            } catch (ModelNotFoundException ex) {
                LOG.debug("User with PK not found.", ex);
            }
        }
        metadata.setSaveTime(modifiedCart.get(SAVE_TIME_KEY));
        metadata.setExpirationTime(modifiedCart.get(EXPIRATION_TIME_KEY));
        metadata.setVisible(BooleanUtils.isTrue(modifiedCart.get(VISIBLE_KEY)));
    }


    public void setModelService(ModelService modelService) {
        this.modelService = modelService;
    }
}
