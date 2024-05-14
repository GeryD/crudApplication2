package com.example.crud2.service;


import com.example.crud2.model.Item;
import com.example.crud2.model.PaymentStatus;
import com.example.crud2.model.Transaction;
import com.example.crud2.repository.TransactionRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@AllArgsConstructor
@Service
public class PaymentService {

    private final TransactionRepository transactionRepository;

    @Transactional
    public Transaction createTransaction(Transaction transaction) {
        validateNewTransaction(transaction);
        transaction.setPaymentStatus(PaymentStatus.NEW);
        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction updateTransaction(Long id, Transaction updatedTransaction) {
        if (updatedTransaction.getPaymentStatus() == null) {
            throw new IllegalArgumentException("Missing payment status");
        }

        Transaction existingTransaction = findTransactionById(id);

        // It is not possible to change the purchase order when the transaction is modified.
        Set<Item> items = updatedTransaction.getItems();
        if (items != null && !items.isEmpty() && !items.equals(existingTransaction.getItems())) {
            throw new IllegalArgumentException("Cannot modify purchase order");
        }

        if (updatedTransaction.getTotalAmount() != null
                && updatedTransaction.getTotalAmount().compareTo(existingTransaction.getTotalAmount()) != 0) {
            throw new IllegalArgumentException("Cannot modify total amount");
        }

        if (updatedTransaction.getPaymentType() != null
                && updatedTransaction.getPaymentType() == existingTransaction.getPaymentType()) {
            throw new IllegalArgumentException("Cannot modify payment type");
        }

        // It is not possible to change the status of a `CAPTURED` or `CANCELED` transaction
        if (existingTransaction.getPaymentStatus() == PaymentStatus.CAPTURED ||
                existingTransaction.getPaymentStatus() == PaymentStatus.CANCELED) {
            throw new IllegalArgumentException("Cannot update the status of a CAPTURED or CANCELED transaction.");
        }

        // It is only possible to change the transaction status to `CAPTURED`
        // when the transaction is currently in status `AUTHORIZED`
        if (existingTransaction.getPaymentStatus() == PaymentStatus.AUTHORIZED
                && updatedTransaction.getPaymentStatus() != PaymentStatus.CAPTURED) {
            throw new IllegalArgumentException("Cannot update the status to CAPTURED, transaction is not AUTHORIZED.");
        }

        existingTransaction.setPaymentStatus(updatedTransaction.getPaymentStatus());

        return transactionRepository.save(existingTransaction);
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public Transaction getTransactionById(Long id) {
        return findTransactionById(id);
    }

    private Transaction findTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found with ID: " + id));
    }

    private void validateNewTransaction(Transaction transaction) {
        BigDecimal totalAmount = transaction.getItems().stream()
                .map(item -> item.getPrice().multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (!totalAmount.equals(transaction.getTotalAmount())) {
            throw new IllegalArgumentException("Invalid total amount for the transaction.");
        }
    }
}
