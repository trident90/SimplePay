const hardhat = require("hardhat");
const { ethers } = hardhat;
const fs = require("fs");

async function main() {
  // keystore 파일 경로와 지갑 비밀번호
  const keystorePath = "keystore/keystore.json";
  const password = process.env.KEYSTORE_PASSWORD || "";

  // 대상 주소와 전송 금액
  const toAddress = "0xeCC4e71B649A5f367d9Cf694D63Bf04bc6aaB0b6";   // <-- 목적지 주소
  const amountInEth = "10.0";                  // <-- 전송할 금액 (ETH 단위)

  if (!password) {
    throw new Error("⚠️  환경변수 KEYSTORE_PASSWORD 를 설정하세요.");
  }

  // 1) keystore JSON 로드
  const keystore = fs.readFileSync(keystorePath, "utf-8");

  // 2) keystore 복호화 → Wallet 객체 생성
  console.log("🔑 keystore 복호화 중...");
  const wallet = await ethers.Wallet.fromEncryptedJson(keystore, password);

  // 3) Metadium dev RPC provider 연결
  const provider = new ethers.JsonRpcProvider("https://api.gov.metadium.club");
  const signer = wallet.connect(provider);

  console.log("지갑 주소:", await signer.getAddress());
  console.log("잔액:", ethers.formatEther(await provider.getBalance(signer.address)), "META");

  // 4) 전송 트랜잭션 작성 및 전송
  const tx = await signer.sendTransaction({
    to: toAddress,
    value: ethers.parseEther(amountInEth),
    gasLimit: 21000n, // 단순 송금
  });

  console.log("📤 전송중... TxHash:", tx.hash);

  const receipt = await tx.wait();
  if (receipt) {
    console.log("✅ 완료! 블록:", receipt.blockNumber);
  } else {
    console.log("❌ 트랜잭션 실패!");
  }
}

main().catch((err) => {
  console.error(err);
  process.exitCode = 1;
});