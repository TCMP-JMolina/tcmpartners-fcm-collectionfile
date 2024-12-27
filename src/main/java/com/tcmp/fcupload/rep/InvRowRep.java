package com.tcmp.fcupload.rep;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tcmp.fcupload.mdl.InvRow;

@Repository
public interface InvRowRep extends JpaRepository<InvRow, String> { 
}
