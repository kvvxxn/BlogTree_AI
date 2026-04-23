# AWS 설정 순서

`ALB + ECS Fargate + RDS PostgreSQL + Amazon MQ` 구성으로 백엔드를 처음 배포할 때는 아래 순서로 진행합니다.

## 1. 리전, 도메인, 인증서
- 먼저 사용할 리전을 하나로 고정합니다. 예: `ap-northeast-2`
- OAuth 설정 전에 API 도메인을 먼저 정합니다.
- 커스텀 도메인에 HTTPS를 붙일 경우 ACM 인증서를 먼저 발급합니다.

## 2. VPC와 서브넷
- 최소 2개 AZ에 걸친 VPC를 생성합니다.
- `ALB`, `NAT Gateway`용 public subnet을 만듭니다.
- ECS task용 private app subnet을 만듭니다.
- `RDS`, `Amazon MQ`용 private data subnet을 만듭니다.
- 백엔드가 OpenAI, Google API를 호출하므로 private subnet의 outbound는 NAT를 통해 나가도록 구성합니다.

## 3. Security Group
- `ALB SG`: 인터넷에서 `80/443` 허용
- `API ECS SG`: `ALB SG`에서 `8080` 허용
- `Worker ECS SG`: public inbound 없음
- `RDS SG`: ECS security group에서만 `5432` 허용
- `Amazon MQ SG`: ECS security group에서 `5671` 허용, 관리용 접근만 `443` 또는 `15671` 허용

## 4. 비밀값 저장
- ECS를 만들기 전에 Secrets Manager 또는 SSM Parameter Store에 비밀값을 저장합니다.
- 최소한 `OPENAI_API_KEY`, `JWT_SECRET`, Google OAuth 값, DB 비밀번호, MQ 비밀번호를 넣습니다.

## 5. ECR
- `api-main`, `fastapi_worker`용 ECR 리포지토리를 각각 생성합니다.
- 저장소의 Dockerfile로 이미지를 빌드하고 push 합니다.
  - `docker build -f api-main/Dockerfile -t blogtree-api .`
  - `docker build -f fastapi_worker/Dockerfile -t blogtree-worker .`

## 6. RDS PostgreSQL
- private PostgreSQL 인스턴스를 생성합니다.
- `vector` extension을 지원하는 엔진 버전을 선택합니다.
- public access는 비활성화합니다.

## 7. Amazon MQ for RabbitMQ
- private RabbitMQ broker를 생성합니다.
- broker user를 생성합니다.
- broker 생성 후 `deploy/aws/init-amazon-mq.sh`를 1회 실행해서 exchange, queue, DLQ, binding을 초기화합니다.

## 8. IAM과 CloudWatch
- `ecsTaskExecutionRole`을 생성하거나 기존 것을 사용합니다.
- 이 역할에 ECR pull, `awslogs`, secret 읽기 권한이 있는지 확인합니다.
- API와 worker용 CloudWatch log group을 생성합니다.

## 9. ECS Cluster와 Task Definition
- ECS cluster를 하나 생성합니다.
- API와 worker용 task definition을 각각 등록합니다.
- 이 폴더의 env 예시 파일을 기준으로 환경 변수를 넣습니다.
- 민감한 값은 plain env가 아니라 ECS `secrets`로 주입합니다.

## 10. ALB와 Target Group
- Spring API용 ALB만 생성합니다.
- target group의 target type은 `ip`로 설정합니다.
- health check 경로는 `/health`로 설정합니다.

## 11. ECS Service
- API service는 ALB 뒤에 붙여 생성합니다.
- worker는 ALB 없이 private ECS service로 생성합니다.
- 초기값은 비용이 최우선이 아니라면 API `desired count = 2`, worker `desired count = 1`로 시작합니다.

## 12. 배포 후 검증
- `https://<api-domain>/health` 확인
- API 로그에서 Flyway migration 성공 확인
- worker 로그에서 RabbitMQ 연결 확인
- RabbitMQ에 queue와 binding이 생성됐는지 확인
- summarize 또는 recommend 작업을 실제로 한 번 end-to-end로 실행

## 참고
- Google OAuth가 아직 준비되지 않았다면, 첫 배포 검증은 dev auth 흐름으로 먼저 진행해도 됩니다.
- 첫 배포는 NAT Gateway 1개로 시작해도 되지만, 운영 안정성은 AZ별 NAT가 더 좋습니다.
