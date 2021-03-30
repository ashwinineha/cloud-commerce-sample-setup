package com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.serializer.json;

import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.Document;
import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.serializer.Serializer;
import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.persistence.polyglot.model.Identity;
import de.hybris.platform.persistence.polyglot.model.NonlocalizedKey;
import de.hybris.platform.persistence.polyglot.model.Reference;
import de.hybris.platform.persistence.polyglot.model.SingleAttributeKey;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.Entity;

import java.util.HashMap;
import java.util.Map;


@UnitTest
public class JsonSerializerTest {

    private Serializer serializer;

    @Mock
    private Identity rootIdentity;

    @Mock
    private Identity identity;

    @Before
    public void setUp(){
        serializer = new JsonSerializer();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testSerializeMap(){
        Document document = new Document(rootIdentity);
        Mockito.when(rootIdentity.toLongValue()).thenReturn(787698686L);
        Entity.EntityBuilder builder = Entity.builder(document);
        builder=builder.withId(identity);
        Map<String, String> stringMap = new HashMap<>();
        stringMap.put("myKey", "myValue");
        SingleAttributeKey stringMapKey = Mockito.mock(SingleAttributeKey.class);
                Mockito.when(stringMapKey.getQualifier()).thenReturn("stringMap");
        builder=builder.withAttribute(stringMapKey, stringMap);

        Map<Identity, Map> nonStringMap = new HashMap<>();
        nonStringMap.put(identity, stringMap);
        SingleAttributeKey nonStringMapKey = Mockito.mock(SingleAttributeKey.class);
        Mockito.when(nonStringMapKey.getQualifier()).thenReturn("nonStringMap");
        builder=builder.withAttribute(nonStringMapKey, nonStringMap);

        Entity entity = builder.build();
        document.addEntity(entity);
        Mockito.when(identity.toLongValue()).thenReturn(787699686L);
        String serializedJson = serializer.serialize(document);
        Document deserializedDocument = serializer.deserialize(serializedJson);
        Entity deserializedEntity = deserializedDocument.allEntities().filter(e -> e.getId().toLongValue()==identity.toLongValue()).findFirst().get();

        Assert.assertNotNull(deserializedEntity);
    }

}
