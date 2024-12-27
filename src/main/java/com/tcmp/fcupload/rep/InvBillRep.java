package com.tcmp.fcupload.rep;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.tcmp.fcupload.mdl.InvBill;

import org.springframework.stereotype.Repository;

@Repository
public interface InvBillRep extends JpaRepository<InvBill, String> { 
    List<InvBill> findAllByCliCIFAndBillCpartAndServiceId(String cliCIF, String billCpart, String serviceId);

    @Transactional
    void deleteByCliCIF(String cliCIF);

    @Modifying
    @Transactional
    @Query("UPDATE InvBill b SET b.billStatus = 'EXPIRED', b.billSubStatus = 'EXPIRED' WHERE b.cliCIF = :cliCIF")
    void updateStatusAndSubStatusByCliCIF(@Param("cliCIF") String cliCIF);
}
