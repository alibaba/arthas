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
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.ThreadLocalWatch;
import io.termd.core.function.Function;

import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;

/**
 * @author yangxiaobing 2021/8/4.
 */
class SqlProfilerAdviceListener extends AdviceListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(SqlProfilerAdviceListener.class);
    public static final List<String> STATEMENT_EXECUTE_METHOD_LIST = Arrays.asList("executeQuery", "executeUpdate", "execute");

    // 缓存Statement、PreparedStatement的需要监听的方法列表
    private static MethodWithSignature PREPARED_STATEMENT_SET_PARAM_METHOD_MAP;
    private static MethodWithSignature STATEMENT_EXECUTE_METHOD_MAP;
    private static MethodWithSignature STATEMENT_EXECUTE_BATCH_METHOD_MAP;
    private static MethodWithSignature STATEMENT_ADD_BATCH_METHOD_MAP;
    private static MethodWithSignature STATEMENT_CLEAR_BATCH_METHOD_MAP;

    private static MethodWithSignature PREPARED_STATEMENT_EXECUTE_METHOD_MAP;
    private static MethodWithSignature PREPARED_STATEMENT_ADD_BATCH_METHOD_MAP;

    private final ThreadLocalWatch threadLocalWatch = new ThreadLocalWatch();
    private SqlProfilerCommand command;
    private CommandProcess process;

    // 缓存PreparedStatement对象对应的Sql语句
    private Map<Statement, StatementSqlContext> statement2SqlMap = new ConcurrentWeakKeyHashMap<Statement, StatementSqlContext>();
    private Map<PreparedStatement, PreparedStatementSqlContext> preparedStatement2SqlMap = new ConcurrentWeakKeyHashMap<PreparedStatement, PreparedStatementSqlContext>();

    // 用来解决connectionProxy、statementProxy会导致sql语句被记录多次的问题
    private ThreadLocal<SqlExecuteStack> sqlStack = new ThreadLocal<SqlExecuteStack>();

    static {
        PREPARED_STATEMENT_SET_PARAM_METHOD_MAP = MethodWithSignature.build(PreparedStatement.class, new Function<Method, Boolean>() {
            @Override
            public Boolean apply(Method method) {
                return method.getName().startsWith("set")
                        && method.getParameterTypes().length >= 2
                        && (Integer.class.equals(method.getParameterTypes()[0])
                        || int.class.equals(method.getParameterTypes()[0]));
            }
        });

        STATEMENT_EXECUTE_METHOD_MAP = MethodWithSignature.build(Statement.class, new Function<Method, Boolean>() {
            @Override
            public Boolean apply(Method method) {
                return STATEMENT_EXECUTE_METHOD_LIST.contains(method.getName())
                        && method.getParameterTypes().length <= 2
                        && method.getParameterTypes().length >= 1
                        && String.class.equals(method.getParameterTypes()[0]);
            }
        });

        STATEMENT_EXECUTE_BATCH_METHOD_MAP = MethodWithSignature.build(Statement.class, new Function<Method, Boolean>() {
            @Override
            public Boolean apply(Method method) {
                return "executeBatch".equals(method.getName())
                        && method.getParameterTypes().length == 0;
            }
        });

        STATEMENT_ADD_BATCH_METHOD_MAP = MethodWithSignature.build(Statement.class, new Function<Method, Boolean>() {
            @Override
            public Boolean apply(Method method) {
                return "addBatch".equals(method.getName())
                        && method.getParameterTypes().length == 1
                        && String.class.equals(method.getParameterTypes()[0]);
            }
        });

        STATEMENT_CLEAR_BATCH_METHOD_MAP = MethodWithSignature.build(Statement.class, new Function<Method, Boolean>() {
            @Override
            public Boolean apply(Method method) {
                return "clearBatch".equals(method.getName())
                        && method.getParameterTypes().length == 0;
            }
        });

        PREPARED_STATEMENT_ADD_BATCH_METHOD_MAP = MethodWithSignature.build(PreparedStatement.class, new Function<Method, Boolean>() {
            @Override
            public Boolean apply(Method method) {
                return "addBatch".equals(method.getName())
                        && method.getParameterTypes().length == 0;
            }
        });

        PREPARED_STATEMENT_EXECUTE_METHOD_MAP = MethodWithSignature.build(PreparedStatement.class, new Function<Method, Boolean>() {
            @Override
            public Boolean apply(Method method) {
                return STATEMENT_EXECUTE_METHOD_LIST.contains(method.getName())
                        && method.getParameterTypes().length == 0;
            }
        });
    }

    public SqlProfilerAdviceListener(SqlProfilerCommand command, CommandProcess process, boolean verbose) {
        this.command = command;
        this.process = process;
        super.setVerbose(verbose);
    }

    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args)
            throws Throwable {
        String sql = null;
        if (target instanceof PreparedStatement) {
            if (PREPARED_STATEMENT_EXECUTE_METHOD_MAP.isMatch(method.getTargetMethod())
                    || STATEMENT_EXECUTE_BATCH_METHOD_MAP.isMatch(method.getTargetMethod())) {
                PreparedStatementSqlContext sqlContext = preparedStatement2SqlMap.get(target);
                if (sqlContext != null) {
                    sql = sqlContext.getSql();
                    sqlContext.enterExecute();

                    logger.debug("sql profiler enter execute: class: {}, method: {}, sql: {}.",
                            clazz.getName(),
                            method.getName(),
                            sql);
                }
            }
        } else if (target instanceof Statement) {
            if (STATEMENT_EXECUTE_METHOD_MAP.isMatch(method.getTargetMethod())) {
                sql = String.valueOf(args[0]);
                StatementSqlContext sqlContext = statement2SqlMap.get(target);
                if (sqlContext != null) {
                    sqlContext.enterExecute();

                    logger.debug("sql profiler enter execute: class: {}, method: {}, sql: {}.",
                            clazz.getName(),
                            method.getName(),
                            sql);
                }
            } else if (STATEMENT_EXECUTE_BATCH_METHOD_MAP.isMatch(method.getTargetMethod())) {
                StatementSqlContext sqlContext = statement2SqlMap.get(target);
                if (sqlContext != null) {
                    sql = sqlContext.getBatchSql();
                    sqlContext.enterExecute();

                    logger.debug("sql profiler enter execute: class: {}, method: {}, sql: {}.",
                            clazz.getName(),
                            method.getName(),
                            sql);
                }
            }
        }

        if (!StringUtils.isEmpty(sql)) {
            // 进入Sql的调用栈
            SqlExecuteStack sqlExecuteStack = sqlStack.get();
            if (sqlExecuteStack == null) {
                sqlExecuteStack = new SqlExecuteStack();
                sqlStack.set(sqlExecuteStack);
            }

            sqlExecuteStack.enter(sql);
        }

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
            if (advice.getTarget() == null) {
                return;
            }

            // 处理Connection的createStatement和prepareStatement方法，构建sqlContext
            if (advice.getTarget() instanceof java.sql.Connection) {
                handleBuildStatement(advice);
                return;
            }

            if (advice.getTarget() instanceof PreparedStatement) {
                handleAfterPrepareStatementMethod(advice, cost, isSuccess);
                return;
            }
            if (advice.getTarget() instanceof Statement) {
                handleAfterStatementMethod(advice, cost, isSuccess);
            }
        } catch (Throwable e) {
            logger.warn("sqlprofiler failed.", e);
            process.end(1, "sqlprofiler failed, sql pattern: " + command.getSqlPattern() + ", condition is: " + command.getConditionExpress() + ", " + e.getMessage()
                    + ", visit " + LogUtil.loggingFile() + " for more details.");
        }
    }

    private void handleAfterStatementMethod(Advice advice, double cost, boolean isSuccess) throws ExpressException {
        if (advice.getMethod().getTargetMethod() == null) {
            return;
        }

        if (STATEMENT_EXECUTE_METHOD_MAP.isMatch(advice.getMethod().getTargetMethod())) {
            StatementSqlContext sqlContext = getAndCheckStatementSqlContext(advice);
            if (sqlContext == null) return;

            sqlContext.exitExecute();
            if (sqlContext.isExecuteStackEmpty()) {
                statement2SqlMap.remove(advice.getTarget());
            }

            String sql = (String) advice.getParams()[0];
            // 退出Sql的执行堆栈, 记录本次执行结果
            if (exitSqlStackAndCheckEmpty(sql)) {
                appendResult(advice, cost, isSuccess, sql, null, null, null);
            }
        } else if (STATEMENT_ADD_BATCH_METHOD_MAP.isMatch(advice.getMethod().getTargetMethod())) {
            String sql = (String) advice.getParams()[0];

            StatementSqlContext sqlContext = getAndCheckStatementSqlContext(advice);
            if (sqlContext == null) return;
            sqlContext.addBatch(sql);
        } else if (STATEMENT_CLEAR_BATCH_METHOD_MAP.isMatch(advice.getMethod().getTargetMethod())) {
            StatementSqlContext sqlContext = getAndCheckStatementSqlContext(advice);
            if (sqlContext == null) return;

            sqlContext.clearBatch();
        } else if (STATEMENT_EXECUTE_BATCH_METHOD_MAP.isMatch(advice.getMethod().getTargetMethod())) {
            StatementSqlContext sqlContext = getAndCheckStatementSqlContext(advice);
            if (sqlContext == null) return;

            sqlContext.exitExecute();
            if (sqlContext.isExecuteStackEmpty()) {
                statement2SqlMap.remove(advice.getTarget());
            }

            // 退出Sql的执行堆栈, 记录本次执行结果
            if (exitSqlStackAndCheckEmpty(sqlContext.getBatchSql())) {
                appendResult(advice,
                        cost,
                        isSuccess,
                        sqlContext.getBatchSql(),
                        null,
                        sqlContext.getBatchSqlList(),
                        null);
            }
        }
    }

    private StatementSqlContext getAndCheckStatementSqlContext(Advice advice) {
        StatementSqlContext sqlContext = statement2SqlMap.get(advice.getTarget());
        if (sqlContext == null) {
            logger.debug("sql context is null, target: {}, class: {}, method: {}, param length: {}",
                    advice.getTarget(),
                    advice.getTarget().getClass(),
                    advice.getMethod().getName(),
                    advice.getMethod().getTargetMethod().getParameterTypes().length);
            return null;
        }

        return sqlContext;
    }

    private PreparedStatementSqlContext getAndCheckPreparedStatementSqlContext(Advice advice) {
        PreparedStatementSqlContext sqlContext = preparedStatement2SqlMap.get(advice.getTarget());
        if (sqlContext == null) {
            logger.debug("sql context is null, target: {}, class: {}, method: {}, param length: {}",
                    advice.getTarget(),
                    advice.getTarget().getClass(),
                    advice.getMethod().getName(),
                    advice.getMethod().getTargetMethod().getParameterTypes().length);
            return null;
        }

        return sqlContext;
    }

    private void handleAfterPrepareStatementMethod(Advice advice, double cost, boolean isSuccess) throws ExpressException {
        if (advice.getMethod().getTargetMethod() == null) {
            return;
        }

        if (PREPARED_STATEMENT_SET_PARAM_METHOD_MAP.isMatch(advice.getMethod().getTargetMethod())) {
            PreparedStatementSqlContext sqlContext = getAndCheckPreparedStatementSqlContext(advice);
            if (sqlContext == null) return;

            Integer parameterIndex = (Integer) advice.getParams()[0];
            sqlContext.setParam(parameterIndex, advice.getParams()[1]);
        } else if (PREPARED_STATEMENT_EXECUTE_METHOD_MAP.isMatch(advice.getMethod().getTargetMethod())) {
            PreparedStatementSqlContext sqlContext = getAndCheckPreparedStatementSqlContext(advice);
            if (sqlContext == null) return;

            sqlContext.exitExecute();
            logger.debug("preparedStatement: {} exit execute. method: {}, class: {}",
                    advice.getReturnObj(),
                    advice.getMethod().getName(),
                    advice.getClazz().getName());

            if (sqlContext.isExecuteStackEmpty()) {
                statement2SqlMap.remove(advice.getTarget());
            }

            // 退出Sql的执行堆栈, 记录本次执行结果
            if (exitSqlStackAndCheckEmpty(sqlContext.getSql())) {
                appendResult(advice, cost, isSuccess, sqlContext.getSql(), sqlContext.getParamsMap().values(), null, null);
            }
        } else if (PREPARED_STATEMENT_ADD_BATCH_METHOD_MAP.isMatch(advice.getMethod().getTargetMethod())) {
            PreparedStatementSqlContext sqlContext = getAndCheckPreparedStatementSqlContext(advice);
            if (sqlContext == null) return;

            sqlContext.addBatch();
        } else if (STATEMENT_CLEAR_BATCH_METHOD_MAP.isMatch(advice.getMethod().getTargetMethod())) {
            PreparedStatementSqlContext sqlContext = getAndCheckPreparedStatementSqlContext(advice);
            if (sqlContext == null) return;

            sqlContext.clearBatch();
        } else if (STATEMENT_EXECUTE_BATCH_METHOD_MAP.isMatch(advice.getMethod().getTargetMethod())) {
            PreparedStatementSqlContext sqlContext = getAndCheckPreparedStatementSqlContext(advice);
            if (sqlContext == null) return;

            sqlContext.exitExecute();
            logger.debug("preparedStatement: {} exit execute. method: {}, class: {}",
                    advice.getReturnObj(),
                    advice.getMethod().getName(),
                    advice.getClazz().getName());

            if (sqlContext.isExecuteStackEmpty()) {
                statement2SqlMap.remove(advice.getTarget());
            }

            // 退出Sql的执行堆栈, 记录本次执行结果
            if (exitSqlStackAndCheckEmpty(sqlContext.getSql())) {
                appendResult(advice,
                        cost,
                        isSuccess,
                        sqlContext.getSql(),
                        null,
                        null,
                        sqlContext.getBatchParamMap());
            }
        }
    }

    private void handleBuildStatement(Advice advice) {
        if ("prepareStatement".equals(advice.getMethod().getName())) {
            if (advice.getReturnObj() != null
                    && advice.getReturnObj() instanceof PreparedStatement) {
                preparedStatement2SqlMap.put((PreparedStatement) advice.getReturnObj(),
                        new PreparedStatementSqlContext(String.valueOf(advice.getParams()[0])));
            }

            logger.debug("build prepareStatement: {}", advice.getReturnObj());
        }

        if ("createStatement".equals(advice.getMethod().getName())) {
            if (advice.getReturnObj() != null
                    && advice.getReturnObj() instanceof Statement) {
                statement2SqlMap.put((Statement) advice.getReturnObj(),
                        new StatementSqlContext());
            }

            logger.debug("build statement: {}", advice.getReturnObj());
        }
    }

    /**
     * 退出sql堆栈
     *
     * @param executingSql
     * @return 如果退出栈后，栈为空，则返回true；否则返回false
     */
    private boolean exitSqlStackAndCheckEmpty(String executingSql) {
        SqlExecuteStack sqlExecuteStack = sqlStack.get();
        if (sqlExecuteStack == null) {
            return true;
        }

        sqlExecuteStack.exit(executingSql);
        return sqlExecuteStack.isEmpty();
    }

    private void appendResult(Advice advice,
                              double cost,
                              boolean isSuccess,
                              String sql,
                              Collection<Object> paramObjectList,
                              List<String> batchSql,
                              List<? extends Map<Integer, Object>> batchParamObjectList) throws ExpressException {
        if (!isConditionMet(command.getConditionExpress(), advice, cost)) {
            return;
        }
        if (sql != null && !command.getSqlMatcher().matching(sql)) {
            return;
        } else if (batchSql != null) {
            boolean isAnyMatch = false;
            for (String itemSql : batchSql) {
                if (command.getSqlMatcher().matching(itemSql)) {
                    isAnyMatch = true;
                    break;
                }
            }

            if (!isAnyMatch) {
                return;
            }
        }

        SqlProfilerModel model = new SqlProfilerModel();
        model.setTs(new Date());
        model.setClassName(advice.getClazz().getName());
        model.setMethodName(advice.getMethod().getName());
        model.setCost(cost);
        model.setSuccess(isSuccess);
        model.setSql(sql);
        model.setParams(convertSqlObjectToString(paramObjectList));
        model.setBatchSql(batchSql);
        model.setBatchParams(convertSqlObjectToString(batchParamObjectList));

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

    private List<List<String>> convertSqlObjectToString(List<? extends Map<Integer, Object>> paramObjectList) {
        if (paramObjectList == null) {
            return null;
        }

        List<List<String>> result = new ArrayList<List<String>>(paramObjectList.size());
        for (Map<Integer, Object> objectHashMap : paramObjectList) {
            result.add(convertSqlObjectToString(objectHashMap.values()));
        }

        return result;
    }

    private static class SqlContext {
        // 是否batch模式
        protected boolean inBatch = false;

        /**
         * 记录Statement对象的execute方法被执行的堆栈深度，
         * 用来解决Statement的execute方法中调用了父类的execute方法导致S他tement2SqlMap被提前释放的问题
         */
        private Integer executeStackDeep = 0;

        public void enterExecute() {
            executeStackDeep++;
        }

        public void exitExecute() {
            executeStackDeep--;
            if (executeStackDeep <= 0) {
                executeStackDeep = 0;
            }
        }

        public boolean isExecuteStackEmpty() {
            return executeStackDeep.equals(0);
        }
    }

    private static class StatementSqlContext extends SqlContext {
        // batch模式下Statement的batchSql
        private List<String> batchSqlList = null;
        private String batchSql;

        public void addBatch(String sql) {
            inBatch = true;
            if (batchSqlList == null) {
                batchSqlList = new ArrayList<String>();
            }
            batchSqlList.add(sql);

            this.batchSql = null;
        }

        public void clearBatch() {
            this.batchSqlList = null;

            this.batchSql = null;
        }

        public String getBatchSql() {
            if (batchSql == null) {
                batchSql = StringUtils.join(batchSqlList == null ? null : batchSqlList.toArray(), ";");
            }

            return batchSql;
        }

        public List<String> getBatchSqlList() {
            return batchSqlList;
        }
    }

    private static class PreparedStatementSqlContext extends SqlContext {
        // sql语句
        private String sql;
        private TreeMap<Integer, Object> paramsMap = new TreeMap<Integer, Object>();

        // batch模式下PreparedStatement的batchParam
        private List<TreeMap<Integer, Object>> batchParamMap = null;

        private PreparedStatementSqlContext(String sql) {
            this.sql = sql;
        }

        public void addBatch() {
            inBatch = true;
            if (batchParamMap == null) {
                batchParamMap = new ArrayList<TreeMap<Integer, Object>>();
            }
            batchParamMap.add(paramsMap);
            paramsMap = new TreeMap<Integer, Object>();
        }

        public void setParam(Integer index, Object param) {
            this.paramsMap.put(index, param);
        }

        public String getSql() {
            return sql;
        }

        public List<TreeMap<Integer, Object>> getBatchParamMap() {
            return batchParamMap;
        }

        public TreeMap<Integer, Object> getParamsMap() {
            return paramsMap;
        }

        public void clearBatch() {
            this.batchParamMap = null;
        }

    }

    private static class SqlExecuteStack {
        private String currentSql;

        private Integer stackDeep = 0;

        public String getCurrentSql() {
            return currentSql;
        }

        public void setCurrentSql(String currentSql) {
            this.currentSql = currentSql;
        }

        public Integer getStackDeep() {
            return stackDeep;
        }

        public void setStackDeep(Integer stackDeep) {
            this.stackDeep = stackDeep;
        }

        public void enter(String sql) {
            if (StringUtils.isEmpty(sql)) {
                return;
            }

            if (currentSql == null || currentSql.equals(sql)) {
                currentSql = sql;
                stackDeep++;
            } else {
                logger.warn("found unresolved sql stack with stackDeep: {}, sql: {}, thread: {}." +
                                " maybe there is concurrent sql statement operation.",
                        stackDeep,
                        currentSql,
                        Thread.currentThread().getName());
                currentSql = sql;
                stackDeep = 1;
            }
        }

        public void exit(String sql) {
            if (StringUtils.isEmpty(sql)) {
                return;
            }

            if (currentSql != null && currentSql.equals(sql)) {
                stackDeep--;
                if (stackDeep <= 0) {
                    currentSql = null;
                    stackDeep = 0;
                }
            } else {
                logger.warn("found resolving not match sql stack with stack's sql: {}, stackDeep: {}" +
                                " new sql: {}, thread: {}," +
                                " maybe there is concurrent sql statement operation.",
                        currentSql,
                        stackDeep,
                        sql,
                        Thread.currentThread().getName());
            }
        }

        public boolean isEmpty() {
            return stackDeep.equals(0);
        }
    }

    private static class MethodWithSignature {
        private Map<String, List<Method>> methodCache = new HashMap<String, List<Method>>();

        private MethodWithSignature(Map<String, List<Method>> methodCache) {
            this.methodCache = methodCache;
        }

        public static MethodWithSignature build(Class clazz, Function<Method, Boolean> methodFilter) {
            Map<String, List<Method>> methodCache = new HashMap<String, List<Method>>();
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (!methodFilter.apply(method)) {
                    continue;
                }

                List<Method> innerMethodList = methodCache.get(method.getName());
                if (innerMethodList == null) {
                    innerMethodList = new ArrayList<Method>();
                }

                innerMethodList.add(method);
                methodCache.put(method.getName(), innerMethodList);
            }

            return new MethodWithSignature(methodCache);
        }

        public boolean isMatch(Method judgeMethod) {
            if (judgeMethod == null) {
                return false;
            }

            List<Method> methods = methodCache.get(judgeMethod.getName());
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
    }
}
