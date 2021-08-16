package com.taobao.arthas.core.command.monitor200;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.concurrent.ConcurrentWeakKeyHashMap;
import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.advisor.AdviceListenerAdapter;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.command.express.ExpressException;
import com.taobao.arthas.core.command.model.SqlProfilerModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.DateUtils;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.ThreadLocalWatch;

import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.util.*;

/**
 * @author yangxiaobing 2021/8/4.
 */
class SqlProfilerAdviceListener extends AdviceListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(SqlProfilerAdviceListener.class);
    public static final List<String> STATEMENT_EXECUTE_METHOD_LIST = Arrays.asList("executeQuery", "executeUpdate");
    public static final List<String> PREPARED_STATEMENT_EXECUTE_METHOD = Arrays.asList("executeQuery", "executeUpdate", "execute");
    private static final Map<String, List<Method>> PREPARED_STATEMENT_SET_PARAM_METHOD_MAP = new HashMap<String, List<Method>>();

    private final ThreadLocalWatch threadLocalWatch = new ThreadLocalWatch();
    private SqlProfilerCommand command;
    private CommandProcess process;

    private Map<PreparedStatement, SqlContext> preparedStatement2SqlMap = new ConcurrentWeakKeyHashMap<PreparedStatement, SqlContext>();

    static {
        Method[] methods = PreparedStatement.class.getMethods();
        for (Method method : methods) {
            if (!method.getName().startsWith("set")
                    || method.getParameterTypes().length < 2
                    || !(Integer.class.equals(method.getParameterTypes()[0])
                    || int.class.equals(method.getParameterTypes()[0]))) {
                continue;
            }

            List<Method> innerMethodList = PREPARED_STATEMENT_SET_PARAM_METHOD_MAP.get(method.getName());
            if (innerMethodList == null) {
                innerMethodList = new ArrayList<Method>();
            }

            innerMethodList.add(method);
            PREPARED_STATEMENT_SET_PARAM_METHOD_MAP.put(method.getName(), innerMethodList);
        }
    }

    public SqlProfilerAdviceListener(SqlProfilerCommand command, CommandProcess process, boolean verbose) {
        this.command = command;
        this.process = process;
        super.setVerbose(verbose);
    }

    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args)
            throws Throwable {
        // 开始计算本次方法调用耗时
        threadLocalWatch.start();
    }

    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                               Object returnObject) throws Throwable {
        Advice advice = Advice.newForAfterRetuning(loader, clazz, method, target, args, returnObject);
        finishing(advice);
    }

    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                              Throwable throwable) {
        Advice advice = Advice.newForAfterThrowing(loader, clazz, method, target, args, throwable);
        finishing(advice);
    }

    private void finishing(Advice advice) {
        double cost = threadLocalWatch.costInMillis();
        boolean isSuccess = advice.getThrowExp() != null;
        try {
            // 检测Connection类的prepareStatement方法，提前把sql语句存储到preparedStatement2SqlMap中
            if (advice.getTarget() instanceof java.sql.Connection) {
                if ("prepareStatement".equals(advice.getMethod().getName())) {
                    if (advice.getReturnObj() != null
                            && advice.getReturnObj() instanceof PreparedStatement) {
                        SqlContext sqlContext = new SqlContext();
                        sqlContext.setSql(String.valueOf(advice.getParams()[0]));
                        preparedStatement2SqlMap.put((PreparedStatement) advice.getReturnObj(), sqlContext);
                    }
                }

                return;
            }

            if (advice.getTarget() instanceof PreparedStatement) {
                SqlContext sqlContext = preparedStatement2SqlMap.get(advice.getTarget());
                if (sqlContext == null) {
                    logger.warn("sql context is null");
                    return;
                }

                // 如果是PreparedStatement的set方法，则记录参数到对应PreparedStatement的sqlContext中
                if (isPreparedStatementSetParamMethod(advice.getMethod().getTargetMethod())) {
                    Integer parameterIndex = (Integer) advice.getParams()[0];
                    sqlContext.getParamsMap().put(parameterIndex, advice.getParams()[1]);
                } else if (isPreparedStatementExecuteMethod(advice.getMethod().getTargetMethod())) {
                    // 如果是PreparedStatement的execute方法，则记录本次执行结果
                    appendResult(advice, sqlContext.getSql(), cost, isSuccess, sqlContext.getParamsMap().values());
                    preparedStatement2SqlMap.remove(advice.getTarget());
                }

                return;
            }

            if (advice.getTarget() instanceof java.sql.Statement) {
                if (!isStatementExecuteMethod(advice.getMethod().getTargetMethod())) {
                    return;
                }

                appendResult(advice, (String) advice.getParams()[0], cost, isSuccess, null);
            }
        } catch (Throwable e) {
            logger.warn("sqlprofiler failed.", e);
            process.end(1, "sqlprofiler failed, sql pattern: " + command.getSqlPattern() + ", condition is: " + command.getConditionExpress() + ", " + e.getMessage()
                    + ", visit " + LogUtil.loggingFile() + " for more details.");
        }
    }

    private void appendResult(Advice advice, String sql, double cost, boolean isSuccess, Collection<Object> paramObjectList) throws ExpressException {
        if (!command.getSqlMatcher().matching(sql)
                || !isConditionMet(command.getConditionExpress(), advice, cost)) {
            return;
        }

        SqlProfilerModel model = new SqlProfilerModel();
        model.setTs(new Date());
        model.setClassName(advice.getClazz().getName());
        model.setMethodName(advice.getMethod().getName());
        model.setSql(sql);
        model.setCost(cost);
        model.setSuccess(isSuccess);
        model.setParams(convertSqlObjectToString(paramObjectList));

        process.appendResult(model);

        process.times().incrementAndGet();
        if (isLimitExceeded(command.getNumberOfLimit(), process.times().get())) {
            abortProcess(process, command.getNumberOfLimit());
        }
    }

    private List<String> convertSqlObjectToString(Collection<Object> paramObjectList) {
        if (paramObjectList == null || paramObjectList.size() == 0) {
            return null;
        }

        List<String> paramStringList = new ArrayList<String>(paramObjectList.size());
        for (Object paramObj : paramObjectList) {
            String paramString = null;
            if (paramObj instanceof String) {
                paramString = paramObj.toString();
            } else if (paramObj instanceof Date) {
                paramString = DateUtils.formatDate((Date) paramObj);
            } else {
                if (paramObj == null) {
                    paramString = "";
                } else {
                    paramString = paramObj.toString();
                }

            }

            paramStringList.add(paramString);
        }

        return paramStringList;
    }

    private boolean isPreparedStatementSetParamMethod(Method judgeMethod) {
        List<Method> methods = PREPARED_STATEMENT_SET_PARAM_METHOD_MAP.get(judgeMethod.getName());
        if (methods == null) {
            return false;
        }

        Class<?>[] judgeMethodParameterTypes = judgeMethod.getParameterTypes();

        for (Method itemMethod : methods) {
            Class<?>[] itemMethodParameterTypes = itemMethod.getParameterTypes();
            if (itemMethodParameterTypes.length != judgeMethodParameterTypes.length) {
                continue;
            }

            boolean isParameterTypeMatched = true;
            for (int i = 0; i < itemMethodParameterTypes.length; i++) {
                if (!itemMethodParameterTypes[i].equals(judgeMethodParameterTypes[i])) {
                    isParameterTypeMatched = false;
                    break;
                }
            }

            if (isParameterTypeMatched) {
                return true;
            }
        }

        return false;
    }

    private boolean isPreparedStatementExecuteMethod(Method method) {
        return method != null
                && PREPARED_STATEMENT_EXECUTE_METHOD.contains(method.getName())
                && method.getParameterTypes().length == 0;
    }

    private boolean isStatementExecuteMethod(Method method) {
        return method != null
                && STATEMENT_EXECUTE_METHOD_LIST.contains(method.getName())
                && method.getParameterTypes().length == 1
                && String.class.equals(method.getParameterTypes()[0]);
    }

    private static class SqlContext {
        private String sql;

        private TreeMap<Integer, Object> paramsMap = new TreeMap<Integer, Object>();

        public String getSql() {
            return sql;
        }

        public void setSql(String sql) {
            this.sql = sql;
        }

        public TreeMap<Integer, Object> getParamsMap() {
            return paramsMap;
        }

        public void setParamsMap(TreeMap<Integer, Object> paramsMap) {
            this.paramsMap = paramsMap;
        }
    }
}
