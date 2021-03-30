/*
 * [y] SAP Commerce Platform
 *
 *  Copyright (c) 2000-2020 SAP SE. All rights reserved.
 *
 *  This software is the confidential and proprietary information of SAP
 *  Customer Experience ("Confidential Information"). You shall not disclose such
 *  Confidential Information and shall use it only in accordance with the
 *  terms of the license agreement you entered into with SAP Customer Experience.
 */

package com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.cart.redis.helper.impl;

import com.sap.cxservices.rediscart.model.RedisCartMetadataModel;
import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.Document;
import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.Entity;
import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.cart.CartAttributes;
import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.cart.redis.helper.RedisCartHelper;
import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.cart.redis.hook.RedisCartMetadataSearchHook;
import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.cart.redis.hook.RedisCartMetadataUpdateHook;
import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.query.EntityCondition;
import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.util.RedisCartRecordUtils;
import de.hybris.platform.core.PK;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.hac.data.dto.PkData;
import de.hybris.platform.hac.facade.HacPkAnalyzerFacade;
import de.hybris.platform.persistence.polyglot.model.Identity;
import de.hybris.platform.persistence.polyglot.search.criteria.Condition;
import de.hybris.platform.persistence.polyglot.search.criteria.Conditions;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.servicelayer.internal.dao.GenericDao;
import de.hybris.platform.servicelayer.model.ModelService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The type Default redis cart helper.
 */
public class DefaultRedisCartHelper implements RedisCartHelper {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultRedisCartHelper.class);

    private HacPkAnalyzerFacade hacPkAnalyzerFacade;

    private GenericDao<RedisCartMetadataModel> redisCartMetaDao;

    private ModelService modelService;

    private RedisTemplate<String, String> redisTemplate;

    private List<RedisCartMetadataUpdateHook> redisCartMetadataUpdateHooks;

    private List<RedisCartMetadataSearchHook> redisCartMetadataSearchHooks;

    private Map<String, String> cartToMetadataAttributesMapping;

    @Override
    public List<String> generateSearchKey(Identity id) {
        LOG.debug("generateSearchKey for identity : {}", id);

        // Identify the composed type to which the id belongs
        PkData pkData = hacPkAnalyzerFacade.parsePkString(String.valueOf(id.toLongValue()));
        String composedTypeCode = pkData.getPkComposedTypeCode();
        LOG.debug("Identity : {}, belongs to composed type {}", id, composedTypeCode);

        List<String> searchKeys = null;

        if (StringUtils.equalsIgnoreCase(CartModel._TYPECODE, composedTypeCode)) {
            // Return the cart pk if the identity belongs to cart
            searchKeys = Arrays.asList(String.valueOf(id.toLongValue()));
        } else {
            // Identify the cart pk from cart entry if the identity belongs to cart entry
            searchKeys = getCartPkForReference(String.valueOf(id.toLongValue()));
        }
        LOG.debug("Search keys for identity {} are : {}", id, searchKeys);
        return searchKeys;
    }


    private List<String> getCartPkForReference(String referencePk) {
            return RedisCartRecordUtils.getValuesFromJSON(redisTemplate.opsForValue().get(referencePk));
    }

    private List<String> getCartPkForCondition(Map<String, Object> parameters) {
        List<String> cartPks = Collections.emptyList();
        if (!parameters.isEmpty()) {
            try {
                if (parameters.containsKey(CartModel.CODE) || parameters.containsKey(CartModel.GUID)) {
                    String key = parameters.entrySet().iterator().next().getKey();
                    String value = (String) parameters.get(key);
                    if (redisTemplate.hasKey(value)) {
                        cartPks= Arrays.asList(redisTemplate.opsForValue().get(value));
                    }
                } else {
                    List<RedisCartMetadataModel> metadataModels = redisCartMetaDao.find(parameters);
                    cartPks= metadataModels.stream().map(model -> String.valueOf(model.getCartPk())).collect(Collectors.toList());
                }
            } catch (UnknownIdentifierException ex) {
                LOG.debug("No metadata found ", ex);
            }
        }
        LOG.debug("Cart pks for condition parameters {} are : {}", parameters, cartPks);
        return cartPks;
    }

    @Override
    public List<String> generateSearchKeys(EntityCondition entityCondition) {
        LOG.debug("generateSearchKey for condition : {}", entityCondition);
        List<String> cartPks = Collections.emptyList();
        Map<String, Object> parameters = new HashMap<>();
        Condition condition = entityCondition.getCondition();
        if (condition instanceof Conditions.ComparisonCondition) {
            LOG.debug("Condition is a comparison condition.");
            Conditions.ComparisonCondition comparisonCondition = (Conditions.ComparisonCondition) condition;
            Object parameterValue =entityCondition.getParams().get(comparisonCondition.getParamNameToCompare().get());
            if(parameterValue instanceof Identity || StringUtils.equals(CartModel.PK, comparisonCondition.getParamNameToCompare().get())){
                LOG.debug("Condition parameter is a key/pk.");
                String parameterValueString = (parameterValue instanceof String)? (String) parameterValue: String.valueOf(((Identity)parameterValue).toLongValue());
                String parameterTypeCode = hacPkAnalyzerFacade.parsePkString(parameterValueString).getPkComposedTypeCode();
                if(StringUtils.equalsIgnoreCase(CartModel._TYPECODE,parameterTypeCode)){
                    LOG.debug("Condition value is cart pk.");
                    cartPks= Arrays.asList(parameterValueString);
                }else {
                    LOG.debug("Condition value is a non cart pk.");
                    cartPks = getCartPkForReference(parameterValueString);
                }
            }else {
                LOG.debug("Condition value is not a key/pk.");
                addParameterForCondition(entityCondition, parameters, comparisonCondition);
                LOG.debug("Search Parameters prepared for entity condition {}.", parameters);
            }
        } else if (condition instanceof Conditions.LogicalAndCondition) {
            LOG.debug("Condition is logical and condition.");
            Conditions.LogicalAndCondition logicalAndCondition = ((Conditions.LogicalAndCondition) condition);
            for (int i = 0; i < logicalAndCondition.getChildCount(); i++) {
                Condition childCondition = logicalAndCondition.getChild(i);
                if (childCondition instanceof Conditions.ComparisonCondition) {
                    addParameterForCondition(entityCondition, parameters, (Conditions.ComparisonCondition) childCondition);
                }
            }
            LOG.debug("Search Parameters prepared for entity condition {}.", parameters);
        }else if(condition instanceof Conditions.EmptyCondition){
            LOG.debug("Condition is an empty condition. i.e to search entity in all documents");
            cartPks= CollectionUtils.emptyIfNull(redisCartMetaDao.find()).stream().map(RedisCartMetadataModel::getCartPk)
                    .map(String::valueOf).collect(Collectors.toList());
        }
        if(CollectionUtils.isEmpty(cartPks)) {
            cartPks = getCartPkForCondition(parameters);
        }
        LOG.debug("Cart pks for condition condition {} are : {}", entityCondition, cartPks);
        return cartPks;
    }

    private void addParameterForCondition(EntityCondition entityCondition, Map<String, Object> parameters, Conditions.ComparisonCondition condition) {
        Conditions.ComparisonCondition comparisonCondition = condition;
        String key = comparisonCondition.getKey().getQualifier();
        if (comparisonCondition.getParamNameToCompare().isPresent()) {
            addParameter(parameters, key, entityCondition.getParams().get(comparisonCondition.getParamNameToCompare().get()));
        }
    }

    private void addParameter(Map<String, Object> parameters, String key, Object value) {
        if (cartToMetadataAttributesMapping.containsKey(key)) {
            key = cartToMetadataAttributesMapping.get(key);
        }
        if (null == value) {
            return;
        } else if (value instanceof String || value instanceof Boolean) {
            parameters.put(key, value);
        } else if (value instanceof Identity) {
            parameters.put(key, modelService.get(PK.fromLong(((Identity) value).toLongValue())));
        }
    }

    @Override
    public void storeMetadata(Document newDocument, Document oldDocument) {
        final Entity cartEntity = newDocument.getEntity(newDocument.getRootId()).get();

        String guid = cartEntity.get(CartAttributes.guid());
        String code = cartEntity.get(CartAttributes.code());

        RedisCartMetadataModel model = null;
        if (newDocument.isNew()) {
            model = modelService.create(RedisCartMetadataModel.class);
        } else if (null != oldDocument) {
            final Entity oldCartEntity = oldDocument.getEntity(oldDocument.getRootId()).get();
            if (CollectionUtils.emptyIfNull(redisCartMetadataUpdateHooks).stream().anyMatch(hook -> hook.isMetadataUpdateRequired(oldCartEntity, cartEntity))) {
                List<RedisCartMetadataModel> metadataModelList = getRedisCartMetaByCartPk(newDocument.getRootId().toLongValue());
                if (CollectionUtils.isNotEmpty(metadataModelList)) {
                    model = metadataModelList.get(0);
                }
            }
        }

        if (null != model) {
            final RedisCartMetadataModel metadata = model;
            String cartPk=String.valueOf(newDocument.getRootId().toLongValue());
            metadata.setCartPk(cartPk);
            metadata.setCode(code);
            metadata.setGuid(guid);
            CollectionUtils.emptyIfNull(redisCartMetadataUpdateHooks).stream().forEach(hook -> hook.updateMetadata(metadata, cartEntity));
            modelService.save(metadata);
            LOG.debug("Metadata stored for cart. Storing the cart code, guid and pk relationships");
            redisTemplate.opsForValue().setIfAbsent(code, cartPk);
            redisTemplate.opsForValue().setIfAbsent(guid, cartPk);
        }
    }

    private List<RedisCartMetadataModel> getRedisCartMetaByCartPk(Long cartPk) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(RedisCartMetadataModel.CARTPK, cartPk);
        try {
            List<RedisCartMetadataModel> metadataModelList = redisCartMetaDao.find(parameters);

            return metadataModelList;
        } catch (UnknownIdentifierException ex) {
            return Collections.emptyList();
        }
    }

    @Override
    public void deleteMetadata(String key) {
        List<RedisCartMetadataModel> carts = getRedisCartMetaByCartPk(Long.valueOf(key));
        carts.stream().forEach(cart -> {
            redisTemplate.delete(cart.getCode());
            redisTemplate.delete(cart.getGuid());
            modelService.remove(cart);
        });
    }


    /**
     * Sets redis cart meta dao.
     *
     * @param redisCartMetaDao the redis cart meta dao
     */
    public void setRedisCartMetaDao(GenericDao<RedisCartMetadataModel> redisCartMetaDao) {
        this.redisCartMetaDao = redisCartMetaDao;
    }

    /**
     * Sets hac pk analyzer facade.
     *
     * @param hacPkAnalyzerFacade the hac pk analyzer facade
     */
    public void setHacPkAnalyzerFacade(HacPkAnalyzerFacade hacPkAnalyzerFacade) {
        this.hacPkAnalyzerFacade = hacPkAnalyzerFacade;
    }

    /**
     * Sets model service.
     *
     * @param modelService the model service
     */
    public void setModelService(ModelService modelService) {
        this.modelService = modelService;
    }

    /**
     * Sets redis template.
     *
     * @param redisTemplate the redis template
     */
    public void setRedisTemplate(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void setRedisCartMetadataUpdateHooks(List<RedisCartMetadataUpdateHook> redisCartMetadataUpdateHooks) {
        this.redisCartMetadataUpdateHooks = redisCartMetadataUpdateHooks;
    }

    public void setRedisCartMetadataSearchHooks(List<RedisCartMetadataSearchHook> redisCartMetadataSearchHooks) {
        this.redisCartMetadataSearchHooks = redisCartMetadataSearchHooks;
    }

    public void setCartToMetadataAttributesMapping(Map<String, String> cartToMetadataAttributesMapping) {
        this.cartToMetadataAttributesMapping = cartToMetadataAttributesMapping;
    }
}