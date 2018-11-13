package eth.devkidult.paperwallet.Controller;


import com.google.gson.Gson;
import eth.devkidult.paperwallet.Component.Dispatcher;
import eth.devkidult.paperwallet.Component.MailService;
import eth.devkidult.paperwallet.etherplorerDispatcher.AddressToToken;
import eth.devkidult.paperwallet.etherplorerDispatcher.TokenInfo;
import eth.devkidult.paperwallet.model.Tokens;
import eth.devkidult.paperwallet.model.TxRecord;
import eth.devkidult.paperwallet.model.User;
import eth.devkidult.paperwallet.model.Wallet;
import eth.devkidult.paperwallet.repository.TokensRepository;
import eth.devkidult.paperwallet.repository.TxRecordRepository;
import eth.devkidult.paperwallet.repository.UserRepository;
import eth.devkidult.paperwallet.repository.WalletRepository;
import eth.devkidult.paperwallet.utils.ConvertValue;
import eth.devkidult.paperwallet.utils.PasswordEncode;
import eth.devkidult.paperwallet.utils.Web3jLogic;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.admin.Admin;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Controller
public class MainController {

    @Autowired
    UserRepository userRepository;

    @Autowired
    WalletRepository walletRepository;

    @Autowired
    TokensRepository tokensRepository;

    @Autowired
    TxRecordRepository txRecordRepository;

    @Autowired
    Admin web3j;

    @Autowired
    Dispatcher dispatcher;

    @Autowired
    MailService mailService;

    @Autowired
    Web3jLogic web3jLogic;

    @Autowired
    ConvertValue convertValue;

    private PasswordEncode passwordEncode = new PasswordEncode();


    private final String DEFAULT_PATH = WalletUtils.getMainnetKeyDirectory() + "/";

    @RequestMapping("/")
    public String home(HttpServletRequest request, HttpSession session, HttpServletResponse response) throws IOException {
        if (checkSignIn(session)) {
            User user = (User)session.getAttribute("sessionUser");
            readyToSignIn(user,session);
            return "/main";
        } else {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equals("webWalletAutoSignIn")) {
                        if (userRepository.findOne(cookie.getValue()) != null) {
                            readyToSignIn(userRepository.findOne(cookie.getValue()), session);
                            cookie.setMaxAge(60 * 60 * 24 * 30);
                            response.addCookie(cookie);
                            return "redirect:/";
                        } else {
                            cookie.setMaxAge(0);
                            response.addCookie(cookie);
                            return "redirect:/";
                        }
                    }
                }
            }
            return "/index";
        }
    }

    @PostMapping("/signIn")
    public String signIn(User user, HttpSession session, RedirectAttributes redirectAttributes, HttpServletResponse response, String autoLogin) throws IOException {
            User dbUser = userRepository.findOne(user.getEmail());
            if (dbUser == null) {
                redirectAttributes.addFlashAttribute("error", "This user does not exist");
                return "redirect:/";
            } else if (passwordEncode.matches(user.getPassword(), dbUser.getPassword())) {
                if (autoLogin != null) {
                    Cookie cookie = new Cookie("webWalletAutoSignIn", user.getEmail());
                    cookie.setMaxAge(60 * 60 * 24 * 30);
                    response.addCookie(cookie);
                }
                readyToSignIn(user, session);
                return "redirect:/";
            } else {
                redirectAttributes.addFlashAttribute("error", "The password is incorrect");
                return "redirect:/";
            }

    }

    @RequestMapping("/signOut")
    public String signOut(HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        session.invalidate();
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("webWalletAutoSignIn")) {
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                    break;
                }
            }
        }
        return "/index";
    }

    @RequestMapping("/getTokenImage/{id}")
    public ResponseEntity<byte[]> getTokenImage(@PathVariable String id) {

        Tokens tokens = tokensRepository.findOne(id);

        byte[] img = tokens.getImage();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("Content-Type", "image/png");
        return new ResponseEntity<byte[]>(img, httpHeaders, HttpStatus.OK);
    }

    @GetMapping("/findPassword")
    public String findPassword() {
        return "/findPassword";
    }

    @GetMapping("/changePassword")
    public String changePassword() {
        return "/changePassword";
    }

    @GetMapping("/createWallet")
    public String createWallet() {
        return "/createWallet";
    }

    @PostMapping("/createWallet")
    public String createWallet(String walletPassword, String walletName, HttpSession session, RedirectAttributes redirectAttributes) throws Exception {
        User user = (User) session.getAttribute("sessionUser");
        int count = walletRepository.countByUser_Email(user.getEmail());
        if (count >= 5) {
            redirectAttributes.addFlashAttribute("error", "You can only create up to 5 wallets");
            return "redirect:/";
        } else {
            String walletPath = DEFAULT_PATH + user.getEmail() + "/";//기본경로+이메일
            File file = new File(walletPath);
            if (!file.exists()) {
                file.mkdirs();
            }
            String fileName = WalletUtils.generateNewWalletFile(walletPassword, new File(walletPath), true);
            Credentials credentials = WalletUtils.loadCredentials(walletPassword, walletPath + fileName);
            Wallet wallet = new Wallet();
            wallet.setFilePath(walletPath + fileName);
            wallet.setUser(user);
            wallet.setAddress(credentials.getAddress());
            wallet.setPassword(walletPassword);
            wallet.setName(walletName);
            walletRepository.save(wallet);

            List<Wallet> walletList = walletRepository.findByUser_Email(user.getEmail());

            session.setAttribute("walletList", walletList);
            session.setAttribute("selectedWallet", wallet);
            session.setAttribute("tokenAndValues", tokenAndValues(wallet));
            return "redirect:/";
        }
    }

    @GetMapping("/selectWallet")
    public String selectWallet(HttpSession session, String walletAddress) throws IOException {
        Wallet wallet = walletRepository.findOne(walletAddress);
        if (wallet != null) {
            session.setAttribute("selectedWallet", wallet);
            session.setAttribute("tokenAndValues", tokenAndValues(wallet));
            return "redirect:/";
        } else {
            return "redirect:/";
        }
    }


    @PostMapping("/sendForm")
    public String sendForm(String tokenAddress, String havingValue, String symbol, Model model, HttpSession session) throws IOException{

        Wallet wallet = (Wallet) session.getAttribute("selectedWallet");
        String walletAddress = wallet.getAddress();

        model.addAttribute("walletAddress", walletAddress);

        model.addAttribute("tokenAddress", tokenAddress);
        if (tokenAddress.equals("ETH")) {
            model.addAttribute("fee", 21000); //eth전송
        } else {
            model.addAttribute("fee", 90000); //token전송
        }
        model.addAttribute("havingValue", havingValue);
        model.addAttribute("symbol", symbol);

        model.addAttribute("havingEth" , web3jLogic.EthBalance(walletAddress).toString());

        Tokens tokens = tokensRepository.findOne(tokenAddress);

        boolean tokenImg = false;

        if(tokens != null){
            if(tokens.getImage() != null){
                tokenImg = true;
            }
        }

        model.addAttribute("tokenImg",tokenImg);

        return "/send";
    }

    @PostMapping("/sending")
    public String sending(HttpSession session, String to, String value, String fee, String tokenAddress, RedirectAttributes redirectAttributes) throws Exception {
        Wallet wallet = (Wallet) session.getAttribute("selectedWallet");
        String havingValue = web3jLogic.EthBalance(wallet.getAddress()).toString();
        double havingValueD = Double.parseDouble(havingValue);
        double valueD = Double.parseDouble(value);
        double feeD = Double.parseDouble(fee);
        if (!tokenAddress.equals("ETH")) {
            if(havingValueD < feeD)
                redirectAttributes.addFlashAttribute("error","The fee is not enough.");
            else
                web3jLogic.SendToken(wallet, to, value, fee, tokenAddress);
        } else {
            if(havingValueD < valueD+feeD)
                redirectAttributes.addFlashAttribute("error","The fee is not enough.");
            else
                web3jLogic.SendEth(wallet, to, value, fee);

        }
        return "redirect:/";
    }


    @GetMapping("/transactionRecords")
    public String transactionRecords(HttpSession session, Model model, @PageableDefault(size = 7) Pageable pageable) {
        Wallet wallet = (Wallet) session.getAttribute("selectedWallet");
        if(wallet == null){
            return "redirect:/";
        }
        String address = wallet.getAddress();
        Page<TxRecord> txRecordList = txRecordRepository.findByFromAddressOrToAddressOrderByAgeDesc(address, address ,pageable);
        System.out.println(txRecordList.getContent().size());
        List<ReturnTxRecord> txRecordList1 = new ArrayList<>();

        for (TxRecord txRecord : txRecordList.getContent()) {
            ReturnTxRecord txRecord1 = new ReturnTxRecord();
            txRecord1.setAge(txRecord.getAge());
            txRecord1.setHash(txRecord.getHash());
            txRecord1.setShortHash(txRecord.getHash().substring(0, 6) + "......");
            if (txRecord.getType().equals("Contract")) {
                txRecord1.setValue("0.0000");
                txRecord1.setStatus("notice");
                txRecord1.setType("ETH");
            } else {
                if (address.equals(txRecord.getFromAddress())) {
                    txRecord1.setValue("-" + convertValue.convertValue(txRecord).toString());
                    txRecord1.setStatus("out");
                    txRecord1.setType(txRecord.getType());
                } else {
                    txRecord1.setValue("+" + convertValue.convertValue(txRecord).toString());
                    txRecord1.setStatus("in");
                    txRecord1.setType(txRecord.getType());
                }
            }
            txRecordList1.add(txRecord1);
        }

        model.addAttribute("txRecordList", txRecordList1);
        return "/transactionRecords";
    }

    @PostMapping("/transactionRecords")
    public String transactionRecords(HttpSession session, Model model, String search) {
        Wallet wallet = (Wallet) session.getAttribute("selectedWallet");
        if(wallet == null){
            return "redirect:/";
        }
        if(search == null)
            search = "";
        else
            search = "%"+search+"%";
        String address = wallet.getAddress();
        List<TxRecord> txRecordList = txRecordRepository.findByFromAddressAndHashLikeOrFromAddressAndTypeLikeOrToAddressAndHashLikeOrToAddressAndTypeLikeOrderByAgeDesc(address,search,address,search,address,search,address,search);
        List<ReturnTxRecord> txRecordList1 = new ArrayList<>();

        for (TxRecord txRecord : txRecordList) {
            ReturnTxRecord txRecord1 = new ReturnTxRecord();
            txRecord1.setAge(txRecord.getAge());
            txRecord1.setHash(txRecord.getHash());
            txRecord1.setShortHash(txRecord.getHash().substring(0, 6) + "......");
            if (txRecord.getType().equals("Contract")) {
                txRecord1.setValue("0.0000");
                txRecord1.setStatus("notice");
                txRecord1.setType("ETH");
            } else {
                if (address.equals(txRecord.getFromAddress())) {
                    txRecord1.setValue("-" + convertValue.convertValue(txRecord).toString());
                    txRecord1.setStatus("out");
                    txRecord1.setType(txRecord.getType());
                } else {
                    txRecord1.setValue("+" + convertValue.convertValue(txRecord).toString());
                    txRecord1.setStatus("in");
                    txRecord1.setType(txRecord.getType());
                }
            }
            txRecordList1.add(txRecord1);
        }

        model.addAttribute("txRecordList", txRecordList1);
        return "/transactionRecords";
    }

    @GetMapping("/transactionInfo")
    public String transactionInfo(String hash, Model model) {
        TxRecord txRecord = txRecordRepository.findOne(hash);

        Map<String, BigDecimal> convertMap = new HashMap<>();
        convertMap.put("convertValue", convertValue.convertValue(txRecord));
        convertMap.put("convertFee", convertValue.convertFee(txRecord.getFee()));

        Tokens tokens = tokensRepository.findOne(txRecord.getTypeAddress());

        boolean tokenImg = false;

        if(tokens != null)
            if(tokens.getImage() != null)
               tokenImg = true;

        model.addAttribute("convertMap", convertMap);
        model.addAttribute("txRecord", txRecord);
        model.addAttribute("tokenImg",tokenImg);
        return "/transactionInfo";
    }

    @GetMapping("/txRecord")
    public String txRecord(HttpSession session, Model model, String typeAddress) throws IOException {
        Wallet wallet = (Wallet) session.getAttribute("selectedWallet");
        String address = wallet.getAddress();

        List<TxRecord> txRecordList = txRecordRepository.findByFromAddressAndTypeAddressOrToAddressAndTypeAddressOrderByAgeDesc(address, typeAddress, address, typeAddress);
        List<ReturnTxRecord> txRecordList1 = new ArrayList<>();

        for (TxRecord txRecord : txRecordList) {
            ReturnTxRecord txRecord1 = new ReturnTxRecord();
            txRecord1.setAge(txRecord.getAge());
            txRecord1.setHash(txRecord.getHash());
            txRecord1.setShortHash(txRecord.getHash().substring(0, 6) + ".....");
            if (txRecord.getType().equals("Contract")) {
                txRecord1.setStatus("notice");
                txRecord1.setValue("0.0000");
                txRecord1.setType("ETH");
            } else {
                if (address.equals(txRecord.getFromAddress())) {
                    txRecord1.setStatus("out");
                    txRecord1.setValue("-" + convertValue.convertValue(txRecord).toString());
                    txRecord1.setType(txRecord.getType());
                } else {
                    txRecord1.setStatus("in");
                    txRecord1.setValue("+" + convertValue.convertValue(txRecord).toString());
                    txRecord1.setType(txRecord.getType());
                }
            }
            txRecordList1.add(txRecord1);
        }

        Tokens tokens = tokensRepository.findOne(typeAddress);

        String symbol = null;

        if (tokens == null)
            symbol = "ETH";
        else
            symbol = tokens.getSymbol();

        model.addAttribute("txRecordList", txRecordList1);
        model.addAttribute("symbol", symbol);
        model.addAttribute("tokenAddress", typeAddress);

        boolean tokenImg = false;
        if(tokens != null)
            if(tokens.getImage() != null)
                tokenImg = true;

        model.addAttribute("tokenImg",tokenImg);

        if (symbol.equals("ETH")) {
            model.addAttribute("value", web3jLogic.EthBalance(address));
        } else {
            int decimal = tokens.getDecimals();
            model.addAttribute("value", web3jLogic.TokenBalance(address, typeAddress, decimal));
        }

        return "/token";
    }

    @GetMapping("/myWallet")
    public String myWallet(HttpSession session, Model model) throws IOException{
        List<Wallet> walletList = (List)session.getAttribute("walletList");
        List<HashMap<String,String>> walletList2 = new ArrayList<>();
        if(walletList != null) {
            for (Wallet wallet : walletList) {
                HashMap<String, String> map = new HashMap<>();
                map.put("name", wallet.getName());
                map.put("address", wallet.getAddress());
                map.put("value", web3jLogic.EthBalance(wallet.getAddress()).toString());
                walletList2.add(map);
            }
        }
        model.addAttribute("myWalletList", walletList2);
        return "/myWallet";
    }

    @GetMapping("/importWallet")
    public String importWallet() {
        return "/importWallet";
    }

    @PostMapping("/importWallet/{type}")
    public String importWallet(@PathVariable String type, String keyword, String password, HttpSession session, MultipartFile file, String walletName, RedirectAttributes redirectAttributes) throws Exception {
        User user = (User) session.getAttribute("sessionUser");
        int count = walletRepository.countByUser_Email(user.getEmail());
        if (count >= 5) {
            redirectAttributes.addFlashAttribute("error", "You can only create up to 5 wallets");
            return "redirect:/";
        }else if (type == null || type.equals("")) {
            return "redirect:/";
        } else if (type.equals("Keystore")) {
            if(file.getOriginalFilename().endsWith(".json")) {
                File walletFile = new File(DEFAULT_PATH + user.getEmail() + "/");
                if (!file.isEmpty()) {
                    byte[] bytes = file.getBytes();
                    if (!walletFile.exists()) {
                        walletFile.mkdirs();
                    }
                    Path path = Paths.get(DEFAULT_PATH + user.getEmail() + "/" + file.getOriginalFilename());//디폴트경로 추가
                    Files.write(path, bytes);

                    try {
                        Credentials credentials = WalletUtils.loadCredentials(password, DEFAULT_PATH + user.getEmail() + "/" + file.getOriginalFilename());

                        String walletAddress = credentials.getAddress();

                        dispatcher.getTokens(walletAddress);
                        Gson gson = new Gson();
                        AddressToToken addressToToken = gson.fromJson(dispatcher.response(), AddressToToken.class);

                        List<eth.devkidult.paperwallet.etherplorerDispatcher.Tokens> tokensList = addressToToken.getTokens();

                        List<String> tokenList = new ArrayList<>();
                        if (tokensList != null) {
                            for (eth.devkidult.paperwallet.etherplorerDispatcher.Tokens tokens : tokensList) {
                                String tokenAddress = tokens.getTokenInfo().getAddress();
                                tokenList.add(tokenAddress);
                                Tokens inputTokens = tokensRepository.findOne(tokenAddress);
                                if (inputTokens == null) {
                                    dispatcher.getTokenInfo(tokenAddress);
                                    System.out.println(tokenAddress);
                                    Gson gson2 = new Gson();
                                    TokenInfo tokenInfo = gson2.fromJson(dispatcher.response(), TokenInfo.class);
                                    if (tokenInfo.getAddress() != null) {
                                        inputTokens = new Tokens();
                                        inputTokens.setContractAddress(tokenInfo.getAddress());
                                        inputTokens.setDecimals(tokenInfo.getDecimals());
                                        inputTokens.setName(tokenInfo.getName());
                                        inputTokens.setSymbol(tokenInfo.getSymbol());
                                        tokensRepository.save(inputTokens);
                                    }
                                }
                            }
                        }
                        String tokenListString = null;
                        if (tokenList.size() > 0) {
                            tokenListString = tokenList.toString().trim().substring(1, tokenList.toString().length() - 1);
                        }

                        Wallet wallet = new Wallet();
                        wallet.setPassword(password);
                        wallet.setAddress(walletAddress);
                        wallet.setUser(user);
                        wallet.setFilePath(DEFAULT_PATH + user.getEmail() + "/" + file.getOriginalFilename());
                        wallet.setTokens(tokenListString);
                        wallet.setName(walletName);
                        walletRepository.save(wallet);
                        session.setAttribute("selectedWallet", wallet);
                    } catch (CipherException e){
                        redirectAttributes.addFlashAttribute("error","wallet password is wrong");
                    }
                }
            } else {
                redirectAttributes.addFlashAttribute("error","only supported json file");
            }
        } else { //private Key
            try {
                String path = DEFAULT_PATH + user.getEmail() + "/";
                File walletFile = new File(path);
                Credentials credentials = Credentials.create(keyword);
                ECKeyPair ecKeyPair = credentials.getEcKeyPair();
                if (!walletFile.exists()) {
                    walletFile.mkdirs();
                }
                String fileName = WalletUtils.generateWalletFile(password, ecKeyPair, walletFile, true);

                String walletAddress = credentials.getAddress();

                dispatcher.getTokens(walletAddress);
                Gson gson = new Gson();
                AddressToToken addressToToken = gson.fromJson(dispatcher.response(), AddressToToken.class);

                System.out.println(addressToToken.toString());

                List<eth.devkidult.paperwallet.etherplorerDispatcher.Tokens> tokensList = addressToToken.getTokens();

                List<String> tokenList = new ArrayList<>();

                if (tokensList.size() > 0) {
                    for (eth.devkidult.paperwallet.etherplorerDispatcher.Tokens tokens : tokensList) {
                        String tokenAddress = tokens.getTokenInfo().getAddress();
                        tokenList.add(tokenAddress);
                        Tokens inputTokens = tokensRepository.findOne(tokenAddress);
                        if (inputTokens == null) {
                            dispatcher.getTokenInfo(tokenAddress);
                            System.out.println(tokenAddress);
                            Gson gson2 = new Gson();
                            TokenInfo tokenInfo = gson2.fromJson(dispatcher.response(), TokenInfo.class);
                            if (tokenInfo.getAddress() != null) {
                                inputTokens = new Tokens();
                                inputTokens.setContractAddress(tokenInfo.getAddress());
                                inputTokens.setDecimals(tokenInfo.getDecimals());
                                inputTokens.setName(tokenInfo.getName());
                                inputTokens.setSymbol(tokenInfo.getSymbol());
                                tokensRepository.save(inputTokens);
                            }
                        }
                    }
                }

                String tokenListString = null;
                if (tokenList.size() > 0) {
                    tokenListString = tokenList.toString().trim().substring(1, tokenList.toString().length() - 1);
                }

                Wallet wallet = new Wallet();
                wallet.setPassword(password);
                wallet.setAddress(walletAddress);
                wallet.setUser(user);
                wallet.setFilePath(path + fileName);
                wallet.setTokens(tokenListString);
                wallet.setName(walletName);
                walletRepository.save(wallet);
                session.setAttribute("selectedWallet", wallet);
            }catch (Exception e){
                redirectAttributes.addFlashAttribute("error","private Key is incorrect");
            }
        }

        List<Wallet> walletList = walletRepository.findByUser_Email(user.getEmail());
        session.setAttribute("walletList", walletList);

        return "redirect:/";
    }

    @GetMapping("/privacy")
    public String privacy(){
        return "/privacy";
    }


    public boolean checkSignIn(HttpSession session) {
        if (session.getAttribute("sessionUser") == null)
            return false;
        else
            return true;
    }

    public void readyToSignIn(User user, HttpSession session) throws IOException {
        List<Wallet> walletList = walletRepository.findByUser_Email(user.getEmail());
        User dbUser = userRepository.findOne(user.getEmail());
        dbUser.setLastLogin(new Date());
        userRepository.save(dbUser);
        session.setAttribute("sessionUser", user);
        System.out.println(walletList.size());
        if (walletList.size() != 0) {
            if(session.getAttribute("selectedWallet") == null) {
                session.setAttribute("walletList", walletList);
                session.setAttribute("selectedWallet", walletList.get(0));
                session.setAttribute("tokenAndValues", tokenAndValues(walletList.get(0)));
            } else{
                session.setAttribute("walletList", walletList);
                Wallet wallet = (Wallet) session.getAttribute("selectedWallet");
                session.setAttribute("tokenAndValues", tokenAndValues(wallet));
            }
        }
    }

    public List<HavingTokenAndValue> tokenAndValues(Wallet wallet) throws IOException {
        String[] tokens = null;
        if (wallet.getTokens() != null) {
            tokens = wallet.getTokens().split(",");
        }
        List<HavingTokenAndValue> tokenAndValues = new ArrayList<HavingTokenAndValue>();
        HavingTokenAndValue eth = new HavingTokenAndValue();
        eth.setTokenName("Ethereum");
        eth.setTokenSymbol("ETH");
        eth.setContractAddress("ETH");
        eth.setTokenValue(web3jLogic.EthBalance(wallet.getAddress()).toString());
        tokenAndValues.add(eth);
        System.out.println(eth.toString());
        if (tokens != null) {
            for (String token : tokens) {
                Tokens findToken = tokensRepository.findOne(token.trim());
                if (findToken == null) {
                    //토큰정보 얻어오기
                    dispatcher.getTokenInfo(token);
                    Gson gson = new Gson();
                    TokenInfo tokenInfo = gson.fromJson(dispatcher.response(), TokenInfo.class);
                    if (tokenInfo.getAddress() != null) {
                        findToken = new Tokens();
                        findToken.setContractAddress(tokenInfo.getAddress());
                        findToken.setDecimals(tokenInfo.getDecimals());
                        findToken.setName(tokenInfo.getName());
                        findToken.setSymbol(tokenInfo.getSymbol());
                        tokensRepository.save(findToken);
                    }
                }
                HavingTokenAndValue tokenAndValue = new HavingTokenAndValue();
                tokenAndValue.setContractAddress(findToken.getContractAddress());
                tokenAndValue.setTokenName(findToken.getName());
                tokenAndValue.setTokenSymbol(findToken.getSymbol());
                tokenAndValue.setTokenValue(web3jLogic.TokenBalance(wallet.getAddress(), findToken.getContractAddress(), findToken.getDecimals()).toString());
                String tokenValue = tokenAndValue.getTokenValue();
                if(tokenValue.length() > 10){
                    tokenValue = tokenValue.substring(0,9)+"...";
                    tokenAndValue.setTokenValue(tokenValue);
                }
                if(findToken.getImage() != null){
                    tokenAndValue.setTokenImg(true);
                }
                System.out.println(tokenAndValue.toString());
                tokenAndValues.add(tokenAndValue);
            }
        }
        return tokenAndValues;
    }

}

@Data
class HavingTokenAndValue implements Serializable {
    private String contractAddress;
    private String tokenName;
    private String tokenSymbol;
    private boolean tokenImg;
    private String tokenValue;
}

@Data
class ReturnTxRecord implements Serializable {
    private String hash;
    private String age;
    private String value;
    private String shortHash;
    private String type;
    private String status;
}