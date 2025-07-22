package org.example.Services;


import org.example.DAO.TransactionDAO;
import org.example.DAO.UserDAO;
import org.example.DAO.WalletDAO;
import org.example.Models.TopUpRequest;
import org.example.Models.Transaction;
import org.example.Models.Wallet;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import java.math.BigDecimal;

public class WalletService {
    private final WalletDAO walletDAO;
    private final TransactionDAO transactionDAO;
    private final UserDAO userDAO;

    public WalletService(SessionFactory sessionFactory) {
        this.walletDAO = new WalletDAO(sessionFactory);
        this.transactionDAO = new TransactionDAO(sessionFactory);
        this.userDAO = new UserDAO(sessionFactory);
    }

    public void topUp(Long userId, TopUpRequest request) {
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid amount");
        }
        try (Session session = walletDAO.sessionFactory.openSession()) {
            session.beginTransaction();
            Wallet wallet = walletDAO.findByUserId(userId);
            if (wallet == null) {
                wallet = new Wallet();
                wallet.setUser(userDAO.findById(userId));
                wallet.setBalance(BigDecimal.ZERO);
                walletDAO.save(wallet);
            }
            wallet.setBalance(wallet.getBalance().add(request.getAmount()));
            walletDAO.update(wallet);

            Transaction transaction = new Transaction();
            transaction.setUser(userDAO.findById(userId));
            transaction.setAmount(request.getAmount());
            transaction.setMethod("online");
            transaction.setStatus("success");
            transactionDAO.save(transaction);

            session.getTransaction().commit();
        } catch (Exception e) {
            throw new RuntimeException("Failed to top up wallet: " + e.getMessage(), e);
        }
    }
}