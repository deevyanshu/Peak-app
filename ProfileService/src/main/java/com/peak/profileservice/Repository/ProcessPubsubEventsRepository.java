package com.peak.profileservice.Repository;

import com.peak.profileservice.Entity.ProcessPubsubEvents;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessPubsubEventsRepository extends JpaRepository<ProcessPubsubEvents, String> {
}
