package com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.cart.redis.hook.impl;

import com.sap.cxservices.rediscart.model.RedisCartMetadataModel;
import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.Entity;
import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.cart.CartAttributes;
import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.cart.redis.hook.RedisCartMetadataUpdateHook;
import de.hybris.platform.basecommerce.model.site.BaseSiteModel;
import de.hybris.platform.core.PK;
import de.hybris.platform.persistence.polyglot.model.Identity;
import de.hybris.platform.persistence.polyglot.model.Reference;
import de.hybris.platform.servicelayer.model.ModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class SiteRedisCartMetadataUpdateHook implements RedisCartMetadataUpdateHook {
    private static final Logger LOG = LoggerFactory.getLogger(SiteRedisCartMetadataUpdateHook.class);

    private ModelService modelService;

    @Override
    public boolean isMetadataUpdateRequired(Entity persistedCart, Entity modifiedCart) {
        Reference persistedSite = persistedCart.get(CartAttributes.site());
        Reference modifiedSite = modifiedCart.get(CartAttributes.site());
        LOG.debug("Persisted site {}, modified site {}", persistedSite, modifiedSite);
        return !Objects.equals(persistedSite, modifiedSite);
    }

    @Override
    public void updateMetadata(RedisCartMetadataModel metadata, Entity modifiedCart) {
        Reference modifiedSite = modifiedCart.get(CartAttributes.site());
        BaseSiteModel site = modelService.get(PK.fromLong(modifiedSite.getIdentity().toLongValue()));
        metadata.setSite(site);
    }

    public void setModelService(ModelService modelService) {
        this.modelService = modelService;
    }
}
