package com.taobao.arthas.core.util;
 import java.text.SimpleDateFormat;
import java.util.Date;
import org.junit.Assert;
import org.junit.Test;
 /**
 * 
 * @author brijeshprasad89
 *
 */
public class DateUtilsTest {
	
    @Test
    public void testGetCurrentDateWithCorrectFormat() {
    	
    	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); //supported date format
    	Assert.assertEquals(DateUtils.getCurrentDate(),dateFormat.format(new Date()).toString());
        
    }
    
    @Test
    public void testGetCurrentDateWithInCorrectFormat() {
    	
    	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm"); // Not supported Date format
    	Assert.assertNotEquals(DateUtils.getCurrentDate(),dateFormat.format(new Date()).toString());
        
    }
 }