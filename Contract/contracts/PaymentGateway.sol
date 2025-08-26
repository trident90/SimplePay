// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import {AccessControl} from "@openzeppelin/contracts/access/AccessControl.sol";
import {Pausable} from "@openzeppelin/contracts/utils/Pausable.sol";
import {IERC20} from "@openzeppelin/contracts/token/ERC20/IERC20.sol";

contract PaymentGateway is AccessControl, Pausable {
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

    /// @notice 편의: 잔액 조회 (ERC-20 balanceOf 래핑)
    function balanceOf(address user) external view returns (uint256) {
        return token.balanceOf(user);
    }
}
