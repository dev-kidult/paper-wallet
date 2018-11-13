package eth.devkidult.paperwallet.Component;

import com.google.gson.Gson;
import eth.devkidult.paperwallet.etherplorerDispatcher.TokenInfo;
import eth.devkidult.paperwallet.model.Tokens;
import eth.devkidult.paperwallet.model.TxRecord;
import eth.devkidult.paperwallet.model.Wallet;
import eth.devkidult.paperwallet.repository.TokensRepository;
import eth.devkidult.paperwallet.repository.TxRecordRepository;
import eth.devkidult.paperwallet.repository.UserRepository;
import eth.devkidult.paperwallet.repository.WalletRepository;
import eth.devkidult.paperwallet.utils.ConvertValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.admin.Admin;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.NoSuchElementException;

@Configuration
public class Ethfilter {

    @Autowired
    Admin web3j;

    @Autowired
    TokensRepository tokensRepository;

    @Autowired
    TxRecordRepository txRecordRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    WalletRepository walletRepository;

    @Autowired
    Dispatcher dispatcher;

    @Autowired
    ConvertValue convertValue;


    public static String ethBlcokNumber;

    @PostConstruct
    private void init() throws IOException {
        System.out.println("init Start");
        web3j.blockObservable(true).subscribe(ethBlock -> {
            List<EthBlock.TransactionResult> transactionResultList = ethBlock.getBlock().getTransactions();
            String blockNumber = ethBlock.getBlock().getNumber().toString(10);
            ethBlcokNumber = blockNumber;
            System.out.println("ethBlockNumber = " + blockNumber);
            System.out.println("transactioncCount = " + transactionResultList.size());
            for (EthBlock.TransactionResult<?> transactionResult : transactionResultList) {
                Transaction tx = (Transaction) transactionResult.get();
                String status = "pending?";
                BigInteger fee = null;
                try {
                    try {
                        TransactionReceipt receipt = web3j.ethGetTransactionReceipt(tx.getHash()).send().getTransactionReceipt().get();
                        status = receipt.getStatus().equals("0x1") ? "Success" : "Fail";
                        fee = receipt.getGasUsed();
                    } catch (NoSuchElementException e) {
                    } finally {
                        String input = tx.getInput();
                        String getFrom = tx.getFrom().toLowerCase();
                        String getTo = tx.getTo();
                        Wallet fromWallet = walletRepository.findOne(getFrom);
                        Wallet toWallet = null;
                        if (getTo != null) {
                            toWallet = walletRepository.findOne(getTo.toLowerCase());
                        }
                        String unixTimeStemp = ethBlock.getBlock().getTimestamp().toString(10);
                        Long unixTimeStemp2 = Long.parseLong(unixTimeStemp);
                        SimpleDateFormat simpleDate = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                        String age = simpleDate.format(unixTimeStemp2 * 1000L);
                        if (input.equals("0x")) {
                            //지갑 주소 비교해서 이더전송 이력 남기기
                            if (checckAddress(fromWallet, toWallet)) {
                                TxRecord txRecord = new TxRecord();
                                txRecord.setAge(age);
                                txRecord.setFee(fee.multiply(tx.getGasPrice()));
                                txRecord.setFromAddress(getFrom);
                                txRecord.setToAddress(getTo);
                                txRecord.setHash(tx.getHash());
                                txRecord.setValue(tx.getValue());
                                txRecord.setType("ETH");
                                txRecord.setTypeAddress("ETH");
                                txRecord.setStatus(status);
                                txRecord.setBlock(blockNumber);
                                System.out.println(txRecord.toString());
                                txRecordRepository.save(txRecord);
                            }
                        } else {
                            String ethGetCode = web3j.ethGetCode(getTo, DefaultBlockParameterName.LATEST).send().getCode();
                            if (ethGetCode == null) {
                                //컨트렉트 생성
                                if (checckAddress(fromWallet, toWallet)) {
                                    TxRecord txRecord = new TxRecord();
                                    txRecord.setAge(age);
                                    txRecord.setFee(fee.multiply(tx.getGasPrice()));
                                    txRecord.setFromAddress(getFrom);
                                    txRecord.setToAddress("Contract Create");
                                    txRecord.setHash(tx.getHash());
                                    txRecord.setValue(BigInteger.ZERO);
                                    txRecord.setType("Contract");
                                    txRecord.setTypeAddress(getTo);
                                    txRecord.setStatus(status);
                                    txRecord.setBlock(blockNumber);
                                    txRecordRepository.save(txRecord);
                                }
                            } else if (ethGetCode.equals("0x")) {
                                //지갑 주소 비교해서 이더전송 이력 남기기
                                if (checckAddress(fromWallet, toWallet)) {
                                    TxRecord txRecord = new TxRecord();
                                    txRecord.setAge(age);
                                    txRecord.setFee(fee.multiply(tx.getGasPrice()));
                                    txRecord.setFromAddress(getFrom);
                                    txRecord.setToAddress(getTo);
                                    txRecord.setHash(tx.getHash());
                                    txRecord.setValue(tx.getValue());
                                    txRecord.setType("ETH");
                                    txRecord.setTypeAddress("ETH");
                                    txRecord.setStatus(status);
                                    txRecord.setBlock(blockNumber);
                                    txRecordRepository.save(txRecord);
                                }
                            } else {
                                if (input == null) {
                                    //from 만 비교 컨트렉트 전송이력 남기기
                                    if (checckAddress(fromWallet, toWallet)) {
                                        TxRecord txRecord = new TxRecord();
                                        txRecord.setAge(age);
                                        txRecord.setFee(fee.multiply(tx.getGasPrice()));
                                        txRecord.setFromAddress(getFrom);
                                        txRecord.setToAddress(getTo);
                                        txRecord.setHash(tx.getHash());
                                        txRecord.setValue(tx.getValue());
                                        txRecord.setType("Contract");
                                        txRecord.setTypeAddress("ETH");
                                        txRecord.setStatus(status);
                                        txRecord.setBlock(blockNumber);
                                        txRecordRepository.save(txRecord);
                                    }
                                } else if(input.length()<10){
                                    System.out.println(input);
                                    System.out.println(tx.getHash());
                                } else if (input.substring(0, 10).equals("0xa9059cbb")) {
                                    //지갑주소 비교
                                    //등록된 토큰인지 비교 후 토큰전송 이력 남기기
                                    String toAddress = "0x" + input.substring(34, 74).toLowerCase();
                                    Wallet toWallet2 = walletRepository.findOne(toAddress);
                                    if (checckAddress(fromWallet, toWallet2)) {
                                        Tokens tokens = tokensRepository.findOne(getTo);
                                        BigInteger value = Numeric.toBigInt(input.substring(74, input.length()));
                                        if (tokens == null) {
                                            //토큰정보 얻어오기
                                            dispatcher.getTokenInfo(getTo);
                                            System.out.println(getTo);
                                            Gson gson = new Gson();
                                            TokenInfo tokenInfo = gson.fromJson(dispatcher.response(), TokenInfo.class);
                                            if (tokenInfo.getAddress() != null) {
                                                tokens = new Tokens();
                                                tokens.setContractAddress(tokenInfo.getAddress());
                                                tokens.setDecimals(tokenInfo.getDecimals());
                                                tokens.setName(tokenInfo.getName());
                                                tokens.setSymbol(tokenInfo.getSymbol());
                                                tokensRepository.save(tokens);
                                            }
                                        }

                                        if(toWallet2 != null) {
                                            if(toWallet2.getTokens() == null){
                                                toWallet2.setTokens(tokens.getContractAddress());
                                            }else{
                                                if(!toWallet2.getTokens().contains(tokens.getContractAddress())) {
                                                    toWallet2.setTokens(toWallet2.getTokens() + "," + tokens.getContractAddress());
                                                }
                                            }
                                            walletRepository.save(toWallet2);
                                        }

                                        TxRecord txRecord = new TxRecord();
                                        txRecord.setAge(age);
                                        txRecord.setFee(fee.multiply(tx.getGasPrice()));
                                        txRecord.setFromAddress(getFrom);
                                        txRecord.setToAddress(toAddress);
                                        txRecord.setHash(tx.getHash());
                                        txRecord.setValue(value);
                                        txRecord.setType(tokens.getSymbol());
                                        txRecord.setTypeAddress(tokens.getContractAddress());
                                        txRecord.setStatus(status);
                                        txRecord.setBlock(blockNumber);
                                        txRecordRepository.save(txRecord);
                                    }
                                } else {
                                    //from 만 비교 컨트렉트 전송이력 남기기
                                    if (checckAddress(fromWallet, toWallet)) {
                                        TxRecord txRecord = new TxRecord();
                                        txRecord.setAge(age);
                                        txRecord.setFee(fee.multiply(tx.getGasPrice()));
                                        txRecord.setFromAddress(getFrom);
                                        txRecord.setToAddress(getTo);
                                        txRecord.setHash(tx.getHash());
                                        txRecord.setValue(tx.getValue());
                                        txRecord.setType("Contract");
                                        txRecord.setTypeAddress("ETH");
                                        txRecord.setStatus(status);
                                        txRecord.setBlock(blockNumber);
                                        txRecordRepository.save(txRecord);
                                    }
                                }
                            }
                        }
                    }
                } catch (NullPointerException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public boolean checckAddress(Wallet fromWallet, Wallet toWallet) {
        if (fromWallet == null && toWallet == null) {
            return false;
        } else {
            return true;
        }
    }
}
