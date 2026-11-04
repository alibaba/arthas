package com.taobao.arthas.core.util;

import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.cli.impl.CliTokenImpl;
import org.junit.Assert;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author earayu
 */
public class TokenUtilsTest {

    private List<CliToken> newCliTokenList(CliToken ... tokens){
        List<CliToken> cliTokens = new ArrayList<CliToken>();
        if(tokens!=null) {
            Collections.addAll(cliTokens, tokens);
        }
        return cliTokens;
    }

    @Test
    public void testFindFirstTextToken(){
        CliToken textCliToken = new CliTokenImpl(true,"textCliToken");
        CliToken nonTextCliToken = new CliTokenImpl(false,"nonTextCliToken");

        //null list
        Assert.assertEquals(null, TokenUtils.findFirstTextToken(null));

        //empty list
        Assert.assertEquals(null, TokenUtils.findFirstTextToken(new ArrayList<CliToken>()));

        //list with null value
        Assert.assertEquals(null,
                TokenUtils.findFirstTextToken(newCliTokenList(new CliToken[]{null})));
        Assert.assertEquals(textCliToken,
                TokenUtils.findFirstTextToken(newCliTokenList(new CliToken[]{null, textCliToken})));
        Assert.assertEquals(textCliToken,
                TokenUtils.findFirstTextToken(newCliTokenList(new CliToken[]{null, nonTextCliToken, textCliToken})));
        Assert.assertEquals(textCliToken,
                TokenUtils.findFirstTextToken(newCliTokenList(new CliToken[]{nonTextCliToken, null, textCliToken})));

        //list with normal inputs
        Assert.assertEquals(textCliToken,
                TokenUtils.findFirstTextToken(newCliTokenList(new CliToken[]{textCliToken})));
        Assert.assertEquals(textCliToken,
                TokenUtils.findFirstTextToken(newCliTokenList(new CliToken[]{nonTextCliToken, textCliToken})));
        Assert.assertEquals(textCliToken,
                TokenUtils.findFirstTextToken(newCliTokenList(new CliToken[]{textCliToken, nonTextCliToken})));

    }



    @Test
    public void testFindLastTextToken(){
        CliToken textCliToken = new CliTokenImpl(true,"textCliToken");
        CliToken nonTextCliToken = new CliTokenImpl(false,"nonTextCliToken");

        //null list
        Assert.assertEquals(null, TokenUtils.findLastTextToken(null));

        //empty list
        Assert.assertEquals(null, TokenUtils.findLastTextToken(new ArrayList<CliToken>()));

        //list with null value
        Assert.assertEquals(null,
                TokenUtils.findLastTextToken(newCliTokenList(new CliToken[]{null})));
        Assert.assertEquals(textCliToken,
                TokenUtils.findLastTextToken(newCliTokenList(new CliToken[]{null, textCliToken})));
        Assert.assertEquals(textCliToken,
                TokenUtils.findLastTextToken(newCliTokenList(new CliToken[]{null, nonTextCliToken, textCliToken})));
        Assert.assertEquals(textCliToken,
                TokenUtils.findLastTextToken(newCliTokenList(new CliToken[]{nonTextCliToken, null, textCliToken})));
        Assert.assertEquals(textCliToken,
                TokenUtils.findLastTextToken(newCliTokenList(new CliToken[]{textCliToken, null, nonTextCliToken})));

        //list with normal inputs
        Assert.assertEquals(textCliToken,
                TokenUtils.findLastTextToken(newCliTokenList(new CliToken[]{textCliToken})));
        Assert.assertEquals(null,
                TokenUtils.findLastTextToken(newCliTokenList(new CliToken[]{nonTextCliToken})));
        Assert.assertEquals(textCliToken,
                TokenUtils.findLastTextToken(newCliTokenList(new CliToken[]{nonTextCliToken, textCliToken})));
        Assert.assertEquals(textCliToken,
                TokenUtils.findLastTextToken(newCliTokenList(new CliToken[]{textCliToken, nonTextCliToken})));

    }


    @Test
    public void testFindSecondTextToken(){
        CliToken textCliToken = new CliTokenImpl(true,"textCliToken");
        CliToken nonTextCliToken = new CliTokenImpl(false,"nonTextCliToken");

        //null list
        Assert.assertEquals(null, TokenUtils.findSecondTokenText(null));

        //empty list
        Assert.assertEquals(null, TokenUtils.findSecondTokenText(new ArrayList<CliToken>()));

        //list with null value
        Assert.assertEquals(null,
                TokenUtils.findSecondTokenText(newCliTokenList(new CliToken[]{null})));
        Assert.assertEquals(null,
                TokenUtils.findSecondTokenText(newCliTokenList(new CliToken[]{null, textCliToken})));
        Assert.assertEquals(null,
                TokenUtils.findSecondTokenText(newCliTokenList(new CliToken[]{null, nonTextCliToken, textCliToken})));
        Assert.assertEquals(textCliToken.value(),
                TokenUtils.findSecondTokenText(newCliTokenList(new CliToken[]{null, nonTextCliToken, textCliToken, textCliToken})));

        //list with normal inputs
        Assert.assertEquals(null,
                TokenUtils.findSecondTokenText(newCliTokenList(new CliToken[]{textCliToken})));
        Assert.assertEquals(null,
                TokenUtils.findSecondTokenText(newCliTokenList(new CliToken[]{nonTextCliToken})));
        Assert.assertEquals(null,
                TokenUtils.findSecondTokenText(newCliTokenList(new CliToken[]{nonTextCliToken, textCliToken})));
        Assert.assertEquals(textCliToken.value(),
                TokenUtils.findSecondTokenText(newCliTokenList(new CliToken[]{textCliToken, nonTextCliToken, textCliToken})));

    }

}
