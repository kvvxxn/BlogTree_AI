package com.navigator.knowledge;

import com.navigator.knowledge.domain.summary.messaging.listener.SummaryTaskListener;
import com.navigator.knowledge.domain.summary.messaging.producer.SummaryTaskProducer;
import com.navigator.knowledge.domain.task.sse.SseEmitterService;
import com.navigator.knowledge.domain.tree.service.KnowledgeService;
import com.navigator.knowledge.global.infra.ai.TextEmbeddingService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = {
        "spring.ai.openai.api-key=test-api-key",
        "spring.ai.openai.embedding.options.model=test-model",
        "oauth2.google.client-id=test-client-id",
        "oauth2.google.client-secret=test-client-secret",
        "jwt.secret=dGVzdC1qd3Qtc2VjcmV0LWZvci1pbnRlZ3JhdGlvbi10ZXN0cw==",
        "jwt.refresh-expiration=1800000",
        "app.cors.allowed-origins=http://localhost:3000",
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.neo4j.Neo4jAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.neo4j.Neo4jDataAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.neo4j.Neo4jRepositoriesAutoConfiguration," +
                "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration"
})
class ApiMainApplicationTests {

    @MockBean
    private SummaryTaskProducer summaryTaskProducer;

    @MockBean
    private SummaryTaskListener summaryTaskListener;

    @MockBean
    private ConnectionFactory connectionFactory;

    @MockBean
    private KnowledgeService knowledgeService;

    @MockBean
    private TextEmbeddingService textEmbeddingService;

    @MockBean
    private SseEmitterService sseEmitterService;

    @Test
    void contextLoads() {
    }

}
