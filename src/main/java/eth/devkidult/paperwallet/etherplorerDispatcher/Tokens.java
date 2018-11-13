package eth.devkidult.paperwallet.etherplorerDispatcher;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class Tokens {

    private TokenInfo tokenInfo;
    private BigDecimal balance;
    private BigDecimal totalIn;
    private BigDecimal totalOut;

}
