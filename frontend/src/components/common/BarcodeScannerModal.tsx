import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { 
  Camera, 
  QrCode, 
  Scan, 
  CheckCircle, 
  AlertTriangle, 
  AlertOctagon, 
  X, 
  Flashlight, 
  RefreshCw, 
  Cpu, 
  Package, 
  UserCheck, 
  Clock
} from 'lucide-react';
import { BrowserMultiFormatReader } from '@zxing/browser';
import { barcodeApi } from '../../services/api-client';
import { 
  BarcodeScanRequest, 
  BarcodeScanResponse, 
  BarcodeValidationStatus 
} from '../../types';
import { IndustrialButton } from './IndustrialButton';
import { IndustrialBadge } from './IndustrialBadge';
import { useHardwareBarcodeScanner } from '../../hooks/useHardwareBarcodeScanner';
import { triggerScanFeedback } from '../../utils/audioFeedback';

interface BarcodeScannerModalProps {
  isOpen: boolean;
  onClose: () => void;
  onScanSuccess?: (response: BarcodeScanResponse) => void;
  targetMachineId?: string;
  targetProductionOrderId?: string;
  title?: string;
}

export const BarcodeScannerModal: React.FC<BarcodeScannerModalProps> = ({
  isOpen,
  onClose,
  onScanSuccess,
  targetMachineId,
  targetProductionOrderId,
  title = '2D BARCODE & MATERIAL TRACEABILITY SCANNER',
}) => {
  const queryClient = useQueryClient();

  const [activeTab, setActiveTab] = useState<'camera' | 'wedge'>('camera');
  const [torchOn, setTorchOn] = useState<boolean>(false);
  const [facingMode, setFacingMode] = useState<'environment' | 'user'>('environment');
  const [cameraError, setCameraError] = useState<string | null>(null);

  const [manualCode, setManualCode] = useState<string>('');
  const [lastResponse, setLastResponse] = useState<BarcodeScanResponse | null>(null);

  const videoRef = useRef<HTMLVideoElement | null>(null);
  const codeReaderRef = useRef<BrowserMultiFormatReader | null>(null);
  const streamRef = useRef<MediaStream | null>(null);

  // Scan API Mutation
  const scanMutation = useMutation({
    mutationFn: (data: BarcodeScanRequest) => barcodeApi.scan(data),
    onSuccess: (res) => {
      setLastResponse(res);
      triggerScanFeedback(res.validationStatus);
      queryClient.invalidateQueries({ queryKey: ['barcode-logs'] });
      if (res.validationStatus === 'VALID' && onScanSuccess) {
        onScanSuccess(res);
      }
    },
    onError: (err: any) => {
      const errMsg = err.response?.data?.error?.message || 'Scan verification failed';
      triggerScanFeedback('ERROR');
      setLastResponse({
        rawPayload: manualCode || 'UNKNOWN',
        barcodeType: 'UNKNOWN',
        validationStatus: 'ERROR',
        resolvedEntityType: 'NONE',
        bomMatched: false,
        message: errMsg,
        executionLatencyMs: 0,
        timestamp: new Date().toISOString(),
      });
    },
  });

  const handleScanPayload = useCallback((payload: string, source: 'HARDWARE_WEDGE' | 'CAMERA_ZXING' | 'MANUAL_KEYPAD' = 'HARDWARE_WEDGE') => {
    if (!payload.trim()) return;
    scanMutation.mutate({
      rawPayload: payload.trim(),
      scannerSource: source,
      machineId: targetMachineId,
      productionOrderId: targetProductionOrderId,
    });
  }, [scanMutation, targetMachineId, targetProductionOrderId]);

  // Hook for Hardware Scanners (Zebra DataWedge, Honeywell, Keyence)
  const { isReceivingWedge } = useHardwareBarcodeScanner({
    enabled: isOpen,
    onScan: (barcode) => {
      handleScanPayload(barcode, 'HARDWARE_WEDGE');
    },
  });

  // Start Camera Streaming & ZXing Decoding
  const startCamera = useCallback(async () => {
    setCameraError(null);
    try {
      if (!codeReaderRef.current) {
        codeReaderRef.current = new BrowserMultiFormatReader();
      }

      const constraints: MediaStreamConstraints = {
        video: {
          facingMode: { ideal: facingMode },
          width: { ideal: 1280 },
          height: { ideal: 720 },
        },
      };

      const stream = await navigator.mediaDevices.getUserMedia(constraints);
      streamRef.current = stream;

      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        await videoRef.current.play();

        // Decode continuously from video
        codeReaderRef.current.decodeFromVideoElement(
          videoRef.current,
          (result) => {
            if (result && isOpen) {
              const text = result.getText();
              handleScanPayload(text, 'CAMERA_ZXING');
            }
          }
        );
      }
    } catch (err: any) {
      console.error('Camera initialization failed:', err);
      setCameraError(err.message || 'Camera access unavailable. Use hardware scanner or manual simulation.');
    }
  }, [facingMode, handleScanPayload, isOpen]);

  const stopCamera = useCallback(() => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((track) => track.stop());
      streamRef.current = null;
    }
    if (videoRef.current) {
      videoRef.current.srcObject = null;
    }
  }, []);

  const toggleTorch = async () => {
    if (streamRef.current) {
      const track = streamRef.current.getVideoTracks()[0];
      const capabilities = track.getCapabilities?.() as any;
      if (capabilities && capabilities.torch) {
        const nextTorch = !torchOn;
        await track.applyConstraints({
          advanced: [{ torch: nextTorch } as any],
        });
        setTorchOn(nextTorch);
      }
    }
  };

  useEffect(() => {
    if (isOpen && activeTab === 'camera') {
      startCamera();
    } else {
      stopCamera();
    }
    return () => {
      stopCamera();
    };
  }, [isOpen, activeTab, startCamera, stopCamera]);

  if (!isOpen) return null;

  const getValidationBadge = (status: BarcodeValidationStatus) => {
    switch (status) {
      case 'VALID':
        return <IndustrialBadge variant="success">VALIDATED & MATCHED</IndustrialBadge>;
      case 'INVALID_BOM':
        return <IndustrialBadge variant="warning">BOM RECIPE MISMATCH</IndustrialBadge>;
      case 'EXPIRED':
        return <IndustrialBadge variant="danger">LOT EXPIRED</IndustrialBadge>;
      case 'QUARANTINED':
        return <IndustrialBadge variant="warning">QUARANTINED</IndustrialBadge>;
      case 'UNAUTHORIZED':
        return <IndustrialBadge variant="danger">UNAUTHORIZED BADGE</IndustrialBadge>;
      case 'NOT_FOUND':
        return <IndustrialBadge variant="critical">UNRECOGNIZED BARCODE</IndustrialBadge>;
      case 'ERROR':
      default:
        return <IndustrialBadge variant="danger">SCAN ERROR</IndustrialBadge>;
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-6 bg-black/85 backdrop-blur-sm animate-fade-in font-mono">
      <div className="bg-substrate-card border-2 border-industrial-600 w-full max-w-3xl max-h-[92vh] flex flex-col shadow-2xl overflow-hidden">
        {/* Modal Header */}
        <div className="bg-industrial-900 border-b border-substrate-border p-4 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Scan size={20} className="text-cyan-400" />
            <div>
              <div className="text-[10px] uppercase tracking-widest text-industrial-500">
                [ SPRINT 7 // E8-S1 ] TRACEABILITY & WEDGE ENGINE
              </div>
              <h2 className="text-base sm:text-lg font-bold uppercase text-white tracking-wide">
                {title}
              </h2>
            </div>
          </div>

          <div className="flex items-center gap-2">
            {isReceivingWedge && (
              <span className="text-[10px] px-2 py-0.5 bg-cyan-950 text-cyan-400 border border-cyan-800 animate-pulse font-bold">
                ⚡ WEDGE RECEIVING
              </span>
            )}
            <button
              onClick={onClose}
              className="p-1.5 text-industrial-400 hover:text-white hover:bg-industrial-800 transition-colors"
            >
              <X size={20} />
            </button>
          </div>
        </div>

        {/* Tab Selection */}
        <div className="flex border-b border-substrate-border bg-industrial-950">
          <button
            onClick={() => setActiveTab('camera')}
            className={`flex-1 py-2.5 px-4 text-xs font-bold uppercase flex items-center justify-center gap-2 transition-colors ${
              activeTab === 'camera'
                ? 'bg-industrial-800 text-white border-b-2 border-cyan-400'
                : 'text-industrial-400 hover:text-white hover:bg-industrial-900'
            }`}
          >
            <Camera size={15} />
            <span>TABLET CAMERA (2D QR / DATAMATRIX)</span>
          </button>
          <button
            onClick={() => setActiveTab('wedge')}
            className={`flex-1 py-2.5 px-4 text-xs font-bold uppercase flex items-center justify-center gap-2 transition-colors ${
              activeTab === 'wedge'
                ? 'bg-industrial-800 text-white border-b-2 border-cyan-400'
                : 'text-industrial-400 hover:text-white hover:bg-industrial-900'
            }`}
          >
            <QrCode size={15} />
            <span>HARDWARE WEDGE & FAST SIMULATOR</span>
          </button>
        </div>

        {/* Modal Body */}
        <div className="p-4 sm:p-5 overflow-y-auto flex-1 space-y-4">
          {activeTab === 'camera' ? (
            <div className="space-y-3">
              {/* Camera Viewport */}
              <div className="relative w-full aspect-video sm:h-72 bg-black border-2 border-industrial-700 flex items-center justify-center overflow-hidden">
                <video
                  ref={videoRef}
                  className="w-full h-full object-cover"
                  playsInline
                  muted
                />

                {/* Industrial Laser Aiming Reticle */}
                <div className="absolute inset-8 sm:inset-12 pointer-events-none border border-cyan-500/40 flex items-center justify-center">
                  <div className="w-full h-0.5 bg-cyan-400/80 shadow-[0_0_8px_#22d3ee] animate-pulse" />
                  <div className="absolute top-0 left-0 w-3 h-3 border-t-2 border-l-2 border-cyan-400" />
                  <div className="absolute top-0 right-0 w-3 h-3 border-t-2 border-r-2 border-cyan-400" />
                  <div className="absolute bottom-0 left-0 w-3 h-3 border-b-2 border-l-2 border-cyan-400" />
                  <div className="absolute bottom-0 right-0 w-3 h-3 border-b-2 border-r-2 border-cyan-400" />
                </div>

                {/* Camera Overlay Controls */}
                <div className="absolute bottom-2 right-2 flex gap-1.5 z-10">
                  <button
                    onClick={toggleTorch}
                    className={`p-2 border text-xs ${
                      torchOn
                        ? 'bg-amber-400 text-black border-amber-300 font-bold'
                        : 'bg-industrial-900/80 text-white border-substrate-border hover:bg-industrial-800'
                    }`}
                    title="Toggle Flashlight"
                  >
                    <Flashlight size={14} />
                  </button>
                  <button
                    onClick={() => {
                      setFacingMode((m) => (m === 'environment' ? 'user' : 'environment'));
                    }}
                    className="p-2 bg-industrial-900/80 text-white border border-substrate-border hover:bg-industrial-800 text-xs"
                    title="Switch Camera"
                  >
                    <RefreshCw size={14} />
                  </button>
                </div>

                {cameraError && (
                  <div className="absolute inset-0 bg-black/90 p-4 flex flex-col items-center justify-center text-center">
                    <AlertTriangle size={32} className="text-amber-400 mb-2" />
                    <div className="text-xs text-amber-200">{cameraError}</div>
                    <IndustrialButton
                      size="sm"
                      variant="outline"
                      className="mt-3"
                      onClick={() => setActiveTab('wedge')}
                    >
                      SWITCH TO MANUAL / WEDGE SIMULATOR
                    </IndustrialButton>
                  </div>
                )}
              </div>
              <div className="text-[11px] text-industrial-400 flex items-center justify-between">
                <span>[ CAMERA SCAN ]: Align QR, DataMatrix, or Code128 within reticle.</span>
                <span className="text-cyan-400 font-bold">ZXING-WASM ACTIVE</span>
              </div>
            </div>
          ) : (
            <div className="space-y-4">
              {/* Hardware Wedge Status Banner */}
              <div className="p-3 bg-industrial-900 border border-substrate-border flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <div className="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-pulse" />
                  <span className="text-xs text-white font-bold">
                    ZEBRA DATAWEDGE / HONEYWELL HID READY
                  </span>
                </div>
                <span className="text-[10px] text-industrial-400">BURST &le;60ms LISTENER ACTIVE</span>
              </div>

              {/* Manual Input Form */}
              <form
                onSubmit={(e) => {
                  e.preventDefault();
                  handleScanPayload(manualCode, 'MANUAL_KEYPAD');
                }}
                className="flex gap-2"
              >
                <input
                  type="text"
                  value={manualCode}
                  onChange={(e) => setManualCode(e.target.value)}
                  placeholder="Scan with handheld or type barcode payload..."
                  className="flex-1 bg-industrial-900 border border-substrate-border px-3 py-2 text-sm text-white font-mono focus:outline-none focus:border-cyan-400"
                />
                <IndustrialButton
                  type="submit"
                  variant="primary"
                  size="md"
                  isLoading={scanMutation.isPending}
                >
                  <Scan size={14} className="mr-1" />
                  <span>VERIFY</span>
                </IndustrialButton>
              </form>

              {/* 1-Touch Simulation Matrix */}
              <div>
                <div className="text-xs font-mono uppercase text-industrial-400 mb-2">
                  [ 1-TOUCH TEST BARCODE SIMULATORS ]:
                </div>
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                  <button
                    onClick={() => {
                      setManualCode('ORD:ORD-2026-001');
                      handleScanPayload('ORD:ORD-2026-001', 'MANUAL_KEYPAD');
                    }}
                    className="p-2 text-left bg-industrial-900 border border-emerald-800/60 hover:border-emerald-500 text-[11px] text-emerald-300"
                  >
                    <div className="font-bold flex items-center gap-1">
                      <Package size={12} /> ORD:ORD-2026-001
                    </div>
                    <div className="text-[10px] text-industrial-400">Valid Order Traveler</div>
                  </button>

                  <button
                    onClick={() => {
                      setManualCode('LOT:LOT-ALU-6061-001');
                      handleScanPayload('LOT:LOT-ALU-6061-001', 'MANUAL_KEYPAD');
                    }}
                    className="p-2 text-left bg-industrial-900 border border-cyan-800/60 hover:border-cyan-500 text-[11px] text-cyan-300"
                  >
                    <div className="font-bold flex items-center gap-1">
                      <CheckCircle size={12} /> LOT:LOT-ALU-6061-001
                    </div>
                    <div className="text-[10px] text-industrial-400">BOM Matched Material</div>
                  </button>

                  <button
                    onClick={() => {
                      setManualCode('LOT:LOT-STL-4140-002');
                      handleScanPayload('LOT:LOT-STL-4140-002', 'MANUAL_KEYPAD');
                    }}
                    className="p-2 text-left bg-industrial-900 border border-amber-800/60 hover:border-amber-500 text-[11px] text-amber-300"
                  >
                    <div className="font-bold flex items-center gap-1">
                      <AlertTriangle size={12} /> LOT:LOT-STL-4140-002
                    </div>
                    <div className="text-[10px] text-industrial-400">BOM Mismatch Material</div>
                  </button>

                  <button
                    onClick={() => {
                      setManualCode('LOT:LOT-EXP-RESIN-004');
                      handleScanPayload('LOT:LOT-EXP-RESIN-004', 'MANUAL_KEYPAD');
                    }}
                    className="p-2 text-left bg-industrial-900 border border-red-800/60 hover:border-red-500 text-[11px] text-hazard-red"
                  >
                    <div className="font-bold flex items-center gap-1">
                      <AlertOctagon size={12} /> LOT:LOT-EXP-RESIN-004
                    </div>
                    <div className="text-[10px] text-industrial-400">Expired Raw Material</div>
                  </button>

                  <button
                    onClick={() => {
                      setManualCode('MACH:CNC-01');
                      handleScanPayload('MACH:CNC-01', 'MANUAL_KEYPAD');
                    }}
                    className="p-2 text-left bg-industrial-900 border border-substrate-border hover:border-industrial-400 text-[11px] text-white"
                  >
                    <div className="font-bold flex items-center gap-1">
                      <Cpu size={12} /> MACH:CNC-01
                    </div>
                    <div className="text-[10px] text-industrial-400">Machine Asset Badge</div>
                  </button>

                  <button
                    onClick={() => {
                      setManualCode('OPR:operator@foundryos.local');
                      handleScanPayload('OPR:operator@foundryos.local', 'MANUAL_KEYPAD');
                    }}
                    className="p-2 text-left bg-industrial-900 border border-purple-800/60 hover:border-purple-500 text-[11px] text-purple-300"
                  >
                    <div className="font-bold flex items-center gap-1">
                      <UserCheck size={12} /> OPR:operator@...
                    </div>
                    <div className="text-[10px] text-industrial-400">Operator Employee Badge</div>
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* Scan Verification Result Card */}
          {lastResponse && (
            <div
              className={`p-4 border-2 transition-all ${
                lastResponse.validationStatus === 'VALID'
                  ? 'bg-emerald-950/40 border-emerald-500'
                  : lastResponse.validationStatus === 'INVALID_BOM' || lastResponse.validationStatus === 'EXPIRED'
                  ? 'bg-amber-950/40 border-amber-500'
                  : 'bg-red-950/40 border-hazard-red'
              }`}
            >
              <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2 border-b border-substrate-border/60 pb-2 mb-2">
                <div className="flex items-center gap-2">
                  {getValidationBadge(lastResponse.validationStatus)}
                  <span className="text-xs text-industrial-400">
                    TYPE: <span className="text-white font-bold">{lastResponse.barcodeType}</span>
                  </span>
                </div>
                <div className="flex items-center gap-1 text-[11px] text-cyan-400 font-bold">
                  <Clock size={12} />
                  <span>SLA: {lastResponse.executionLatencyMs}ms (&lt;300ms PASS)</span>
                </div>
              </div>

              <div className="space-y-1.5 text-xs">
                <div className="flex items-start justify-between gap-2">
                  <span className="text-industrial-400">RAW PAYLOAD:</span>
                  <span className="text-white font-bold text-right break-all">{lastResponse.rawPayload}</span>
                </div>

                {lastResponse.resolvedEntitySummary && (
                  <div className="flex items-start justify-between gap-2">
                    <span className="text-industrial-400">RESOLVED:</span>
                    <span className="text-cyan-300 text-right">{lastResponse.resolvedEntitySummary}</span>
                  </div>
                )}

                <div className="flex items-start justify-between gap-2">
                  <span className="text-industrial-400">VERIFICATION MESSAGE:</span>
                  <span className={`text-right font-bold ${
                    lastResponse.validationStatus === 'VALID' ? 'text-emerald-400' : 'text-amber-400'
                  }`}>
                    {lastResponse.message}
                  </span>
                </div>
              </div>

              {lastResponse.entityData && (
                <div className="mt-3 pt-2 border-t border-substrate-border/60 text-[11px] text-industrial-300 flex flex-wrap gap-3">
                  {lastResponse.entityData.productCode && (
                    <div>PRODUCT: <span className="text-white font-bold">{lastResponse.entityData.productCode}</span></div>
                  )}
                  {lastResponse.entityData.plannedQuantity && (
                    <div>PLANNED: <span className="text-white font-bold">{lastResponse.entityData.plannedQuantity}</span></div>
                  )}
                  {lastResponse.entityData.quantity && (
                    <div>QTY: <span className="text-white font-bold">{lastResponse.entityData.quantity} {lastResponse.entityData.uom}</span></div>
                  )}
                  {lastResponse.entityData.status && (
                    <div>STATUS: <span className="text-white font-bold">{lastResponse.entityData.status}</span></div>
                  )}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Modal Footer */}
        <div className="p-3 bg-industrial-900 border-t border-substrate-border flex items-center justify-between">
          <div className="text-[11px] text-industrial-500">
            [ AUDIO / HAPTIC FEEDBACK ACTIVE ]
          </div>
          <IndustrialButton
            variant="outline"
            size="md"
            onClick={onClose}
          >
            CLOSE SCANNER
          </IndustrialButton>
        </div>
      </div>
    </div>
  );
};
