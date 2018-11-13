package com.sweden.webwallet.repository;

import com.sweden.webwallet.model.TxRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TxRecordRepository extends JpaRepository<TxRecord, String> {
    List<TxRecord> findByFromAddressAndTypeAddressOrToAddressAndTypeAddressOrderByAgeDesc(String fromAddress, String typeAddress1, String toAddress, String typeAddress2);

    Page<TxRecord> findByFromAddressOrToAddressOrderByAgeDesc(String fromAddress, String toAddress, Pageable pageable);

    Page<TxRecord> findByFromAddressLikeOrToAddressLikeOrHashLikeOrTypeLikeOrderByAgeDesc(String fromAddress, String toAddress, String hash, String type,Pageable pageable);

    List<TxRecord> findByFromAddressAndHashLikeOrFromAddressAndTypeLikeOrToAddressAndHashLikeOrToAddressAndTypeLikeOrderByAgeDesc(String fromAddress1, String hash1, String fromAddress2, String type1, String toAddress1, String hash2, String toAddress2, String type2);

    @Query(nativeQuery = true, value = "select hash,age,block,fee,from_address,status,to_address,type,type_address,sum(value) as value from tx_record group by type order by sum(value) desc limit 10")
    List<TxRecord> getTop10();
}
