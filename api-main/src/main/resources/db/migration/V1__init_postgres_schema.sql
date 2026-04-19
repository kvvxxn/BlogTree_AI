CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    profile_image_url VARCHAR(255),
    role VARCHAR(255) NOT NULL,
    career_goal VARCHAR(1000)
);

CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    token VARCHAR(512) NOT NULL
);

CREATE TABLE tasks (
    task_id VARCHAR(36) PRIMARY KEY,
    user_id BIGINT,
    source_url VARCHAR(500),
    task_type VARCHAR(20) NOT NULL,
    status VARCHAR(20),
    error_message TEXT,
    expires_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE knowledge_categories (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT uk_knowledge_categories_user_name UNIQUE (user_id, name)
);

CREATE TABLE knowledge_topics (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL REFERENCES knowledge_categories(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT uk_knowledge_topics_category_name UNIQUE (category_id, name)
);

CREATE TABLE knowledge_keywords (
    id BIGSERIAL PRIMARY KEY,
    topic_id BIGINT NOT NULL REFERENCES knowledge_topics(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT uk_knowledge_keywords_topic_name UNIQUE (topic_id, name)
);

CREATE TABLE summaries (
    summary_id BIGSERIAL PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL UNIQUE REFERENCES tasks(task_id),
    user_id BIGINT,
    source_url VARCHAR(500),
    content TEXT,
    keyword_id BIGINT REFERENCES knowledge_keywords(id),
    embedding VECTOR(1536),
    embedding_model VARCHAR(100),
    created_at TIMESTAMP
);

CREATE TABLE recommendations (
    recommendation_id BIGSERIAL PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL UNIQUE REFERENCES tasks(task_id),
    user_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    category VARCHAR(255) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    keyword VARCHAR(255) NOT NULL,
    created_at TIMESTAMP
);
