package org.hashdb.ms.support;

import com.singularsys.jep.Jep;
import com.singularsys.jep.ParseException;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindingPostProcessor;

/**
 * Date: 2024/2/22 16:03
 * 在功能上拓展了 {@link ConfigurationPropertiesBindingPostProcessor}
 * 允许用户在配置文件中使用数学表达式
 *
 * @author huanyuMake-pecdle
 * @see ConfigurationPropertiesBindingPostProcessor
 */
public class JepExpressionEnvironmentPostProcessor extends ExpressionEnvironmentPostProcessor {

    @Override
    @NotNull
    protected Object evaluate(String expression) {
        // 这个对象频繁创建, 可以优化
        var jep = new Jep();
        try {
            jep.parse(expression);
            return jep.evaluate();
        } catch (ParseException ignore) {
        }
        // 表达式里的数字里可能有下划线做分隔, 重试一次
        var retry = new Jep();
        var retryExpress = expression.replaceAll("_", "");
        // 没有这种情况
        if (retryExpress.length() == expression.length()) {
            return expression;
        }
        try {
            retry.parse(retryExpress);
            return retry.evaluate();
        } catch (ParseException ex) {
            return expression;
        }
    }
}
