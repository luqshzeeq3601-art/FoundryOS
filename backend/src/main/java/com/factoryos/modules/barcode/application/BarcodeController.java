package com.factoryos.modules.barcode.application;

import com.factoryos.common.dto.ApiResponse;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.auth.repository.UserRepository;
import com.factoryos.modules.barcode.dto.BarcodeScanLogDto;
import com.factoryos.modules.barcode.dto.BarcodeScanRequest;
import com.factoryos.modules.barcode.dto.BarcodeScanResponse;
import com.factoryos.modules.barcode.dto.BomItemDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v2/barcode")
public class BarcodeController {

    private final BarcodeScanningService barcodeScanningService;
    private final UserRepository userRepository;

    public BarcodeController(BarcodeScanningService barcodeScanningService, UserRepository userRepository) {
        this.barcodeScanningService = barcodeScanningService;
        this.userRepository = userRepository;
    }

    @PostMapping("/scan")
    public ResponseEntity<ApiResponse<BarcodeScanResponse>> scanBarcode(@Valid @RequestBody BarcodeScanRequest request) {
        UUID userId = null;
        String userName = "Operator";

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            Optional<User> userOpt = userRepository.findByEmailIgnoreCaseAndIsDeletedFalse(auth.getName());
            if (userOpt.isPresent()) {
                userId = userOpt.get().getId();
                userName = userOpt.get().getDisplayName();
            } else {
                userName = auth.getName();
            }
        }

        BarcodeScanResponse response = barcodeScanningService.processScan(request, userId, userName);
        return ResponseEntity.ok(ApiResponse.ok(response, "Barcode processed in " + response.getExecutionLatencyMs() + "ms"));
    }

    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<List<BarcodeScanLogDto>>> getRecentScanLogs() {
        List<BarcodeScanLogDto> logs = barcodeScanningService.getRecentScanLogs();
        return ResponseEntity.ok(ApiResponse.ok(logs, "Recent barcode scan logs retrieved"));
    }

    @GetMapping("/bom/{productCode}")
    public ResponseEntity<ApiResponse<List<BomItemDto>>> getBomForProduct(@PathVariable String productCode) {
        List<BomItemDto> bomItems = barcodeScanningService.getBomForProduct(productCode);
        return ResponseEntity.ok(ApiResponse.ok(bomItems, "Bill of materials retrieved for product " + productCode));
    }
}
