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

package com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.cart.redis.helper;

import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.Document;
import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.query.EntityCondition;
import de.hybris.platform.persistence.polyglot.model.Identity;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * The interface Redis cart key helper.
 */
public interface RedisCartHelper {

    /**
     * The constant PK_ATTRIBUTES.
     */
    static final List<String> PK_ATTRIBUTES= Arrays.asList("cart","user","site");

    /**
     * Generate search key list.
     *
     * @param id the id
     * @return the list
     */
    List<String> generateSearchKey(Identity id);

    /**
     * Generate search keys list.
     *
     * @param condition the condition
     * @return the list
     */
    List<String> generateSearchKeys(EntityCondition condition);

    /**
     * Store metadata if there is a change for metadata.
     *
     * @param newDocument the new document
     * @param oldDocument the old document
     */
    void storeMetadata(Document newDocument, Document oldDocument);

    /**
     * Delete metadata.
     *
     * @param key the key
     */
    void deleteMetadata(String key);

}
