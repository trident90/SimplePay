const { ethers } = require("ethers");
const fs = require("fs");

async function main() {
  // keystore 파일 경로와 지갑 비밀번호
  const keystorePath = "../keystore/keystore.json";
  const password = process.env.KEYSTORE_PASSWORD || "";

  // 커맨드라인 인자: node send-ether.js <toAddress> <amountInEth>
  const args = process.argv.slice(2);
  if (args.length < 2) {
    console.error("사용법: node send-ether.js <목적지주소> <금액(ETH단위)>");
    process.exit(1);
  }
  const [toAddress, amountInEth] = args;

  if (!password) {
    throw new Error("⚠️  환경변수 KEYSTORE_PASSWORD 를 설정하세요.");
  }

  // 1) keystore JSON 로드
  const keystore = fs.readFileSync(keystorePath, "utf-8");

  // 2) keystore 복호화 → Wallet 객체 생성
  console.log("🔑 keystore 복호화 중...");
  const wallet = await ethers.Wallet.fromEncryptedJson(keystore, password);

  // 3) RPC provider 연결 (환경 변수 사용)
  const rpcUrl = process.env.RPC_URL || "https://api.gov.metadium.club";
  const provider = new ethers.JsonRpcProvider(rpcUrl);
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