package com.tcmp.fcupload.rep;

import java.util.List;

import com.tcmp.fcupload.mdl.InvBiller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.stereotype.Repository;

@Repository
public interface InvBillerRepository extends JpaRepository<InvBiller, String> {
    List<InvBiller> findAllByClientCIFAndCounterpartAndServiceId(String clientCIF, String counterpart, String serviceId);

    @Transactional
    void deleteByClientCIF(String cliCIF);

    @Modifying
    @Transactional
    @Query("UPDATE InvBiller b SET b.status = 'EXPIRED', b.subStatus = 'EXPIRED' WHERE b.clientCIF = :clientCIF")
    void updateStatusAndSubStatusByClientCIF(@Param("clientCIF") String clientCIF);
}
