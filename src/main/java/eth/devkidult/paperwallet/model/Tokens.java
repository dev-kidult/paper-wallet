package eth.devkidult.paperwallet.model;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Data
public class Tokens {

    @Id
    private String contractAddress;

    @Column(nullable = false , length = 4)
    private int decimals;

    @Column(nullable = false , length = 20)
    private String name;

    @Column(nullable = false , length = 20)
    private String symbol;

    @Column(columnDefinition = "longblob")
    private byte[] image;

    @Column(nullable = false)
    private boolean exposureStatusToUser;

    @Column(nullable = false)
    private boolean exposureStatusToAdmin;
}
