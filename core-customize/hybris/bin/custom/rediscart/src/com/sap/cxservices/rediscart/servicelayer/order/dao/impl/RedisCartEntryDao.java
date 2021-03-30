package com.sap.cxservices.rediscart.servicelayer.order.dao.impl;

import de.hybris.platform.commerceservices.order.dao.impl.DefaultCartEntryDao;
import de.hybris.platform.core.model.order.CartEntryModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.servicelayer.search.SearchResult;
import de.hybris.platform.storelocator.model.PointOfServiceModel;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

public class RedisCartEntryDao extends DefaultCartEntryDao {
    @Override
    public List<CartEntryModel> findEntriesByProductAndPointOfService(final CartModel cart, final ProductModel product,
                                                                      final PointOfServiceModel pointOfService)
    {
        validateParameterNotNull(cart, "cart must not be null");
        validateParameterNotNull(product, "product must not be null");

        List<CartEntryModel> cartEntries = CollectionUtils.emptyIfNull(cart.getEntries()).stream().map(entry -> (CartEntryModel) entry).filter(entry -> product.equals(entry.getProduct())).collect(Collectors.toList());


        if (pointOfService == null)
        {
            cartEntries = cartEntries.stream().filter(entry -> null == entry.getDeliveryPointOfService()).collect(Collectors.toList());
        }
        else
        {
            cartEntries = cartEntries.stream().filter(entry -> pointOfService.equals(entry.getDeliveryPointOfService())).collect(Collectors.toList());
        }

        return cartEntries;
    }
}
