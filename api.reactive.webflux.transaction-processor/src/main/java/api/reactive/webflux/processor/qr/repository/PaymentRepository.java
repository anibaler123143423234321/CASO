package api.reactive.webflux.processor.qr.repository;

import api.reactive.webflux.processor.qr.entity.MerchantAccount;
import api.reactive.webflux.processor.qr.entity.Transaction;
import api.reactive.webflux.processor.qr.entity.UserAccount;
import reactor.core.publisher.Mono;

public interface PaymentRepository {
    Mono<UserAccount> getUserAccount(String userId);
    Mono<MerchantAccount> getMerchantAccount(String merchantId);
    Mono<Transaction> getTransaction(String paymentId);
    Mono<Void> saveUserAccount(UserAccount userAccount);
    Mono<Void> saveMerchantAccount(MerchantAccount merchantAccount);
    Mono<Transaction> saveTransaction(Transaction transaction);
}
