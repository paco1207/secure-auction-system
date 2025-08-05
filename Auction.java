import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.PublicKey;

public interface Auction extends Remote {
    public int register(String email, PublicKey pkey) throws RemoteException;
    public ChallengeInfo challenge(int userID, String clientChallenge) throws RemoteException;
    public TokenInfo authenticate(int userID, byte signature[]) throws RemoteException;
    public AuctionItem getSpec(int userID, int itemID, String token) throws RemoteException;
    public int newAuction(int userID, AuctionSaleItem item, String token) throws RemoteException;
    public AuctionItem[] listItems(int userID, String token) throws RemoteException;
    public AuctionResult closeAuction(int userID, int itemID, String token) throws RemoteException;
    public boolean bid(int userID, int itemID, int price, String token) throws RemoteException;

}
