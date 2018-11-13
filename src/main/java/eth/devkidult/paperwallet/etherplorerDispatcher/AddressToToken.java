package com.sweden.webwallet.etherplorerDispatcher;

import lombok.Data;

import java.util.List;

@Data
public class AddressToToken {

    private String address;
    private Eth eth;
    private int countTxs;
    private List<Tokens> tokens;

}
