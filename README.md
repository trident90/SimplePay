# SimplePay

SimplePay는 메타디움(Metadium) 블록체인에서 작동하는 토큰 결제 시스템입니다. ERC-20 토큰과 결제 게이트웨이를 통한 안전한 토큰 전송 및 출금 기능을 제공합니다.

## 프로젝트 구조

```
SimplePay/
├── Contract/          # 스마트 컨트랙트 (Hardhat 프로젝트)
│   ├── contracts/     # Solidity 컨트랙트 파일
│   ├── scripts/       # 배포 및 테스트 스크립트
│   └── artifacts/     # 컴파일된 컨트랙트
├── Wallet/           # Java 지갑 애플리케이션 (Maven 프로젝트)
│   └── src/          # Java 소스 코드
└── keystore/         # 지갑 키스토어 파일
```

## 요구사항

### 시스템 요구사항
- **Node.js**: v16.0.0 이상
- **Java**: JDK 11 이상  
- **Maven**: 3.6.0 이상
- **Git**: 최신 버전

### 설치 확인
```bash
node --version    # v16.0.0+
java --version    # JDK 11+
mvn --version     # 3.6.0+
git --version     # 최신 버전
```

## 설치 및 설정

### 1. 저장소 클론

```bash
git clone https://github.com/trident90/SimplePay.git
cd SimplePay
```

### 2. Contract 환경 설정 (Hardhat)

```bash
cd Contract
npm install
```

#### 필수 패키지 설치
```bash
npm install --save-dev hardhat
npm install @openzeppelin/contracts
npm install @nomicfoundation/hardhat-toolbox
```

### 3. Wallet 환경 설정 (Maven)

```bash
cd ../Wallet
mvn clean install
```

## 컨트랙트 배포

### 1. Hardhat 컴파일

```bash
cd Contract
npx hardhat compile
```

### 2. 메타디움 네트워크에 배포

배포 전 `hardhat.config.js`에서 네트워크 설정 확인:

```javascript
module.exports = {
  networks: {
    metadium: {
      url: "https://api.metadium.com/dev",
      accounts: [process.env.DEPLOYER_PRIVATE_KEY],
      gasPrice: 80000000001, // 80 Gwei + 1 wei
    }
  }
};
```

배포 실행:
```bash
npx hardhat run scripts/deploy.js [--config hardhat.config.js] --network metadium
```

## Java 애플리케이션 실행

### 1. 프로젝트 빌드

```bash
cd Wallet
mvn clean compile
```

### 2. 환경 변수 설정

실행 전 다음 환경 변수들을 설정해야 합니다:

```bash
export RPC_URL="https://api.metadium.com/dev"
export TOKEN_ADDR="0x8982D9EE19466fF1AEEa694894d285bF2d585c84"
export GATEWAY_ADDR="0x489E98a3C3D23A18113df4fd0aa810aA68FdB833"
export ADMIN_PRIVATE_KEY="your_private_key_here"
```

### 3. 애플리케이션 실행

#### 간단한 데모 실행:
```bash
mvn exec:java -Dexec.mainClass="com.simplepay.demo.SimplePayDemo"
```

#### 실제 컨트랙트와 상호작용하는 데모 실행:
```bash
RPC_URL="https://api.metadium.com/dev" \
TOKEN_ADDR="0x8982D9EE19466fF1AEEa694894d285bF2d585c84" \
GATEWAY_ADDR="0x489E98a3C3D23A18113df4fd0aa810aA68FdB833" \
ADMIN_PRIVATE_KEY="your_private_key_here" \
mvn exec:java -Dexec.mainClass="com.simplepay.demo.SimplePayDemoWithContracts"
```

## 설정 세부사항

### 가스 설정
- **가스 가격**: 81 Gwei (메타디움 네트워크에 최적화)
- **가스 한도**: 6,700,000

### 지원하는 기능
1. **토큰 발행(Mint)**: 새로운 PAY 토큰 생성
2. **토큰 전송**: 사용자간 토큰 전송
3. **토큰 승인**: 스마트 컨트랙트가 토큰을 사용할 수 있도록 승인
4. **출금 요청**: PaymentGateway를 통한 출금 처리

### 키스토어 관리
- 키스토어 파일은 `keystore/` 디렉토리에 저장
- 프로덕션에서는 강력한 비밀번호 사용 권장

## 트러블슈팅

### 일반적인 오류

#### "transaction underpriced" 오류
메타디움 네트워크의 최소 가스 가격보다 낮게 설정된 경우 발생합니다.
```java
// 가스 가격을 100 Gwei 이상으로 설정
StaticGasProvider gasProvider = new StaticGasProvider(
    BigInteger.valueOf(100_000_000_000L), // 100 Gwei
    BigInteger.valueOf(6_700_000L)
);
```

#### RPC 연결 오류
네트워크 연결 또는 RPC URL 확인:
```bash
curl -X POST -H "Content-Type: application/json" \
  --data '{"jsonrpc":"2.0","method":"eth_blockNumber","params":[],"id":1}' \
  https://api.metadium.com/dev
```

#### Maven 빌드 오류
의존성 재설치:
```bash
mvn clean install -U
```

### 로그 확인
애플리케이션 실행 시 자세한 로그가 출력됩니다:
- 트랜잭션 해시
- 가스 사용량
- 잔액 정보
- 오류 상세 정보

## 개발 가이드

### 새로운 기능 추가
1. Contract 폴더에서 Solidity 컨트랙트 수정
2. `npx hardhat compile`로 컴파일
3. Wallet 폴더에서 Java 코드 수정
4. `mvn compile`로 빌드
5. 테스트 실행

### 테스트 실행
```bash
# Hardhat 테스트
cd Contract
npx hardhat test

# Maven 테스트
cd Wallet
mvn test
```

## 라이센스

이 프로젝트는 MIT 라이센스 하에 배포됩니다.

## 기여하기

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 지원

문제가 발생하면 GitHub Issues를 통해 문의해주세요.

---

**주의사항**: 실제 메인넷에서 사용하기 전에 충분한 테스트를 진행하고, private key를 안전하게 보관하세요.