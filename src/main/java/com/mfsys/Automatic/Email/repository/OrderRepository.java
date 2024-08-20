package com.mfsys.Automatic.Email.repository;

import com.mfsys.Automatic.Email.model.FreightOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<FreightOrder, Long> {
}
