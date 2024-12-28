package com.tcmp.fcupload.rep;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tcmp.fcupload.mdl.InvMaster;

import java.util.List;

@Repository
public interface InvMasterRepository extends JpaRepository<InvMaster, String> {
    List<InvMaster> findByUploadedAndSubStatus(String uploaded, String subStatus);
}
