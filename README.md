# Secure Auction System (SCC311)

A Java-based secure auction system created for the SCC311 module. This version improves upon earlier implementations by adding RSA-based encryption and secure token authentication to ensure privacy and integrity in client-server auction interactions.

## 🔐 Description

This system simulates a secure online auction platform where:
- Clients interact with a server to place bids
- RSA encryption is used to secure communication
- Tokens and challenge-response methods protect against unauthorized access

## 🧠 Technologies Used

- Java SE
- TCP Sockets
- RSA Encryption (Public/Private Key)
- Token-based authentication
- Challenge-response security model
- Shell scripting

## 💡 Features

- Encrypted communication using RSA
- Secure user authentication with tokens
- Challenge-response mechanism
- Auction items listing and bidding
- Modular server-side class design

## 🗂️ Project Structure

```
SCC311 Secure Auctions/
├── RSAKeyGenerator.java        # Generates RSA keys
├── Auction.java               # Main auction logic
├── server.sh                  # Server startup script
├── Part_B-2.pdf               # Project documentation
├── server/
│   ├── AuctionItem.java
│   ├── AuctionResult.java
│   ├── AuctionSaleItem.java
│   ├── TokenInfo.java
│   ├── ChallengeInfo.java
│   ├── Server.java
```

## 🚀 How to Run

1. Compile the Java files:
   ```bash
   javac server/*.java *.java
   ```

2. Run the server:
   ```bash
   sh server.sh
   ```

3. Run the client (if applicable):
   ```bash
   java Auction
   ```

> Make sure Java is installed.

## 📜 License

This project is licensed under the MIT License. See the `LICENSE` file for details.
