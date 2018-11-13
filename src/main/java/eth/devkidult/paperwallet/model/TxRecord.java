package com.sweden.webwallet.model;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.Digits;
import java.math.BigInteger;

@Entity
@Data
public class TxRecord {

    @Column(nullable = false , length = 20)
    private String type;

    @Column(nullable = false)
    private String typeAddress;

    @Column(nullable = false)
    private String fromAddress;

    @Column(nullable = false)
    private String toAddress;

    @Digits(integer = 37, fraction = 0)
    @Column(nullable = false)
    private BigInteger value;

    @Id
    private String hash;

    @Column(nullable = false , length = 30)
    private String age;

    @Digits(integer = 24, fraction = 0)
    @Column(nullable = false)
    private BigInteger fee;

    @Column(nullable = false , length = 20)
    private String block;

    @Column(nullable = false , length = 20)
    private String status;

}
