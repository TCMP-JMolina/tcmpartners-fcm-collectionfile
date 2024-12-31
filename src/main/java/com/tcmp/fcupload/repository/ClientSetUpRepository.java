package com.tcmp.fcupload.repository;

import com.tcmp.fcupload.model.ClientSetUp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.List;

@Repository
public interface ClientSetUpRepository extends JpaRepository<ClientSetUp, String> {
	List<ClientSetUp> findByClientId(String clientId);
}
