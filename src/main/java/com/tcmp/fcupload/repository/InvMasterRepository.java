package com.tcmp.fcupload.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tcmp.fcupload.model.InvMaster;

import java.util.List;

@Repository
public interface InvMasterRepository extends JpaRepository<InvMaster, String> {
    List<InvMaster> findByUploadedAndSubStatus(String uploaded, String subStatus);
}
