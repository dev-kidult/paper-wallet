package eth.devkidult.paperwallet.repository;

import eth.devkidult.paperwallet.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WalletRepository extends JpaRepository<Wallet,String> {
    List<Wallet> findByUser_Email(String user_Email);
    int countByUser_Email(String user_Email);
}
