package com.tcmp.fcupload.rep;

import com.tcmp.fcupload.mdl.InvBill;
import com.tcmp.fcupload.mdl.InvoiceMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface InvoiceMasterRepository extends JpaRepository<InvoiceMaster, String> {

    List<InvoiceMaster> findByUploadedAndSubStatus(String uploaded, String subStatus);
}
