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
package com.jd.live.agent.core.bootstrap.env.http;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.bootstrap.EnvSupplier;
import com.jd.live.agent.core.bootstrap.env.AbstractEnvSupplier;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.util.http.HttpResponse;
import com.jd.live.agent.core.util.http.HttpStatus;
import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.core.util.template.Template;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.jd.live.agent.core.util.StringUtils.choose;
import static com.jd.live.agent.core.util.StringUtils.isEmpty;

@Injectable
@Extension(value = "HttpEnvSupplier", order = EnvSupplier.ORDER_HTTP_ENV_SUPPLIER)
public class HttpEnvSupplier extends AbstractEnvSupplier {

    private static final Logger logger = LoggerFactory.getLogger(HttpEnvSupplier.class);

    @Config("env.http.url")
    private String url;

    @Config("env.http.parameters")
    private Map<String, String> parameters;

    @Config("app.name")
    private String application;

    @Config("app.service.namespace")
    private String namespace;

    @Inject(ObjectParser.JSON)
    private ObjectParser parser;

    @Override
    public void process(Map<String, Object> env) {
        if (isEmpty(url)) {
            logger.info("Ignore loading env from http, caused by empty url.");
            return;
        }
        String app = choose(application, (String) env.get(Application.KEY_APPLICATION_NAME));
        String ns = choose(namespace, (String) env.get(Application.KEY_APPLICATION_SERVICE_NAMESPACE));
        if (isEmpty(app)) {
            logger.info("Ignore loading env from http, caused by empty application name.");
            return;
        } else if (isEmpty(ns)) {
            logger.info("Ignore loading env from http, caused by empty service namespace.");
            return;
        }
        try {
            Template template = Template.parse(url);
            Map<String, String> context = new HashMap<>();
            context.put("space_id", ns);
            context.put("application", app);
            String newUrl = template.render(context, false);
            logger.info("load env from " + newUrl);
            HttpResponse<HttpEnvResponse> response = HttpUtils.get(newUrl,
                    cnn -> Optional.ofNullable(parameters).ifPresent(p -> p.forEach(cnn::setRequestProperty)),
                    reader -> parser.read(reader, HttpEnvResponse.class));
            if (response.getStatus() == HttpStatus.OK) {
                HttpEnvResponse resp = response.getData();
                HttpEnvError error = resp.getError();
                if (error == null) {
                    resp.getData().forEach((k, v) -> env.putIfAbsent(k.toString(), v));
                } else {
                    logger.error("Failed to load env from " + url + ", code=" + error.getCode() + ", message=" + error.getMessage());
                }
            } else {
                logger.error("Failed to load env from " + url + ", status=" + response.getCode() + ", message=" + response.getMessage());
            }
        } catch (Throwable e) {
            logger.error("Failed to load env from " + url + ", caused by " + e.getMessage());
        }
    }
}
