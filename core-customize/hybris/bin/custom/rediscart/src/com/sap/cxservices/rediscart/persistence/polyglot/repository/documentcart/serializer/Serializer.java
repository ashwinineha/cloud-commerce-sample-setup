/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.serializer;

import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.Document;


public interface Serializer
{
	String serialize(Document document);

	String serializeWithOverriddenVersion(Document document, long version);

	Document deserialize(String string);
}