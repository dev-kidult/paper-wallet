package eth.devkidult.paperwallet.model;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Data
public class Wallet implements Serializable {

    private static final long serialVersionUID = -4595215878490108799L;

    @Id
    @Column(length = 50)
    private String address;

    @Column(nullable = false)
    private String password;

    @ManyToOne
    private User user;

    @Column(nullable = false)
    private String filePath;

    private String tokens;

    @Column(nullable = false)
    private String name;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false, updatable = false, insertable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Date createDate;

    @Override
    public String toString() {
        return "Wallet{" +
                "address='" + address + '\'' +
                ", password='" + password + '\'' +
                ", user=" + user.getEmail() +
                ", filePath='" + filePath + '\'' +
                ", tokens='" + tokens + '\'' +
                ", name='" + name + '\'' +
                ", createDate=" + createDate +
                '}';
    }
}
