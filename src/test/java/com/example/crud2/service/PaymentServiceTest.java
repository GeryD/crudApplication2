package com.example.crud2.service;

import com.example.crud2.model.Item;
import com.example.crud2.model.PaymentStatus;
import com.example.crud2.model.PaymentType;
import com.example.crud2.model.Transaction;
import com.example.crud2.repository.TransactionRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    TransactionRepository transactionRepository;

    @InjectMocks
    PaymentService paymentService;

    @Test
    void createTransaction() {
        Transaction expectedTransaction = getExistingTransaction(1L, "3.00", PaymentType.CREDIT_CARD, PaymentStatus.NEW);
        Set<Item> items = Set.of(
                new Item(1L, "foo", new BigDecimal("1.00"), 1),
                new Item(2L, "bar", new BigDecimal("2.00"), 1)
        );
        expectedTransaction.setItems(items);
        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(expectedTransaction);

        Transaction actualTransaction = paymentService.createTransaction(expectedTransaction);

        assertThat(actualTransaction, notNullValue());
        assertThat(actualTransaction.getId(), is(expectedTransaction.getId()));
        assertThat(actualTransaction.getTotalAmount(), is(expectedTransaction.getTotalAmount()));
        assertThat(actualTransaction.getPaymentType(), is(expectedTransaction.getPaymentType()));
        assertThat(actualTransaction.getPaymentStatus(), is(expectedTransaction.getPaymentStatus()));
        assertThat(actualTransaction.getItems(), is(expectedTransaction.getItems()));
    }

    @Test
    void createTransactionInvalidTotalAmount() {
        String invalidAmount = "1.00";
        Transaction transaction = getExistingTransaction(1L, invalidAmount, PaymentType.PAYPAL, PaymentStatus.NEW);
        Set<Item> items = Set.of(
                new Item(1L, "foo", new BigDecimal("1.00"), 1),
                new Item(2L, "bar", new BigDecimal("2.00"), 1)
        );
        transaction.setItems(items);

        assertThrows(IllegalArgumentException.class, () -> paymentService.createTransaction(transaction));
        verify(transactionRepository, times(0)).save(any(Transaction.class));
    }

    @Test
    void updateTransaction() {
        Long id = 1L;
        Transaction existingTransaction = getExistingTransaction(id, "2.00", PaymentType.PAYPAL, PaymentStatus.AUTHORIZED);
        when(transactionRepository.findById(id)).thenReturn(Optional.of(existingTransaction));

        Transaction updatedTransaction = getUpdatedTransaction(PaymentStatus.CAPTURED);

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(i -> i.getArgument(0));

        Transaction actualTransaction = paymentService.updateTransaction(id, updatedTransaction);

        assertThat(actualTransaction, notNullValue());
        assertThat("id", actualTransaction.getId(), is(id));
        assertThat("total amount", actualTransaction.getTotalAmount(), is(existingTransaction.getTotalAmount()));
        assertThat("payment status", actualTransaction.getPaymentStatus(), is(PaymentStatus.CAPTURED));
        assertThat("payment type", actualTransaction.getPaymentType(), is(existingTransaction.getPaymentType()));

        verify(transactionRepository, times(1)).findById(id);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void updateTransactionWithNoPaymentStatus() {
        Transaction updatedTransaction = getUpdatedTransaction(null);
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> paymentService.updateTransaction(1L, updatedTransaction));
    }

    // todo : same test with captured transaction
    @ParameterizedTest
    @EnumSource(names = {"AUTHORIZED", "NEW", "CAPTURED"})
    void updateCancelledTransaction(PaymentStatus status) {
        Long id = 1L;
        Transaction existingTransaction = getExistingTransaction(id, "2.00", PaymentType.PAYPAL, PaymentStatus.CANCELED);

        when(transactionRepository.findById(id)).thenReturn(Optional.of(existingTransaction));

        Transaction updatedTransaction = getUpdatedTransaction(status);

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> paymentService.updateTransaction(id, updatedTransaction));
    }

    @ParameterizedTest
    @EnumSource(names = {"AUTHORIZED", "NEW", "CANCELED"})
    void updateAuthorizedTransaction(PaymentStatus status) {
        Long id = 1L;
        Transaction existingTransaction = getExistingTransaction(id, "2.00", PaymentType.PAYPAL, PaymentStatus.AUTHORIZED);

        when(transactionRepository.findById(id)).thenReturn(Optional.of(existingTransaction));

        Transaction updatedTransaction = getUpdatedTransaction(status);

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> paymentService.updateTransaction(id, updatedTransaction));
    }

    @Test
    void getAllTransactions() {
        Long id = 1L;
        Transaction existingTransaction = getExistingTransaction(id, "2.00", PaymentType.PAYPAL, PaymentStatus.AUTHORIZED);

        when(transactionRepository.findAll()).thenReturn(List.of(existingTransaction));
        List<Transaction> allTransactions = paymentService.getAllTransactions();
        assertThat(allTransactions.size(), is(1));
        assertThat(allTransactions.get(0), is(existingTransaction));
    }

    @Test
    void getTransactionById() {
        Long id = 1L;
        Transaction existingTransaction = getExistingTransaction(id, "2.00", PaymentType.PAYPAL, PaymentStatus.AUTHORIZED);

        when(transactionRepository.findById(id)).thenReturn(Optional.of(existingTransaction));
        assertThat(paymentService.getTransactionById(1L), is(existingTransaction));
    }

    @Test
    void getTransactionByInvalidId() {
        Long id = 1L;
        when(transactionRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> paymentService.getTransactionById(id));
    }

    private Transaction getExistingTransaction(Long id, String s, PaymentType paypal, PaymentStatus canceled) {
        Transaction existingTransaction = new Transaction();
        existingTransaction.setId(id);
        existingTransaction.setTotalAmount(new BigDecimal(s));
        existingTransaction.setPaymentType(paypal);
        existingTransaction.setPaymentStatus(canceled);
        return existingTransaction;
    }

    private Transaction getUpdatedTransaction(PaymentStatus captured) {
        Transaction updatedTransaction = new Transaction();
        updatedTransaction.setId(null);
        updatedTransaction.setTotalAmount(null);
        updatedTransaction.setPaymentType(null);
        updatedTransaction.setPaymentStatus(captured);
        return updatedTransaction;
    }
}