import { ethers } from 'ethers';
import fs from 'fs';

// 방법 1: 파일에서 직접 읽기
async function createWalletFromKeystoreFile(keystorePath, password) {
  try {
    // keystore 파일 읽기
    const keystoreJson = fs.readFileSync(keystorePath, 'utf8');
    
    // Wallet 생성
    const wallet = await ethers.Wallet.fromEncryptedJson(keystoreJson, password);
    
    console.log('Wallet Address:', wallet.address);
    console.log('Private Key:', wallet.privateKey);
    
    return wallet;
  } catch (error) {
    console.error('Wallet 생성 실패:', error.message);
    throw error;
  }
}

// 방법 2: JSON 객체에서 직접 생성
async function createWalletFromKeystoreJson(keystoreJson, password) {
  try {
    // JSON 문자열이면 파싱
    const keystore = typeof keystoreJson === 'string' 
      ? JSON.parse(keystoreJson) 
      : keystoreJson;
    
    // Wallet 생성
    const wallet = await ethers.Wallet.fromEncryptedJson(
      JSON.stringify(keystore), 
      password
    );
    
    console.log('Wallet Address:', wallet.address);
    
    return wallet;
  } catch (error) {
    console.error('Wallet 생성 실패:', error.message);
    throw error;
  }
}

// 방법 3: Provider와 연결하여 사용
async function createConnectedWallet(keystorePath, password, providerUrl) {
  try {
    const keystoreJson = fs.readFileSync(keystorePath, 'utf8');
    const wallet = await ethers.Wallet.fromEncryptedJson(keystoreJson, password);
    
    // Provider 생성 및 연결
    const provider = new ethers.JsonRpcProvider(providerUrl);
    const connectedWallet = wallet.connect(provider);
    
    // 잔액 확인
    const balance = await connectedWallet.provider.getBalance(connectedWallet.address);
    console.log('Balance:', ethers.formatEther(balance), 'ETH');
    
    return connectedWallet;
  } catch (error) {
    console.error('Connected Wallet 생성 실패:', error.message);
    throw error;
  }
}

// 사용 예시
async function main() {
  const keystorePath = './keystore.json';
  const password = "cplabs34";
  const providerUrl = 'https://api.metadium.com/dev';
  
  try {
    // 0. 유효성 검사 
    console.log('=== Keystore 유효성 검사 ===');
    validateKeystoreFile(keystorePath);

    // 1. 기본 Wallet 생성
    console.log('=== 기본 Wallet 생성 ===');
    const wallet1 = await createWalletFromKeystoreFile(keystorePath, password);
    
    // 2. Provider와 연결된 Wallet 생성
    console.log('\n=== Provider 연결된 Wallet ===');
    const wallet2 = await createConnectedWallet(keystorePath, password, providerUrl);
    
    // 3. 트랜잭션 서명 예시
    console.log('\n=== 트랜잭션 서명 ===');
    const tx = {
      to: '0x742d35Cc6634C0532925a3b8D598a15A67f01234',
      value: ethers.parseEther('0.001'),
      gasLimit: 21000,
    };
    
    const signedTx = await wallet2.signTransaction(tx);
    console.log('Signed Transaction:', signedTx);
    
  } catch (error) {
    console.error('실행 실패:', error.message);
  }
}

// 실행
main();

// 추가: keystore 파일 유효성 검사
function validateKeystoreFile(keystorePath) {
  try {
    const keystoreJson = fs.readFileSync(keystorePath, 'utf8');
    const keystore = JSON.parse(keystoreJson);
    
    // Version 3 확인
    if (keystore.version !== 3) {
      throw new Error(`Unsupported keystore version: ${keystore.version}`);
    }
    
    // 필수 필드 확인
    const requiredFields = ['address', 'crypto', 'id', 'version'];
    for (const field of requiredFields) {
      if (!keystore[field]) {
        throw new Error(`Missing required field: ${field}`);
      }
    }
    
    console.log('Keystore 파일이 유효합니다.');
    console.log('Address:', keystore.address);
    console.log('Version:', keystore.version);
    
    return true;
  } catch (error) {
    console.error('Keystore 파일 검증 실패:', error.message);
    return false;
  }
}
