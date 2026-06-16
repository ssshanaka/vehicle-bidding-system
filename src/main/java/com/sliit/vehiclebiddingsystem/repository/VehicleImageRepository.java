package com.sliit.vehiclebiddingsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sliit.vehiclebiddingsystem.entity.VehicleImage;

@Repository
public interface VehicleImageRepository extends JpaRepository<VehicleImage, Long> {
}
