package server.dao;

import java.util.List;
import server.model.entity.Wallet;

public interface WalletDao extends GenericDao<Wallet> {

    Wallet findWalletWithLock(Long userId);        // SELECT ... FOR UPDATE
    
    Wallet findByUserIdNoLock(Long userId);  // READ ONLY

    void updateBalance(Wallet wallet);       // atomic update

    void createWallet(Long userId);          // init wallet

    List<Wallet> findAll();                  // admin view (override optional)
}