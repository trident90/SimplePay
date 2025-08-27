require('@nomicfoundation/hardhat-toolbox');
require('dotenv').config();

module.exports = {
  solidity: "0.8.24",
  networks: {
    metadium: {
      url: "https://api.metadium.com/dev",
      accounts: ["0x91ebbcdd7d3a3da370a7d17b94faaed712618ecf957416fde77320d9056de5ec"],
      gasPrice: 81000000001,
    }
  }
};