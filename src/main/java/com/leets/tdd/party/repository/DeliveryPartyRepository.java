package com.leets.tdd.party.repository;

import java.util.List;

import com.leets.tdd.party.domain.DeliveryParty;

import org.springframework.data.jpa.repository.JpaRepository;


public interface DeliveryPartyRepository extends JpaRepository<DeliveryParty, Long> {

    List<DeliveryParty> findAllByOrderByCreatedAtDesc();

}
