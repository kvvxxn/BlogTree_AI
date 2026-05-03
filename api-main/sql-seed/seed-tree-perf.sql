BEGIN;

WITH u AS (
    INSERT INTO users(email, name, profile_image_url, role, career_goal)
    VALUES (
    'tree-perf@example.com', 'Tree Perf User', 'https://example.com/tree-perf.png', 'USER', 'Backend Developer'
    )
    ON CONFLICT (email) DO
    UPDATE
        SET name = EXCLUDED.name,
        career_goal = EXCLUDED.career_goal
        RETURNING id
),

ins_categories AS (
INSERT
INTO knowledge_categories(user_id, name)
SELECT u.id, format('perf_cat_%02s', c)
FROM u, generate_series(1, 5) c
ON CONFLICT (user_id, name) DO NOTHING
    ),
    categories AS (
SELECT kc.id
FROM knowledge_categories kc
    JOIN u
ON u.id = kc.user_id
WHERE kc.name LIKE 'perf_cat_%'
    )
    , ins_topics AS (
INSERT
INTO knowledge_topics(category_id, name)
SELECT c.id, format('perf_topic_%02s', t)
FROM categories c, generate_series(1, 10) t
ON CONFLICT (category_id, name) DO NOTHING
    ),
    topics AS (
SELECT kt.id
FROM knowledge_topics kt
    JOIN categories c
ON c.id = kt.category_id
WHERE kt.name LIKE 'perf_topic_%'
    )
    , ins_keywords AS (
INSERT
INTO knowledge_keywords(topic_id, name)
SELECT t.id, format('perf_keyword_%02s', k)
FROM topics t, generate_series(1, 10) k
ON CONFLICT (topic_id, name) DO NOTHING
    )
SELECT (SELECT count(*) FROM categories)              AS categories,
       (SELECT count(*) FROM topics)                  AS topics,
       (SELECT count(*)
        FROM knowledge_keywords kk
                 JOIN topics t ON t.id = kk.topic_id) AS keywords;

COMMIT;

BEGIN;

WITH u AS (
    INSERT INTO users(email, name, profile_image_url, role, career_goal)
    SELECT
        format('tree-perf-other-%s@example.com', lpad(n::text, 2, '0')),
        format('Tree Perf Other User %s', lpad(n::text, 2, '0')),
        'https://example.com/tree-perf-other.png',
        'USER',
        'Backend Developer'
    FROM generate_series(1, 10) n
    ON CONFLICT (email) DO UPDATE
        SET name = EXCLUDED.name,
            career_goal = EXCLUDED.career_goal
    RETURNING id, email
),
ins_categories AS (
    INSERT INTO knowledge_categories(user_id, name)
    SELECT u.id, format('perf_cat_%02s', c)
    FROM u, generate_series(1, 5) c
    ON CONFLICT (user_id, name) DO NOTHING
),
categories AS (
    SELECT kc.id, kc.user_id
    FROM knowledge_categories kc
    JOIN u ON u.id = kc.user_id
    WHERE kc.name LIKE 'perf_cat_%'
),
ins_topics AS (
    INSERT INTO knowledge_topics(category_id, name)
    SELECT c.id, format('perf_topic_%02s', t)
    FROM categories c, generate_series(1, 10) t
    ON CONFLICT (category_id, name) DO NOTHING
),
topics AS (
    SELECT kt.id, c.user_id
    FROM knowledge_topics kt
    JOIN categories c ON c.id = kt.category_id
    WHERE kt.name LIKE 'perf_topic_%'
),
ins_keywords AS (
    INSERT INTO knowledge_keywords(topic_id, name)
    SELECT t.id, format('perf_keyword_%02s', k)
    FROM topics t, generate_series(1, 10) k
    ON CONFLICT (topic_id, name) DO NOTHING
)
SELECT
    (SELECT count(*) FROM u) AS users,
    (SELECT count(*) FROM categories) AS categories,
    (SELECT count(*) FROM topics) AS topics,
    (SELECT count(*)
     FROM knowledge_keywords kk
     JOIN topics t ON t.id = kk.topic_id) AS keywords;

COMMIT;
