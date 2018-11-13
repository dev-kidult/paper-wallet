package eth.devkidult.paperwallet.etherplorerDispatcher;

import lombok.Data;

@Data
public class TokenInfo {

    private String address;
    private String name;
    private int decimals;
    private String symbol;

}
