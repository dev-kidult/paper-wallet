package com.sweden.webwallet.utils;

import com.sweden.webwallet.model.TxRecord;
import com.sweden.webwallet.repository.TokensRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

@Service
public class ConvertValue {

    @Autowired
    TokensRepository tokensRepository;

    public BigDecimal convertValue(TxRecord txRecord) {
        String type = txRecord.getTypeAddress();
        BigDecimal diviser = null;
        BigDecimal returnValue = new BigDecimal(txRecord.getValue().toString());
        if (type.equals("ETH")) {
            diviser = BigDecimal.valueOf(Math.pow(10, 18));
            returnValue = returnValue.divide(diviser,6,BigDecimal.ROUND_HALF_UP);
            if(returnValue.compareTo(new BigDecimal(100)) == 1){
                return returnValue.setScale(2,RoundingMode.HALF_UP);
            }else {
                return returnValue;
            }
        } else {
            int decimal = tokensRepository.findOne(type).getDecimals();
            if (decimal != 0)
                diviser = BigDecimal.valueOf(Math.pow(10, decimal));
            else
                diviser = BigDecimal.ONE;

            returnValue = returnValue.divide(diviser,6,BigDecimal.ROUND_HALF_UP);
            if(returnValue.compareTo(new BigDecimal(100)) == 1){
                return returnValue.setScale(2,RoundingMode.HALF_UP);
            }else {
                return returnValue;
            }
        }
    }

    public BigDecimal convertFee(BigInteger fee){
        BigDecimal diviser = BigDecimal.valueOf(Math.pow(10,18));
        return new BigDecimal(fee.toString()).divide(diviser,6,BigDecimal.ROUND_HALF_UP);
    }

}
