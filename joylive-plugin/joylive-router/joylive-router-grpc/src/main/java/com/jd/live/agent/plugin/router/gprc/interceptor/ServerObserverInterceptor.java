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
package com.jd.live.agent.plugin.router.gprc.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.Status;

import java.lang.reflect.Field;

import static com.jd.live.agent.governance.util.ResponseUtils.labelHeaders;

/**
 * A server observer interceptor that handles exceptions and closes the server call accordingly.
 */
public class ServerObserverInterceptor extends InterceptorAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(ServerObserverInterceptor.class);

    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        try {
            if (Observer.INSTANCE.handle(mc.getArgument(0), mc.getTarget())) {
                mc.skip();
            }
        } catch (Throwable e) {
            mc.skipWithThrowable(e);
        }
    }

    /**
     * A utility class for handling exceptions and closing server calls.
     */
    private static final class Observer {

        /**
         * The singleton instance of the observer.
         */
        private static final Observer INSTANCE = new Observer();

        /**
         * The field representing the server call in the server call stream observer implementation.
         */
        private Field callField;

        /**
         * The field representing whether the server call has been aborted.
         */
        private Field abortedField;

        Observer() {
            try {
                Class<?> serverCallClass = Class.forName("io.grpc.stub.ServerCalls$ServerCallStreamObserverImpl");
                callField = serverCallClass.getDeclaredField("call");
                callField.setAccessible(true);
                abortedField = serverCallClass.getDeclaredField("aborted");
                abortedField.setAccessible(true);
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
        }

        /**
         * Handles an exception by closing the corresponding server call.
         *
         * @param throwable the exception to handle
         * @param target    the target object of the server call
         * @return true if the exception was handled successfully, false otherwise
         * @throws Throwable if an error occurs while handling the exception
         */
        public boolean handle(Throwable throwable, Object target) throws Throwable {
            if (callField == null || abortedField == null) {
                return false;
            }

            ServerCall<?, ?> call = (ServerCall<?, ?>) callField.get(target);
            abortedField.set(target, true);

            Metadata metadata = getMetadata(throwable);
            labelHeaders(throwable, (key, value) -> metadata.put(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER), value));
            call.close(Status.fromThrowable(throwable), metadata);
            return true;
        }

        /**
         * Retrieves metadata from a Throwable object.
         *
         * @param throwable the Throwable object to retrieve metadata from
         * @return the Metadata object containing the trailers from the Throwable, or a new empty Metadata object if no trailers are found
         */
        private Metadata getMetadata(Throwable throwable) {
            Metadata metadata = Status.trailersFromThrowable(throwable);
            if (metadata == null) {
                metadata = new Metadata();
            }
            return metadata;
        }
    }
}
