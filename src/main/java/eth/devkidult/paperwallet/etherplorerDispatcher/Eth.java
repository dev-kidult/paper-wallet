package com.sweden.webwallet.etherplorerDispatcher;

import lombok.Data;

import java.math.BigDecimal;


@Data
public class Eth {

    private BigDecimal balance;
    private BigDecimal totalIn;
    private BigDecimal totalOut;

}
