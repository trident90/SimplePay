package com.simplepay.demo;

import com.metadium.did.MetadiumWallet;
import com.simplepay.demo.contracts.PayToken;
import com.simplepay.demo.contracts.PaymentGateway;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

/**
 * 실제 스마트 컨트랙트와 상호작용하는 SimplePayDemo 구현
 */
public class SimplePayDemoWithContracts {
    private static final Logger logger = LoggerFactory.getLogger(SimplePayDemoWithContracts.class);
    
    // 환경 변수에서 읽어올 값들
    private static final String RPC_URL = System.getenv().getOrDefault("RPC_URL", "http://localhost:8545");
    private static final String TOKEN_ADDRESS = System.getenv("TOKEN_ADDR");
    private static final String GATEWAY_ADDRESS = System.getenv("GATEWAY_ADDR");
    private static final String ADMIN_PRIVATE_KEY = System.getenv("ADMIN_PRIVATE_KEY");
    
    private Web3j web3j;
    private String adminAddress;
    private String userAAddress;
    private String userBAddress;
    private PayToken tokenContract;
    private PaymentGateway gatewayContract;
    
    public SimplePayDemoWithContracts() {
        this.web3j = Web3j.build(new HttpService(RPC_URL));
    }
    
    public void runDemo() {
        try {
            logger.info("TOKEN_ADDR from env: {}", TOKEN_ADDRESS);
            logger.info("GATEWAY_ADDR from env: {}", GATEWAY_ADDRESS);
            
            if (TOKEN_ADDRESS == null || GATEWAY_ADDRESS == null) {
                logger.error("TOKEN_ADDR and GATEWAY_ADDR environment variables must be set");
                return;
            }
            
            // 지갑 및 컨트랙트 초기화
            initializeWalletsAndContracts();
            
            // 데모 플로우 실행
            executeDemoFlow();
            
        } catch (Exception e) {
            logger.error("Demo execution failed", e);
        } finally {
            if (web3j != null) {
                web3j.shutdown();
            }
        }
    }
    
    private void initializeWalletsAndContracts() throws Exception {
        // 지갑 주소 설정
        if (ADMIN_PRIVATE_KEY != null) {
            // 실제 private key가 있는 경우 해당 주소를 사용
            Credentials adminCredentials = Credentials.create(ADMIN_PRIVATE_KEY);
            adminAddress = adminCredentials.getAddress();
        } else {
            // 개발용: 기존 keystore에서 admin 주소 사용
            adminAddress = "0x062EE7DC41380F819827959d92BcF13bebA3176E";
            logger.warn("No ADMIN_PRIVATE_KEY provided, using default address: {}", adminAddress);
        }
        
    // UserA, UserB 지갑을 keystore 파일에서 로드
    // 프로젝트 홈은 Wallet이 아닌 그 위의 SimplePay 폴더
    String walletDir = System.getProperty("user.dir");
    String projectHome = new java.io.File(walletDir).getParent();
    String keystoreDir = projectHome + "/keystore";
    String userAKeyPath = keystoreDir + "/user_a";
    String userBKeyPath = keystoreDir + "/user_b";
    String password = "demo";

    Credentials userACredentials = org.web3j.crypto.WalletUtils.loadCredentials(password, userAKeyPath);
    Credentials userBCredentials = org.web3j.crypto.WalletUtils.loadCredentials(password, userBKeyPath);
    userAAddress = userACredentials.getAddress();
    userBAddress = userBCredentials.getAddress();

    logger.info("Admin Address: {}", adminAddress);
    logger.info("UserA Address: {}", userAAddress);
    logger.info("UserB Address: {}", userBAddress);
        
        // 스마트 컨트랙트 인스턴스 생성
        if (ADMIN_PRIVATE_KEY != null) {
            Credentials adminCredentials = Credentials.create(ADMIN_PRIVATE_KEY);
            // Metadium 네트워크용 가스 설정 (100 Gwei gas price)
            StaticGasProvider gasProvider = new StaticGasProvider(
                BigInteger.valueOf(100_000_000_000L), // 100 Gwei gas price
                BigInteger.valueOf(6_700_000L)        // Gas limit
            );
            tokenContract = PayToken.load(TOKEN_ADDRESS, web3j, adminCredentials, gasProvider);
            gatewayContract = PaymentGateway.load(GATEWAY_ADDRESS, web3j, adminCredentials, gasProvider);
        } else {
            logger.warn("ADMIN_PRIVATE_KEY not provided, contract interactions will be simulated");
        }
        
        if (tokenContract != null) {
            logger.info("PayToken instance address: {}", tokenContract.getContractAddress());
            logger.info("PaymentGateway instance address: {}", gatewayContract.getContractAddress());
        }
    }
    
    private String generateRandomAddress() {
        // 시뮬레이션용 랜덤 주소 생성
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            sb.append(Integer.toHexString((int) (Math.random() * 16)));
        }
        return sb.toString();
    }
    
    private void executeDemoFlow() throws Exception {
        // 1. Admin -> UserA에게 1000 토큰 발행
        logger.info("\n1. Minting 1000 tokens to UserA...");
        mintTokens(userAAddress, new BigInteger("1000"));
        
        // 2. Admin에게 300 토큰 발행
        logger.info("\n2. Minting 300 tokens to Admin...");
        mintTokens(adminAddress, new BigInteger("300"));
        
        // 3. Admin -> UserB에게 150 토큰 전송
        logger.info("\n3. Transferring 150 tokens from Admin to UserB...");
        transferTokens(userBAddress, new BigInteger("150"));
        
        // 4. Admin의 출금 요청 시뮬레이션
        logger.info("\n4. Simulating withdrawal from Admin...");
        simulateWithdrawal();
        
        // 최종 잔액 확인
        checkFinalBalances();
    }
    
    private void mintTokens(String toAddress, BigInteger amount) throws Exception {
        BigInteger amountInWei = amount.multiply(BigInteger.TEN.pow(18)); // 18 decimals
        
        if (tokenContract != null) {
            logger.info("PayToken instance address before balance check: {}", tokenContract.getContractAddress());
            BigInteger adminBalanceBefore = tokenContract.balanceOf(adminAddress).send();
            logger.info("Admin balance before minting to {}: {}", toAddress, 
                adminBalanceBefore.divide(BigInteger.TEN.pow(18)));
            
            TransactionReceipt receipt = tokenContract.mint(toAddress, amountInWei).send();
            logger.info("Mint to {} successful. Transaction hash: {}", toAddress, receipt.getTransactionHash());
            
            logger.info("PayToken instance address after minting: {}", tokenContract.getContractAddress());
            BigInteger adminBalanceAfter = tokenContract.balanceOf(adminAddress).send();
            logger.info("Admin balance after minting: {}", adminBalanceAfter.divide(BigInteger.TEN.pow(18)));
        } else {
            logger.info("Simulating mint of {} tokens to {}", amount, toAddress);
            logger.info("Mint successful (simulated)");
        }
    }
    
    private void transferTokens(String toAddress, BigInteger amount) throws Exception {
        BigInteger amountInWei = amount.multiply(BigInteger.TEN.pow(18));
        
        if (tokenContract != null) {
            logger.info("PayToken instance address before balance check: {}", tokenContract.getContractAddress());
            BigInteger adminBalanceBefore = tokenContract.balanceOf(adminAddress).send();
            logger.info("Admin balance before transfer: {}", adminBalanceBefore.divide(BigInteger.TEN.pow(18)));
            
            TransactionReceipt receipt = tokenContract.transfer(toAddress, amountInWei).send();
            logger.info("Transfer successful. Transaction hash: {}", receipt.getTransactionHash());
            
            logger.info("PayToken instance address after transfer: {}", tokenContract.getContractAddress());
            BigInteger adminBalanceAfter = tokenContract.balanceOf(adminAddress).send();
            logger.info("Admin balance after transfer: {}", adminBalanceAfter.divide(BigInteger.TEN.pow(18)));
        } else {
            logger.info("Simulating transfer of {} tokens to {}", amount, toAddress);
            logger.info("Transfer successful (simulated)");
        }
    }
    
    private void simulateWithdrawal() throws Exception {
        // 5초 지연
        TimeUnit.SECONDS.sleep(5);
        
        BigInteger withdrawAmount = new BigInteger("150").multiply(BigInteger.TEN.pow(18));
        
        if (tokenContract != null && gatewayContract != null) {
            logger.info("PayToken instance address before balance check: {}", tokenContract.getContractAddress());
            BigInteger adminBalanceBeforeWithdraw = tokenContract.balanceOf(adminAddress).send();
            logger.info("Admin balance before withdrawal: {}", adminBalanceBeforeWithdraw.divide(BigInteger.TEN.pow(18)));
            
            BigInteger allowanceBefore = tokenContract.allowance(adminAddress, GATEWAY_ADDRESS).send();
            logger.info("Allowance before withdrawal: {}", allowanceBefore.divide(BigInteger.TEN.pow(18)));
            
            // Approve 토큰 사용 허가
            TransactionReceipt approveReceipt = tokenContract.approve(GATEWAY_ADDRESS, withdrawAmount).send();
            logger.info("Approve transaction hash: {}", approveReceipt.getTransactionHash());
            
            BigInteger allowanceAfter = tokenContract.allowance(adminAddress, GATEWAY_ADDRESS).send();
            logger.info("Allowance after approve: {}", allowanceAfter.divide(BigInteger.TEN.pow(18)));
            
            // 출금 요청
            TransactionReceipt withdrawReceipt = gatewayContract.requestWithdraw(withdrawAmount).send();
            logger.info("Withdrawal request successful. Transaction hash: {}", withdrawReceipt.getTransactionHash());
        } else {
            logger.info("Simulating withdrawal of {} tokens", new BigInteger("150"));
            logger.info("Allowance before withdrawal: 0");
            logger.info("Approving {} tokens", new BigInteger("150"));
            logger.info("Allowance after approve: {}", new BigInteger("150"));
            logger.info("Withdrawal request successful (simulated)");
        }
    }
    
    private void checkFinalBalances() throws Exception {
        logger.info("\n--- Final Balances ---");
        
        if (tokenContract != null) {
            BigInteger adminBalance = tokenContract.balanceOf(adminAddress).send();
            BigInteger userABalance = tokenContract.balanceOf(userAAddress).send();
            BigInteger userBBalance = tokenContract.balanceOf(userBAddress).send();
            
            logger.info("Admin: {} PAY", adminBalance.divide(BigInteger.TEN.pow(18)));
            logger.info("UserA: {} PAY", userABalance.divide(BigInteger.TEN.pow(18)));
            logger.info("UserB: {} PAY", userBBalance.divide(BigInteger.TEN.pow(18)));
        } else {
            logger.info("Admin: 150 PAY (estimated)");
            logger.info("UserA: 1000 PAY (estimated)");
            logger.info("UserB: 150 PAY (estimated)");
        }
    }
    
    public static void main(String[] args) {
        try {
            SimplePayDemoWithContracts demo = new SimplePayDemoWithContracts();
            demo.runDemo();
        } catch (Exception e) {
            logger.error("Application failed", e);
            System.exit(1);
        }
    }
}