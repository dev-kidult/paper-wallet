package eth.devkidult.paperwallet.repository;

import eth.devkidult.paperwallet.model.Tokens;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TokensRepository extends JpaRepository<Tokens, String> {
    Page<Tokens> findByExposureStatusToAdmin(Boolean admin, Pageable pageable);
    List<Tokens> findByExposureStatusToUser(Boolean user);
    Page<Tokens> findByContractAddressLikeAndExposureStatusToAdminOrNameLikeAndExposureStatusToAdminOrSymbolLikeAndExposureStatusToAdmin(String contractAddress, Boolean admin, String name, Boolean admin2, String symbol, Boolean admin3, Pageable pageable);
}
