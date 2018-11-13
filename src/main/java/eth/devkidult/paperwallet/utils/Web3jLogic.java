package com.sweden.webwallet.utils;

import com.sweden.webwallet.model.Tokens;
import com.sweden.webwallet.model.TxRecord;
import com.sweden.webwallet.model.Wallet;
import com.sweden.webwallet.repository.TokensRepository;
import com.sweden.webwallet.repository.TxRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.admin.Admin;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

@Service
public class Web3jLogic{

    @Autowired
    Admin web3j;

    @Autowired
    TokensRepository tokensRepository;

    @Autowired
    TxRecordRepository txRecordRepository;

    public BigDecimal EthBalance(String address) throws IOException {
        EthGetBalance ethGetBalance = web3j.ethGetBalance(address,DefaultBlockParameterName.LATEST).send();
        BigDecimal ethbalancetoDecimal = BigDecimal.ZERO;
        if(!ethGetBalance.getBalance().toString().equals("0x"))
            ethbalancetoDecimal = new BigDecimal(ethGetBalance.getBalance().toString());
        BigDecimal diviser = BigDecimal.valueOf(Math.pow(10,18));
        return ethbalancetoDecimal.divide(diviser,6,BigDecimal.ROUND_HALF_UP);
    }

    public BigDecimal TokenBalance(String address , String contractAddress , int decimal) throws IOException{
        String data = "0x70a08231000000000000000000000000"+address.substring(2,address.length());
        Transaction transaction = Transaction.createEthCallTransaction(address,contractAddress,data);
        EthCall ethCall = web3j.ethCall(transaction,DefaultBlockParameterName.LATEST).send();
        BigDecimal tokenBalance = BigDecimal.ZERO;
        if(!ethCall.getValue().equals("0x")) {
            //tokenBalance = BigDecimal.valueOf(Numeric.toBigInt(ethCall.getValue()).longValue());
            tokenBalance = new BigDecimal(Numeric.toBigInt(ethCall.getValue()).toString());
        }

        if(decimal != 0) {
            BigDecimal diviser = BigDecimal.valueOf(Math.pow(10, (double) decimal));
            if(tokenBalance.compareTo(BigDecimal.ZERO) == -1){
                tokenBalance = tokenBalance.divide(diviser,6,BigDecimal.ROUND_HALF_UP);
            }else {
                tokenBalance = tokenBalance.divide(diviser,2,BigDecimal.ROUND_HALF_UP);
            }
            return tokenBalance;
        }else {
            return tokenBalance;
        }
    }

    public void SendEth(Wallet wallet , String to , String value , String fee) throws Exception{
        //필요한거 월렛 ( 패스 , 어드레스 ) , 받는사람 , 벨류 , 수수료
        Double doubleValue = Double.parseDouble(value)*Math.pow(10,18);
        Double doubleFee = Double.parseDouble(fee)*Math.pow(10,18);
        BigInteger convertValue = new BigInteger(BigDecimal.valueOf(doubleValue).toPlainString());
        BigInteger convertFee = new BigInteger(BigDecimal.valueOf(doubleFee).toPlainString());
        convertFee = convertFee.divide(GasLimit.ETH_GAS_LIMIT);

        Credentials credentials = WalletUtils.loadCredentials(wallet.getPassword(),wallet.getFilePath());
        BigInteger nonce = web3j.ethGetTransactionCount(wallet.getAddress(),DefaultBlockParameterName.LATEST).send().getTransactionCount();

        System.out.println("nonce:"+nonce+"/ convertFee:"+convertFee+"/ to:"+to+"/ convertValue:"+convertValue);

        RawTransaction txRaw = RawTransaction.createEtherTransaction(nonce,convertFee,GasLimit.ETH_GAS_LIMIT,to,convertValue);

        byte[] txSignedBytes = TransactionEncoder.signMessage(txRaw,credentials);
        String txSigned = Numeric.toHexString(txSignedBytes);

        String hash = web3j.ethSendRawTransaction(txSigned).send().getTransactionHash();

        TxRecord txRecord = new TxRecord();
        txRecord.setBlock("pending");
        txRecord.setType("ETH");
        txRecord.setTypeAddress("ETH");
        txRecord.setToAddress(to);
        txRecord.setFromAddress(wallet.getAddress());
        txRecord.setValue(convertValue);
        txRecord.setAge("pending");
        txRecord.setFee(convertFee.multiply(GasLimit.ETH_GAS_LIMIT));
        txRecord.setHash(hash);
        txRecord.setStatus("pending");

        System.out.println(txRecord.toString());

        txRecordRepository.save(txRecord);
    }

    public void SendToken(Wallet wallet , String to , String value , String fee , String tokenAddress) throws Exception{
        //필요한거 월렛 ( 패스 , 어드레스 ) , 받는사람 , 벨류 , 수수료 , 그 토큰 어드레스 , 그 토큰 데시멀
        String convertTo = to.substring(2,to.length());

        Tokens tokens = tokensRepository.findOne(tokenAddress);
        int tokenDecimal = tokens.getDecimals();
        if(tokenDecimal == 0){tokenDecimal = 1;}

        Double doubleValue = Double.parseDouble(value)*Math.pow(10,tokenDecimal);
        BigInteger bigIntegerValue = new BigInteger(BigDecimal.valueOf(doubleValue).toPlainString());
        String zeroCount = "";
        String convertValue = bigIntegerValue.toString(16);

        Double doubleFee = Double.parseDouble(fee)*Math.pow(10,18);
        BigInteger convertFee = new BigInteger(BigDecimal.valueOf(doubleFee).toPlainString());
        convertFee = convertFee.divide(GasLimit.TOKEN_GAS_LIMIT);

        for(int i = convertValue.length(); i<64;i++){
            zeroCount = zeroCount+"0";
        }
        convertValue = zeroCount+convertValue;

        Credentials credentials = WalletUtils.loadCredentials(wallet.getPassword(),wallet.getFilePath());
        BigInteger nonce = web3j.ethGetTransactionCount(wallet.getAddress(),DefaultBlockParameterName.LATEST).send().getTransactionCount();

        System.out.println("nonce:"+nonce+"/ convertFee:"+convertFee+"/ tokenAddress:"+tokenAddress+"/ convertValue:"+convertValue +"/ convertTo:"+convertTo);

        RawTransaction txRaw = RawTransaction.createTransaction(nonce,convertFee,GasLimit.TOKEN_GAS_LIMIT,tokenAddress,"0xa9059cbb000000000000000000000000"+convertTo+convertValue);

        byte[] txSignedBytes = TransactionEncoder.signMessage(txRaw,credentials);
        String txSigned = Numeric.toHexString(txSignedBytes);

        String hash = web3j.ethSendRawTransaction(txSigned).send().getTransactionHash();

        TxRecord txRecord = new TxRecord();
        txRecord.setBlock("pending");
        txRecord.setType(tokens.getSymbol());
        txRecord.setTypeAddress(tokenAddress);
        txRecord.setToAddress(to);
        txRecord.setFromAddress(wallet.getAddress());
        txRecord.setValue(bigIntegerValue);
        txRecord.setAge("pending");
        txRecord.setFee(convertFee.multiply(GasLimit.TOKEN_GAS_LIMIT));
        txRecord.setHash(hash);
        txRecord.setStatus("pending");
        txRecordRepository.save(txRecord);
    }

}
