package com.leets.tdd.deliveryparty.repository;

import com.leets.tdd.deliveryparty.domain.DeliveryParty;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryPartyRepository extends JpaRepository<DeliveryParty, Long> {
}
