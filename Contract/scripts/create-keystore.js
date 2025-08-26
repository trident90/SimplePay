const hardhat = require("hardhat");
const { ethers } = hardhat;
const fs = require("fs");
const path = require("path");

async function main() {
  const password = process.env.KEYSTORE_PASSWORD || "";
  if (!password) {
    throw new Error("⚠️  환경변수 KEYSTORE_PASSWORD 를 설정하세요.");
  }

  const outDir = process.env.OUT_DIR || ".";

  // 1) 지갑 준비 (기존 프키 사용 or 신규 생성)
  const pk = process.env.PRIVATE_KEY;
  const wallet = pk
    ? new ethers.Wallet(pk)
    : ethers.Wallet.createRandom();

  console.log("주소:", wallet.address);
  if (!pk) {
    console.log("⚠️ 백업용 니모닉(절대 유출 금지):", wallet.mnemonic?.phrase);
    console.log("🔑 새로 생성된 Private Key:", wallet.privateKey);
  }

  // 2) keystore(JSON) 생성
  console.log(`🔐 keystore 생성 중...`);
  const keystoreJson = await wallet.encrypt(password);

  // 3) 파일명(UTC--...--address) 생성 & 저장
  const now = new Date();
  const pad = (n) => n.toString().padStart(2, "0");
  const ts =
    `${now.getUTCFullYear()}-${pad(now.getUTCMonth() + 1)}-${pad(now.getUTCDate())}` +
    `T${pad(now.getUTCHours())}-${pad(now.getUTCMinutes())}-${pad(now.getUTCSeconds())}.000000000Z`;

  const filename = `UTC--${ts}--${wallet.address.slice(2)}`;
  const outPath = path.join(outDir, filename);

  if (!fs.existsSync(outDir)) fs.mkdirSync(outDir, { recursive: true });
  fs.writeFileSync(outPath, keystoreJson);

  console.log(`✅ 저장 완료: ${outPath}`);
}

main().catch((err) => {
  console.error(err);
  process.exitCode = 1;
});