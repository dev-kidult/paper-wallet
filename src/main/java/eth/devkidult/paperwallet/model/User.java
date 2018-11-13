package eth.devkidult.paperwallet.model;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Entity
@Data
public class User implements Serializable {

    private static final long serialVersionUID = -1747808363259429716L;

    @Id
    private String email;

    @Column(nullable = false)
    private String password;

    private String appFCMCode;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false, updatable = false, insertable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Date joinDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Date lastLogin;

    @OneToMany(mappedBy = "user")
    private List<Wallet> wallets;
}
