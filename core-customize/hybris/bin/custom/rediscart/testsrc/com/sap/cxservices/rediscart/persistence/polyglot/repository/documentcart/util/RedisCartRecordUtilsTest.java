package com.sap.cxservices.rediscart.persistence.polyglot.repository.documentcart.util;

import org.junit.Assert;
import org.junit.Test;

public class RedisCartRecordUtilsTest {

    @Test
    public void testAddValue_ExistingValue_Null(){
        Assert.assertEquals("[\"986675768687\"]", RedisCartRecordUtils.addValue(null, "986675768687"));
    }

    @Test
    public void testAddValue_ExistingValue_Empty(){
        Assert.assertEquals("[\"986675768687\"]", RedisCartRecordUtils.addValue("[]", "986675768687"));
    }

    @Test
    public void testAddValue_WithExistingValue(){
        Assert.assertEquals("[\"986675768686\",\"986675768687\"]", RedisCartRecordUtils.addValue("[\"986675768686\"]", "986675768687"));
    }

    @Test
    public void testRemoveValue_ExistingValue_Null(){
        Assert.assertNull(RedisCartRecordUtils.removeValue(null, "986675768687"));
    }

    @Test
    public void testRemoveValue_ExistingValue_Empty(){
        Assert.assertEquals("[]", RedisCartRecordUtils.removeValue("[]", "986675768687"));
    }

    @Test
    public void testRemoveValue_WithExistingValue(){
        Assert.assertEquals("[\"986675768686\"]", RedisCartRecordUtils.removeValue("[\"986675768686\",\"986675768687\"]", "986675768687"));
    }

    @Test
    public void testRemoveValue_WithOnlyValue(){
        Assert.assertEquals("[]", RedisCartRecordUtils.removeValue("[\"986675768687\"]", "986675768687"));
    }

    @Test
    public void testGetValuesFromJSON_NullJson(){
        Assert.assertTrue(RedisCartRecordUtils.getValuesFromJSON(null).isEmpty());
    }

    @Test
    public void testGetValuesFromJSON_EmptyJson(){
        Assert.assertTrue(RedisCartRecordUtils.getValuesFromJSON("[]").isEmpty());
    }

    @Test
    public void testGetValuesFromJSON_SingleValue(){
        Assert.assertEquals(1,RedisCartRecordUtils.getValuesFromJSON("[\"986675768687\"]").size());
    }

    @Test
    public void testGetValuesFromJSON_MultipleValue(){
        Assert.assertTrue(RedisCartRecordUtils.getValuesFromJSON("[\"986675768686\",\"986675768687\"]").size()>1);
    }
}
