package eth.devkidult.paperwallet.Controller;

import eth.devkidult.paperwallet.Component.MailService;
import eth.devkidult.paperwallet.model.TxRecord;
import eth.devkidult.paperwallet.model.User;
import eth.devkidult.paperwallet.model.Wallet;
import eth.devkidult.paperwallet.repository.TxRecordRepository;
import eth.devkidult.paperwallet.repository.UserRepository;
import eth.devkidult.paperwallet.repository.WalletRepository;
import eth.devkidult.paperwallet.utils.CharMix;
import eth.devkidult.paperwallet.utils.ConvertValue;
import eth.devkidult.paperwallet.utils.PasswordEncode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

@RestController
public class AjaxRestController {

    @Autowired
    WalletRepository walletRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    MailService mailService;

    @Autowired
    TxRecordRepository txRecordRepository;

    @Autowired
    ConvertValue convertValue;

    PasswordEncode passwordEncode = new PasswordEncode();

    private final String SUCCESS = "Success";
    private final String FAIL = "Fail";
    private final String DUPLICATE = "Duplicate";

    @PostMapping("/checkWalletPassword")
    public String checkWalletPassword(String walletAddress , String password){
        Wallet wallet = walletRepository.findOne(walletAddress);
        if(wallet.getPassword().equals(password)){
            return SUCCESS;
        }else {
            return FAIL;
        }
    }

    @PostMapping("/exportPrivateKey")
    public String exportPrivateKey(String address) throws Exception {
        Wallet wallet = walletRepository.findOne(address);
        String walletPassword = wallet.getPassword();
        Credentials credentials = WalletUtils.loadCredentials(walletPassword, wallet.getFilePath());
        return credentials.getEcKeyPair().getPrivateKey().toString(16);
}

    @PostMapping("/changePassword")
    public String changePassword(String password, HttpSession session , String oldPassword) {
        User user = (User) session.getAttribute("sessionUser");
        User DBUser = userRepository.findOne(user.getEmail());
        if (user != null) {
            if(passwordEncode.matches(oldPassword,DBUser.getPassword())){
                user.setPassword(passwordEncode.encode(password));
                userRepository.save(user);
                return SUCCESS;
            } else {
                return FAIL;
            }
        } else {
            return FAIL;
        }
    }

    @PostMapping("/exportKeystore")
    public StreamingResponseBody exportWallet(HttpServletResponse response, String address) throws IOException {
        Wallet wallet = walletRepository.findOne(address);
        String fullPath = wallet.getFilePath();
        String findFileName[] = fullPath.split("/");
        response.setContentType("multipart/formed-data");
        response.setHeader("Content-Disposition", "attachment; filename=" + findFileName[findFileName.length - 1]);
        InputStream inputStream = new FileInputStream(new File(fullPath));
        return outputStream -> {
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                outputStream.write(data, 0, nRead);
            }
        };
    }

    @PostMapping("/findPassword")
    public String findPassword(String email) {
        User user = userRepository.findOne(email);
        if (user != null) {
            String randomCode = CharMix.makeMessage();
            user.setPassword(passwordEncode.encode(randomCode));
            userRepository.save(user);
            String title = "< PaperWallet - Find Password >";
            String content = "<h2>Temporary password : " + randomCode + "</h2><br/>We will provide you with a temporary password.<br/>Please sign in with the temporary password and change your password";
            mailService.sendMail(email, title, content);
            return SUCCESS;
        } else {
            return FAIL;
        }
    }

    @PostMapping("/checkEmail")
    public String checkEmail(String email){
        User user = userRepository.findOne(email);
        if(user == null){
            return SUCCESS;
        } else {
            return DUPLICATE;
        }
    }

    @PostMapping("/verifyEmail")
    public String verifyEmail(String email) {
        User user = userRepository.findOne(email);
        if (user == null) {
            if (email != null) {
                String randomCode = CharMix.makeMessage();
                String title = "< paperWallet - Email verification code >";
                String content = "<h3>We will inform you of the e-mail authentication certificate you requested.</h3>" +
                        "" +
                        "E-mail authentication prevents e-mail theft and damage by entering false information and erroneous information," +
                        "<br/>This is the process to verify that the email entered in the member information is correct." +
                        "" +
                        "<br/>* Please copy and validate your <Strong>email verification code</Strong> below." +
                        "Code : <strong>" + randomCode + "</strong>";
                mailService.sendMail(email, title, content);
                return randomCode;
            } else {
                return FAIL;
            }
        } else {
            return DUPLICATE;
        }
    }

    @PostMapping("/signUp")
    public void signUp(String email, String password) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncode.encode(password));
        user.setJoinDate(new Date());
        user.setLastLogin(new Date());
        userRepository.save(user);
    }

    @PostMapping("/changeWalletName")
    public void changeWalletName(String walletName, HttpSession session) {
        Wallet wallet = (Wallet) session.getAttribute("selectedWallet");
        wallet.setName(walletName);
        walletRepository.save(wallet);
        session.setAttribute("selectedWallet",wallet);
    }

    @RequestMapping("/transactionRecordsPaging")
    public String pagingTransaction(@PageableDefault(size = 7) Pageable pageable, HttpSession session){
        Wallet wallet = (Wallet)session.getAttribute("selectedWallet");
        String address = wallet.getAddress();
        Page<TxRecord> txRecords = txRecordRepository.findByFromAddressOrToAddressOrderByAgeDesc(address,address,pageable);
        String htmlSource = "";
        if(txRecords.getContent() != null) {
            for (TxRecord txRecord : txRecords.getContent()) {
                String hash = txRecord.getHash();
                String shorthash = txRecord.getHash().substring(0, 6)+"......";
                String value = "";
                String status = "";
                String type = "";
                if (txRecord.getType().equals("Contract")) {
                    value = "0.0000";
                    status = "notice";
                    type = "ETH";
                } else {
                    if (address.equals(txRecord.getFromAddress())) {
                        value = "-" + convertValue.convertValue(txRecord).toString();
                        status = "out";
                        type = txRecord.getType();
                    } else {
                        value = "+" + convertValue.convertValue(txRecord).toString();
                        status = "in";
                        type = txRecord.getType();
                    }
                }
                htmlSource += "<a href=\"/transactionInfo?hash=" + txRecord.getHash() + "\" class=\"list-group-item list-group-item-action list-item\">\n" +
                        "                    <div class=\"row\">\n" +
                        "                        <div class=\"in-img col-1\" style=\"\">\n" +
                        "                            <i class=\"fas fa-arrow-alt-circle-down " + status + "\"></i>\n" +
                        "                        </div>\n" +
                        "                        <div class=\" col-4\">\n" +
                        "                            <div class=\"record-address\"><h4>" + txRecord.getHash().substring(0, 6) + "......</h4></div>\n" +
                        "                            <div><span class=\"age\">" + txRecord.getAge() + "</span></div>\n" +
                        "                        </div>\n" +
                        "                        <div class=\"col-2\"></div>\n" +
                        "                        <div class=\"em-place col-3\">\n" +
                        "                            <div class=\"record-in\"><h3>" + value + "</h3> " + type + "</div>\n" +
                        "                        </div>\n" +
                        "                    </div>\n" +
                        "                </a>";
            }
        }
        return htmlSource;
    }
}
