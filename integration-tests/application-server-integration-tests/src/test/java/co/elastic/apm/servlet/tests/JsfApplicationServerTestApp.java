/*-
 * #%L
 * Elastic APM Java agent
 * %%
 * Copyright (C) 2018 - 2020 Elastic and contributors
 * %%
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * #L%
 */
package co.elastic.apm.servlet.tests;

import co.elastic.apm.servlet.AbstractServletContainerIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class JsfApplicationServerTestApp extends TestApp {

    public JsfApplicationServerTestApp() {
        super("../jsf-app/jsf-app-dependent", "jsf-http-get.war", "/jsf-http-get/status.html", "jsf-http-get");
    }

    @Override
    public void test(AbstractServletContainerIntegrationTest test) throws Exception {
        testJsfRequest(test, "/faces/index.xhtml", "");
        testJsfRequest(test, "/faces/login.xhtml", "?name=Jack");
    }

    private void testJsfRequest(AbstractServletContainerIntegrationTest containerIntegrationTest, String testedPath,
                                String additionalPathInfo) throws IOException, InterruptedException {
        containerIntegrationTest.clearMockServerLog();
        String viewPath = "/jsf-http-get" + testedPath;
        String fullTestPath = viewPath + additionalPathInfo;

        containerIntegrationTest.executeAndValidateRequest(fullTestPath, "HTTP GET", 200, null);
        JsonNode transaction = containerIntegrationTest.assertTransactionReported(viewPath, 200);

        assertThat(transaction.get("name").textValue()).isEqualTo(testedPath);
        assertThat(transaction.get("context").get("service").get("framework").get("name").textValue()).isEqualTo("JavaServer Faces");
        String transactionId = transaction.get("id").textValue();
        List<JsonNode> spans = containerIntegrationTest.assertSpansTransactionId(
            containerIntegrationTest::getReportedSpans,
            transactionId);
        assertThat(spans.size()).isEqualTo(2);
        Iterator<JsonNode> iterator = spans.iterator();
        JsonNode executeSpan = iterator.next();
        assertThat(executeSpan.get("name").textValue()).isEqualTo("JSF Execute");
        assertThat(executeSpan.get("type").textValue()).isEqualTo("template.jsf.execute");
        JsonNode renderSpan = iterator.next();
        assertThat(renderSpan.get("name").textValue()).isEqualTo("JSF Render");
        assertThat(renderSpan.get("type").textValue()).isEqualTo("template.jsf.render");
    }

    private static final class ViewStateAndAction {
        private final String viewState;
        private final String action;

        public ViewStateAndAction(String viewState, String action) {
            this.viewState = viewState;
            this.action = action;
        }

        public String getViewState() {
            return viewState;
        }

        public String getAction() {
            return action;
        }
    }
}
