package com.alibaba.arthas.tunnel.server.app.feature.web.security.util;

import com.alibaba.arthas.tunnel.server.app.feature.dto.Response;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Servlet 工具集
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 3.6.7
 */
@UtilityClass
public class ServletUtils {

    public static final String APPLICATION_JSON_UTF8_VALUE = "application/json;charset=UTF-8";

    public static void wrap(HttpServletResponse response, int statueCode,
                            String errCode, String errMessage) throws IOException {
        response.setStatus(statueCode);
        response.setContentType(APPLICATION_JSON_UTF8_VALUE);
        PrintWriter out = response.getWriter();
        String result = JacksonUtils.toJSONString(Response.buildFailed(errCode, errMessage));
        out.write(result);
    }

    public static ServletRequestAttributes getRequestAttributes() {
        return (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    }

    public static HttpServletRequest getRequest() {
        return getRequestAttributes().getRequest();
    }

    public static HttpServletResponse getResponse() {
        return getRequestAttributes().getResponse();
    }

    public static HttpSession getSession() {
        return getRequest().getSession();
    }

    public static String getRemoteUser() {
        HttpServletRequest request = getRequest();
        return getRemoteUser(request);
    }

    public static String getRemoteUser(HttpServletRequest request) {
        return StringUtils.trimToEmpty(request.getRemoteUser());
    }


}
