package com.navigator.knowledge.domain.task.mq.consumer;

import com.navigator.knowledge.domain.task.mq.dto.SummaryTaskResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SummaryTaskListener {

    private static final String RESPONSE_QUEUE = "summary.response.queue";

    @RabbitListener(queues = RESPONSE_QUEUE)
    public void receiveSummaryResponse(SummaryTaskResponseDto responseDto) {
        log.info("Receive a summary task result from FastAPI. Task ID: {}, Status: {}",
            responseDto.taskId(), responseDto.status());

        switch (responseDto.status()) {
            case "SUCCESS" -> handleSuccess(responseDto);
            case "PARTIAL_SUCCESS" -> handlePartialSuccess(responseDto);
            case "FAILED" -> handleFailure(responseDto);
            default -> log.warn("Unknown Status In Summary Response: {}", responseDto.status());
        }
    }

    private void handleSuccess(SummaryTaskResponseDto responseDto) {
        var data = responseDto.data();

        // 1. MySQL: Task 상태를 '완료'로 업데이트
        // 2. VectorDB: data.summaryContent() 저장 (유사도 검색용)
        // 3. Neo4j: data.knowledgeTree()의 카테고리-토픽-키워드 노드 및 관계 생성

        log.info("Summary and keyword extraction successful. Category: {}, Topic: {}, Keywords: {}", data.knowledgeTree().category(), data.knowledgeTree().topic(), data.knowledgeTree().keyword());
    }

    private void handlePartialSuccess(SummaryTaskResponseDto responseDto) {
        var data = responseDto.data();

        // 1. MySQL: Task 상태를 '부분 완료'로 업데이트
        // 2. VectorDB: data.summaryContent() 와 비슷한 vector의 category-topic-keyword 가져오기
        // 3. Neo4j: 가져온 category-topic-node 생성

        log.warn("Summary succeeded but keyword extraction failed. (Task ID: {})", responseDto.taskId());
    }

    private void handleFailure(SummaryTaskResponseDto responseDto) {
        var error = responseDto.error();

        // 1. MySQL: Task 상태를 '실패'로 업데이트하고, 에러 코드와 메시지 기록

        log.error("Task failed! Error Code: {}, Message: {}", error.code(), error.message());
    }
}