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
package com.jd.live.agent.bootstrap.vm;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.VirtualMachine;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;

/**
 * The {@code LiveVirtualMachine} class extends the {@link VirtualMachine} class and
 * provides methods to load agent libraries and agents into a target JVM.
 * It uses reflection to invoke methods from the {@link HotSpotVirtualMachine} class.
 */
public class LiveVirtualMachine extends VirtualMachine {

    /*
     * The possible errors returned by JPLIS's agentmain
     */
    private static final int JNI_ENOMEM = -4;
    private static final int ATTACH_ERROR_BADJAR = 100;
    private static final int ATTACH_ERROR_NOTONCP = 101;
    private static final int ATTACH_ERROR_STARTFAIL = 102;

    private final VirtualMachine delegate;

    private Method method;

    public LiveVirtualMachine(VirtualMachine delegate) {
        super(delegate.provider(), delegate.id());
        this.delegate = delegate;
        try {
            //addAndExport();
            this.method = delegate.getClass().getDeclaredMethod("execute", String.class, Object[].class);
            if (!this.method.isAccessible()) {
                this.method.setAccessible(true);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            this.method = null;
        }
    }

    private void loadAgentLibrary(String agentLibrary, boolean isAbsolute, String options)
            throws AgentLoadException, AgentInitializationException, IOException {
        try (InputStream in = (InputStream) method.invoke(delegate, "load",
                new Object[]{agentLibrary, isAbsolute ? "true" : "false", options})) {
            int result = readInt(in);
            if (result != 0) {
                throw new AgentInitializationException("Agent_OnAttach failed", result);
            }
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            cause = cause == null ? e : cause;
            if (cause instanceof AgentLoadException) {
                throw (AgentLoadException) cause;
            } else if (cause instanceof AgentInitializationException) {
                throw (AgentInitializationException) cause;
            } else if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw new AgentLoadException(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new AgentLoadException(e.getMessage());
        }
    }

    @Override
    public void loadAgentLibrary(String agentLibrary, String options) throws AgentLoadException, AgentInitializationException, IOException {
        if (method != null) {
            loadAgentLibrary(agentLibrary, false, options);
        } else {
            delegate.loadAgentLibrary(agentLibrary);
        }

    }

    @Override
    public void loadAgentPath(String agentLibrary, String options) throws AgentLoadException, AgentInitializationException, IOException {
        if (method != null) {
            loadAgentLibrary(agentLibrary, true, options);
        } else {
            delegate.loadAgentPath(agentLibrary);
        }
    }

    @Override
    public void loadAgent(String agent, String options) throws AgentLoadException, AgentInitializationException, IOException {
        String args = agent;
        if (options != null) {
            args = args + "=" + options;
        }
        try {
            loadAgentLibrary("instrument", args);
        } catch (AgentLoadException x) {
            throw new InternalError("instrument library is missing in target VM", x);
        } catch (AgentInitializationException x) {
            /*
             * Translate interesting errors into the right exception and
             * message (FIXME: create a better interface to the instrument
             * implementation so this isn't necessary)
             */
            int rc = x.returnValue();
            switch (rc) {
                case JNI_ENOMEM:
                    throw new AgentLoadException("Insuffient memory");
                case ATTACH_ERROR_BADJAR:
                    throw new AgentLoadException("Agent JAR not found or no Agent-Class attribute");
                case ATTACH_ERROR_NOTONCP:
                    throw new AgentLoadException("Unable to add JAR file to system class path");
                case ATTACH_ERROR_STARTFAIL:
                    throw new AgentInitializationException("Agent JAR loaded but agent failed to initialize");
                default:
                    throw new AgentLoadException("Failed to load agent - unknown reason: " + rc);
            }
        }
    }

    @Override
    public Properties getSystemProperties() throws IOException {
        return delegate.getSystemProperties();
    }

    @Override
    public Properties getAgentProperties() throws IOException {
        return delegate.getSystemProperties();
    }

    @Override
    public void startManagementAgent(Properties properties) throws IOException {
        delegate.startManagementAgent(properties);
    }

    @Override
    public String startLocalManagementAgent() throws IOException {
        return delegate.startLocalManagementAgent();
    }

    private int readInt(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();

        // read to \n or EOF
        int n;
        byte[] buf = new byte[1];
        do {
            n = in.read(buf, 0, 1);
            if (n > 0) {
                char c = (char) buf[0];
                if (c == '\n') {
                    break;                  // EOL found
                } else {
                    sb.append(c);
                }
            }
        } while (n > 0);

        if (sb.length() == 0) {
            throw new IOException("Premature EOF");
        }

        String result = sb.toString();
        String prefix = "return code: ";
        if (result.startsWith(prefix))
            result = result.substring(prefix.length());
        int value;
        try {
            value = Integer.parseInt(result);
        } catch (NumberFormatException x) {
            throw new IOException("Non-numeric value found - int expected");
        }
        return value;
    }

    @Override
    public void detach() throws IOException {
        delegate.detach();
    }
}
