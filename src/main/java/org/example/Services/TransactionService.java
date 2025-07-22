package org.example.Services;


import org.example.DAO.TransactionDAO;
import org.example.Models.Transaction;
import org.hibernate.SessionFactory;
import java.util.List;

public class TransactionService {
    private final TransactionDAO transactionDAO;

    public TransactionService(SessionFactory sessionFactory) {
        this.transactionDAO = new TransactionDAO(sessionFactory);
    }

    public List<Transaction> getUserTransactions(Long userId) {
        return transactionDAO.findByUserId(userId);
    }
    public List<Transaction> getAll(){
        return transactionDAO.findAll();
    }
}