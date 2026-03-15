# Environment Setup

Windows 기준으로 `Spring Boot + Kotlin` 본체와 Python 워커를 실행하기 위한 초기 환경 설정 가이드다.

## 1. Install JDK 21

권장: `Microsoft OpenJDK 21` 또는 `Eclipse Temurin 21`

`winget` 예시:

```powershell
winget install --id Microsoft.OpenJDK.21 --exact --accept-package-agreements --accept-source-agreements
```

대안:

```powershell
winget install --id EclipseAdoptium.Temurin.21.JDK --exact --accept-package-agreements --accept-source-agreements
```

설치 후 새 PowerShell을 열고 확인:

```powershell
java -version
$env:JAVA_HOME
```

기대 상태:

- `java -version` 이 `21.x`
- `JAVA_HOME` 이 JDK 설치 경로를 가리킴

## 2. Verify Python

현재 Python 워커는 이미 문법 검증을 통과했다. 다만 실제 실행 전에 버전을 다시 확인한다.

```powershell
python --version
```

권장 버전:

- `Python 3.12+`

## 3. Gradle Strategy

전역 Gradle 설치보다 `Gradle Wrapper` 고정을 권장한다.

현재 저장소는 이미 `Gradle Wrapper` 생성까지 끝난 상태다. 새 PowerShell을 연 뒤 아래만 확인하면 된다.

```powershell
java -version
.\gradlew.bat --version
```

초기 부트스트랩을 다시 해야 하는 경우 순서는 다음이 적절하다.

1. JDK 21 설치
2. 임시로 Gradle 사용 가능 환경 확보
3. 프로젝트 루트에 wrapper 생성
4. 이후부터는 `gradlew.bat` 사용

전역 설치가 꼭 필요하면:

```powershell
winget install --id Gradle.Gradle --exact --accept-package-agreements --accept-source-agreements
```

설치 후 wrapper 생성:

```powershell
gradle wrapper
```

wrapper 생성 후에는 전역 Gradle 대신 아래만 사용:

```powershell
.\gradlew.bat test
.\gradlew.bat :apps:admin-api:test
```

## 4. First Verification

현재 기준 최소 확인 순서:

```powershell
.\gradlew.bat --version
.\gradlew.bat test
```

Python 워커는 각 디렉터리에서 확인:

```powershell
cd python\ingestion-worker
python -m py_compile src\ingestion_worker\app.py

cd ..\rag-orchestrator
python -m py_compile src\rag_orchestrator\app.py
```

## 5. Recommended Next Bootstraps

환경이 준비되면 바로 아래 순서로 진행한다.

1. `identity-access` 도메인 계약 추가
2. `organization-directory` 조직 스코프 계약 추가
3. `admin-api` 의 `GET /admin/auth/me` 를 실제 세션 복원으로 교체
4. `ingestion-worker` 에 crawl source 실행 루프 추가
5. CI 초기 스크립트 고정

## 6. Notes

- Windows에서 `OpenRAG`, `Lightpanda` 같은 Linux 친화 도구를 다룰 때는 `WSL2 + Docker` 경로를 별도 PoC 트랙으로 두는 것이 안전하다.
- 제품 본체는 우선 `Spring Boot + Kotlin + PostgreSQL + Redis` 부트스트랩에 집중한다.
