package com.accountservice.service.impl;

import com.accountservice.common.BaseResponse;
import com.accountservice.common.Const;
import com.accountservice.common.PagingResponse;
import com.accountservice.model.Balance;
import com.accountservice.model.PaymentMethod;
import com.accountservice.model.TradingAccount;
import com.accountservice.model.Transaction;
import com.accountservice.payload.request.client.*;
import com.accountservice.payload.response.client.*;
import com.accountservice.payload.response.internal.HasTradingAccountAndPaymentMethodResponse;
import com.accountservice.repository.BalanceRepository;
import com.accountservice.repository.PaymentMethodRepository;
import com.accountservice.repository.TradingAccountRepository;
import com.accountservice.service.TradingAccountService;
import com.accountservice.service.TransactionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Service
@Transactional
@AllArgsConstructor
@Slf4j
public class TradingAccountServiceImpl implements TradingAccountService {
    private final TradingAccountRepository tradingAccountRepository;
    private final BalanceRepository balanceRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    private final TransactionService transactionService;

    @Override
    public BaseResponse<?> createTradingAccount(String userId, CreateTradingAccountRequest createTradingAccountRequest) {
        TradingAccount newTradingAccount = new TradingAccount();
        newTradingAccount.setUserId(userId);
        newTradingAccount.setNickname(createTradingAccountRequest.getNickname());
        newTradingAccount.setAccountNumber(generateTradingAccountNumber());
        newTradingAccount.setStatus(TradingAccount.AccountStatus.ACTIVE.name());
        newTradingAccount.setCreatedAt(Instant.now());

        TradingAccount savedTradingAccount = tradingAccountRepository.save(newTradingAccount);

        Balance newBalance = new Balance();
        newBalance.setAccountId(savedTradingAccount.getId());
        newBalance.setAvailable(0.00F);
        newBalance.setReserved(0.00F);
        newBalance.setTotal(0.00F);
        newBalance.setCurrency("VND");

        Balance savedBalance = balanceRepository.save(newBalance);

        return new BaseResponse<>(
            Const.STATUS_RESPONSE.SUCCESS,
            "Trading account created successfully",
            new RetrieveAccountInfoResponse(
                savedTradingAccount.getId(),
                savedTradingAccount.getAccountNumber(),
                savedTradingAccount.getNickname(),
                savedTradingAccount.getStatus(),
                savedTradingAccount.getCreatedAt(),
                savedTradingAccount.getUpdatedAt(),
                new RetrieveBalanceInfoResponse(
                    savedBalance.getCurrency(),
                    savedBalance.getAvailable(),
                    savedBalance.getReserved(),
                    savedBalance.getTotal()
                )
            )
        );
    }

    @Override
    public BaseResponse<?> getAccountDetails(String accountId) {
        TradingAccount tradingAccount = tradingAccountRepository.findTradingAccountById(accountId).orElse(null);
        if (tradingAccount == null) {
            return new BaseResponse<>(
                Const.STATUS_RESPONSE.ERROR,
                "Account not found with accountId: " + accountId,
                ""
            );
        }

        Balance balance = balanceRepository.findBalanceByAccountId(accountId).orElse(null);
        if (balance == null) {
            return new BaseResponse<>(
                Const.STATUS_RESPONSE.ERROR,
                "Balance not found with accountId: " + accountId,
                ""
            );
        }

        Transaction lastTransaction = transactionService.getLastTransaction(accountId);

        return new BaseResponse<>(
            Const.STATUS_RESPONSE.SUCCESS,
            "Account details retrieved successfully",
            new GetAccountDetailsResponse(
                tradingAccount.getId(),
                tradingAccount.getAccountNumber(),
                tradingAccount.getNickname(),
                tradingAccount.getStatus(),
                tradingAccount.getCreatedAt(),
                tradingAccount.getUpdatedAt(),
                new RetrieveBalanceInfoResponse(
                    balance.getCurrency(),
                    balance.getAvailable(),
                    balance.getReserved(),
                    balance.getTotal()
                ),
                lastTransaction == null
                    ? null
                    : lastTransaction.getCompletedAt()
            )
        );
    }

    @Override
    public BaseResponse<?> getUserAccounts(GetUserAccountsRequest getUserAccountsRequest) {
        String userId = getUserAccountsRequest.getUserId();
        String status = getUserAccountsRequest.getStatus();
        Integer page = getUserAccountsRequest.getPage();
        Integer size = getUserAccountsRequest.getSize();

        if (page == null) { page = 0; }
        if (size == null) { size = 20; }

        Pageable pageable = PageRequest.of(page, size);
        Page<TradingAccount> result = status == null
                ? tradingAccountRepository.findAllByUserId(userId, pageable)
                : tradingAccountRepository.findTradingAccountsByUserIdAndStatus(userId, status, pageable);

        List<TradingAccount> tradingAccounts = result.getContent();
        List<Balance> balances = tradingAccounts.stream().map(tradingAccount ->
            balanceRepository.findBalanceByAccountId(tradingAccount.getId()).orElse(null)
        ).toList();
        List<RetrieveAccountInfoResponse> retrieveAccountInfoResponses = new ArrayList<>();
        for (int i = 0; i < tradingAccounts.size(); i++) {
            RetrieveBalanceInfoResponse balanceInfoResponse = new RetrieveBalanceInfoResponse();
            balanceInfoResponse.setCurrency(balances.get(i).getCurrency());
            balanceInfoResponse.setAvailable(balances.get(i).getAvailable());
            balanceInfoResponse.setReserved(balances.get(i).getReserved());
            balanceInfoResponse.setTotal(balances.get(i).getTotal());

            RetrieveAccountInfoResponse accountInfoResponse = new RetrieveAccountInfoResponse();
            accountInfoResponse.setId(tradingAccounts.get(i).getId());
            accountInfoResponse.setAccountNumber(tradingAccounts.get(i).getAccountNumber());
            accountInfoResponse.setStatus(tradingAccounts.get(i).getStatus());
            accountInfoResponse.setNickname(tradingAccounts.get(i).getNickname());
            accountInfoResponse.setCreatedAt(tradingAccounts.get(i).getCreatedAt());
            accountInfoResponse.setUpdatedAt(tradingAccounts.get(i).getUpdatedAt());
            accountInfoResponse.setBalance(balanceInfoResponse);

            retrieveAccountInfoResponses.add(accountInfoResponse);
        }

        return new BaseResponse<>(
            Const.STATUS_RESPONSE.SUCCESS,
            "Accounts retrieved successfully",
            new GetUserAccountsResponse(
                retrieveAccountInfoResponses,
                new PagingResponse(
                    page,
                    size,
                    result.getTotalElements(),
                    result.getTotalPages()
                )
            )
        );
    }

    @Override
    public BaseResponse<?> updateTradingAccount(UpdateTradingAccountRequest updateTradingAccountRequest) {
        String accountId = updateTradingAccountRequest.getAccountId();
        String nickname = updateTradingAccountRequest.getNickname();
        String status = updateTradingAccountRequest.getStatus();

        TradingAccount tradingAccount = tradingAccountRepository.findTradingAccountById(accountId).orElse(null);
        if (tradingAccount == null) {
            return new BaseResponse<>(
                Const.STATUS_RESPONSE.ERROR,
                "Account not found with accountId: " + accountId,
                ""
            );
        }

        tradingAccount.setNickname(nickname);
        tradingAccount.setStatus(status);
        tradingAccount.setUpdatedAt(Instant.now());

        tradingAccountRepository.save(tradingAccount);

        return new BaseResponse<>(
            Const.STATUS_RESPONSE.SUCCESS,
            "Account updated successfully",
            new UpdateTradingAccountResponse(
                accountId,
                tradingAccount.getAccountNumber(),
                nickname,
                status,
                tradingAccount.getUpdatedAt()
            )
        );
    }

//    @Override
//    public BaseResponse<?> getBalanceHistory(GetBalanceHistoryRequest getBalanceHistoryRequest) {
//        String accountId = getBalanceHistoryRequest.getAccountId();
//        String startDate = getBalanceHistoryRequest.getStartDate();
//        String endDate = getBalanceHistoryRequest.getEndDate();
//        Integer page = getBalanceHistoryRequest.getPage();
//        Integer size = getBalanceHistoryRequest.getSize();
//
//        BaseResponse<List<Transaction>> getTransactionsResponse = transactionService.getTransactions(
//            new GetTransactionsRequest(
//                accountId,
//                startDate,
//                endDate,
//                List.of(Transaction.TransactionType.DEPOSIT.name(), Transaction.TransactionType.WITHDRAWAL.name()),
//                List.of(),
//                page,
//                size
//            )
//        );
//        List<Transaction> transactions = getTransactionsResponse.getData();
//        if (transactions.isEmpty()) {
//            return new BaseResponse<>(
//                Const.STATUS_RESPONSE.ERROR,
//                "Transactions not found",
//                    ""
//            );
//        }
//
//        for (int i = 0; i < transactions.size(); ++i) {
//            Transaction transaction = transactions.get(i);
//            String completedDate = transaction.getCompletedAt().toString().split("T")[0];
//
//        }
//    }

    @Override
    public HasTradingAccountAndPaymentMethodResponse hasAccountAndPaymentMethod(String userId) {
        TradingAccount tradingAccount = tradingAccountRepository.findTradingAccountByUserId(userId).orElse(null);
        PaymentMethod paymentMethod = paymentMethodRepository.findPaymentMethodByUserId(userId).orElse(null);

        return new HasTradingAccountAndPaymentMethodResponse(
        tradingAccount != null,
        paymentMethod != null
        );
    }

    /*====================================================PRIVATE FUNCTIONS==========================================================================================================*/
    private String generateTradingAccountNumber() {
        String prefix = "TRD";
        String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
        Random rand = new Random();
        int randomNumber = 10000 + rand.nextInt(90000);
        return prefix + "-" + date + "-" + randomNumber;
    }
}
