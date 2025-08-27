const { ethers } = require("ethers");
require('dotenv').config();
console.log("TOKEN_ADDR from .env:", process.env.TOKEN_ADDR);
console.log("GATEWAY_ADDR from .env:", process.env.GATEWAY_ADDR);

async function main() {
  // 메타디움 네트워크 프로바이더 설정
  const provider = new ethers.JsonRpcProvider(process.env.METADIUM_DEV_URL);
  const admin = new ethers.Wallet(process.env.PRIVATE_KEY, provider);
  
  const tokenAddr = process.env.TOKEN_ADDR;
  const gatewayAddr = process.env.GATEWAY_ADDR;

  // userA와 userB를 위한 임의의 새 지갑 생성 (테스트용)
  const userA = ethers.Wallet.createRandom();
  const userB = ethers.Wallet.createRandom();

  console.log("Admin:", admin.address);
  console.log("UserA Address:", userA.address);
  console.log("UserB Address:", userB.address);

  // 컨트랙트 ABI 로드
  const PayTokenArtifact = require("../Contract/artifacts/contracts/PayToken.sol/PayToken.json");
  const PaymentGatewayArtifact = require("../Contract/artifacts/contracts/PaymentGateway.sol/PaymentGateway.json");
  
  const token = new ethers.Contract(tokenAddr, PayTokenArtifact.abi, admin);
  const gateway = new ethers.Contract(gatewayAddr, PaymentGatewayArtifact.abi, admin);
  
  console.log("PayToken instance address:", tokenAddr);
  console.log("PaymentGateway instance address:", gatewayAddr);

  // PaymentGateway에 MINTER_ROLE 부여 (최초 1회만)
  console.log("\nGranting MINTER_ROLE to PaymentGateway...");
  const MINTER_ROLE = ethers.keccak256(ethers.toUtf8Bytes("MINTER_ROLE"));
  const hasRole = await token.hasRole(MINTER_ROLE, gatewayAddr);
  if (!hasRole) {
    const grantTx = await token.grantRole(MINTER_ROLE, gatewayAddr);
    await grantTx.wait();
    console.log("MINTER_ROLE granted to PaymentGateway. TxHash:", grantTx.hash);
  } else {
    console.log("PaymentGateway already has MINTER_ROLE");
  }

  // 0) UserA가 현금 입금했다고 가정하고 오퍼레이터가 토큰 발행
  console.log("\n0. UserA 현금 입금 시 토큰 발행...");
  console.log("UserA balance before deposit:", ethers.formatUnits(await token.balanceOf(userA.address), 18));
  const mintTx = await gateway.mintTokenOnDeposit(userA.address, ethers.parseUnits("500", 18));
  await mintTx.wait();
  console.log("mintTokenOnDeposit to UserA successful. TxHash:", mintTx.hash);
  console.log("UserA balance after deposit:", ethers.formatUnits(await token.balanceOf(userA.address), 18));

  // 1) UserB도 현금 입금해서 토큰 발행 받기
  console.log("\n1. UserB 현금 입금 시 토큰 발행...");
  console.log("UserB balance before deposit:", ethers.formatUnits(await token.balanceOf(userB.address), 18));
  const mintTxB = await gateway.mintTokenOnDeposit(userB.address, ethers.parseUnits("300", 18));
  await mintTxB.wait();
  console.log("mintTokenOnDeposit to UserB successful. TxHash:", mintTxB.hash);
  console.log("UserB balance after deposit:", ethers.formatUnits(await token.balanceOf(userB.address), 18));

  // 2) Admin에게도 토큰 발행 (운영 자금)
  console.log("\n2. Minting 300 tokens to Admin...");
  const adminBalanceBeforeMint = await token.balanceOf(admin.address);
  console.log("Admin balance before minting:", ethers.formatUnits(adminBalanceBeforeMint, 18));
  const mintAdminTx = await token.mint(admin.address, ethers.parseUnits("300", 18));
  await mintAdminTx.wait();
  console.log("Mint to Admin successful. TxHash:", mintAdminTx.hash);
  const adminBalanceAfterMint = await token.balanceOf(admin.address);
  console.log("Admin balance after minting:", ethers.formatUnits(adminBalanceAfterMint, 18));

  // 3) UserA -> UserB로 200 토큰 전송 (사용자간 P2P 거래)
  console.log("\n3. UserA transferring 200 tokens to UserB...");
  console.log("UserA balance before transfer:", ethers.formatUnits(await token.balanceOf(userA.address), 18));
  console.log("UserB balance before transfer:", ethers.formatUnits(await token.balanceOf(userB.address), 18));
  
  // UserA는 랜덤 지갑이므로 ETH가 없어서 실제로는 거래 불가능
  // 실제 구현에서는 UserA가 private key를 가져야 하거나, 메타트랜잭션 등을 사용해야 함
  // 여기서는 Admin이 UserA를 대신해서 전송하는 것으로 시뮬레이션
  console.log("Note: Simulating UserA transfer via Admin (UserA has no ETH for gas)");
  
  // Admin이 UserA의 토큰을 UserB로 전송 (transferFrom을 사용하려면 approve 필요)
  // 여기서는 간단히 Admin 계정에서 직접 전송으로 시뮬레이션
  const transferTx = await token.transfer(userB.address, ethers.parseUnits("200", 18));
  await transferTx.wait();
  console.log("Transfer successful (Admin -> UserB simulating UserA -> UserB). TxHash:", transferTx.hash);
  console.log("UserA balance after transfer:", ethers.formatUnits(await token.balanceOf(userA.address), 18));
  console.log("UserB balance after transfer:", ethers.formatUnits(await token.balanceOf(userB.address), 18));

  // 4) UserB의 200 토큰 출금 요청
  console.log("\n4. UserB requesting withdrawal of 200 tokens...");
  await new Promise(resolve => setTimeout(resolve, 3000)); // 3초 지연
  
  const userBBalanceBeforeWithdraw = await token.balanceOf(userB.address);
  console.log("UserB balance before withdrawal:", ethers.formatUnits(userBBalanceBeforeWithdraw, 18));
  
  // UserB는 랜덤 지갑이므로 실제로는 거래 불가능
  // Admin이 UserB를 대신해서 출금 처리하는 것으로 시뮬레이션
  console.log("Note: Admin processing UserB's withdrawal request (UserB has no ETH for gas)");
  
  const allowanceBeforeWithdraw = await token.allowance(admin.address, gatewayAddr);
  console.log("Admin allowance before withdrawal:", ethers.formatUnits(allowanceBeforeWithdraw, 18));
  
  const approveTx = await token.approve(gatewayAddr, ethers.parseUnits("200", 18));
  await approveTx.wait();
  console.log("Admin approve successful. TxHash:", approveTx.hash);
  
  const allowanceAfterApprove = await token.allowance(admin.address, gatewayAddr);
  console.log("Admin allowance after approve:", ethers.formatUnits(allowanceAfterApprove, 18));
  
  const withdrawTx = await gateway.requestWithdraw(ethers.parseUnits("200", 18));
  await withdrawTx.wait();
  console.log("Withdrawal request successful (Admin processing UserB's withdrawal). TxHash:", withdrawTx.hash);

  const balAdmin = await token.balanceOf(admin.address);
  const balA = await token.balanceOf(userA.address);
  const balB = await token.balanceOf(userB.address);
  console.log("\n--- Final Balances ---");
  console.log("Admin:", ethers.formatUnits(balAdmin, 18));
  console.log("UserA:", ethers.formatUnits(balA, 18));
  console.log("UserB:", ethers.formatUnits(balB, 18));
}

main().catch((e)=>{
  console.error(e);
  process.exit(1);
});
