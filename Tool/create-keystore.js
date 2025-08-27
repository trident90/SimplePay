const { ethers } = require("ethers");
const fs = require("fs");
const path = require("path");

async function main() {
  // 인자: node create-keystore.js <filename> <password>
  const args = process.argv.slice(2);
  if (args.length < 2) {
    console.error("사용법: node create-keystore.js <filename> <password>");
    process.exit(1);
  }
  const [filename, password, pk] = args;
  const outDir = path.join(__dirname, "../keystore");

  // 1) 지갑 준비 
  const wallet = ethers.Wallet.createRandom();

  console.log("주소:", wallet.address);
  if (!pk || pk === 'false') {
    console.log("⚠️ 백업용 니모닉(절대 유출 금지):", wallet.mnemonic?.phrase);
    console.log("🔑 새로 생성된 Private Key:", wallet.privateKey);
  }

  // 2) keystore(JSON) 생성
  console.log(`🔐 keystore 생성 중...`);
  const keystoreJson = await wallet.encrypt(password);

  // 3) 파일명(UTC--...--address) 생성 & 저장
  const outPath = path.join(outDir, filename);
  if (!fs.existsSync(outDir)) fs.mkdirSync(outDir, { recursive: true });
  fs.writeFileSync(outPath, keystoreJson);
  console.log(`✅ 저장 완료: ${outPath}`);
}

main().catch((err) => {
  console.error(err);
  process.exitCode = 1;
});