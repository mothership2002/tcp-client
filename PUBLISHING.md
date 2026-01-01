# 패키징 및 배포 가이드

TCP Client Spring Boot Starter를 패키징하고 배포하는 방법입니다.

## 현재 빌드 설정

```gradle
// build.gradle
plugins {
    id 'java-library'  // ✅ 라이브러리 플러그인
    id 'org.springframework.boot' version '4.0.1'
    id 'io.spring.dependency-management' version '1.1.7'
}

group = 'hyun'
version = '1.1.1'  // ← 버전 관리

// ✅ 라이브러리이므로 bootJar 비활성화, jar 활성화
tasks.named('bootJar') {
    enabled = false
}

tasks.named('jar') {
    enabled = true
}
```

---

## 빌드 방법

### 1. Plain JAR 빌드

```bash
# JAR 파일 생성
./gradlew jar

# 생성 위치
build/libs/message-connecter-1.1.1.jar
```

**특징:**
- ✅ 의존성 제외 (사용자가 직접 의존성 관리)
- ✅ 라이브러리로 사용 가능
- ✅ 크기 작음 (~50KB)

### 2. Fat JAR 빌드 (선택적)

의존성을 포함한 단일 JAR가 필요한 경우:

```gradle
// build.gradle에 추가
tasks.register('fatJar', Jar) {
    archiveBaseName = 'tcp-client-all'
    from {
        configurations.runtimeClasspath.collect { it.isDirectory() ? it : zipTree(it) }
    }
    with jar
}
```

```bash
./gradlew fatJar

# 생성 위치
build/libs/tcp-client-all-1.1.1.jar  (~20MB)
```

### 3. Sources JAR 생성

소스 코드 포함 JAR:

```gradle
// build.gradle에 추가
java {
    withSourcesJar()
    withJavadocJar()
}
```

```bash
./gradlew build

# 생성 파일
build/libs/
├── message-connecter-1.1.1.jar          # 메인 JAR
├── message-connecter-1.1.1-sources.jar  # 소스 코드
└── message-connecter-1.1.1-javadoc.jar  # JavaDoc
```

---

## 배포 옵션

### 옵션 1: JitPack (가장 쉬움) ⭐ 추천

**장점:**
- ✅ 설정 불필요 (GitHub 저장소만 있으면 됨)
- ✅ 자동 빌드
- ✅ 버전 관리 자동 (Git 태그 기반)
- ✅ 무료

**단점:**
- ⚠️ 첫 요청 시 빌드 시간 소요 (~1분)
- ⚠️ GitHub 공개 저장소만 지원

#### 사용 방법

**1. JitPack 준비 (이미 완료됨)**

현재 저장소가 이미 JitPack 호환:
- ✅ `build.gradle` 존재
- ✅ Git 태그 (`v1.1.1`) 존재
- ✅ Public 저장소

**2. 사용자가 의존성 추가**

```gradle
// 사용자의 build.gradle
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }  // ← JitPack 저장소
}

dependencies {
    implementation 'com.github.mothership2002:tcp-client:v1.1.1'
}
```

**3. 빌드 상태 확인**

https://jitpack.io/#mothership2002/tcp-client

**4. README에 배지 추가**

```markdown
[![](https://jitpack.io/v/mothership2002/tcp-client.svg)](https://jitpack.io/#mothership2002/tcp-client)
```

---

### 옵션 2: Maven Central (공식, 복잡함)

**장점:**
- ✅ 공식 저장소 (신뢰성 높음)
- ✅ `mavenCentral()`만으로 사용 가능
- ✅ 프로덕션 환경에서 선호

**단점:**
- ❌ 초기 설정 복잡 (~1일 소요)
- ❌ Sonatype 계정 필요
- ❌ GPG 서명 필요
- ❌ 도메인 소유 증명 필요 (또는 com.github.username 사용)

#### 필요한 작업

**1. Sonatype JIRA 계정 생성**
- https://issues.sonatype.org 계정 생성
- 티켓 생성하여 그룹 ID 승인 요청
  - `com.github.mothership2002` (GitHub 기반)
  - 또는 소유 도메인 사용

**2. GPG 키 생성 및 업로드**

```bash
# GPG 키 생성
gpg --gen-key

# 공개 키 서버에 업로드
gpg --keyserver keyserver.ubuntu.com --send-keys [KEY_ID]
```

**3. build.gradle 수정**

```gradle
plugins {
    id 'java-library'
    id 'maven-publish'
    id 'signing'
}

group = 'com.github.mothership2002'
version = '1.1.1'

publishing {
    publications {
        mavenJava(MavenPublication) {
            from components.java

            pom {
                name = 'TCP Client Spring Boot Starter'
                description = 'Declarative TCP client library for Spring Boot'
                url = 'https://github.com/mothership2002/tcp-client'

                licenses {
                    license {
                        name = 'MIT License'
                        url = 'https://opensource.org/licenses/MIT'
                    }
                }

                developers {
                    developer {
                        id = 'mothership2002'
                        name = '임동현'
                        email = 'your-email@example.com'
                    }
                }

                scm {
                    connection = 'scm:git:git://github.com/mothership2002/tcp-client.git'
                    developerConnection = 'scm:git:ssh://github.com/mothership2002/tcp-client.git'
                    url = 'https://github.com/mothership2002/tcp-client'
                }
            }
        }
    }

    repositories {
        maven {
            name = "OSSRH"
            url = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
            credentials {
                username = project.findProperty("ossrhUsername") ?: ""
                password = project.findProperty("ossrhPassword") ?: ""
            }
        }
    }
}

signing {
    sign publishing.publications.mavenJava
}
```

**4. gradle.properties 설정**

```properties
# ~/.gradle/gradle.properties (로컬에만 저장)
ossrhUsername=your-jira-username
ossrhPassword=your-jira-password

signing.keyId=your-gpg-key-id
signing.password=your-gpg-password
signing.secretKeyRingFile=/path/to/.gnupg/secring.gpg
```

**5. 배포**

```bash
./gradlew publish

# Sonatype Nexus에서 수동으로 Release
# https://s01.oss.sonatype.org
```

**6. 사용자 의존성**

```gradle
dependencies {
    implementation 'com.github.mothership2002:tcp-client:1.1.1'
}
```

---

### 옵션 3: GitHub Packages

**장점:**
- ✅ GitHub 통합
- ✅ Private 저장소 지원
- ✅ 설정 간단

**단점:**
- ⚠️ GitHub 토큰 필요
- ⚠️ 사용자도 GitHub 인증 필요

#### 사용 방법

**1. build.gradle 수정**

```gradle
publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/mothership2002/tcp-client")
            credentials {
                username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
    publications {
        gpr(MavenPublication) {
            from(components.java)
        }
    }
}
```

**2. 배포**

```bash
./gradlew publish
```

**3. 사용자 설정**

```gradle
// 사용자의 build.gradle
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/mothership2002/tcp-client")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation 'hyun:message-connecter:1.1.1'
}
```

---

### 옵션 4: 수동 배포 (로컬/사설)

**장점:**
- ✅ 완전한 제어
- ✅ 사내망에서 사용 가능

**단점:**
- ❌ 수동 배포 필요
- ❌ 버전 관리 수동

#### 사용 방법

**1. JAR 빌드**

```bash
./gradlew jar

# JAR 파일 복사
cp build/libs/message-connecter-1.1.1.jar /path/to/local-repo/
```

**2. 사용자 설정**

```gradle
// 로컬 저장소
repositories {
    flatDir {
        dirs '/path/to/local-repo'
    }
}

dependencies {
    implementation name: 'message-connecter-1.1.1'
}

// 또는 Maven 로컬
repositories {
    mavenLocal()
}

dependencies {
    implementation 'hyun:message-connecter:1.1.1'
}
```

**3. Maven 로컬에 설치**

```bash
./gradlew publishToMavenLocal

# 설치 위치
~/.m2/repository/hyun/message-connecter/1.1.1/
```

---

## 권장 사항

### 개발 초기 (현재)

**JitPack 사용** ⭐

- 설정 불필요
- 즉시 사용 가능
- GitHub 태그만 관리

```gradle
// 사용자
repositories {
    maven { url 'https://jitpack.io' }
}
dependencies {
    implementation 'com.github.mothership2002:tcp-client:v1.1.1'
}
```

### 프로덕션 환경

**Maven Central 등록**

- 신뢰성 향상
- 기업 환경에서 선호
- 검색 용이성

---

## 버전 관리

### Git 태그 기반 (JitPack)

```bash
# 버전 태그 생성
git tag -a v1.2.0 -m "Release v1.2.0"
git push origin v1.2.0

# JitPack이 자동으로 빌드
# https://jitpack.io/#mothership2002/tcp-client/v1.2.0
```

### build.gradle 버전 (Maven Central)

```gradle
version = '1.2.0'  // ← 수동 업데이트

// 또는 Git 태그에서 자동 추출
version = project.hasProperty('releaseVersion') ?
    project.releaseVersion : '1.2.0-SNAPSHOT'
```

---

## 체크리스트

### 릴리즈 전

- [ ] 버전 업데이트 (build.gradle)
- [ ] CHANGELOG.md 업데이트
- [ ] 모든 테스트 통과
- [ ] Git 태그 생성
- [ ] GitHub 푸시

### 배포 후

- [ ] JitPack 빌드 확인
- [ ] README 배지 업데이트
- [ ] GitHub Release 생성
- [ ] 사용 예제 테스트

---

## 다음 단계

### 현재 (v1.1.1)

**JitPack만 사용** ✅

```markdown
<!-- README.md에 추가 -->
## Installation

### Gradle
```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.mothership2002:tcp-client:v1.1.1'
}
```

### v1.2.0 이후

**Maven Central 등록 검토**

- 사용자 증가 시
- 기업 채택 증가 시
- 커뮤니티 요청 시

---

**현재 추천**: JitPack (설정 불필요, 즉시 사용 가능)
**장기 목표**: Maven Central (공식 저장소 등록)
