package com.tcmp.fcupload.rep;

import com.tcmp.fcupload.mdl.DtlCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.List;

@Repository
public interface DtlCompanyRep extends JpaRepository<DtlCompany, String> {
	List<DtlCompany> findByClientId(String clientId);
}
