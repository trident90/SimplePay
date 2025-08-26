const { ethers } = require("hardhat");
const dotenv = require("dotenv");
dotenv.config();

async function main() {
  const [deployer] = await ethers.getSigners();
  const admin = deployer.address;
  const treasury = process.env.TREASURY;
  const name = process.env.TOKEN_NAME || "SimplePay Token";
  const symbol = process.env.TOKEN_SYMBOL || "SPT";

  if (!treasury) throw new Error("Set TREASURY in .env");

  console.log("Deployer/Admin:", admin);
  console.log("Treasury:", treasury);

  const PayToken = await ethers.getContractFactory("PayToken");
  const token = await PayToken.deploy(name, symbol, admin);
  await token.waitForDeployment();
  console.log("PayToken:", await token.getAddress());

  const PaymentGateway = await ethers.getContractFactory("PaymentGateway");
  const gateway = await PaymentGateway.deploy(await token.getAddress(), admin, treasury);
  await gateway.waitForDeployment();
  console.log("PaymentGateway:", await gateway.getAddress());

  // 권한 예: 별도 운영자에 MINTER_ROLE 부여 가능
  // const MINTER_ROLE = ethers.id("MINTER_ROLE");
  // await (await token.grantRole(MINTER_ROLE, someOperator)).wait();
}

main().catch((e) => { console.error(e); process.exit(1); });