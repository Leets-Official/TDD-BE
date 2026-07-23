package com.leets.tdd.party.repository;

import com.leets.tdd.party.domain.DeliveryParty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryPartyRepository extends JpaRepository<DeliveryParty, Long> {

    List<DeliveryParty> findAllByOrderByCreatedAtDesc();

}
