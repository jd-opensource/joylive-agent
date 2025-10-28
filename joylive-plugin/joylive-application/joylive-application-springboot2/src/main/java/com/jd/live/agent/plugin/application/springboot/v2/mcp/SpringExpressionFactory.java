/*
 * Copyright © ${year} ${owner} (${email})
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.live.agent.plugin.application.springboot.v2.mcp;

import com.jd.live.agent.governance.mcp.Expression;
import com.jd.live.agent.governance.mcp.ExpressionFactory;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;

/**
 * Spring-based implementation of ExpressionFactory that uses Spring's expression resolution mechanism.
 * Supports both property placeholders and SpEL expressions.
 */
public class SpringExpressionFactory implements ExpressionFactory {

    protected final ConfigurableBeanFactory beanFactory;

    protected final BeanExpressionResolver exprResolver;

    protected final BeanExpressionContext context;

    public SpringExpressionFactory(ConfigurableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        this.exprResolver = beanFactory == null ? null : beanFactory.getBeanExpressionResolver();
        this.context = new BeanExpressionContext(beanFactory, null);
    }

    @Override
    public Expression parse(String expression) {
        if (expression != null && beanFactory != null) {
            return new SpringExpression(beanFactory.resolveEmbeddedValue(expression));
        }
        return null;
    }

    @Override
    public Object evaluate(Expression expression) {
        if (exprResolver != null && expression != null) {
            return expression.isLiteral()
                    ? expression.toString()
                    : exprResolver.evaluate(expression.toString(), context);
        }
        return null;
    }
}
