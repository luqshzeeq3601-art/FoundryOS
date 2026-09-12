package com.factoryos.modules.vibration.application;

import com.factoryos.modules.vibration.domain.IsoSeverityZone;
import com.factoryos.modules.vibration.domain.MachineVibrationClass;
import com.factoryos.modules.vibration.dto.IsoSeverityResultDto;
import org.springframework.stereotype.Service;

@Service
public class Iso10816StandardsEngine {

    /**
     * Evaluates measured overall vibration velocity RMS against ISO 10816-3 severity zones.
     *
     * @param rmsVelocityMmS overall vibration velocity RMS in mm/s (integrated over 10 Hz - 1000 Hz)
     * @param vibrationClass machine power & foundation mount class (Class I, II, III, IV)
     * @return ISO severity classification result
     */
    public IsoSeverityResultDto evaluateSeverity(double rmsVelocityMmS, MachineVibrationClass vibrationClass) {
        if (vibrationClass == null) {
            vibrationClass = MachineVibrationClass.CLASS_II_MEDIUM;
        }

        IsoSeverityZone zone = vibrationClass.evaluateZone(rmsVelocityMmS);
        return new IsoSeverityResultDto(zone, vibrationClass, rmsVelocityMmS);
    }
}
