package com.example.crud2;

import com.example.crud2.model.PaymentStatus;
import com.example.crud2.model.Transaction;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ApplicationTest {

    @Autowired
    WebApplicationContext wac;

    WebTestClient client;
    static int request = 0;

    @BeforeEach
    void setUp() {
        client = MockMvcWebTestClient.bindToApplicationContext(this.wac).build();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void scn1() {
        String uri = "/transactions/" + ++request;
        client.post().uri("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "totalAmount": 99.95,
                          "paymentType": "CREDIT_CARD",
                          "paymentStatus": "NEW",
                          "items": [
                            {
                              "name": "t-shirt",
                              "price": 19.99,
                              "quantity": 5
                            }
                          ]
                        }
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().location("http://localhost" + uri);

        client.patch().uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                            {
                               "paymentStatus": "AUTHORIZED"
                            }
                        """)
                .exchange()
                .expectStatus().isOk();

        client.patch().uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                            {
                                "paymentStatus": "CAPTURED"
                            }
                        """)
                .exchange()
                .expectStatus().isOk();

        EntityExchangeResult<Transaction> result = client.get().uri(uri)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Transaction.class).returnResult();
        MatcherAssert.assertThat("payment status",
                result.getResponseBody().getPaymentStatus(),
                Matchers.is(PaymentStatus.CAPTURED));

    }

    @Test
    void scn2() {
        request++;
        String uri = "/transactions/" + request;
        System.out.println(uri);
        client.post().uri("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                           "totalAmount": 238,
                           "paymentType": "PAYPAL",
                           "paymentStatus": "NEW",
                           "items": [
                             {
                               "name": "bike",
                               "price": 208,
                               "quantity": 1
                             },
                             {
                               "name": "shoes",
                               "price": 30,
                               "quantity": 1
                             }
                           ]
                         }
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().location("http://localhost" + uri);

        client.patch().uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                            {
                               "paymentStatus": "CANCELED"
                            }
                        """)
                .exchange()
                .expectStatus().isOk();

        EntityExchangeResult<Transaction> result = client.get().uri(uri)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Transaction.class).returnResult();
        MatcherAssert.assertThat("payment status",
                result.getResponseBody().getPaymentStatus(),
                Matchers.is(PaymentStatus.CANCELED));
    }

    @Test
    void capturedBadRequest() {
        String uri = "/transactions/" + ++request;
        client.post().uri("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "totalAmount": 99.95,
                          "paymentType": "CREDIT_CARD",
                          "paymentStatus": "NEW",
                          "items": [
                            {
                              "name": "t-shirt",
                              "price": 19.99,
                              "quantity": 5
                            }
                          ]
                        }
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().location("http://localhost" + uri);

        client.patch().uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                            {
                               "paymentStatus": "CAPTURED"
                            }
                        """)
                .exchange()
                .expectStatus().isBadRequest();

    }

}
