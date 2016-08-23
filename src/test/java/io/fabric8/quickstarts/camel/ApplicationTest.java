/**
 *  Copyright 2005-2016 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package io.fabric8.quickstarts.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.NotifyBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ApplicationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CamelContext camelContext;

    @Test
    public void newOrderTest() {
        // Wait for maximum 5s until the first order gets inserted and processed
        NotifyBuilder notify = new NotifyBuilder(camelContext)
            .fromRoute("generate-order")
            .whenDone(1)
            .and()
            .fromRoute("process-order")
            .whenDone(1)
            .create();
        assertThat(notify.matches(5, TimeUnit.SECONDS)).isTrue();

        // Then call the REST API
        ResponseEntity<Order> response = restTemplate.getForEntity("/camel-rest-sql/books/order/1", Order.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Order order = response.getBody();
        assertThat(order.getId()).isEqualTo(1);
        assertThat(order.getAmount()).isBetween(1, 10);
        assertThat(order.getBook().getItem()).isIn("Camel", "ActiveMQ");
        assertThat(order.getBook().getDescription()).isIn("Camel in Action", "ActiveMQ in Action");
        assertThat(order.isProcessed()).isTrue();
    }
}