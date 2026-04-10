MERGE (u:User {userId: 1})

MERGE (u)-[:OWNS_CATEGORY]->(:Category {name: 'Backend'})
MERGE (u)-[:OWNS_CATEGORY]->(:Category {name: 'Database'})
MERGE (u)-[:OWNS_CATEGORY]->(:Category {name: 'Infra'})

WITH u
MATCH (u)-[:OWNS_CATEGORY]->(backend:Category {name: 'Backend'})
MERGE (backend)-[:HAS_TOPIC]->(:Topic {name: 'Spring'})
MERGE (backend)-[:HAS_TOPIC]->(:Topic {name: 'Java'})

WITH u
MATCH (u)-[:OWNS_CATEGORY]->(database:Category {name: 'Database'})
MERGE (database)-[:HAS_TOPIC]->(:Topic {name: 'MySQL'})
MERGE (database)-[:HAS_TOPIC]->(:Topic {name: 'Redis'})

WITH u
MATCH (u)-[:OWNS_CATEGORY]->(infra:Category {name: 'Infra'})
MERGE (infra)-[:HAS_TOPIC]->(:Topic {name: 'Docker'})

WITH u
MATCH (u)-[:OWNS_CATEGORY]->(:Category {name: 'Backend'})-[:HAS_TOPIC]->(spring:Topic {name: 'Spring'})
MERGE (spring)-[:HAS_KEYWORD]->(:Keyword {name: 'Spring Boot'})
MERGE (spring)-[:HAS_KEYWORD]->(:Keyword {name: 'JPA'})

WITH u
MATCH (u)-[:OWNS_CATEGORY]->(:Category {name: 'Backend'})-[:HAS_TOPIC]->(java:Topic {name: 'Java'})
MERGE (java)-[:HAS_KEYWORD]->(:Keyword {name: 'Stream'})
MERGE (java)-[:HAS_KEYWORD]->(:Keyword {name: 'Optional'})

WITH u
MATCH (u)-[:OWNS_CATEGORY]->(:Category {name: 'Database'})-[:HAS_TOPIC]->(mysql:Topic {name: 'MySQL'})
MERGE (mysql)-[:HAS_KEYWORD]->(:Keyword {name: 'Index'})
MERGE (mysql)-[:HAS_KEYWORD]->(:Keyword {name: 'Transaction'})

WITH u
MATCH (u)-[:OWNS_CATEGORY]->(:Category {name: 'Database'})-[:HAS_TOPIC]->(redis:Topic {name: 'Redis'})
MERGE (redis)-[:HAS_KEYWORD]->(:Keyword {name: 'Cache'})

WITH u
MATCH (u)-[:OWNS_CATEGORY]->(:Category {name: 'Infra'})-[:HAS_TOPIC]->(docker:Topic {name: 'Docker'})
MERGE (docker)-[:HAS_KEYWORD]->(:Keyword {name: 'Container'})
MERGE (docker)-[:HAS_KEYWORD]->(:Keyword {name: 'Compose'})
