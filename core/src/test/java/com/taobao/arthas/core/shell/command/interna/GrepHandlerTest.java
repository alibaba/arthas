package com.taobao.arthas.core.shell.command.interna;

import java.io.File;
import java.lang.reflect.Constructor;
import java.nio.charset.Charset;
import org.junit.Assert;
import org.junit.Test;
import com.taobao.arthas.core.shell.command.internal.GrepHandler;
import com.taobao.arthas.core.util.FileUtils;

public class GrepHandlerTest {

  private static final class Hold {
      static final  Constructor<?> constructor;
      static {
        constructor = GrepHandler.class.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
      }
      public static GrepHandler createInst(String keyword, boolean ignoreCase, boolean invertMatch, boolean regexpMode
          , boolean showLineNumber,  int beforeLines, int afterLines, String output) {
          return createInst(keyword, ignoreCase, invertMatch, regexpMode, showLineNumber, beforeLines, afterLines, output, 0);
      }
    //new GrepHandler(keyword, ignoreCase, invertMatch, regexpMode, showLineNumber, beforeLines, afterLines,output)
      public static GrepHandler createInst(String keyword, boolean ignoreCase, boolean invertMatch, boolean regexpMode
         , boolean showLineNumber,  int beforeLines, int afterLines, String output, int maxCount) {
       try {
       final Object[] initargs = new Object[] {   keyword,ignoreCase,invertMatch,regexpMode,showLineNumber
           ,beforeLines,afterLines,output,maxCount
       };
       GrepHandler handler = (GrepHandler)constructor.newInstance(initargs);
       return handler;
       }catch(RuntimeException ex) {
         throw ex;
       }catch(Exception ex) {
         throw new IllegalStateException(ex);
       }
     }
  }
  @Test
  public void test4grep_ABC() { //-A -B -C
    Object[][] samples = new Object[][] {
      {"ABC\n1\n2\n3\n4\nc", "ABC", 0, 4, "ABC\n1\n2\n3\n4"},
      {"ABC\n1\n2\n3\n4\nABC\n5", "ABC", 2, 1, "ABC\n1\n3\n4\nABC\n5"},
      {"ABC\n1\n2\n3\n4\na", "ABC", 2, 1, "ABC\n1"},
      {"ABC\n1\n2\n3\n4\nb", "ABC", 0, 0, "ABC"},
      {"ABC\n1\n2\n3\n4\nc", "ABC", 0, 5, "ABC\n1\n2\n3\n4\nc"},
      {"ABC\n1\n2\n3\n4\nc", "ABC", 0, 10, "ABC\n1\n2\n3\n4\nc"},
      {"ABC\n1\n2\n3\n4\nc", "ABC", 0, 2, "ABC\n1\n2"},
      {"1\n2\n3\n4\nABC", "ABC", 5, 1, "1\n2\n3\n4\nABC"},
      {"1\n2\n3\n4\nABC", "ABC", 4, 1, "1\n2\n3\n4\nABC"},
      {"1\n2\n3\n4\nABC", "ABC", 2, 1, "3\n4\nABC"}
    };
    
    for(Object[] args : samples) {
      String word = (String)args[1];
      int beforeLines = (Integer)args[2];
      int afterLines = (Integer)args[3];
      GrepHandler handler = Hold.createInst(word,false,false,true,false,beforeLines,afterLines,null);
      String input = (String)args[0];
      final String ret = handler.apply(input);
      final String expected = (String)args[4];
      Assert.assertEquals(expected, ret.substring(0,ret.length()-1));
    }
  }
  @Test
  public void test4grep_v() {//-v
    Object[][] samples = new Object[][] {
      {"ABC\n1\n2\nc", "ABC", 0, 4, "1\n2\nc"},
      {"ABC\n1\n2\n", "ABC", 0, 0, "1\n2"},
      {"ABC\n1\n2\nc", "ABC", 0, 1, "1\n2\nc"}
    };
    
    for(Object[] args : samples) {
      String word = (String)args[1];
      int beforeLines = (Integer)args[2];
      int afterLines = (Integer)args[3];
      GrepHandler handler = Hold.createInst(word,false,true,true,false,beforeLines,afterLines,null);
      String input = (String)args[0];
      final String ret = handler.apply(input);
      final String expected = (String)args[4];
      Assert.assertEquals(expected, ret.substring(0,ret.length()-1));
    }
  }
  
  @Test
  public void test4grep_e() {//-e
    Object[][] samples = new Object[][] {
      {"java\n1python\n2\nc", "java|python", "java\n1python"},
      {"java\n1python\n2\nc", "ja|py", "java\n1python"}
    };
    
    for(Object[] args : samples) {
      String word = (String)args[1];
      GrepHandler handler = Hold.createInst(word,false,false,true,false,0,0,null);
      String input = (String)args[0];
      final String ret = handler.apply(input);
      final String expected = (String)args[2];
      Assert.assertEquals(expected, ret.substring(0,ret.length()-1));
    }
  }

  @Test
  public void test4grep_m() {//-e
    Object[][] samples = new Object[][] {
      {"java\n1python\n2\nc", "java|python", "java", 1},
      {"java\n1python\n2\nc", "ja|py", "java\n1python",2},
      {"java\n1python\n2\nc", "ja|py", "java\n1python",3}
    };
    
    for(Object[] args : samples) {
      String word = (String)args[1];
      int maxCount = args.length > 3 ? (Integer)args[3] : 0;
      GrepHandler handler = Hold.createInst(word,false,false,true,false,0,0,null, maxCount);
      String input = (String)args[0];
      final String ret = handler.apply(input);
      final String expected = (String)args[2];
      Assert.assertEquals(expected, ret.substring(0,ret.length()-1));
    }
  }
  
  @Test
  public void test4grep_n() {//-n
    Object[][] samples = new Object[][] {
      {"java\n1\npython\n2\nc","1:java\n3:python", "java|python" },
      {"java\n1\npython\njava\nc","1:java\n4:java", "java",false }
    };
    
    for(Object[] args : samples) {
      String word = (String)args[2];
      boolean regexpMode = args.length >3 ? (Boolean)args[3] : true;
      GrepHandler handler = Hold.createInst(word,false,false,regexpMode,true,0,0,null);
      String input = (String)args[0];
      final String ret = handler.apply(input);
      final String expected = (String)args[1];
      Assert.assertEquals(expected, ret.substring(0,ret.length()-1));
    }
  }
  
  @Test
  public void test4grep_f() throws Exception {//-f
    Object[][] samples = new Object[][] {
      {"java\n1\npython\n2\nc","1:java\n3:python", "java|python" },
      {"java\n1\npython\njava\nc","1:java\n4:java", "java",false }
    };
    String output = File.createTempFile("arthas_test", ".log").getAbsolutePath();
    final Charset charset = Charset.forName("UTF-8");
    for(Object[] args : samples) {
      String word = (String)args[2];
      boolean regexpMode = args.length >3 ? (Boolean)args[3] : true;
      GrepHandler handler = Hold.createInst(word,false,false,regexpMode,true,0,0,output+":false");
      String input = (String)args[0];
      final String ret = handler.apply(input);
      final String expected = (String)args[1];
      Assert.assertEquals(expected, ret.substring(0,ret.length()-1));
      final String ret2 = FileUtils.readFileToString(new File(output), charset);
      Assert.assertEquals(expected, ret.substring(0,ret2.length()-1));
    }
  }
  
}
