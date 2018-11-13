package eth.devkidult.paperwallet.repository;

import eth.devkidult.paperwallet.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User,String> {
    Page<User> findByEmailLike (String email, Pageable pasPageable);
}
