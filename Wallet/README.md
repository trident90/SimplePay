# SimplePay Demo with Metadium DID SDK

이 프로젝트는 Metadium DID SDK의 MetadiumWallet을 사용하여 Contract/scripts/demo-flow.js의 기능을 Java로 구현한 Maven 프로젝트입니다.

## 프로젝트 구조

```
Wallet/
├── pom.xml                                    # Maven 설정 파일
├── README.md                                  # 이 파일
└── src/main/java/com/simplepay/demo/
    ├── SimplePayDemo.java                     # 기본 데모 (시뮬레이션)
    ├── SimplePayDemoWithContracts.java        # 실제 컨트랙트와 상호작용하는 데모
    └── contracts/
        ├── PayToken.java                      # PayToken 컨트랙트 래퍼
        └── PaymentGateway.java                # PaymentGateway 컨트랙트 래퍼
```

## 주요 기능

JavaScript demo-flow.js에서 구현된 다음 기능들을 Java로 재구현합니다:

1. **지갑 생성**: MetadiumWallet을 사용한 Admin, UserA, UserB 지갑 생성
2. **토큰 발행**: Admin이 UserA에게 1000 토큰, 자신에게 300 토큰 발행
3. **토큰 전송**: Admin이 UserB에게 150 토큰 전송
4. **출금 시뮬레이션**: Admin의 150 토큰 출금 요청 처리
5. **잔액 확인**: 모든 계정의 최종 잔액 출력

## 종속성

- **Metadium DID SDK**: 지갑 생성 및 관리
- **Web3j**: 이더리움 블록체인과의 상호작용
- **Jackson**: JSON 처리
- **SLF4J**: 로깅
- **BouncyCastle**: 암호화 지원

## 환경 설정

다음 환경 변수를 설정해야 합니다:

```bash
export RPC_URL="http://localhost:8545"          # 블록체인 RPC URL (기본값)
export TOKEN_ADDR="0x..."                       # PayToken 컨트랙트 주소
export GATEWAY_ADDR="0x..."                     # PaymentGateway 컨트랙트 주소
export ADMIN_PRIVATE_KEY="0x..."                # Admin 계정의 Private Key (선택사항)
```

## 실행 방법

### 1. 종속성 설치
```bash
cd Wallet
mvn clean install
```

### 2. 기본 데모 실행 (시뮬레이션)
```bash
mvn exec:java -Dexec.mainClass="com.simplepay.demo.SimplePayDemo"
```

### 3. 실제 컨트랙트와 상호작용하는 데모 실행
```bash
# 환경 변수 설정 후
mvn exec:java -Dexec.mainClass="com.simplepay.demo.SimplePayDemoWithContracts"
```

## 구현 클래스 설명

### SimplePayDemo.java
- 기본적인 데모 흐름을 시뮬레이션으로 보여주는 클래스
- 실제 블록체인 트랜잭션 없이 로그를 통해 동작을 확인할 수 있음

### SimplePayDemoWithContracts.java
- 실제 스마트 컨트랙트와 상호작용하는 완전한 구현
- TOKEN_ADDR, GATEWAY_ADDR 환경 변수 필수
- 실제 블록체인 트랜잭션을 실행하고 결과를 확인

### contracts/ 패키지
- PayToken.java: ERC20 토큰 컨트랙트의 Java 래퍼
- PaymentGateway.java: 결제 게이트웨이 컨트랙트의 Java 래퍼

## 주의사항

1. 실제 블록체인과 상호작용할 때는 충분한 ETH가 가스비로 필요합니다
2. 컨트랙트 주소가 올바르게 설정되었는지 확인하세요
3. Private Key는 보안에 주의하여 관리하세요
4. 테스트넷에서 먼저 테스트한 후 메인넷에서 사용하세요