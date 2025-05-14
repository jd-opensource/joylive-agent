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

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Extension(value = "jdk", order = XmlPathParser.ORDER_JDK)
public class JdkXmlPathParser implements XmlPathParser {

    private final DocumentBuilderFactory factory;

    private final XPath xpath;

    private final Map<String, XPathExpression> expressions = new ConcurrentHashMap<>();

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
    }

    @Override
    public String read(InputStream in, String path) {
        if (in == null || path == null || path.isEmpty()) {
            return null;
        }
        try {
            XPathExpression expr = expressions.get(path);
            if (expr == null) {
                expr = xpath.compile(path);
                XPathExpression old = expressions.putIfAbsent(path, expr);
                if (old != null) {
                    expr = old;
                }
            }
            return expr.evaluate(factory.newDocumentBuilder().parse(in));
        } catch (Throwable e) {
            throw new ParseException("Failed to parse XML with JDK, path: " + path, e);
        }
    }
}
