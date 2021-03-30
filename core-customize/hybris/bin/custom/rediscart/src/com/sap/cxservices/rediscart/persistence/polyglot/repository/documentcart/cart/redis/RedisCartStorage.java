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

package com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.cart.redis;

import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.Document;
import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.DocumentConcurrentModificationException;
import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.Entity;
import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.QueryResult;
import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.cart.CartAttributes;
import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.cart.redis.helper.RedisCartHelper;
import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.query.EntityCondition;
import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.serializer.Serializer;
import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.storage.BaseStorage;
import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.util.RedisCartRecordUtils;
import de.hybris.platform.persistence.polyglot.PolyglotPersistence;
import de.hybris.platform.persistence.polyglot.model.*;
import de.hybris.platform.persistence.polyglot.view.ItemStateView;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.StopWatch;

import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

/**
 * The type Redis cart storage.
 */
public class RedisCartStorage extends BaseStorage {

    private static final Logger LOG = LoggerFactory.getLogger(RedisCartStorage.class);

    private RedisTemplate<String, String> redisTemplate;

    private final Serializer serializer;

    private RedisCartHelper redisCartHelper;

    private long ttl;

    /**
     * Instantiates a new Redis cart storage.
     *
     * @param serializer the serializer
     */
    public RedisCartStorage(final Serializer serializer) {
        this.serializer = Objects.requireNonNull(serializer, "serializer mustn't be null.");
    }

    @Override
    protected Document instantiateNewDocument(final Identity rootId) {
        return new Document(rootId);
    }


    @Override
    public void save(final Document document) {
        StopWatch stopWatch = new StopWatch("RedisCartStorage.save");
        stopWatch.start("save");
        validateParameterNotNull(document, "Document cannot be null.");
        RedisCart cartToStore = new RedisCart(document);
        LOG.debug("Document to save: {}", cartToStore.cartJSON);

        String key = String.valueOf(document.getRootId().toLongValue());

        // Handle the document mismatch between redis and database.
        if (document.isNew()) {
            LOG.debug("Document to save is a new document, as per document context.");
            if (redisTemplate.hasKey(key)) {
                LOG.debug("Document to save is a new document as per context, however a document is present is Redis for same key.");
                throw DocumentConcurrentModificationException.documentAlreadyExist(document);
            }
        } else {
            LOG.debug("Document to save is not a new document.");
            if (!redisTemplate.hasKey(key)) {
                LOG.debug("Document to save is not a new document, but the document for this key is also not present in Redis. Hence deleting the metadata if present.");

                redisCartHelper.deleteMetadata(key);
            }
        }

        Document storedDocument = null;
        Set<String> entitiesToStore = getNonCartEntities(document, key);
        LOG.debug("Keys of non cart entities in the document: {}", entitiesToStore);

        if (redisTemplate.hasKey(key)) {
            LOG.debug("Document with the key {} exists in Redis, hence updating the same", key);
            String cartJSONFromRedis=redisTemplate.opsForValue().get(key);

            storedDocument = serializer.deserialize(cartJSONFromRedis);
            LOG.debug("Existing document from Redis: {}", cartJSONFromRedis);

            if (storedDocument.getVersion() > document.getVersion()) {
                LOG.warn("Existing document in Redis has the later version then the version in document to save. Ignoring it.");
                return;
            }

            Set<String> storedEntities = getNonCartEntities(storedDocument, key);
            LOG.debug("Keys of non cart entities in Redis for the document with key {} are {}: ", key, storedEntities);

            // Delete removed cart entries
            Set<String> entitiesToRemove = storedEntities.stream().filter(entry -> !entitiesToStore.contains(entry)).collect(Collectors.toSet());
            if (CollectionUtils.isNotEmpty(entitiesToRemove)) {
                LOG.debug("Keys of non cart entities to remove from Redis : {}", entitiesToRemove);
                entitiesToRemove.forEach(entity-> updateRelatedEntities(entity, key, true));
                LOG.debug("Keys of non cart entities to removed from Redis : {}", entitiesToRemove);
            }

        }

        try {
            LOG.debug("Storing the metadata for the document with key : {}", key);
            redisCartHelper.storeMetadata(document, storedDocument);

            LOG.debug("Stored the metadata for the document with key : {}, Now storing the document in cart.", key);

            putInRedis(key, cartToStore.cartJSON);

            LOG.debug("Stored the document in Redis for key : {}, now storing the non cart entities reference with cart in Redis {}", key, entitiesToStore);
            // Put all cart entries in redis
            entitiesToStore.forEach(entry -> updateRelatedEntities(entry, key, false));

            LOG.debug("Stored the non cart entities reference with cart in Redis for document with key : {}", key);
        } catch (Exception ex) {
            LOG.error("Exception occurred while persisting the cart", ex);
            throw new DocumentConcurrentModificationException(String.format("Document with id '%s' couldn't be persisted.", document.getRootId()));
        }

        stopWatch.stop();
        LOG.debug(stopWatch.prettyPrint());
    }

    private void putInRedis(String key, String value) {
        LOG.debug("Global TTL for Redis entry is {} seconds.", ttl);
        LOG.debug("Entry to persist in Redis has key: {}, and value: {}.", key, value);
        if (ttl < 0) {
            redisTemplate.opsForValue().set(key, value);
        } else {
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(this.ttl));
        }
        LOG.debug("Entry persisted in Redis.");
    }

    private void updateRelatedEntities(String key, String value, boolean remove) {
        LOG.debug("Global TTL for Redis entry is {} seconds.", ttl);
        LOG.debug("Entry to persist in Redis has key: {}, and value: {}.", key, value);
        if(redisTemplate.hasKey(key)){
            if(remove){
                value = RedisCartRecordUtils.removeValue(redisTemplate.opsForValue().get(key), value);
            }else {
                value = RedisCartRecordUtils.addValue(redisTemplate.opsForValue().get(key), value);
            }
        }else{
            value = RedisCartRecordUtils.addValue(null, value);
        }
        if(null==value || "[]".equals(value)){
            redisTemplate.delete(key);
        }else {
            putInRedis(key, value);
        }
        LOG.debug("Entry updated in Redis.");
    }



    @Override
    public void remove(final Document document) {
        StopWatch stopWatch = new StopWatch("RedisCartStorage");
        stopWatch.start("remove");
        String key = String.valueOf(document.getRootId().toLongValue());
        LOG.debug("Key of the document to be removed from Redis: {}", key);
        // Delete metadata from db and then cart entries and cart document from redis
        LOG.debug("Deleting the metadata first.");
        redisCartHelper.deleteMetadata(key);
        LOG.debug("Metadata deleted for key: {}.", key);
        if (redisTemplate.hasKey(key)) {
            LOG.debug("Redis has document for key: {}. Deleting the non cart entities relationships.", key);
            Set<String> entities = getNonCartEntities(document, key);
            entities.forEach(entity-> updateRelatedEntities(entity, key, true));
            LOG.debug("Relationships for Non cart entities {} deleted from Redis for key: {}. Now deleting the document", entities, key);
        }
        if (!redisTemplate.delete(key)) {
            throw DocumentConcurrentModificationException.documentHasBeenModified(document);
        }
        LOG.debug("Document deleted from Redis for key: {}.", key);
        stopWatch.stop();
        LOG.debug(stopWatch.prettyPrint());
    }

    private Set<String> getNonCartEntities(Document document, String cartKey) {
        return CollectionUtils.emptyIfNull(document.getEntityIds()).stream()
                .map(Identity::toLongValue).map(String::valueOf)
                .filter(entityKey-> !StringUtils.equals(cartKey, entityKey)).collect(Collectors.toSet());
    }

    @Override
    protected QueryResult findByRootId(final Identity id) {
        LOG.debug("findByRootId: {}", id.toString());
        StopWatch stopWatch = new StopWatch("RedisCartStorage.findByRootId");
        stopWatch.start("findByRootId");
        QueryResult result = QueryResult.fromNullable(null);
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        String cartJSon = redisTemplate.opsForValue().get(String.valueOf(id.toLongValue()));
        LOG.debug("Document retrieved from Redis for key: {}.", id.toLongValue());
        if (null != cartJSon) {
            result = QueryResult.from(serializer.deserialize(cartJSon));
        }
        stopWatch.stop();
        LOG.debug(stopWatch.prettyPrint());
        return result;
    }

    @Override
    protected QueryResult findByRootAttributes(final EntityCondition condition) {
        LOG.debug("findByRootAttributes: {}", condition);
        StopWatch stopWatch = new StopWatch("RedisCartStorage.findByRootAttributes");
        stopWatch.start("findByRootAttributes");

        QueryResult result = QueryResult.empty();
        List<String> keys = redisCartHelper.generateSearchKeys(condition);
        LOG.debug("Document keys identified for the given condition: {}.", keys);

        final Predicate<ItemStateView> predicate = condition.getPredicate();
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        if (CollectionUtils.isNotEmpty(keys)) {
            result =
                    QueryResult.from(keys.stream().map(valueOperations::get).filter(Objects::nonNull).map(serializer::deserialize).map(RedisCart::new).map(ItemStateAdapter::new).filter(predicate)
                            .map(ItemStateAdapter::toDocument).collect(Collectors.toList()));
        }
        LOG.debug("Results retrieved for the given condition: {}.", result);
        stopWatch.stop();
        LOG.debug(stopWatch.prettyPrint());
        return result;
    }

    @Override
    protected QueryResult findByEntityId(final Identity id) {
        LOG.debug("findByEntityId: {}", id.toString());
        StopWatch stopWatch = new StopWatch("RedisCartStorage.findByEntityId");
        stopWatch.start("findByEntityId");
        QueryResult result = QueryResult.fromNullable(null);
        List<String> keys = redisCartHelper.generateSearchKey(id);
        LOG.debug("Document keys identified for the given condition: {}.", keys);

        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        if (CollectionUtils.isNotEmpty(keys)) {
            result =
                    QueryResult.fromNullable(keys.stream().map(valueOperations::get).filter(Objects::nonNull).map(serializer::deserialize).filter(document -> document.containsEntity(id)).findFirst().get());
        }
        LOG.debug("Results retrieved for the given condition: {}.", result);
        stopWatch.stop();
        LOG.debug(stopWatch.prettyPrint());
        return result;
    }

    @Override
    protected QueryResult findByEntityAttributes(final EntityCondition condition) {
        LOG.debug("findByRootAttributes: {}", condition.toString());
        StopWatch stopWatch = new StopWatch("RedisCartStorage.findByEntityAttributes");
        stopWatch.start("findByEntityAttributes");
        QueryResult result = QueryResult.empty();
        final Predicate<ItemStateView> predicate = condition.getPredicate();
        List<String> keys = redisCartHelper.generateSearchKeys(condition);
        LOG.debug("Document keys identified for the given condition: {}.", keys);
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        if (CollectionUtils.isNotEmpty(keys)) {

            result = QueryResult.from(keys.stream().filter(redisTemplate::hasKey).map(valueOperations::get).map(serializer::deserialize)
                    .flatMap(Document::allEntities).filter(predicate).map(Entity::getDocument).distinct()
                    .collect(Collectors.toList()));
        }
        LOG.debug("Results retrieved for the given condition: {}.", result);
        stopWatch.stop();
        LOG.debug(stopWatch.prettyPrint());
        return result;
    }

    /**
     * Sets redis template.
     *
     * @param redisTemplate the redis template
     */
    public void setRedisTemplate(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void setRedisCartHelper(RedisCartHelper redisCartHelper) {
        this.redisCartHelper = redisCartHelper;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    private static class ItemStateAdapter implements ItemState {
        private final RedisCart cart;

        private Entity deserializedRootEntity;

        /**
         * Instantiates a new Item state adapter.
         *
         * @param cart the cart
         */
        ItemStateAdapter(final RedisCart cart) {
            this.cart = cart;
        }

        /**
         * To document document.
         *
         * @return the document
         */
        Document toDocument() {
            if (deserializedRootEntity != null) {
                return deserializedRootEntity.getDocument();
            }
            return cart.toDocument();
        }

        @Override
        public <T> T get(final Key key) {
            if (PolyglotPersistence.CoreAttributes.isPk(key)) {
                return (T) cart.cartId;
            }
            if (PolyglotPersistence.CoreAttributes.isVersion(key)) {
                return (T) Long.valueOf(cart.version);
            }
            if (PolyglotPersistence.CoreAttributes.isType(key)) {
                return (T) cart.typeRef;
            }
            if (CartAttributes.isCode(key)) {
                return (T) cart.code;
            }
            if (CartAttributes.isGuid(key)) {
                return (T) cart.guid;
            }
            if (deserializedRootEntity == null) {
                LOG.debug("Deserializing root entity because of '{}' attribute.", key);
                deserializedRootEntity = cart.toDocument().getRootEntity();
            }

            return deserializedRootEntity.get(key);
        }

        @Override
        public ChangeSet beginModification() {
            throw new UnsupportedOperationException();
        }
    }

    private class RedisCart implements Serializable {
        private final String cartJSON;
        private final Identity cartId;
        private final long version;
        private final Set<Identity> embeddedIds;
        private final String code;
        private final String guid;
        private final Reference typeRef;

        /**
         * Instantiates a new Redis cart.
         *
         * @param document the document
         */
        RedisCart(final Document document) {
            this.version = document.getVersion() + 1;
            this.cartJSON = serializer.serializeWithOverriddenVersion(document, version);
            this.embeddedIds = document.getEntityIds();
            this.cartId = document.getRootId();

            final Entity cartEntity = document.getEntity(document.getRootId()).get();

            this.guid = cartEntity.get(CartAttributes.guid());
            this.code = cartEntity.get(CartAttributes.code());
            this.typeRef = cartEntity.get(PolyglotPersistence.CoreAttributes.type());
        }

        /**
         * Contains boolean.
         *
         * @param id the id
         * @return the boolean
         */
        public boolean contains(final Identity id) {
            return embeddedIds.contains(id);
        }

        /**
         * To document document.
         *
         * @return the document
         */
        public Document toDocument() {
            return serializer.deserialize(cartJSON);
        }
    }

}
