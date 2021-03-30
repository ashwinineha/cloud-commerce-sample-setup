/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart;

public interface Storage
{
	QueryResult find(Query query);

	void save(Document document);

	void remove(Document document);
}
