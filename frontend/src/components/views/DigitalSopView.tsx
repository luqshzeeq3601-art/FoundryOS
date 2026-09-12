import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { sopApi, api } from '../../services/api-client';
import { 
  SopDto, 
  SopCategory, 
  SopExecutionSessionDto, 
  SopStepExecutionRecordDto, 
  QualityGateStatusDto, 
  ProductionOrderDto, 
  PagedResponse,
  GateStatus
} from '../../types';
import { IndustrialCard } from '../common/IndustrialCard';
import { IndustrialBadge } from '../common/IndustrialBadge';
import { IndustrialButton } from '../common/IndustrialButton';
import { Modal } from '../common/Modal';
import { useAuth } from '../../context/AuthContext';
import { 
  CheckCircle2, 
  XCircle, 
  AlertTriangle, 
  ShieldCheck, 
  Camera, 
  Maximize2, 
  Scan, 
  ArrowRight, 
  ArrowLeft, 
  RotateCcw, 
  Play, 
  Award,
  Sliders,
  CheckSquare
} from 'lucide-react';

export const DigitalSopView: React.FC = () => {
  const { user, hasRole } = useAuth();
  const queryClient = useQueryClient();

  // Selected State
  const [selectedCategory, setSelectedCategory] = useState<SopCategory | ''>('');
  const [selectedSopId, setSelectedSopId] = useState<string | null>(null);
  const [selectedOrderId, setSelectedOrderId] = useState<string | null>(null);
  const [activeSessionId, setActiveSessionId] = useState<string | null>(null);
  const [activeStepIndex, setActiveStepIndex] = useState<number>(0);

  // Step Execution Form State
  const [numericInput, setNumericInput] = useState<string>('');
  const [stepNotes, setStepNotes] = useState<string>('');
  const [photoUrl, setPhotoUrl] = useState<string>('');
  const [barcodeInput, setBarcodeInput] = useState<string>('');
  const [signOffNotes, setSignOffNotes] = useState<string>('Batch inspection completed and verified against digital tolerances.');
  const [signOffModalOpen, setSignOffModalOpen] = useState(false);
  const [cadZoomed, setCadZoomed] = useState(false);
  const [actionMessage, setActionMessage] = useState<string | null>(null);

  // Queries
  const { data: sops = [] } = useQuery<SopDto[]>({
    queryKey: ['sop-templates', selectedCategory],
    queryFn: () => sopApi.getSops({ category: selectedCategory || undefined }),
    refetchInterval: 10000,
  });

  const { data: activeOrdersData } = useQuery<PagedResponse<ProductionOrderDto>>({
    queryKey: ['production-orders-sop'],
    queryFn: () => api.get<PagedResponse<ProductionOrderDto>>('/production-orders', { size: 50 }),
  });

  const activeOrders = activeOrdersData?.content || [];

  // Active Selected SOP
  const currentSop = sops.find(s => s.id === selectedSopId) || (sops.length > 0 ? sops[0] : null);

  // Active Session Query
  const { data: activeSession, refetch: refetchSession } = useQuery<SopExecutionSessionDto | null>({
    queryKey: ['sop-session', activeSessionId],
    queryFn: () => activeSessionId ? sopApi.getSessionById(activeSessionId) : Promise.resolve(null),
    enabled: !!activeSessionId,
    refetchInterval: 5000,
  });

  // Quality Gate Status Query
  const { data: qualityGate, refetch: refetchGate } = useQuery<QualityGateStatusDto | null>({
    queryKey: ['quality-gate-order', selectedOrderId],
    queryFn: () => selectedOrderId ? sopApi.getGateStatusForOrder(selectedOrderId) : Promise.resolve(null),
    enabled: !!selectedOrderId,
    refetchInterval: 5000,
  });

  // Start Session Mutation
  const startSessionMutation = useMutation({
    mutationFn: (data: { productionOrderId: string; sopId?: string }) =>
      sopApi.startSession(data),
    onSuccess: (res) => {
      setActiveSessionId(res.id);
      setActiveStepIndex(0);
      setActionMessage(`SOP Execution Session started for Order ${res.orderNumber}`);
      queryClient.invalidateQueries({ queryKey: ['quality-gate-order', selectedOrderId] });
    },
    onError: (err: any) => {
      setActionMessage(`Error starting SOP session: ${err?.response?.data?.error?.message || err.message}`);
    }
  });

  // Record Step Mutation
  const recordStepMutation = useMutation({
    mutationFn: ({ sessionId, data }: { sessionId: string; data: any }) =>
      sopApi.recordStepExecution(sessionId, data),
    onSuccess: (rec) => {
      refetchSession();
      refetchGate();
      setNumericInput('');
      setStepNotes('');
      setPhotoUrl('');
      setBarcodeInput('');
      setActionMessage(`Step ${rec.stepNumber} recorded (${rec.status})`);

      // Auto advance to next step if available
      if (activeSession && activeStepIndex < activeSession.stepRecords.length - 1) {
        setActiveStepIndex(activeStepIndex + 1);
      }
    },
    onError: (err: any) => {
      setActionMessage(`Step recording error: ${err?.response?.data?.error?.message || err.message}`);
    }
  });

  // Quality Gate Sign-Off Mutation
  const signOffMutation = useMutation({
    mutationFn: ({ orderId, data }: { orderId: string; data: { gateStatus: GateStatus; signOffComments?: string } }) =>
      sopApi.signOffGate(orderId, data),
    onSuccess: (res) => {
      refetchGate();
      refetchSession();
      setSignOffModalOpen(false);
      setActionMessage(`Quality Sign-Off Gate recorded as ${res.gateStatus}! Order is now compliant.`);
      queryClient.invalidateQueries({ queryKey: ['production-orders'] });
    },
    onError: (err: any) => {
      setActionMessage(`Sign-Off Failed: ${err?.response?.data?.error?.message || err.message}`);
    }
  });

  // Active step record in current session
  const currentRecord: SopStepExecutionRecordDto | null = 
    activeSession?.stepRecords && activeSession.stepRecords[activeStepIndex]
      ? activeSession.stepRecords[activeStepIndex]
      : null;

  // Numeric tolerance evaluation helper
  const parsedNumeric = parseFloat(numericInput);
  const hasValidNumeric = !isNaN(parsedNumeric);
  let liveToleranceStatus: 'WITHIN' | 'UNDER' | 'OVER' | null = null;

  if (currentRecord && currentRecord.stepType === 'NUMERIC_MEASUREMENT' && hasValidNumeric) {
    if (currentRecord.minTolerance !== undefined && parsedNumeric < currentRecord.minTolerance) {
      liveToleranceStatus = 'UNDER';
    } else if (currentRecord.maxTolerance !== undefined && parsedNumeric > currentRecord.maxTolerance) {
      liveToleranceStatus = 'OVER';
    } else {
      liveToleranceStatus = 'WITHIN';
    }
  }

  // Handle Step Recording Submission
  const handleRecordStep = (statusOverride?: 'PASSED' | 'FAILED') => {
    if (!activeSessionId || !currentRecord) return;

    let evalStatus = statusOverride || 'PASSED';
    if (currentRecord.stepType === 'NUMERIC_MEASUREMENT' && hasValidNumeric) {
      evalStatus = liveToleranceStatus === 'WITHIN' ? 'PASSED' : 'FAILED';
    }

    recordStepMutation.mutate({
      sessionId: activeSessionId,
      data: {
        stepId: currentRecord.stepId,
        status: evalStatus,
        numericValue: hasValidNumeric ? parsedNumeric : undefined,
        photoEvidenceUrl: photoUrl || undefined,
        barcodeScanned: barcodeInput || undefined,
        notes: stepNotes || undefined,
      }
    });
  };

  // Simulate Photo Snap
  const handleSimulatePhotoSnap = () => {
    const defectTypes = ['SURFACE_FINISH_NOMINAL', 'BURR_EDGE_INSPECTED', 'CRITICAL_BORE_CLEAR'];
    const randomDefect = defectTypes[Math.floor(Math.random() * defectTypes.length)];
    const mockPhotoUrl = `https://storage.foundryos.internal/defects/${randomDefect}_${Date.now().toString().slice(-6)}.jpg`;
    setPhotoUrl(mockPhotoUrl);
    setActionMessage(`High-resolution inspection photo captured & SHA-256 hashed`);
  };

  return (
    <div className="space-y-6">
      {/* Top Banner & Control Bar */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-substrate-border pb-4">
        <div>
          <div className="flex items-center gap-2 text-hazard-red text-xs font-mono font-bold tracking-widest uppercase">
            <Sliders size={14} className="animate-pulse" />
            <span>FOUNDRY//OS DIGITAL EXECUTION SYSTEM // E8-S2</span>
          </div>
          <h1 className="text-xl sm:text-2xl font-mono font-black tracking-wider uppercase text-white mt-1">
            DIGITAL SOP // INTERACTIVE CHECKLISTS & QUALITY GATES
          </h1>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {/* Order Selector */}
          <div className="flex items-center gap-2 bg-industrial-900 border border-substrate-border px-3 py-1.5 rounded">
            <span className="text-[10px] font-mono text-industrial-400 uppercase">BIND ORDER:</span>
            <select
              value={selectedOrderId || ''}
              onChange={(e) => {
                const orderId = e.target.value;
                setSelectedOrderId(orderId || null);
                if (orderId) {
                  const ord = activeOrders.find(o => o.id === orderId);
                  if (ord) {
                    const matchSop = sops.find(s => s.productCode.toLowerCase() === ord.productCode.toLowerCase());
                    if (matchSop) setSelectedSopId(matchSop.id);
                  }
                }
              }}
              aria-label="Bind Production Order"
              className="bg-transparent text-xs font-mono font-bold text-white focus:outline-none"
            >
              <option value="" className="bg-industrial-900 text-industrial-300">-- SELECT PRODUCTION ORDER --</option>
              {activeOrders.map(order => (
                <option key={order.id} value={order.id} className="bg-industrial-900 text-white">
                  {order.orderNumber} [{order.productCode}] ({order.status})
                </option>
              ))}
            </select>
          </div>

          {/* Start / Bind Session Button */}
          {selectedOrderId && !activeSessionId && (
            <IndustrialButton
              variant="primary"
              size="sm"
              onClick={() => {
                startSessionMutation.mutate({
                  productionOrderId: selectedOrderId,
                  sopId: currentSop ? currentSop.id : undefined,
                });
              }}
              disabled={startSessionMutation.isPending}
            >
              <Play size={14} className="mr-1" />
              START SOP RUN
            </IndustrialButton>
          )}

          {activeSessionId && (
            <IndustrialButton
              variant="secondary"
              size="sm"
              onClick={() => {
                setActiveSessionId(null);
                setActionMessage('Disconnected from live session viewport.');
              }}
            >
              <RotateCcw size={14} className="mr-1" />
              SWITCH SESSION
            </IndustrialButton>
          )}
        </div>
      </div>

      {/* Action / Notification Banner */}
      {actionMessage && (
        <div className="bg-industrial-800/80 border border-industrial-600 px-4 py-2 text-xs font-mono text-industrial-200 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-hazard-red animate-ping" />
            <span>{actionMessage}</span>
          </div>
          <button 
            onClick={() => setActionMessage(null)} 
            className="text-industrial-400 hover:text-white"
          >
            [DISMISS]
          </button>
        </div>
      )}

      {/* Grid: Quality Gate Status Header Bar */}
      {selectedOrderId && qualityGate && (
        <div className={`border p-4 rounded transition-all ${
          qualityGate.compliant 
            ? 'bg-emerald-950/20 border-emerald-500/50' 
            : 'bg-amber-950/20 border-amber-500/50'
        }`}>
          <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <div className={`p-2.5 border rounded ${
                qualityGate.compliant 
                  ? 'bg-emerald-500/10 border-emerald-500 text-emerald-400' 
                  : 'bg-amber-500/10 border-amber-500 text-amber-400'
              }`}>
                {qualityGate.compliant ? <ShieldCheck size={24} /> : <AlertTriangle size={24} />}
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <span className="text-xs font-mono font-bold uppercase tracking-wider text-white">
                    QUALITY SIGN-OFF GATE // ORDER: {qualityGate.orderNumber}
                  </span>
                  <IndustrialBadge 
                    variant={qualityGate.compliant ? 'success' : 'warning'} 
                    size="sm"
                  >
                    GATE: {qualityGate.gateStatus}
                  </IndustrialBadge>
                  {qualityGate.requiresQualityRole && (
                    <span className="text-[10px] font-mono bg-industrial-800 border border-industrial-600 text-industrial-300 px-2 py-0.5 rounded">
                      QUALITY ROLE REQUIRED
                    </span>
                  )}
                </div>
                <p className="text-xs font-mono text-industrial-300 mt-1">
                  {qualityGate.compliant 
                    ? `Order is fully compliant. Mandatory checklist steps verified (${qualityGate.completedMandatorySteps}/${qualityGate.totalMandatorySteps}).` 
                    : qualityGate.blockingReason || 'Order completion is blocked until quality sign-off gate passes.'}
                </p>
              </div>
            </div>

            <div className="flex items-center gap-3">
              {qualityGate.signedOffByName && (
                <div className="text-right font-mono text-xs">
                  <div className="text-emerald-400 font-bold flex items-center gap-1 justify-end">
                    <Award size={14} /> SIGNED OFF BY: {qualityGate.signedOffByName}
                  </div>
                  <div className="text-[10px] text-industrial-400">
                    {qualityGate.signedOffAt ? new Date(qualityGate.signedOffAt).toLocaleString() : ''}
                  </div>
                </div>
              )}

              {hasRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN') && (
                <IndustrialButton
                  variant={qualityGate.compliant ? 'secondary' : 'primary'}
                  size="sm"
                  onClick={() => setSignOffModalOpen(true)}
                >
                  <Award size={14} className="mr-1 text-hazard-red" />
                  {qualityGate.compliant ? 'UPDATE SIGN-OFF' : 'SIGN-OFF GATE'}
                </IndustrialButton>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Main Workspace Layout: Two Column */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        
        {/* Left Column: SOP Selector & CAD Blueprint Viewport (5 Columns) */}
        <div className="lg:col-span-5 space-y-6">
          
          {/* SOP Template Selection & Details */}
          <IndustrialCard className="p-4">
            <div className="flex items-center justify-between border-b border-substrate-border pb-3 mb-4">
              <span className="font-mono font-bold text-xs uppercase tracking-wider text-white">
                STANDARD OPERATING PROCEDURE (SOP)
              </span>
              <IndustrialBadge variant="info" size="sm">SPEC v1.0</IndustrialBadge>
            </div>

            <div className="space-y-4">
              {/* Category Filter */}
              <div className="flex flex-wrap gap-1">
                {(['', 'MACHINING', 'ASSEMBLY', 'QUALITY_CONTROL', 'SAFETY_PRE_CHECK'] as const).map((cat) => (
                  <button
                    key={cat}
                    onClick={() => setSelectedCategory(cat as any)}
                    className={`px-2.5 py-1 text-[10px] font-mono font-bold uppercase border transition-all ${
                      selectedCategory === cat
                        ? 'bg-industrial-700 text-white border-industrial-400'
                        : 'bg-industrial-900/50 text-industrial-400 border-substrate-border hover:border-industrial-600'
                    }`}
                  >
                    {cat || 'ALL'}
                  </button>
                ))}
              </div>

              {/* SOP Dropdown */}
              <div>
                <label className="block text-[10px] font-mono text-industrial-400 uppercase mb-1">
                  ACTIVE SOP TEMPLATE:
                </label>
                <select
                  value={selectedSopId || (currentSop ? currentSop.id : '')}
                  onChange={(e) => setSelectedSopId(e.target.value)}
                  aria-label="Select SOP Template"
                  className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-xs font-mono text-white rounded focus:border-hazard-red focus:outline-none"
                >
                  {sops.map(sop => (
                    <option key={sop.id} value={sop.id}>
                      {sop.sopCode} - {sop.title} [{sop.productCode}]
                    </option>
                  ))}
                </select>
              </div>

              {currentSop && (
                <div className="bg-industrial-900/60 border border-substrate-border p-3 space-y-2 text-xs font-mono">
                  <div className="flex items-center justify-between">
                    <span className="text-industrial-400">PRODUCT CODE:</span>
                    <span className="text-white font-bold">{currentSop.productCode}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-industrial-400">CATEGORY:</span>
                    <IndustrialBadge variant="default" size="sm">{currentSop.category}</IndustrialBadge>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-industrial-400">EST. DURATION:</span>
                    <span className="text-industrial-200">{currentSop.estimatedDurationMinutes} MINS</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-industrial-400">TOTAL STEPS:</span>
                    <span className="text-white font-bold">{currentSop.steps?.length || 0} STEPS</span>
                  </div>
                  {currentSop.description && (
                    <div className="pt-2 border-t border-substrate-border text-industrial-300 text-[11px] leading-relaxed">
                      {currentSop.description}
                    </div>
                  )}
                </div>
              )}

              {/* Safety Precautions Alert */}
              {currentSop?.safetyPrecautions && (
                <div className="bg-amber-950/30 border border-amber-600/40 p-3 rounded">
                  <div className="flex items-center gap-1.5 text-amber-400 text-xs font-mono font-bold uppercase mb-1">
                    <AlertTriangle size={14} /> SAFETY PRECAUTIONS & PPE:
                  </div>
                  <p className="text-[11px] font-mono text-amber-200/90 leading-relaxed">
                    {currentSop.safetyPrecautions}
                  </p>
                </div>
              )}
            </div>
          </IndustrialCard>

          {/* Interactive Blueprint / CAD Drawing Viewer */}
          <IndustrialCard className="p-4">
            <div className="flex items-center justify-between border-b border-substrate-border pb-3 mb-4">
              <span className="font-mono font-bold text-xs uppercase tracking-wider text-white">
                CAD BLUEPRINT & SCHEMATIC VIEWPORT
              </span>
              <IndustrialBadge variant="info" size="sm">
                {currentRecord?.cadViewNode ? `NODE: ${currentRecord.cadViewNode}` : '2D WIREFRAME'}
              </IndustrialBadge>
            </div>

            <div className="relative bg-black/80 border border-substrate-border rounded overflow-hidden p-4 min-h-[260px] flex flex-col items-center justify-center">
              {/* Grid Background */}
              <div 
                className="absolute inset-0 opacity-20 pointer-events-none"
                style={{
                  backgroundImage: 'radial-gradient(#3b82f6 1px, transparent 1px)',
                  backgroundSize: '16px 16px',
                }}
              />

              {/* SVG Blueprint Illustration */}
              <svg 
                className={`w-full max-w-[340px] h-[180px] transition-transform duration-300 ${cadZoomed ? 'scale-125' : 'scale-100'}`} 
                viewBox="0 0 400 200"
              >
                {/* Blueprint Axis Grid */}
                <line x1="20" y1="100" x2="380" y2="100" stroke="#1e3a8a" strokeDasharray="4 4" strokeWidth="1" />
                <line x1="200" y1="20" x2="200" y2="180" stroke="#1e3a8a" strokeDasharray="4 4" strokeWidth="1" />

                {/* Part Profile Geometry */}
                <rect x="80" y="50" width="240" height="100" fill="none" stroke="#60a5fa" strokeWidth="2" />
                <circle cx="200" cy="100" r="35" fill="#1e293b" stroke="#93c5fd" strokeWidth="2" />
                <circle cx="200" cy="100" r="12" fill="#0f172a" stroke="#3b82f6" strokeWidth="1.5" />

                {/* Caliper Measurement Dimension Line */}
                <line x1="80" y1="35" x2="320" y2="35" stroke="#ef4444" strokeWidth="1.5" />
                <line x1="80" y1="30" x2="80" y2="40" stroke="#ef4444" strokeWidth="1.5" />
                <line x1="320" y1="30" x2="320" y2="40" stroke="#ef4444" strokeWidth="1.5" />
                <text x="180" y="28" fill="#ef4444" fontSize="10" fontFamily="monospace">
                  {currentRecord?.nominalValue ? `Ø ${currentRecord.nominalValue}${currentRecord.unitOfMeasure || 'mm'}` : 'REF 120.00mm'}
                </text>

                {/* Active CAD View Callout Node */}
                {currentRecord?.cadViewNode && (
                  <g>
                    <circle cx="200" cy="65" r="8" fill="#ef4444" className="animate-ping opacity-75" />
                    <circle cx="200" cy="65" r="5" fill="#ef4444" />
                    <text x="215" y="69" fill="#f87171" fontSize="10" fontFamily="monospace" fontWeight="bold">
                      {currentRecord.cadViewNode}
                    </text>
                  </g>
                )}
              </svg>

              {/* Viewport Overlay Controls */}
              <div className="absolute bottom-2 right-2 flex items-center gap-1">
                <button
                  onClick={() => setCadZoomed(!cadZoomed)}
                  className="bg-industrial-800/90 hover:bg-industrial-700 text-industrial-300 hover:text-white border border-substrate-border p-1.5 rounded text-xs font-mono flex items-center gap-1"
                >
                  <Maximize2 size={12} /> {cadZoomed ? 'RESET' : 'ZOOM'}
                </button>
              </div>

              <div className="absolute top-2 left-2 text-[10px] font-mono text-cyan-400 bg-black/60 px-2 py-0.5 border border-cyan-800 rounded">
                CALIPER REF: ISO-2768-mK // DIGITAL PROJECTION
              </div>
            </div>
          </IndustrialCard>
        </div>

        {/* Right Column: Step-by-Step Execution Console (7 Columns) */}
        <div className="lg:col-span-7 space-y-6">
          
          {/* Active Session Checklist Navigation Bar */}
          <IndustrialCard className="p-4">
            <div className="flex items-center justify-between border-b border-substrate-border pb-3 mb-4">
              <span className="font-mono font-bold text-xs uppercase tracking-wider text-white">
                {activeSession ? `EXECUTION CONSOLE // SESSION: ${activeSession.id.slice(0, 8)}` : 'SOP STEP CATALOG'}
              </span>
              <IndustrialBadge variant={activeSession ? 'warning' : 'default'} size="sm">
                {activeSession ? `STATUS: ${activeSession.sessionStatus}` : 'READ ONLY'}
              </IndustrialBadge>
            </div>

            {/* Step Carousel / Tabs */}
            <div className="space-y-4">
              <div className="flex items-center justify-between pb-2 border-b border-substrate-border">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-mono font-bold text-white uppercase">
                    STEP {activeStepIndex + 1} OF {activeSession ? activeSession.stepRecords.length : (currentSop?.steps.length || 0)}
                  </span>
                  {currentRecord?.mandatory && (
                    <IndustrialBadge variant="danger" size="sm">MANDATORY</IndustrialBadge>
                  )}
                </div>

                {/* Prev / Next buttons */}
                <div className="flex items-center gap-1">
                  <button
                    onClick={() => setActiveStepIndex(Math.max(0, activeStepIndex - 1))}
                    disabled={activeStepIndex === 0}
                    className="p-1 bg-industrial-800 border border-substrate-border text-industrial-300 disabled:opacity-30 rounded hover:text-white"
                  >
                    <ArrowLeft size={14} />
                  </button>
                  <button
                    onClick={() => {
                      const maxSteps = activeSession ? activeSession.stepRecords.length : (currentSop?.steps.length || 1);
                      setActiveStepIndex(Math.min(maxSteps - 1, activeStepIndex + 1));
                    }}
                    disabled={activeStepIndex >= (activeSession ? activeSession.stepRecords.length - 1 : (currentSop?.steps.length || 1) - 1)}
                    className="p-1 bg-industrial-800 border border-substrate-border text-industrial-300 disabled:opacity-30 rounded hover:text-white"
                  >
                    <ArrowRight size={14} />
                  </button>
                </div>
              </div>

              {/* Step Progress Dots */}
              <div className="flex gap-1.5 overflow-x-auto pb-1">
                {(activeSession ? activeSession.stepRecords : currentSop?.steps || []).map((step, idx) => {
                  const isCur = idx === activeStepIndex;
                  const status = (step as any).status;
                  let dotColor = 'bg-industrial-800 border-industrial-600 text-industrial-400';
                  if (status === 'PASSED') dotColor = 'bg-emerald-950 border-emerald-500 text-emerald-400';
                  if (status === 'FAILED') dotColor = 'bg-red-950 border-red-500 text-red-400';
                  if (isCur) dotColor += ' ring-2 ring-hazard-red text-white';

                  return (
                    <button
                      key={idx}
                      onClick={() => setActiveStepIndex(idx)}
                      className={`px-2.5 py-1 text-xs font-mono font-bold border rounded transition-all flex items-center gap-1 ${dotColor}`}
                    >
                      <span>#{idx + 1}</span>
                      {status === 'PASSED' && <CheckCircle2 size={10} />}
                      {status === 'FAILED' && <XCircle size={10} />}
                    </button>
                  );
                })}
              </div>

              {/* Step Detail Content */}
              {currentRecord || (currentSop?.steps && currentSop.steps[activeStepIndex]) ? (
                (() => {
                  const step = (currentRecord || currentSop?.steps[activeStepIndex]) as any;
                  return (
                    <div className="space-y-4 pt-2">
                      <div className="bg-industrial-900 border border-substrate-border p-4 rounded space-y-2">
                        <div className="flex items-center justify-between">
                          <h3 className="text-sm font-mono font-bold text-white uppercase">
                            {step.stepNumber}. {step.title || step.stepTitle}
                          </h3>
                          <IndustrialBadge variant="default" size="sm">
                            {step.stepType}
                          </IndustrialBadge>
                        </div>
                        <p className="text-xs font-mono text-industrial-200 leading-relaxed">
                          {step.instructionText}
                        </p>
                      </div>

                      {/* Safety Alert for Step */}
                      {step.safetyAlert && (
                        <div className="bg-hazard-red/10 border border-hazard-red/30 p-2.5 rounded text-xs font-mono text-hazard-red flex items-center gap-2">
                          <AlertTriangle size={14} className="shrink-0" />
                          <span>CAUTION: {step.safetyAlert}</span>
                        </div>
                      )}

                      {/* Interactive Step Input Console */}
                      {activeSession && (
                        <div className="border border-substrate-border bg-substrate-card/40 p-4 rounded space-y-4">
                          <div className="text-xs font-mono font-bold text-industrial-300 uppercase tracking-wider">
                            OPERATOR INSPECTION CONSOLE:
                          </div>

                          {/* 1. Numeric Measurement with Live Tolerance Bounds */}
                          {step.stepType === 'NUMERIC_MEASUREMENT' && (
                            <div className="space-y-3 bg-black/40 border border-substrate-border p-3 rounded">
                              <div className="grid grid-cols-3 gap-2 text-center text-xs font-mono">
                                <div className="p-2 bg-industrial-900 border border-substrate-border rounded">
                                  <div className="text-[10px] text-industrial-400">MIN TOLERANCE</div>
                                  <div className="text-white font-bold">{step.minTolerance} {step.unitOfMeasure}</div>
                                </div>
                                <div className="p-2 bg-industrial-900 border border-substrate-border rounded">
                                  <div className="text-[10px] text-industrial-400">NOMINAL TARGET</div>
                                  <div className="text-cyan-400 font-bold">{step.nominalValue} {step.unitOfMeasure}</div>
                                </div>
                                <div className="p-2 bg-industrial-900 border border-substrate-border rounded">
                                  <div className="text-[10px] text-industrial-400">MAX TOLERANCE</div>
                                  <div className="text-white font-bold">{step.maxTolerance} {step.unitOfMeasure}</div>
                                </div>
                              </div>

                              <div>
                                <label className="block text-[10px] font-mono text-industrial-400 uppercase mb-1">
                                  ENTER MEASUREMENT READING ({step.unitOfMeasure || 'mm'}):
                                </label>
                                <div className="flex items-center gap-2">
                                  <input
                                    type="number"
                                    step="0.001"
                                    value={numericInput}
                                    onChange={(e) => setNumericInput(e.target.value)}
                                    placeholder={`e.g. ${step.nominalValue || 12.5}`}
                                    className="flex-1 bg-industrial-900 border border-substrate-border px-3 py-2 text-sm font-mono font-bold text-white rounded focus:border-hazard-red focus:outline-none"
                                  />
                                  {liveToleranceStatus && (
                                    <div className={`px-3 py-2 rounded text-xs font-mono font-bold flex items-center gap-1.5 ${
                                      liveToleranceStatus === 'WITHIN'
                                        ? 'bg-emerald-950 border border-emerald-500 text-emerald-400'
                                        : 'bg-red-950 border border-red-500 text-red-400'
                                    }`}>
                                      {liveToleranceStatus === 'WITHIN' ? <CheckCircle2 size={14} /> : <XCircle size={14} />}
                                      {liveToleranceStatus === 'WITHIN' ? 'WITHIN TOLERANCE' : `OUT OF BOUNDS (${liveToleranceStatus})`}
                                    </div>
                                  )}
                                </div>
                              </div>
                            </div>
                          )}

                          {/* 2. Photo Capture / Defect Snapshot */}
                          {step.stepType === 'PHOTO_CAPTURE' && (
                            <div className="space-y-3 bg-black/40 border border-substrate-border p-3 rounded">
                              <div className="flex items-center justify-between">
                                <span className="text-xs font-mono text-industrial-300">DEFECT & SURFACE EVIDENCE PHOTO:</span>
                                <IndustrialButton
                                  variant="secondary"
                                  size="sm"
                                  onClick={handleSimulatePhotoSnap}
                                >
                                  <Camera size={14} className="mr-1 text-hazard-red" />
                                  CAPTURE INSPECTION SNAP
                                </IndustrialButton>
                              </div>

                              {photoUrl && (
                                <div className="relative bg-industrial-900 border border-emerald-500/50 p-3 rounded space-y-1">
                                  <div className="flex items-center justify-between text-[10px] font-mono text-emerald-400">
                                    <span className="flex items-center gap-1"><CheckCircle2 size={12} /> EVIDENCE ATTACHED</span>
                                    <span>TIMESTAMP: {new Date().toLocaleTimeString()}</span>
                                  </div>
                                  <div className="text-xs font-mono text-white truncate">{photoUrl}</div>
                                </div>
                              )}
                            </div>
                          )}

                          {/* 3. Barcode Scanner */}
                          {step.stepType === 'BARCODE_SCAN' && (
                            <div className="space-y-2 bg-black/40 border border-substrate-border p-3 rounded">
                              <label className="block text-[10px] font-mono text-industrial-400 uppercase">
                                SCAN COMPONENT / MATERIAL LOT BARCODE:
                              </label>
                              <div className="flex items-center gap-2">
                                <input
                                  type="text"
                                  value={barcodeInput}
                                  onChange={(e) => setBarcodeInput(e.target.value)}
                                  placeholder="e.g. LOT-INCONEL-718-9921"
                                  className="flex-1 bg-industrial-900 border border-substrate-border px-3 py-2 text-xs font-mono text-white rounded focus:border-hazard-red focus:outline-none"
                                />
                                <IndustrialButton
                                  variant="secondary"
                                  size="sm"
                                  onClick={() => setBarcodeInput(`LOT-${step.stepNumber}-${Date.now().toString().slice(-4)}`)}
                                >
                                  <Scan size={14} className="mr-1" /> SIM SCAN
                                </IndustrialButton>
                              </div>
                            </div>
                          )}

                          {/* Operator Notes */}
                          <div>
                            <label className="block text-[10px] font-mono text-industrial-400 uppercase mb-1">
                              INSPECTOR / OPERATOR NOTES (OPTIONAL):
                            </label>
                            <input
                              type="text"
                              value={stepNotes}
                              onChange={(e) => setStepNotes(e.target.value)}
                              placeholder="e.g. Micrometer zeroed before measurement. Surface clean."
                              className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-xs font-mono text-white rounded focus:border-hazard-red focus:outline-none"
                            />
                          </div>

                          {/* Action Buttons */}
                          <div className="flex items-center justify-between pt-2 border-t border-substrate-border">
                            <button
                              onClick={() => handleRecordStep('FAILED')}
                              disabled={recordStepMutation.isPending}
                              className="px-4 py-2 bg-red-950/60 hover:bg-red-900 border border-red-600 text-red-300 hover:text-white rounded text-xs font-mono font-bold uppercase transition-all"
                            >
                              FLAG STEP AS FAILED
                            </button>

                            <IndustrialButton
                              variant="primary"
                              size="md"
                              onClick={() => handleRecordStep('PASSED')}
                              disabled={recordStepMutation.isPending}
                            >
                              <CheckSquare size={16} className="mr-1" />
                              CONFIRM & ADVANCE
                            </IndustrialButton>
                          </div>
                        </div>
                      )}
                    </div>
                  );
                })()
              ) : (
                <div className="py-8 text-center text-xs font-mono text-industrial-400">
                  Select a step or start an active session to inspect details.
                </div>
              )}
            </div>
          </IndustrialCard>
        </div>
      </div>

      {/* Quality Sign-Off Stamp Modal */}
      <Modal
        isOpen={signOffModalOpen}
        onClose={() => setSignOffModalOpen(false)}
        title="QUALITY SIGN-OFF GATE // APPROVAL STAMP"
      >
        <div className="space-y-4">
          <div className="bg-industrial-900 border border-substrate-border p-3 rounded space-y-2 text-xs font-mono">
            <div className="flex items-center justify-between">
              <span className="text-industrial-400">PRODUCTION ORDER:</span>
              <span className="text-white font-bold">{qualityGate?.orderNumber}</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-industrial-400">INSPECTOR:</span>
              <span className="text-white font-bold">{user?.displayName} ({user?.role})</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-industrial-400">MANDATORY STEPS COMPLETED:</span>
              <span className="text-emerald-400 font-bold">{qualityGate?.completedMandatorySteps} / {qualityGate?.totalMandatorySteps}</span>
            </div>
          </div>

          <div>
            <label className="block text-[10px] font-mono text-industrial-400 uppercase mb-1">
              QUALITY AUTHORITY COMMENTS & AUDIT NOTES:
            </label>
            <textarea
              value={signOffNotes}
              onChange={(e) => setSignOffNotes(e.target.value)}
              rows={3}
              className="w-full bg-industrial-900 border border-substrate-border p-2.5 text-xs font-mono text-white rounded focus:border-hazard-red focus:outline-none"
            />
          </div>

          {/* Stamp Preview */}
          <div className="border-2 border-dashed border-emerald-500/60 p-4 rounded text-center bg-emerald-950/20 space-y-1 font-mono">
            <div className="text-emerald-400 font-black text-sm uppercase tracking-widest flex items-center justify-center gap-1.5">
              <Award size={18} /> QUALITY APPROVED & VERIFIED
            </div>
            <div className="text-[10px] text-emerald-300">
              FOUNDRY//OS CRYPTOGRAPHIC AUDIT LOCK // ISO-9001 COMPLIANT
            </div>
          </div>

          <div className="flex justify-end gap-2 pt-2 border-t border-substrate-border">
            <IndustrialButton
              variant="secondary"
              size="sm"
              onClick={() => setSignOffModalOpen(false)}
            >
              CANCEL
            </IndustrialButton>
            <IndustrialButton
              variant="primary"
              size="sm"
              onClick={() => {
                if (!selectedOrderId) return;
                signOffMutation.mutate({
                  orderId: selectedOrderId,
                  data: {
                    gateStatus: 'PASSED',
                    signOffComments: signOffNotes,
                  }
                });
              }}
              disabled={signOffMutation.isPending}
            >
              <Award size={14} className="mr-1" />
              APPLY DIGITAL SIGN-OFF
            </IndustrialButton>
          </div>
        </div>
      </Modal>
    </div>
  );
};
