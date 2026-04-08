package com.navigator.knowledge;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.ai.openai.api-key=test-api-key",
    "spring.ai.openai.embedding.options.model=test-model",
    "oauth2.google.client-id=test-client-id",
    "oauth2.google.client-secret=test-client-secret",
    "jwt.secret=dGVzdC1qd3Qtc2VjcmV0LWZvci1pbnRlZ3JhdGlvbi10ZXN0cw==",
    "spring.autoconfigure.exclude=" +
        "org.springframework.boot.autoconfigure.neo4j.Neo4jAutoConfiguration," +
        "org.springframework.boot.autoconfigure.data.neo4j.Neo4jDataAutoConfiguration," +
        "org.springframework.boot.autoconfigure.data.neo4j.Neo4jRepositoriesAutoConfiguration," +
        "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration"
})
class ApiMainApplicationTests {

    @Test
    void contextLoads() {
    }

}
