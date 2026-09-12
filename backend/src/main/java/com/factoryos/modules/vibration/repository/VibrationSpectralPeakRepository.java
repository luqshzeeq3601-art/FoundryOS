package com.factoryos.modules.vibration.repository;

import com.factoryos.modules.vibration.domain.VibrationSpectralPeak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VibrationSpectralPeakRepository extends JpaRepository<VibrationSpectralPeak, UUID> {

    List<VibrationSpectralPeak> findByBurstIdOrderByAmplitudeMmSDesc(UUID burstId);
}
