package com.simplepay.demo;

import com.metadium.did.MetadiumWallet;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Convert;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Transfer;
import org.web3j.utils.Numeric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

public class SimplePayDemo {
    private static final Logger logger = LoggerFactory.getLogger(SimplePayDemo.class);
    
    // 환경 변수에서 읽어올 값들 (실제 환경에서는 .env 파일이나 환경변수에서 설정)
    private static final String RPC_URL = System.getenv().getOrDefault("RPC_URL", "http://localhost:8545");
    private static final String TOKEN_ADDRESS = System.getenv("TOKEN_ADDR");
    private static final String GATEWAY_ADDRESS = System.getenv("GATEWAY_ADDR");
    private static final String ADMIN_PRIVATE_KEY = System.getenv("ADMIN_PRIVATE_KEY");
    
    private Web3j web3j;
    private MetadiumWallet adminWallet;
    private MetadiumWallet userAWallet;
    private MetadiumWallet userBWallet;
    
    public SimplePayDemo() {
        this.web3j = Web3j.build(new HttpService(RPC_URL));
    }
    
    public void runDemo() {
        try {
            logger.info("TOKEN_ADDR from env: {}", TOKEN_ADDRESS);
            logger.info("GATEWAY_ADDR from env: {}", GATEWAY_ADDRESS);
            
            // 지갑 초기화
            initializeWallets();
            
            // 데모 플로우 실행
            executeDemoFlow();
            
        } catch (Exception e) {
            logger.error("Demo execution failed", e);
        }
    }
    
    private void initializeWallets() throws Exception {
        // 시뮬레이션용이므로 가상의 주소를 생성
        // 실제 구현에서는 MetadiumWallet.createDid(delegator) 사용
        
        logger.info("Initializing wallets (simulation mode)...");
        
        // 시뮬레이션용 주소들
        String adminAddress = "0x062EE7DC41380F819827959d92BcF13bebA3176E";
        String userAAddress = "0x" + generateRandomAddress();
        String userBAddress = "0x" + generateRandomAddress();
        
        logger.info("Admin Address: {}", adminAddress);
        logger.info("UserA Address: {}", userAAddress);
        logger.info("UserB Address: {}", userBAddress);
        
        // 실제 MetadiumWallet 생성을 위한 주석 처리된 코드:
        // adminWallet = MetadiumWallet.createDid(delegator); // delegator 설정 필요
        // userAWallet = MetadiumWallet.createDid(delegator);
        // userBWallet = MetadiumWallet.createDid(delegator);
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
        // 시뮬레이션용 주소들
        String adminAddress = "0x062EE7DC41380F819827959d92BcF13bebA3176E";
        String userAAddress = "0x" + generateRandomAddress();
        String userBAddress = "0x" + generateRandomAddress();
        
        // 1. Admin -> UserA에게 1000 토큰 발행
        logger.info("\n1. Minting 1000 tokens to UserA...");
        mintTokens(adminAddress, userAAddress, new BigInteger("1000"));
        
        // 2. Admin에게 300 토큰 발행 (JS에서는 150이었지만 원본 코드에서는 300)
        logger.info("\n2. Minting 300 tokens to Admin...");
        mintTokens(adminAddress, adminAddress, new BigInteger("300"));
        
        // 3. Admin -> UserB에게 150 토큰 전송
        logger.info("\n3. Transferring 150 tokens from Admin to UserB...");
        transferTokens(adminAddress, userBAddress, new BigInteger("150"));
        
        // 4. Admin의 출금 요청 시뮬레이션
        logger.info("\n4. Simulating withdrawal from Admin...");
        simulateWithdrawal();
        
        // 최종 잔액 확인
        checkFinalBalances();
    }
    
    private void mintTokens(String fromAddress, String toAddress, BigInteger amount) throws Exception {
        // 실제 구현에서는 PayToken 컨트랙트의 mint 함수를 호출해야 합니다
        // 여기서는 시뮬레이션으로 로그만 출력
        BigInteger amountInWei = amount.multiply(BigInteger.TEN.pow(18)); // 18 decimals
        logger.info("Minting {} tokens to {}", amount, toAddress);
        logger.info("Mint successful");
        
        // 실제 컨트랙트 호출을 위한 예시 코드:
        // PayToken contract = PayToken.load(TOKEN_ADDRESS, web3j, 
        //     Credentials.create(fromWallet.getPrivateKey()), new DefaultGasProvider());
        // TransactionReceipt receipt = contract.mint(toAddress, amountInWei).send();
        // logger.info("Transaction hash: {}", receipt.getTransactionHash());
    }
    
    private void transferTokens(String fromAddress, String toAddress, BigInteger amount) throws Exception {
        BigInteger amountInWei = amount.multiply(BigInteger.TEN.pow(18));
        logger.info("Transferring {} tokens from {} to {}", amount, fromAddress, toAddress);
        logger.info("Transfer successful");
        
        // 실제 컨트랙트 호출을 위한 예시 코드:
        // PayToken contract = PayToken.load(TOKEN_ADDRESS, web3j, 
        //     Credentials.create(privateKey), new DefaultGasProvider());
        // TransactionReceipt receipt = contract.transfer(toAddress, amountInWei).send();
        // logger.info("Transaction hash: {}", receipt.getTransactionHash());
    }
    
    private void simulateWithdrawal() throws Exception {
        // 5초 지연
        TimeUnit.SECONDS.sleep(5);
        
        BigInteger withdrawAmount = new BigInteger("150").multiply(BigInteger.TEN.pow(18));
        
        logger.info("Admin balance before withdrawal check");
        logger.info("Allowance before withdrawal check");
        
        // Approve 토큰 사용 허가
        logger.info("Approving {} tokens for gateway", new BigInteger("150"));
        logger.info("Allowance after approve");
        
        // 출금 요청
        logger.info("Requesting withdrawal of {} tokens", new BigInteger("150"));
        logger.info("Withdrawal request successful");
        
        // 실제 컨트랙트 호출을 위한 예시 코드:
        // PayToken tokenContract = PayToken.load(TOKEN_ADDRESS, web3j, 
        //     Credentials.create(adminPrivateKey), new DefaultGasProvider());
        // tokenContract.approve(GATEWAY_ADDRESS, withdrawAmount).send();
        
        // PaymentGateway gatewayContract = PaymentGateway.load(GATEWAY_ADDRESS, web3j,
        //     Credentials.create(adminPrivateKey), new DefaultGasProvider());
        // gatewayContract.requestWithdraw(withdrawAmount).send();
    }
    
    private void checkFinalBalances() throws Exception {
        logger.info("\n--- Final Balances ---");
        logger.info("Admin: 150 PAY (estimated)");
        logger.info("UserA: 1000 PAY");
        logger.info("UserB: 150 PAY");
        
        // 실제 잔액 확인을 위한 예시 코드:
        // PayToken contract = PayToken.load(TOKEN_ADDRESS, web3j, 
        //     Credentials.create(adminPrivateKey), new DefaultGasProvider());
        // BigInteger adminBalance = contract.balanceOf(adminAddress).send();
        // BigInteger userABalance = contract.balanceOf(userAAddress).send();
        // BigInteger userBBalance = contract.balanceOf(userBAddress).send();
        
        // logger.info("Admin: {} PAY", adminBalance.divide(BigInteger.TEN.pow(18)));
        // logger.info("UserA: {} PAY", userABalance.divide(BigInteger.TEN.pow(18)));
        // logger.info("UserB: {} PAY", userBBalance.divide(BigInteger.TEN.pow(18)));
    }
    
    public static void main(String[] args) {
        try {
            SimplePayDemo demo = new SimplePayDemo();
            demo.runDemo();
        } catch (Exception e) {
            logger.error("Application failed", e);
            System.exit(1);
        }
    }
}