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
    
    // ../Contract/.env 파일에서 값 읽기
    private static final java.util.Map<String, String> ENV = loadEnv();
    private static final String RPC_URL = ENV.getOrDefault("METADIUM_DEV_URL", "https://api.metadium.com/dev");
    private static final String TOKEN_ADDRESS = ENV.get("TOKEN_ADDR");
    private static final String GATEWAY_ADDRESS = ENV.get("GATEWAY_ADDR");
    private static final String ADMIN_PRIVATE_KEY = ENV.get("PRIVATE_KEY");

    private static java.util.Map<String, String> loadEnv() {
        java.util.Map<String, String> env = new java.util.HashMap<>();
        try {
            String walletDir = System.getProperty("user.dir");
            String projectHome = new java.io.File(walletDir).getParent();
            java.nio.file.Path envPath = java.nio.file.Paths.get(projectHome, "Contract", ".env");
            java.util.List<String> lines = java.nio.file.Files.readAllLines(envPath);
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int idx = line.indexOf('=');
                if (idx > 0) {
                    String key = line.substring(0, idx).trim();
                    String value = line.substring(idx + 1).trim();
                    env.put(key, value);
                }
            }
        } catch (Exception e) {
            System.err.println(".env 파일을 읽을 수 없습니다: " + e.getMessage());
        }
        return env;
    }
    
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
    String password = "iitp69";

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
        // 0. UserA 현금 입금 시 토큰 발행 (오퍼레이터가 호출)
        logger.info("\n0. UserA 현금 입금 시 토큰 발행...");
        callMintTokenOnDeposit(userAAddress, new BigInteger("500"));
        
        // 1. UserB 현금 입금 시 토큰 발행
        logger.info("\n1. UserB 현금 입금 시 토큰 발행...");
        callMintTokenOnDeposit(userBAddress, new BigInteger("300"));
        
        // 2. Admin에게 300 토큰 발행 (운영 자금)
        logger.info("\n2. Minting 300 tokens to Admin...");
        mintTokens(adminAddress, new BigInteger("300"));
        
        // 3. UserA -> UserB에게 200 토큰 전송 (사용자간 P2P 거래)
        logger.info("\n3. UserA transferring 200 tokens to UserB...");
        logger.info("Note: Simulating UserA transfer via Admin (UserA has no ETH for gas)");
        transferTokens(adminAddress, userBAddress, new BigInteger("200"));
        
        // 4. UserB의 200 토큰 출금 요청
        logger.info("\n4. UserB requesting withdrawal of 200 tokens...");
        logger.info("Note: Admin processing UserB's withdrawal request (UserB has no ETH for gas)");
        simulateWithdrawal(new BigInteger("200"));
        
        // 최종 잔액 확인
        checkFinalBalances();
    }
    
    // PaymentGateway의 mintTokenOnDeposit 호출
    private void callMintTokenOnDeposit(String userAddress, BigInteger amount) throws Exception {
        BigInteger amountInWei = amount.multiply(BigInteger.TEN.pow(18)); // 18 decimals
        
        if (gatewayContract != null) {
            logger.info("{} balance before deposit: {}", userAddress, 
                tokenContract.balanceOf(userAddress).send().divide(BigInteger.TEN.pow(18)));
            
            // CompletableFuture 제거하고 직접 호출
            TransactionReceipt receipt = gatewayContract.mintTokenOnDeposit(userAddress, amountInWei).send();
            logger.info("mintTokenOnDeposit to {} successful. TxHash: {}", userAddress, receipt != null ? receipt.getTransactionHash() : "ERROR");
            
            logger.info("{} balance after deposit: {}", userAddress, 
                tokenContract.balanceOf(userAddress).send().divide(BigInteger.TEN.pow(18)));
        } else {
            logger.info("Simulating mintTokenOnDeposit to {} ({} tokens)", userAddress, amount);
        }
    }
    
    private void mintTokens(String toAddress, BigInteger amount) throws Exception {
        BigInteger amountInWei = amount.multiply(BigInteger.TEN.pow(18)); // 18 decimals
        
        if (tokenContract != null) {
            BigInteger toAddressBalanceBefore = tokenContract.balanceOf(toAddress).send();
            logger.info("{} balance before minting: {}", toAddress, toAddressBalanceBefore.divide(BigInteger.TEN.pow(18)));
            
            // CompletableFuture 제거하고 직접 호출
            TransactionReceipt receipt = tokenContract.mint(toAddress, amountInWei).send();
            logger.info("Mint to {} successful. Transaction hash: {}", toAddress, receipt != null ? receipt.getTransactionHash() : "ERROR");
            
            BigInteger toAddressBalanceAfter = tokenContract.balanceOf(toAddress).send();
            logger.info("{} balance after minting: {}", toAddress, toAddressBalanceAfter.divide(BigInteger.TEN.pow(18)));
        } else {
            logger.info("Simulating mint of {} tokens to {}", amount, toAddress);
            logger.info("Mint successful (simulated)");
        }
    }
    
    private void transferTokens(String fromAddress, String toAddress, BigInteger amount) throws Exception {
        BigInteger amountInWei = amount.multiply(BigInteger.TEN.pow(18));
        if (tokenContract != null) {
            BigInteger toAddressBalanceBefore = tokenContract.balanceOf(toAddress).send();
            logger.info("{} balance before transfer: {}", toAddress, toAddressBalanceBefore.divide(BigInteger.TEN.pow(18)));

            // CompletableFuture 제거하고 직접 호출
            TransactionReceipt receipt = tokenContract.transfer(toAddress, amountInWei).send();
            logger.info("Transfer successful (Admin -> UserB simulating UserA -> UserB). Transaction hash: {}", receipt != null ? receipt.getTransactionHash() : "ERROR");

            BigInteger toAddressBalanceAfter = tokenContract.balanceOf(toAddress).send();
            logger.info("{} balance after transfer: {}", toAddress, toAddressBalanceAfter.divide(BigInteger.TEN.pow(18)));
        } else {
            logger.info("Simulating transfer of {} tokens from {} to {}", amount, fromAddress, toAddress);
            logger.info("Transfer successful (simulated)");
        }
    }
    
    private void simulateWithdrawal(BigInteger amount) throws Exception {
        // 1초 지연으로 단축
        TimeUnit.SECONDS.sleep(1);
        
        BigInteger withdrawAmount = amount.multiply(BigInteger.TEN.pow(18));
        
        if (tokenContract != null && gatewayContract != null) {
            BigInteger userBBalanceBeforeWithdraw = tokenContract.balanceOf(userBAddress).send();
            logger.info("UserB balance before withdrawal: {}", userBBalanceBeforeWithdraw.divide(BigInteger.TEN.pow(18)));
            
            BigInteger allowanceBefore = tokenContract.allowance(adminAddress, GATEWAY_ADDRESS).send();
            logger.info("Admin allowance before withdrawal: {}", allowanceBefore.divide(BigInteger.TEN.pow(18)));
            
            // Approve 토큰 사용 허가 (Admin이 대신 처리) - CompletableFuture 제거
            TransactionReceipt approveReceipt = tokenContract.approve(GATEWAY_ADDRESS, withdrawAmount).send();
            logger.info("Admin approve successful. Transaction hash: {}", approveReceipt != null ? approveReceipt.getTransactionHash() : "ERROR");
            
            BigInteger allowanceAfter = tokenContract.allowance(adminAddress, GATEWAY_ADDRESS).send();
            logger.info("Admin allowance after approve: {}", allowanceAfter.divide(BigInteger.TEN.pow(18)));
            
            // 출금 요청 (Admin이 UserB를 대신해서 처리) - CompletableFuture 제거
            TransactionReceipt withdrawReceipt = gatewayContract.requestWithdraw(withdrawAmount).send();
            logger.info("Withdrawal request successful (Admin processing UserB's withdrawal). Transaction hash: {}", withdrawReceipt != null ? withdrawReceipt.getTransactionHash() : "ERROR");
        } else {
            logger.info("Simulating withdrawal of {} tokens", amount);
            logger.info("Admin allowance before withdrawal: 0");
            logger.info("Admin approving {} tokens", amount);
            logger.info("Admin allowance after approve: {}", amount);
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
            logger.info("Admin: 100 PAY (estimated)"); // 300 발행 - 200 전송
            logger.info("UserA: 500 PAY (estimated)"); // 500 입금
            logger.info("UserB: 300 PAY (estimated)"); // 300 입금 + 200 받음 - 200 출금
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