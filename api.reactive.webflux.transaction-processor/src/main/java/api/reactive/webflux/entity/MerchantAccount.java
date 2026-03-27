package api.reactive.webflux.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class MerchantAccount {

    private String merchantId;
    private BigDecimal balance;
    private Long version;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("merchantId")
    public String getMerchantId() {
        return merchantId;
    }

    @DynamoDbAttribute("balance")
    public BigDecimal getBalance() {
        return balance;
    }

    @DynamoDbAttribute("version")
    public Long getVersion() {
        return version;
    }
}
