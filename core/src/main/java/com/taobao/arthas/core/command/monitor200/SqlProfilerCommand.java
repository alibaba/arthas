package com.taobao.arthas.core.command.monitor200;


import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.arthas.core.util.matcher.RegexMatcher;
import com.taobao.arthas.core.util.matcher.WildcardMatcher;
import com.taobao.middleware.cli.annotations.*;

/**
 * Sql语句监控命令<br/>
 *
 * @author yangxiaobing 2021/8/4
 */
@Name("sqlprofiler")
@Summary("monitor sql profiler")
@Description("sql profiler")
public class SqlProfilerCommand extends EnhancerCommand {

    private int numberOfLimit = 100;
    private String sqlPattern;
    private String conditionExpress;
    private boolean isRegEx = false;
    private String action = "trace";
    private Long monitorDuration = 5L;

    private Matcher sqlMatcher;

    public int getNumberOfLimit() {
        return numberOfLimit;
    }

    @Option(shortName = "n", longName = "limits")
    @Description("Threshold of execution times")
    public void setNumberOfLimit(int numberOfLimit) {
        this.numberOfLimit = numberOfLimit;
    }

    @Argument(index = 0, argName = "sql-pattern", required = false)
    @DefaultValue("")
    @Description("The sql you want to watch.")
    public void setSqlPattern(String sqlPattern) {
        this.sqlPattern = sqlPattern;
    }

    @Argument(index = 1, argName = "condition-express", required = false)
    @Description("Conditional expression in ognl style, currently only support #cost.")
    public void setConditionExpress(String conditionExpress) {
        this.conditionExpress = conditionExpress;
    }

    @Option(shortName = "E", longName = "regex", flag = true)
    @Description("Enable regular expression to match (wildcard matching by default)")
    public void setRegEx(boolean regEx) {
        isRegEx = regEx;
    }

    @Option(shortName = "a", longName = "action", required = false)
    @Description("Action to execute, monitor or trace")
    public void setAction(String action) {
        this.action = action;
    }

    @Option(shortName = "d", longName = "duration", required = false)
    @Description("run profiling for <duration> seconds")
    public void setDuration(long duration) {
        this.monitorDuration = duration;
    }

    public String getConditionExpress() {
        return conditionExpress;
    }

    public String getSqlPattern() {
        return sqlPattern;
    }

    public String getAction() {
        return action;
    }

    public Long getMonitorDuration() {
        return monitorDuration;
    }

    public boolean isRegEx() {
        return isRegEx;
    }

    @Override
    protected Matcher getClassNameMatcher() {
        if (classNameMatcher == null) {
            classNameMatcher = SearchUtils.classNameMatcher("java\\.sql\\.Connection|java\\.sql\\.Statement", true);
        }
        return classNameMatcher;
    }

    @Override
    protected Matcher getClassNameExcludeMatcher() {
        if (classNameExcludeMatcher == null && getExcludeClassPattern() != null) {
            classNameExcludeMatcher = SearchUtils.classNameMatcher(getExcludeClassPattern(), isRegEx());
        }
        return classNameExcludeMatcher;
    }

    @Override
    protected Matcher getMethodNameMatcher() {
        if (methodNameMatcher == null) {
            methodNameMatcher = SearchUtils.classNameMatcher(null, isRegEx());
        }
        return methodNameMatcher;
    }

    public Matcher getSqlMatcher() {
        if (sqlMatcher == null) {
            if (StringUtils.isEmpty(sqlPattern)) {
                sqlPattern = isRegEx ? ".*" : "*";
            }
            sqlMatcher = isRegEx ? new RegexMatcher(sqlPattern) : new WildcardMatcher(sqlPattern);
        }

        return sqlMatcher;
    }

    @Override
    public void process(CommandProcess process) {
        if (!"trace".equalsIgnoreCase(this.action)
                && !"monitor".equalsIgnoreCase(this.action)) {
            process.end(-1, "invalid action argument");
            return;
        }

        super.process(process);
    }

    @Override
    protected AdviceListener getAdviceListener(CommandProcess process) {
        final AdviceListener listener = new SqlProfilerAdviceListener(this, process, GlobalOptions.verbose || this.verbose);
        /*
         * 通过handle回调，在suspend时停止timer，resume时重启timer
         */
        process.suspendHandler(new Handler<Void>() {
            @Override
            public void handle(Void event) {
                listener.destroy();
            }
        });
        process.resumeHandler(new Handler<Void>() {
            @Override
            public void handle(Void event) {
                listener.create();
            }
        });
        return listener;
    }
}
