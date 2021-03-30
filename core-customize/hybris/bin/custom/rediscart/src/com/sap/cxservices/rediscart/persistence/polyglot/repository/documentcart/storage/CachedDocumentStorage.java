/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.storage;

import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.Document;
import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.Query;
import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.QueryResult;
import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.Storage;
import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.storage.cache.StorageCache;

import java.util.Objects;


public class CachedDocumentStorage implements Storage
{

	private final StorageCache storageCache;

	private final Storage targetStorage;


	public CachedDocumentStorage(final StorageCache storageCache, final Storage targetStorage)
	{
		this.storageCache = Objects.requireNonNull(storageCache);
		this.targetStorage = Objects.requireNonNull(targetStorage);
	}

	@Override
	public QueryResult find(final Query query)
	{
		return storageCache.findInCache(query, targetStorage::find);
	}

	@Override
	public void save(final Document document)
	{
		if (!storageCache.save(document))
		{
			targetStorage.save(document);
		}
	}

	@Override
	public void remove(final Document document)
	{
		if (!storageCache.remove(document))
		{
			targetStorage.remove(document);
		}
	}

	public StorageCache.CacheContext initCacheContext()
	{
		return storageCache.initCacheContext(targetStorage);
	}
}
