# AWS 백엔드 배포

이 폴더에는 `ALB + ECS Fargate + RDS PostgreSQL + Amazon MQ` 구성을 위한 백엔드 배포 자산이 들어 있습니다.

## 파일
- `api-main.env.example`: Spring API task용 환경 변수 예시
- `fastapi_worker.env.example`: FastAPI worker task용 환경 변수 예시
- `init-amazon-mq.sh`: exchange, queue, DLQ, binding을 초기화하는 1회성 RabbitMQ 스크립트
- `setup-order.md`: 이 저장소 기준 AWS 설정 순서 문서

## 컨테이너 빌드
- Spring API: `docker build -f api-main/Dockerfile -t blogtree-api .`
- FastAPI worker: `docker build -f fastapi_worker/Dockerfile -t blogtree-worker .`

## ECS 구성
- `api-main` service: 인터넷 공개 ALB 뒤에 배치, target group type은 `ip`, container port는 `8080`, health check 경로는 `/health`
- `fastapi_worker` service: ALB 없이 private ECS service로 배치, container port는 `8000`
- 두 서비스 모두 private subnet에 두고, OpenAI와 Google API 호출을 위해 NAT를 통한 outbound를 허용합니다.

## RDS
- PostgreSQL에서 `vector` extension을 사용할 수 있어야 합니다. 초기 Flyway migration에서 `CREATE EXTENSION IF NOT EXISTS vector;`를 실행합니다.
- inbound `5432`는 ECS task security group에서만 허용합니다.

## Amazon MQ
- RabbitMQ는 TLS 포트 `5671`을 사용합니다.
- inbound `5671`은 ECS task security group에서만 허용합니다.
- broker 생성 후 management API 엔드포인트 `443` 또는 `15671`로 `init-amazon-mq.sh`를 1회 실행합니다.

## 비밀값
- 자격 증명과 API 키는 Secrets Manager 또는 SSM Parameter Store에 저장한 뒤 ECS task definition의 `secrets`로 주입합니다.
- secret을 교체하면 새 task가 값을 다시 읽도록 ECS service를 재배포해야 합니다.
