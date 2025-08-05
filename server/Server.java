import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
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

public class Server implements Auction{
    private static ConcurrentHashMap<String, AuctionItem> 
        auctionItems = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, String>
        users = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, String>
        highestBidders = new ConcurrentHashMap<>();
    private static int userID = 0;
    private static int auctionID = 0;
    private static ConcurrentHashMap<String, PublicKey> 
        userKeys = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, String> 
        serverChallenge = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, String> 
        activeTokens = new ConcurrentHashMap<>();

    public Server() {
        super();
    }
    
    public static PrivateKey loadPrivateKey(String filename) throws Exception {
        FileInputStream fis = new FileInputStream(filename);
        byte[] keyBytes = fis.readAllBytes();
        fis.close();
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(spec);
    }
    public int register(String email, PublicKey pkey) throws RemoteException{
        userKeys.put(""+userID, pkey);
        users.put(""+userID, email);
        return userID++;
    }

    public int newAuction(int userID, AuctionSaleItem item, String token) throws RemoteException{
        if (!validateToken(userID, token)) return -1;
        AuctionItem newAuctionItem = new AuctionItem();
        newAuctionItem.itemID = auctionID;
        newAuctionItem.name = item.name;
        newAuctionItem.description = item.description;
        newAuctionItem.highestBid = item.reservePrice;
        auctionItems.put(""+userID+"_"+auctionID, newAuctionItem);
        return auctionID++;
    }

    public AuctionItem getSpec(int userID, int itemID, String token) {
        if (!validateToken(userID, token)) return null;
        Set<String> keys = auctionItems.keySet();
        for (String key : keys) {
            String[] parts = key.split("_");
            if(parts.length ==2 && parts[1].equals(""+itemID)){
                return auctionItems.get(key);
            }
        }
        return null;
    }
    public AuctionItem[] listItems(int userID, String token) throws RemoteException{
        if (!validateToken(userID, token)) return null;
        return auctionItems.values().toArray(new AuctionItem[0]);
    }
    public AuctionResult closeAuction(int userID, int itemID, String token) throws RemoteException{
        if (!validateToken(userID, token)) return null;
        String key = ""+userID+"_"+itemID;
        AuctionItem removedValue = auctionItems.remove(key);
        if(removedValue!=null){
            String email = highestBidders.get(""+itemID);
            AuctionResult res = new AuctionResult();
            res.winningPrice = removedValue.highestBid;
            res.winningEmail = email;
            return res;
        }
        return null;
    }
    public boolean bid(int userID, int itemID, int price, String token) throws RemoteException{
        if (!validateToken(userID, token)) return false;
        AuctionItem auctionItem = this.getSpec(userID, itemID, token);
        //System.out.println("item "+itemID+" highest price is" + auctionItem.highestBid);
        if (auctionItem != null && price > auctionItem.highestBid) {
            auctionItem.highestBid = price;
            String itemKey = "" +itemID;
            // Track highest bidder
            if (!highestBidders.containsKey(itemKey)) {
                    highestBidders.put(itemKey, users.get(""+userID));
            } else {
                highestBidders.replace(itemKey, users.get(""+userID));
            }  
            return true;
        }
        return false;
    }
    //level 4
    public ChallengeInfo challenge(int userID, String clientChallenge) throws RemoteException {
        try {
            PrivateKey serverPrivateKey=loadPrivateKey("../keys/server_private.key");
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(serverPrivateKey);
            signature.update(clientChallenge.getBytes());
            byte[] response = signature.sign();

            ChallengeInfo challengeInfo = new ChallengeInfo();
            challengeInfo.response = response;
            challengeInfo.serverChallenge = generateChallenge();
            serverChallenge.put(""+userID, challengeInfo.serverChallenge);
            return challengeInfo;
        } catch (Exception e) {
            throw new RemoteException("Challenge failed", e);
        }
    }

    public TokenInfo authenticate(int userID, byte[] signature) throws RemoteException {
        try {
            PublicKey clientPublicKey = userKeys.get(""+userID);
            if(clientPublicKey==null){
                System.out.println("Client "+userID+" public key is not exist!");
            }
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(clientPublicKey);
            sig.update(serverChallenge.get(""+userID).getBytes());
            if (sig.verify(signature)) {
                TokenInfo tokenInfo = new TokenInfo();
                tokenInfo.token = generateToken();
                tokenInfo.expiryTime = System.currentTimeMillis() + 10000; // 10 seconds
                String userKey = "" + userID;
                if (!activeTokens.containsKey(userKey)) {
                    activeTokens.put(userKey, tokenInfo.token);
                } else {
                    activeTokens.replace(userKey, tokenInfo.token);
                }
                return tokenInfo;
            } else {
                throw new RemoteException("Authentication failed with no exception!");
            }
        } catch (Exception e) {
            throw new RemoteException("Authentication failed with exception", e);
        }
    }

    private String generateChallenge() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private boolean validateToken(int userID, String token) {
        boolean res = activeTokens.containsKey(""+userID);
        if (!res){
            return false;
        }
        String storedToken = activeTokens.get(""+userID);
        if(!storedToken.equals(token)){
            System.out.println("User "+userID+" request token is "+ token);
            System.out.println("User "+userID+" stored token is "+ storedToken);
            return false;
        }
        return true;

    }

    public static void main(String[] args) {
        try{
            Server s = new Server();
            String name = "Auction";
            Auction stub = (Auction) UnicastRemoteObject.exportObject(s, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(name, stub);
            System.out.println("Server ready");
                
        } catch (Exception e) {
            System.err.println("Exception:");
            e.printStackTrace();
        }
    }
}
