package eth.devkidult.paperwallet.gasStaionDispatcher;

import lombok.Data;

@Data
public class Gas {
    private double fast;
    private double safeLow;
    private double average;
}
