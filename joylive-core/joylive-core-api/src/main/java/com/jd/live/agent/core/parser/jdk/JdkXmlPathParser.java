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
package com.jd.live.agent.core.parser.jdk;

import com.jd.live.agent.core.exception.ParseException;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.parser.XmlPathParser;
import com.jd.live.agent.core.util.pool.ObjectPool;
import com.jd.live.agent.core.util.pool.robust.RobustObjectPool;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Extension(value = "jdk", order = XmlPathParser.ORDER_JDK)
public class JdkXmlPathParser implements XmlPathParser {

    private final DocumentBuilderFactory factory;

    private final XPath xpath;

    private final Map<String, SoftReference<XPathExpression>> expressions = new ConcurrentHashMap<>();

    private final ObjectPool<DocumentBuilder> pool;

    public JdkXmlPathParser() {
        this.factory = DocumentBuilderFactory.newInstance();
        this.xpath = XPathFactory.newInstance().newXPath();

        try {
            // XXE protection
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        } catch (ParserConfigurationException ignored) {
        }

        pool = new RobustObjectPool<>(() -> {
            try {
                return factory.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                throw new ParseException(e);
            }
        }, 1000);
    }

    @Override
    public String read(InputStream in, String path) {
        if (in == null || !validate(path)) {
            return null;
        }
        XPathExpression expr = Optional.ofNullable(expressions.get(path))
                .map(SoftReference::get)
                .orElseGet(() -> {
                    try {
                        XPathExpression newExpr = xpath.compile(path);
                        expressions.put(path, new SoftReference<>(newExpr));
                        return newExpr;
                    } catch (XPathExpressionException e) {
                        throw new ParseException("Failed to parse XML with JDK, path: " + path, e);
                    }
                });
        DocumentBuilder builder = null;
        try {
            builder = pool.borrow();
            Document document = builder.parse(in);
            return expr.evaluate(document);
        } catch (Throwable e) {
            throw new ParseException("Failed to parse XML with JDK, path: " + path, e);
        } finally {
            pool.release(builder);
        }
    }
}
