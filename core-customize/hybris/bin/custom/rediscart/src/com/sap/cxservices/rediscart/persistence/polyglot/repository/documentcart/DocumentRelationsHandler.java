/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart;

import de.hybris.platform.core.PK;
import de.hybris.platform.core.Registry;
import de.hybris.platform.jalo.type.CollectionType;
import de.hybris.platform.jalo.type.Type;
import de.hybris.platform.jalo.type.TypeManager;
import de.hybris.platform.persistence.PolyglotPersistenceGenericItemSupport;
import de.hybris.platform.persistence.polyglot.PolyglotPersistence;
import de.hybris.platform.persistence.polyglot.model.Identity;
import de.hybris.platform.persistence.polyglot.model.Reference;
import de.hybris.platform.persistence.polyglot.model.SingleAttributeKey;
import de.hybris.platform.util.RelationsInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public class DocumentRelationsHandler
{
	public Collection<Reference> getRelatedItems(final RelationsInfo relationsInfo, final Map<Identity, Entity> entities)
	{
		Objects.requireNonNull(relationsInfo, "relationsInfo mustn't be null");

		final String targetItemType = relationsInfo.getTargetItemType();

		final String foreignKeyAttr = relationsInfo.getForeignKeyAttrPk().getLeft();
		final PK foreignKeyAttrPk = relationsInfo.getForeignKeyAttrPk().getRight();

		final SingleAttributeKey foreignKeyAttrKey = PolyglotPersistence.getNonlocalizedKey(foreignKeyAttr);

		final Set<Reference> relatedItems = entities.values()
		                                            .stream()
		                                            .filter(e -> isEntityRelatedToTargetItemType(e, targetItemType))
		                                            .filter(e -> isAttrRelatedToForeignKeyAttr(foreignKeyAttrKey,
				                                            foreignKeyAttrPk, e))
		                                            .map(e -> PolyglotPersistence.getReferenceTo(e.getId()))
		                                            .collect(Collectors.toSet());

		if (relatedItems.isEmpty())
		{
			return null; //NOSONAR (NP driven logic (OneToManyHandler))
		}

		final int typeOfCollection = relationsInfo.getTypeOfCollection();

		if (typeOfCollection == CollectionType.SET)
		{
			return relatedItems;
		}
		return new ArrayList<>(relatedItems);
	}

	private boolean isAttrRelatedToForeignKeyAttr(final SingleAttributeKey key, final PK foreignKeyAttrPk,
	                                              final Entity entity)
	{
		final Object entityValue = entity.get(key);
		if (entityValue instanceof Reference)
		{
			final Reference entityRef = (Reference) entityValue;
			final PK entityRefPk = PolyglotPersistenceGenericItemSupport.PolyglotJaloConverter.toJaloLayer(
					entityRef.getIdentity());
			return entityRefPk.equals(foreignKeyAttrPk);
		}
		return false;
	}

	private boolean isEntityRelatedToTargetItemType(final Entity entity, final String targetItemType)
	{
		final String entityCode = getEntityCode(entity);
		if (StringUtils.isEmpty(entityCode))
		{
			return false;
		}

		final Set<String> targetItemTypeRelatedCodes = getAllEntityCodeRelatedTypeCodes(entityCode);

		return targetItemTypeRelatedCodes.contains(targetItemType);
	}

	String getEntityCode(final Entity entity)
	{
		final int typeCode = PolyglotPersistenceGenericItemSupport.PolyglotJaloConverter.toJaloLayer(entity.getId())
		                                                                                .getTypeCode();

		final String jndiName = Registry.getCurrentTenant().getPersistenceManager().getJNDIName(typeCode);
		final List<String> packageClass = Arrays.asList(StringUtils.split(jndiName, "_"));
		final int packageClassExpectedSize = 2;
		if (packageClass.size() < packageClassExpectedSize)
		{
			return StringUtils.EMPTY;
		}
		return packageClass.get(1);
	}

	protected Set<String> getAllEntityCodeRelatedTypeCodes(final String entityCode)
	{
		final Set<String> codes = new HashSet<>();
		codes.add(entityCode);
		codes.addAll(TypeManager.getInstance()
		                        .getComposedType(entityCode)
		                        .getAllSuperTypes()
		                        .stream()
		                        .map(Type::getCode)
		                        .collect(
				                        Collectors.toSet()));
		return codes;
	}
}
