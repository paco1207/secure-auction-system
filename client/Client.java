import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;
import java.security.SecureRandom;
import java.util.Base64;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.UUID;

public class Client{
    private String token;
    private PublicKey publicKey;
    private PrivateKey privateKey;

    public static PublicKey loadPublicKey(String filepath) throws Exception {
        byte[] keyBytes = Files.readAllBytes(Paths.get(filepath));
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(spec);
    }

     public static PrivateKey loadPrivateKey(String filename) throws Exception {
        FileInputStream fis = new FileInputStream(filename);
        byte[] keyBytes = fis.readAllBytes();
        fis.close();
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(spec);
    }
    private static String generateChallenge() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Input Seller | Buyer is required!");
            return;
        }
        String clientType = args[0];
        if(!clientType.equals("Seller") && !clientType.equals("Buyer")){
            System.out.println("Input error!"+args[0]);
            return;
        }
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair keyPair = keyGen.generateKeyPair();
        
            // Extract public and private keys
            PublicKey clientPublicKey = keyPair.getPublic();
            PrivateKey clientPrivateKey = keyPair.getPrivate();            
            Registry registry = LocateRegistry.getRegistry("localhost");
            Auction auction = (Auction) registry.lookup("Auction");
            System.out.println("Please type your email for registration:");
            
            //generate user 
            Scanner scanner = new Scanner(System.in);
            String sellerEmail = scanner.nextLine();
            int id = auction.register(sellerEmail, clientPublicKey);
            System.out.println("User ID: " + id + " is registered!");
            
            //step 1 
            Client client = new Client();
            String clientChallenge = generateChallenge();
            //step 2
            ChallengeInfo challengeInfo = auction.challenge(id, clientChallenge);

            // Step 3: Verify server's response using server's public key
            Signature verifySignature = Signature.getInstance("SHA256withRSA");
            PublicKey serverPublicKey = loadPublicKey("../keys/server_public.key");
            verifySignature.initVerify(serverPublicKey);
            verifySignature.update(clientChallenge.getBytes()); // Update with the original clientChallenge
            boolean isVerified = verifySignature.verify(challengeInfo.response); // Verify server's signed response
            if (!isVerified) {
                throw new SecurityException("Server verification failed!"); // Handle verification failure
            }
            // Step 4: Client signs serverChallenge using its private key to generate clientResponse
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(clientPrivateKey);
            signature.update(challengeInfo.serverChallenge.getBytes()); // Sign serverChallenge
            byte[] clientResponse = signature.sign();

            // Step 5: Send clientResponse to server for authentication and receive TokenInfo
            TokenInfo tokenInfo;
            if(clientType.equals("Seller")){
                int sellerID = id;
                int EXPECTED_PRICE = 500;
                // Create new auction items
                AuctionSaleItem saleItem = new AuctionSaleItem();
                saleItem.name = "Painting";
                saleItem.description = "From user "+ sellerID;
                saleItem.reservePrice = 200;
                tokenInfo = auction.authenticate(sellerID, clientResponse);
                //System.out.println("Token for newAuction is "+tokenInfo.token);
                int auctionID = auction.newAuction(sellerID, saleItem, tokenInfo.token);
                System.out.println("Seller "+sellerID+" creates auction ID: " + auctionID);
                while(true){
                    tokenInfo = auction.authenticate(sellerID, clientResponse);
                    //System.out.println("Token for listItems is "+tokenInfo.token);
                    AuctionItem[] allItems = auction.listItems(sellerID, tokenInfo.token);
                    for (AuctionItem item : allItems) {
                        System.out.println("ID: " + item.itemID + ", Name: " + item.name + ", Description: " 
                           + item.description + ", Highest Bid: " + item.highestBid);
                    }
                    tokenInfo = auction.authenticate(sellerID, clientResponse);
                    //System.out.println("Token for getSpec is "+tokenInfo.token);
                    AuctionItem item = auction.getSpec(sellerID, auctionID, tokenInfo.token);
                    if(item !=null && item.highestBid >= EXPECTED_PRICE){
                        // Close the auction and announce the winner
                        tokenInfo = auction.authenticate(sellerID, clientResponse);
                        //System.out.println("Token for closeAuction is "+tokenInfo.token);
                        AuctionResult result = auction.closeAuction(sellerID, auctionID, tokenInfo.token);
                        if (result != null) {
                            System.out.println("Auction" + auctionID + " closed. Winner: " + result.winningEmail + ", Winning Price: " + result.winningPrice);
                        } else {
                            System.out.println("Auction" + auctionID + " closed with no winning bid.");
                        }
                    }
                    Thread.sleep(1000);
                }
            }else{
                int buyerID = id;
                while(true){
                    tokenInfo = auction.authenticate(buyerID, clientResponse);
                    AuctionItem[] items = auction.listItems(buyerID, tokenInfo.token);
                    for (AuctionItem item : items) {
                        System.out.println("ID: " + item.itemID + ", Name: " + item.name + ", Description: " 
                           + item.description + ", Highest Bid: " + item.highestBid);
                        System.out.println("Do you want to bid a price (y/n): ");
                        String ans = scanner.nextLine();
                        if(ans.equals("y")){
                            System.out.println("Please type a price: ");
                            int price = scanner.nextInt();
                            tokenInfo = auction.authenticate(buyerID, clientResponse);
                            //System.out.println("Token for bid is "+tokenInfo.token);
                            if(auction.bid(buyerID, item.itemID, price, tokenInfo.token)){
                                System.out.println("Great, your price is the highest!");
                                System.out.println("Please check seller's response!");
                            }else{
                                System.out.println("Sorry, your price is below the highest!");
                            }
                        }
                        Thread.sleep(1000);
                    }
                    Thread.sleep(1000); 
                }
            }
            //scanner.close();
        }  catch (Exception e) {
            System.err.println("Exception:");
            e.printStackTrace();
        }
    }
}