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
package com.jd.live.agent.implement.parser.joox;

import com.jd.live.agent.core.exception.ParseException;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.parser.XmlPathParser;
import com.jd.live.agent.core.util.pool.ObjectPool;
import com.jd.live.agent.core.util.pool.robust.RobustObjectPool;
import org.apache.commons.jxpath.CompiledExpression;
import org.apache.commons.jxpath.JXPathContext;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Extension(value = "jxpath", order = XmlPathParser.ORDER_JXPATH)
public class JXPathParser implements XmlPathParser {

    private final DocumentBuilderFactory factory;

    private final Map<String, SoftReference<CompiledExpression>> expressions = new ConcurrentHashMap<>(128);

    private final ObjectPool<DocumentBuilder> pool;

    public JXPathParser() {
        factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setValidating(false);
        try {
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (ParserConfigurationException ignored) {
        }
        pool = new RobustObjectPool<>(() -> {
            try {
                return factory.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                throw new ParseException(e);
            }
        }, 1000, null);
    }

    @Override
    public String read(InputStream in, String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        DocumentBuilder builder = null;
        try {
            builder = pool.borrow();
            Document doc = builder.parse(in);
            CompiledExpression expr = Optional.ofNullable(expressions.get(path))
                    .map(SoftReference::get)
                    .orElseGet(() -> {
                        CompiledExpression newExpr = JXPathContext.compile(path);
                        expressions.put(path, new SoftReference<>(newExpr));
                        return newExpr;
                    });
            JXPathContext context = JXPathContext.newContext(doc);
            context.setLenient(true);
            return expr.getValue(context).toString();
        } catch (Throwable e) {
            throw new ParseException("Failed to parse XML with JXPath, path: " + path, e);
        } finally {
            pool.release(builder);
        }
    }
}
