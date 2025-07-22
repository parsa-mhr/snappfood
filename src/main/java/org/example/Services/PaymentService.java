package org.example.Services;


import org.example.DAO.OrderDAO;
import org.example.DAO.TransactionDAO;
import org.example.DAO.UserDAO;
import org.example.DAO.WalletDAO;
import org.example.Details.Cart;
import org.example.Details.OrderStatus;
import org.example.Models.PaymentRequest;
import org.example.Models.Transaction;
import org.example.Models.Wallet;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import java.math.BigDecimal;

public class PaymentService {
    private final OrderDAO orderDAO;
    private final WalletDAO walletDAO;
    private final TransactionDAO transactionDAO;
    private final UserDAO userDAO;

    public PaymentService(SessionFactory sessionFactory) {
        this.orderDAO = new OrderDAO(sessionFactory);
        this.walletDAO = new WalletDAO(sessionFactory);
        this.transactionDAO = new TransactionDAO(sessionFactory);
        this.userDAO = new UserDAO(sessionFactory);
    }

    public Transaction processOnlinePayment(Long userId, PaymentRequest request) {
        try (Session session = orderDAO.sessionFactory.openSession()) {
            session.beginTransaction();
            Cart order = orderDAO.findById(request.getOrderId());
            if (order == null) {
                throw new RuntimeException("Order not found: " + request.getOrderId());
            }
            if (!order.getBuyer().getId().equals(userId)) {
                throw new RuntimeException("Unauthorized order access");
            }
            if (!order.getStatus().equals(OrderStatus.SUBMITTED)) {
                throw new RuntimeException("Order cannot be paid");
            }

            BigDecimal amount = BigDecimal.valueOf(order.getPay_price());
            if (request.getMethod().equals("wallet")) {
                Wallet wallet = walletDAO.findByUserId(userId);
                if (wallet == null || wallet.getBalance().compareTo(amount) < 0) {
                    throw new RuntimeException("Insufficient wallet balance  BALANCE : " + wallet.getBalance());
                }
                wallet.setBalance(wallet.getBalance().subtract(amount));
                walletDAO.update(wallet);
            }

            Transaction transaction = new Transaction();
            transaction.setUser(userDAO.findById(userId));
            transaction.setOrderId(request.getOrderId());
            transaction.setAmount(amount);
            transaction.setMethod(request.getMethod());
            transaction.setStatus("success");
            transactionDAO.save(transaction);

            order.setStatus(OrderStatus.WAITING_VENDOR);
            orderDAO.update(order);

            session.getTransaction().commit();
            return transaction;
        } catch (Exception e) {
            throw new RuntimeException("Payment failed: " + e.getMessage(), e);
        }
    }
}