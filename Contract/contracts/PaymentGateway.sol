// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {AccessControl} from "@openzeppelin/contracts/access/AccessControl.sol";
import {Pausable} from "@openzeppelin/contracts/utils/Pausable.sol";
import {IERC20} from "@openzeppelin/contracts/token/ERC20/IERC20.sol";

contract PaymentGateway is AccessControl, Pausable {
    event Deposit(address indexed user, uint256 amount);
    bytes32 public constant OPERATOR_ROLE = keccak256("OPERATOR_ROLE");

    IERC20 public immutable token;
    address public treasury;

    event WithdrawRequested(address indexed user, uint256 amount, address indexed treasury);
    event TreasuryUpdated(address indexed oldTreasury, address indexed newTreasury);

    constructor(IERC20 token_, address admin_, address treasury_) {
        require(address(token_) != address(0), "token required");
        require(admin_ != address(0), "admin required");
        require(treasury_ != address(0), "treasury required");

        token = token_;
        treasury = treasury_;

        _grantRole(DEFAULT_ADMIN_ROLE, admin_);
        _grantRole(OPERATOR_ROLE, admin_);
    }

    function setTreasury(address newTreasury) external onlyRole(DEFAULT_ADMIN_ROLE) {
        require(newTreasury != address(0), "invalid");
        emit TreasuryUpdated(treasury, newTreasury);
        treasury = newTreasury;
    }

    function pause() external onlyRole(DEFAULT_ADMIN_ROLE) { _pause(); }
    function unpause() external onlyRole(DEFAULT_ADMIN_ROLE) { _unpause(); }

    /// @notice 사용자가 출금을 요청하면 토큰이 금고로 이체(회사 회수)
    /// 오프체인 정산(현금/포인트 환불)은 백오피스에서 처리
    function requestWithdraw(uint256 amount) external whenNotPaused {
        require(amount > 0, "amount=0");
        require(token.transferFrom(msg.sender, treasury, amount), "transfer failed");
        emit WithdrawRequested(msg.sender, amount, treasury);
    }

    /// @notice 현금 입금 시 오퍼레이터가 토큰을 발행 (mint)
    function mintTokenOnDeposit(address user, uint256 amount) external onlyRole(OPERATOR_ROLE) whenNotPaused {
        require(user != address(0), "user required");
        require(amount > 0, "amount=0");
        // IERC20에 mint 함수가 있어야 함 (PayToken 등에서 지원)
        // 아래는 인터페이스에 mint가 있다고 가정
        // 실제 PayToken은 mint(address,uint256) 함수 필요
        // (ERC20 기본에는 없음, 확장 필요)
        // @openzeppelin/contracts/token/ERC20/extensions/ERC20Mintable.sol 등 사용 가능
        // token.mint(user, amount);
        (bool success, ) = address(token).call(abi.encodeWithSignature("mint(address,uint256)", user, amount));
        require(success, "mint failed");
        emit Deposit(user, amount);
    }

    /// @notice 편의: 잔액 조회 (ERC-20 balanceOf 래핑)
    function balanceOf(address user) external view returns (uint256) {
        return token.balanceOf(user);
    }
}
