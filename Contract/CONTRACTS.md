# SimplePay 스마트 컨트랙트 문서

SimplePay 시스템은 두 개의 주요 스마트 컨트랙트로 구성됩니다:
1. **PayToken**: ERC-20 기반 결제 토큰
2. **PaymentGateway**: 토큰 출금 및 결제 처리 게이트웨이

## 1. PayToken 컨트랙트

### 개요
PayToken은 OpenZeppelin의 ERC20, Pausable, AccessControl을 상속받은 표준 ERC-20 토큰입니다.

### 주요 기능

#### 상속된 컨트랙트
- **ERC20**: 표준 토큰 기능 (전송, 승인, 잔액 조회 등)
- **Pausable**: 비상 시 컨트랙트 일시정지 기능
- **AccessControl**: 역할 기반 접근 제어

#### 역할 (Roles)
```solidity
bytes32 public constant MINTER_ROLE = keccak256("MINTER_ROLE");
bytes32 public constant DEFAULT_ADMIN_ROLE; // OpenZeppelin에서 정의
```

- **DEFAULT_ADMIN_ROLE**: 관리자 역할 (pause/unpause, 역할 관리)
- **MINTER_ROLE**: 토큰 발행(mint) 권한

### 함수 명세

#### 생성자 (Constructor)
```solidity
constructor(string memory name_, string memory symbol_, address admin)
```
- **매개변수**:
  - `name_`: 토큰 이름 (예: "PayToken")
  - `symbol_`: 토큰 심볼 (예: "PAY")
  - `admin`: 관리자 주소 (DEFAULT_ADMIN_ROLE과 MINTER_ROLE 부여)

#### 관리자 함수
```solidity
function pause() external onlyRole(DEFAULT_ADMIN_ROLE)
```
- 컨트랙트를 일시정지합니다
- 모든 토큰 전송이 중단됩니다

```solidity
function unpause() external onlyRole(DEFAULT_ADMIN_ROLE)
```
- 컨트랙트 일시정지를 해제합니다

#### 토큰 발행
```solidity
function mint(address to, uint256 amount) external onlyRole(MINTER_ROLE) whenNotPaused
```
- **기능**: 지정된 주소에 토큰을 발행합니다
- **권한**: MINTER_ROLE 필요
- **매개변수**:
  - `to`: 토큰을 받을 주소
  - `amount`: 발행할 토큰 수량 (wei 단위)

#### 토큰 소각
```solidity
function burn(uint256 amount) external whenNotPaused
```
- **기능**: 호출자의 토큰을 소각합니다
- **매개변수**:
  - `amount`: 소각할 토큰 수량 (wei 단위)

### 보안 특징
- **일시정지 기능**: 비상 상황 시 모든 토큰 전송 중단 가능
- **역할 기반 접근 제어**: 관리자와 발행자 권한 분리
- **OpenZeppelin 보안 라이브러리 사용**: 검증된 보안 패턴 적용

---

## 2. PaymentGateway 컨트랙트

### 개요
PaymentGateway는 토큰 출금 요청을 처리하는 게이트웨이 컨트랙트입니다. 사용자가 토큰을 현금으로 환전하고자 할 때 사용됩니다.

### 주요 기능

#### 상속된 컨트랙트
- **AccessControl**: 역할 기반 접근 제어
- **Pausable**: 비상 시 컨트랙트 일시정지 기능

#### 역할 (Roles)
```solidity
bytes32 public constant OPERATOR_ROLE = keccak256("OPERATOR_ROLE");
bytes32 public constant DEFAULT_ADMIN_ROLE; // OpenZeppelin에서 정의
```

- **DEFAULT_ADMIN_ROLE**: 관리자 역할 (pause/unpause, treasury 설정, 역할 관리)
- **OPERATOR_ROLE**: 운영자 역할

#### 상태 변수
```solidity
IERC20 public immutable token;  // PayToken 컨트랙트 주소
address public treasury;        // 토큰이 회수될 금고 주소
```

### 함수 명세

#### 생성자 (Constructor)
```solidity
constructor(IERC20 token_, address admin_, address treasury_)
```
- **매개변수**:
  - `token_`: PayToken 컨트랙트 주소
  - `admin_`: 관리자 주소
  - `treasury_`: 금고 주소 (토큰 회수 대상)

#### 관리자 함수
```solidity
function setTreasury(address newTreasury) external onlyRole(DEFAULT_ADMIN_ROLE)
```
- **기능**: 금고 주소를 변경합니다
- **이벤트**: `TreasuryUpdated` 발생

```solidity
function pause() external onlyRole(DEFAULT_ADMIN_ROLE)
function unpause() external onlyRole(DEFAULT_ADMIN_ROLE)
```
- 컨트랙트 일시정지/해제

#### 출금 요청
```solidity
function requestWithdraw(uint256 amount) external whenNotPaused
```
- **기능**: 사용자가 토큰 출금을 요청합니다
- **프로세스**:
  1. 사용자가 미리 `approve`를 통해 토큰 사용 승인
  2. 토큰이 사용자 → treasury로 이체
  3. `WithdrawRequested` 이벤트 발생
  4. 오프체인에서 현금/포인트 환불 처리
- **매개변수**:
  - `amount`: 출금 요청할 토큰 수량 (wei 단위)

#### 편의 함수
```solidity
function balanceOf(address user) external view returns (uint256)
```
- **기능**: 사용자의 토큰 잔액을 조회합니다 (PayToken.balanceOf 래핑)

### 이벤트

#### WithdrawRequested
```solidity
event WithdrawRequested(address indexed user, uint256 amount, address indexed treasury);
```
- 출금 요청 시 발생
- 오프체인 시스템에서 이 이벤트를 모니터링하여 현금 환불 처리

#### TreasuryUpdated
```solidity
event TreasuryUpdated(address indexed oldTreasury, address indexed newTreasury);
```
- 금고 주소 변경 시 발생

---

## 배포 가이드

### 1. 환경 준비
```bash
cd Contract
npm install
```

### 2. 컴파일
```bash
npx hardhat compile
```

### 3. 배포 스크립트 작성 예시
```javascript
// scripts/deploy.js
const { ethers } = require("hardhat");

async function main() {
    const [deployer] = await ethers.getSigners();
    
    console.log("Deploying contracts with account:", deployer.address);
    
    // 1. PayToken 배포
    const PayToken = await ethers.getContractFactory("PayToken");
    const payToken = await PayToken.deploy(
        "PayToken",           // 토큰 이름
        "PAY",               // 토큰 심볼
        deployer.address     // 관리자 주소
    );
    
    await payToken.waitForDeployment();
    console.log("PayToken deployed to:", await payToken.getAddress());
    
    // 2. PaymentGateway 배포
    const PaymentGateway = await ethers.getContractFactory("PaymentGateway");
    const paymentGateway = await PaymentGateway.deploy(
        await payToken.getAddress(),  // PayToken 주소
        deployer.address,            // 관리자 주소
        deployer.address             // 금고 주소 (초기값)
    );
    
    await paymentGateway.waitForDeployment();
    console.log("PaymentGateway deployed to:", await paymentGateway.getAddress());
}

main().catch((error) => {
    console.error(error);
    process.exit(1);
});
```

### 4. 메타디움 네트워크 배포
```bash
npx hardhat run scripts/deploy.js --network metadium
```

---

## 사용 예시

### 1. Java에서 컨트랙트 사용

#### 토큰 발행 (Mint)
```java
// 관리자 권한으로 토큰 발행
TransactionReceipt receipt = payToken.mint(userAddress, amount).send();
```

#### 토큰 전송 (Transfer)
```java
// 사용자간 토큰 전송
TransactionReceipt receipt = payToken.transfer(toAddress, amount).send();
```

#### 출금 요청 (Withdrawal)
```java
// 1. 먼저 PaymentGateway에 토큰 사용 승인
TransactionReceipt approveReceipt = payToken.approve(gatewayAddress, amount).send();

// 2. 출금 요청
TransactionReceipt withdrawReceipt = paymentGateway.requestWithdraw(amount).send();
```

### 2. Web3.js 사용 예시
```javascript
// 토큰 잔액 조회
const balance = await payToken.methods.balanceOf(userAddress).call();

// 출금 요청
const tx = await paymentGateway.methods.requestWithdraw(amount).send({
    from: userAddress,
    gas: 200000
});
```

---

## 보안 고려사항

### 1. 접근 제어
- 관리자 권한은 다중서명 지갑 사용 권장
- 역할별 권한 분리를 통한 리스크 최소화

### 2. 일시정지 기능
- 비상 상황 시 즉시 컨트랙트 정지 가능
- 보안 사고 대응을 위한 필수 기능

### 3. 금고 관리
- Treasury 주소는 안전한 멀티시그 지갑 사용
- 정기적인 금고 주소 검증 및 모니터링

### 4. 이벤트 모니터링
- 모든 중요한 거래에 대한 이벤트 로깅
- 오프체인 시스템과의 연동을 위한 이벤트 구조

---

## 가스 최적화

### 1. 권장 가스 설정
- **Gas Price**: 81 Gwei (메타디움 네트워크)
- **Gas Limit**: 
  - PayToken 배포: ~2,000,000
  - PaymentGateway 배포: ~1,500,000
  - mint/transfer: ~100,000
  - requestWithdraw: ~150,000

### 2. 배치 처리
대량의 토큰 발행이나 전송은 배치 함수를 구현하여 가스비 절약 가능

---

## 업그레이드 가능성

현재 컨트랙트는 업그레이드 불가능한 구조입니다. 업그레이드가 필요한 경우:
1. 새 버전 컨트랙트 배포
2. 기존 컨트랙트 일시정지
3. 토큰 마이그레이션 절차 수행

프로덕션 환경에서는 OpenZeppelin의 Proxy 패턴 사용을 고려해볼 수 있습니다.