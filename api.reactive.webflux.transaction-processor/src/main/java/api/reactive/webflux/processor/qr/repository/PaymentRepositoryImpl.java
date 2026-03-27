package api.reactive.webflux.processor.qr.repository;

import api.reactive.webflux.processor.qr.entity.MerchantAccount;
import api.reactive.webflux.processor.qr.entity.Transaction;
import api.reactive.webflux.processor.qr.entity.UserAccount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Slf4j
@Repository
public class PaymentRepositoryImpl implements PaymentRepository {

    private final DynamoDbAsyncTable<UserAccount> userTable;
    private final DynamoDbAsyncTable<MerchantAccount> merchantTable;
    private final DynamoDbAsyncTable<Transaction> transactionTable;

    public PaymentRepositoryImpl(DynamoDbEnhancedAsyncClient enhancedClient) {
        this.userTable = enhancedClient.table("user-accounts-dev", TableSchema.fromBean(UserAccount.class));
        this.merchantTable = enhancedClient.table("merchant-accounts-dev", TableSchema.fromBean(MerchantAccount.class));
        this.transactionTable = enhancedClient.table("transaction-processor-table-dev", TableSchema.fromBean(Transaction.class));
    }

    @Override
    public Mono<UserAccount> getUserAccount(String userId) {
        return Mono.fromFuture(userTable.getItem(Key.builder().partitionValue(userId).build()));
    }

    @Override
    public Mono<MerchantAccount> getMerchantAccount(String merchantId) {
        return Mono.fromFuture(merchantTable.getItem(Key.builder().partitionValue(merchantId).build()));
    }

    @Override
    public Mono<Transaction> getTransaction(String paymentId) {
        return Mono.fromFuture(transactionTable.getItem(Key.builder().partitionValue(paymentId).build()));
    }

    @Override
    public Mono<Void> saveUserAccount(UserAccount userAccount) {
        return Mono.fromFuture(userTable.putItem(userAccount)).then();
    }

    @Override
    public Mono<Void> saveMerchantAccount(MerchantAccount merchantAccount) {
        return Mono.fromFuture(merchantTable.putItem(merchantAccount)).then();
    }

    @Override
    public Mono<Transaction> saveTransaction(Transaction transaction) {
        return Mono.fromFuture(transactionTable.putItem(transaction)).thenReturn(transaction);
    }
}
