const hardhat = require("hardhat");
const { ethers } = hardhat;
console.log("TOKEN_ADDR from .env:", process.env.TOKEN_ADDR);
console.log("GATEWAY_ADDR from .env:", process.env.GATEWAY_ADDR);

async function main() {
  const [admin] = await ethers.getSigners();
  const tokenAddr = process.env.TOKEN_ADDR;
  const gatewayAddr = process.env.GATEWAY_ADDR;

  // userA와 userB를 위한 임의의 새 지갑 생성
  const userA = ethers.Wallet.createRandom();
  const userB = ethers.Wallet.createRandom();

  console.log("Admin:", admin.address);
  console.log("UserA Address:", userA.address);
  console.log("UserB Address:", userB.address);

  const token = await ethers.getContractAt("PayToken", tokenAddr);
  const gateway = await ethers.getContractAt("PaymentGateway", gatewayAddr);
  console.log("PayToken instance address:", await token.getAddress());
  console.log("PaymentGateway instance address:", await gateway.getAddress());

  // 1) Admin -> UserA 에게 1000 토큰 발행
  console.log("\n1. Minting 1000 tokens to UserA...");
  console.log("PayToken instance address before balance check:", await token.getAddress());
  const adminBalanceBeforeMintUserA = await token.balanceOf(admin.address);
  console.log("Admin balance before minting to UserA:", ethers.formatUnits(adminBalanceBeforeMintUserA, 18));
  await (await token.connect(admin).mint(userA.address, ethers.parseUnits("1000", 18))).wait();
  console.log("Mint to UserA successful.");
  console.log("PayToken instance address after minting to UserA:", await token.getAddress());
  const adminBalanceAfterMintUserA = await token.balanceOf(admin.address);
  console.log("Admin balance after minting to UserA:", ethers.formatUnits(adminBalanceAfterMintUserA, 18));

  // 2) Admin이 자신에게 150 토큰 발행
  console.log("\n2. Minting 150 tokens to Admin...");
  console.log("PayToken instance address before balance check:", await token.getAddress());
  const adminBalanceBeforeMintAdmin = await token.balanceOf(admin.address);
  console.log("Admin balance before minting to Admin:", ethers.formatUnits(adminBalanceBeforeMintAdmin, 18));
  await (await token.connect(admin).mint(admin.address, ethers.parseUnits("300", 18))).wait();
  console.log("Mint to Admin successful.");
  console.log("PayToken instance address after minting to Admin:", await token.getAddress());
  const adminBalanceAfterMintAdmin = await token.balanceOf(admin.address);
  console.log("Admin balance after minting to Admin:", ethers.formatUnits(adminBalanceAfterMintAdmin, 18));

  // 3) Admin -> UserB 에게 150 토큰 전송
  console.log("\n3. Transferring 150 tokens from Admin to UserB...");
  console.log("PayToken instance address before balance check:", await token.getAddress());
  const adminBalanceBeforeTransfer = await token.balanceOf(admin.address);
  console.log("Admin balance before transfer:", ethers.formatUnits(adminBalanceBeforeTransfer, 18));
  await (await token.connect(admin).transfer(userB.address, ethers.parseUnits("150", 18))).wait();
  console.log("Transfer successful.");
  console.log("PayToken instance address after transfer:", await token.getAddress());
  const adminBalanceAfterTransfer = await token.balanceOf(admin.address);
  console.log("Admin balance after transfer:", ethers.formatUnits(adminBalanceAfterTransfer, 18));

  // 4) Admin의 출금 요청 시뮬레이션
  console.log("\n4. Simulating withdrawal from Admin...");
  await new Promise(resolve => setTimeout(resolve, 5000)); // 5초 지연
  console.log("PayToken instance address before balance check:", await token.getAddress());
  const adminBalanceBeforeWithdraw = await token.balanceOf(admin.address);
  console.log("Admin balance before withdrawal:", ethers.formatUnits(adminBalanceBeforeWithdraw, 18));
  const allowanceBeforeWithdraw = await token.allowance(admin.address, gatewayAddr);
  console.log("Allowance before withdrawal:", ethers.formatUnits(allowanceBeforeWithdraw, 18));
  await (await token.connect(admin).approve(gatewayAddr, ethers.parseUnits("150", 18))).wait();
  const allowanceAfterApprove = await token.allowance(admin.address, gatewayAddr);
  console.log("Allowance after approve:", ethers.formatUnits(allowanceAfterApprove, 18));
  await (await gateway.connect(admin).requestWithdraw(ethers.parseUnits("150", 18))).wait();
  console.log("Withdrawal request successful.");

  const balAdmin = await token.balanceOf(admin.address);
  const balA = await token.balanceOf(userA.address);
  const balB = await token.balanceOf(userB.address);
  console.log("\n--- Balances ---");
  console.log("Admin:", ethers.formatUnits(balAdmin, 18));
  console.log("UserA:", ethers.formatUnits(balA, 18));
  console.log("UserB:", ethers.formatUnits(balB, 18));
}

main().catch((e)=>{
  console.error(e);
  process.exit(1);
});
