export type RoleType = 
  | 'ADMIN' 
  | 'PRODUCTION_MANAGER' 
  | 'ENGINEER' 
  | 'TECHNICIAN' 
  | 'OPERATOR' 
  | 'VIEWER';

export interface UserDto {
  id: string;
  email: string;
  displayName: string;
  role: RoleType;
  isActive: boolean;
  mustChangePassword: boolean;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  user: UserDto;
}

export interface TokenRefreshResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
}

export interface ChangePasswordRequest {
  currentPassword?: string;
  newPassword: string;
}

export interface CreateUserRequest {
  email: string;
  displayName: string;
  temporaryPassword: string;
  role: RoleType;
}

export interface UpdateUserRequest {
  displayName?: string;
  role?: RoleType;
  isActive?: boolean;
  expectedVersion: number;
}

export interface ResetPasswordRequest {
  temporaryPassword: string;
  expectedVersion: number;
}

export type MachineStatus = 'IDLE' | 'RUNNING' | 'DOWN';

export interface MachineDto {
  id: string;
  serialNumber: string;
  name: string;
  location: string;
  description?: string;
  status: MachineStatus;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface CreateMachineRequest {
  serialNumber: string;
  name: string;
  location: string;
  description?: string;
  status?: MachineStatus;
}

export interface UpdateMachineRequest {
  name?: string;
  location?: string;
  description?: string;
  expectedVersion: number;
}

export interface UpdateMachineStatusRequest {
  status: MachineStatus;
  expectedVersion: number;
}

export type DowntimeReasonCode = 
  | 'BREAKDOWN' 
  | 'SETUP' 
  | 'MATERIAL_SHORTAGE' 
  | 'MICRO_STOP' 
  | 'TOOLING_JAM' 
  | 'OPERATOR_PAUSE' 
  | 'UNPLANNED_MAINTENANCE' 
  | 'OTHER';

export type DowntimeTriggerSource = 'MANUAL' | 'AUTOMATED_SENSOR' | 'HEARTBEAT_TIMEOUT';

export interface DowntimeEventDto {
  id: string;
  machineId: string;
  machineName: string;
  reasonCode: DowntimeReasonCode;
  triggerSource: DowntimeTriggerSource;
  isMicroStop: boolean;
  description?: string;
  startTime: string;
  endTime?: string;
  resolutionNote?: string;
  resolvedBy?: string;
  resolverName?: string;
  rootCausePromptedAt?: string;
  rootCauseAcknowledgedAt?: string;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface AcknowledgeRootCauseRequest {
  reasonCode: DowntimeReasonCode;
  resolutionNote?: string;
}

export interface MicroStopSummary {
  machineId: string;
  machineName: string;
  from: string;
  to: string;
  microStopCount: number;
  totalMicroStopDurationSeconds: number;
  majorDowntimeCount: number;
  totalMajorDowntimeDurationSeconds: number;
  microStopPercentage: number;
}

export interface AutomatedEvaluationResult {
  machineId: string;
  previousStatus: MachineStatus;
  currentStatus: MachineStatus;
  transitionOccurred: boolean;
  downtimeEventId?: string;
  isMicroStop: boolean;
  rootCauseRequired: boolean;
  message: string;
  evaluatedAt: string;
}

export interface CreateDowntimeRequest {
  machineId: string;
  reasonCode: DowntimeReasonCode;
  description?: string;
  startTime?: string;
}

export interface ResolveDowntimeRequest {
  resolutionNote: string;
  endTime?: string;
  expectedVersion: number;
}

export type ProductionOrderStatus = 'DRAFT' | 'RELEASED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export interface ProductionOrderDto {
  id: string;
  orderNumber: string;
  machineId: string;
  machineName: string;
  productCode: string;
  productDescription?: string;
  plannedQuantity: number;
  goodQuantity: number;
  scrapQuantity: number;
  status: ProductionOrderStatus;
  startedAt?: string;
  completedAt?: string;
  closedAt?: string;
  closureNote?: string;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface CreateProductionOrderRequest {
  orderNumber: string;
  machineId: string;
  productCode: string;
  productDescription?: string;
  plannedQuantity: number;
}

export interface UpdateProductionOrderRequest {
  productCode?: string;
  productDescription?: string;
  plannedQuantity?: number;
  expectedVersion: number;
}

export interface UpdateProductionProgressRequest {
  goodQuantity: number;
  scrapQuantity: number;
  expectedVersion: number;
}

export interface TransitionOrderStatusRequest {
  targetStatus: ProductionOrderStatus;
  closureNote?: string;
  expectedVersion: number;
}

export type MaintenancePriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type MaintenanceStatus = 'OPEN' | 'ASSIGNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export interface DiagnosticSnapshotDto {
  assessmentId: string;
  machineId: string;
  machineName: string;
  plantName?: string;
  axis?: string;
  healthScore: number;
  healthStatus: MachineHealthStatus;
  isoSeverityZone: IsoSeverityZone;
  rmsVelocityMmS: number;
  spindleTemperatureC?: number;
  crestFactor?: number;
  kurtosis?: number;
  runningSpeedRpm?: number;
  dominantFault: FaultHarmonicType;
  suspectedSubsystem: string;
  recommendedParts: string[];
  prescriptiveAction: string;
  dominantPeaks: SpectralPeakDto[];
  capturedAt: string;
}

export interface WorkOrderDto {
  id: string;
  workOrderNumber: string;
  machineId: string;
  machineName: string;
  downtimeEventId?: string;
  title: string;
  description: string;
  priority: MaintenancePriority;
  status: MaintenanceStatus;
  assignedToId?: string;
  assignedToName?: string;
  dueAt?: string;
  startedAt?: string;
  completedAt?: string;
  closedAt?: string;
  completionNote?: string;
  cancellationNote?: string;
  isPrescriptive?: boolean;
  healthAssessmentId?: string;
  diagnosticSnapshot?: string;
  suspectedSubsystem?: string;
  recommendedParts?: string;
  lastTriggeredAt?: string;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface CreateWorkOrderRequest {
  workOrderNumber: string;
  machineId: string;
  downtimeEventId?: string;
  title: string;
  description: string;
  priority?: MaintenancePriority;
  assignedTo?: string;
  dueAt?: string;
}

export interface UpdateWorkOrderRequest {
  title?: string;
  description?: string;
  priority?: MaintenancePriority;
  dueAt?: string;
  expectedVersion: number;
}

export interface AssignWorkOrderRequest {
  assignedTo: string;
  expectedVersion: number;
}

export interface CompleteWorkOrderRequest {
  completionNote: string;
  expectedVersion: number;
}

export interface CancelWorkOrderRequest {
  cancellationNote: string;
  expectedVersion: number;
}

export interface AuditDto {
  id: string;
  actorId?: string;
  action: string;
  entityType: string;
  entityId: string;
  beforeData?: string;
  afterData?: string;
  traceId: string;
  createdAt: string;
}

export interface DashboardSummaryDto {
  totalMachines: number;
  runningMachines: number;
  idleMachines: number;
  downMachines: number;

  totalProductionOrders: number;
  activeProductionOrders: number;
  completedProductionOrders: number;
  totalGoodQuantity: number;
  totalScrapQuantity: number;
  scrapRate: number;

  openDowntimeEvents: number;
  openWorkOrders: number;
  inProgressWorkOrders: number;
  criticalWorkOrders: number;

  plantAvailability: number;
  plantPerformance: number;
  plantQuality: number;
  plantOee: number;

  downtimeReasonBreakdown?: Record<string, number>;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: {
    code: string;
    message: string;
    details?: Record<string, string>;
  };
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

// ==========================================
// Telemetry & IIoT Protocol Types (Sprint 6)
// ==========================================

export type ProtocolType = 'OPC_UA' | 'MQTT_SPARKPLUG_B' | 'MODBUS_TCP';
export type TelemetryQuality = 'GOOD' | 'BAD' | 'UNCERTAIN';

export interface TagMapping {
  id: string;
  machineId: string;
  machineName?: string;
  tagName: string;
  protocol: ProtocolType;
  tagAddress: string;
  dataType: string;
  unitOfMeasure?: string;
  scaleFactor: number;
  isActive: boolean;
  createdAt: string;
}

export interface CreateTagMappingRequest {
  tagName: string;
  protocol: ProtocolType;
  tagAddress: string;
  dataType?: string;
  unitOfMeasure?: string;
  scaleFactor?: number;
}

export interface MachineLiveTelemetry {
  machineId: string;
  machineName: string;
  serialNumber: string;
  machineStatus: MachineStatus;
  spindleSpeedRpm: number;
  vibrationMmPerSec: number;
  motorCurrentAmps: number;
  bearingTempCelsius: number;
  healthScore: number;
  activeProtocol: ProtocolType;
  connectionStatus: 'ONLINE' | 'WARNING' | 'CRITICAL' | 'OFFLINE';
  lastHeartbeat: string | null;
  configuredTagsCount: number;
}

export interface TelemetryPoint {
  tagName: string;
  value: number;
  unit?: string;
  quality?: TelemetryQuality;
  timestamp?: string;
}

export interface TelemetryBatchIngestRequest {
  machineId: string;
  gatewayId?: string;
  points: TelemetryPoint[];
}

export interface TelemetryIngestResponse {
  machineId: string;
  ingestedCount: number;
  status: string;
  timestamp: string;
  alerts: string[];
}

export interface TimeSeriesBucket {
  bucket: string;
  avg: number;
  min: number;
  max: number;
  count: number;
  quality: string;
}

export interface TimeSeriesResponse {
  machineId: string;
  machineName: string;
  tagName: string;
  unit: string;
  bucketResolution: string;
  from: string;
  to: string;
  pointCount: number;
  queryExecutionMs: number;
  series: TimeSeriesBucket[];
}

export interface RetentionReport {
  rawPointsPruned: number;
  rollups1mPruned: number;
  rollups1hPruned: number;
  executionTimeMs: number;
  executedAt: string;
}

// Barcode Scanning & Traceability Types (v2 Epic 8)
export type BarcodeType = 'TRAVELER' | 'MATERIAL_LOT' | 'MACHINE_ASSET' | 'OPERATOR_BADGE' | 'UNKNOWN';

export type BarcodeValidationStatus = 'VALID' | 'INVALID_BOM' | 'NOT_FOUND' | 'EXPIRED' | 'QUARANTINED' | 'UNAUTHORIZED' | 'ERROR';

export type ResolvedEntityType = 'PRODUCTION_ORDER' | 'MATERIAL_LOT' | 'MACHINE' | 'USER' | 'NONE';

export interface BarcodeScanRequest {
  rawPayload: string;
  barcodeFormat?: string;
  scannerSource?: 'HARDWARE_WEDGE' | 'CAMERA_ZXING' | 'MANUAL_KEYPAD';
  machineId?: string;
  productionOrderId?: string;
}

export interface BarcodeScanResponse {
  scanLogId?: string;
  rawPayload: string;
  barcodeType: BarcodeType;
  validationStatus: BarcodeValidationStatus;
  resolvedEntityType: ResolvedEntityType;
  resolvedEntityId?: string;
  resolvedEntitySummary?: string;
  bomMatched: boolean;
  message: string;
  executionLatencyMs: number;
  timestamp: string;
  entityData?: any;
}

export interface BomItemDto {
  id: string;
  productCode: string;
  materialCode: string;
  materialName: string;
  requiredQuantityPerUnit: number;
  uom: string;
}

export interface BarcodeScanLogDto {
  id: string;
  scanPayload: string;
  barcodeFormat: string;
  barcodeType: BarcodeType;
  scannerSource: string;
  resolvedEntityType: ResolvedEntityType;
  resolvedEntityId?: string;
  resolvedEntitySummary?: string;
  machineId?: string;
  productionOrderId?: string;
  validationStatus: BarcodeValidationStatus;
  bomMatched: boolean;
  errorMessage?: string;
  scannedByUserId?: string;
  scannedByName?: string;
  latencyMs: number;
  createdAt: string;
}

// Multi-Tenant Hierarchy Types (v2 Epic 5)
export interface EnterpriseDto {
  id: string;
  code: string;
  name: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface PlantDto {
  id: string;
  enterpriseId?: string;
  enterpriseName?: string;
  code: string;
  name: string;
  timezone: string;
  address?: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface ProductionAreaDto {
  id: string;
  plantId: string;
  plantName?: string;
  code: string;
  name: string;
  description?: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface ProductionLineDto {
  id: string;
  areaId: string;
  areaName?: string;
  code: string;
  name: string;
  description?: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface WorkCellDto {
  id: string;
  lineId: string;
  lineName?: string;
  code: string;
  name: string;
  description?: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface WorkCellNodeDto {
  cellId: string;
  cellCode: string;
  cellName: string;
  machines: MachineDto[];
}

export interface LineNodeDto {
  lineId: string;
  lineCode: string;
  lineName: string;
  workCells: WorkCellNodeDto[];
}

export interface AreaNodeDto {
  areaId: string;
  areaCode: string;
  areaName: string;
  lines: LineNodeDto[];
}

export interface HierarchyTreeDto {
  plantId: string;
  plantCode: string;
  plantName: string;
  timezone: string;
  status: string;
  areas: AreaNodeDto[];
}

export interface UserPlantMembershipDto {
  id: string;
  userId: string;
  userEmail: string;
  userDisplayName: string;
  plantId: string;
  plantCode: string;
  plantName: string;
  roleId?: string;
  roleName?: string;
  isDefault: boolean;
  createdAt: string;
}

export interface SwitchPlantRequest {
  plantId: string;
}

export interface CreatePlantRequest {
  enterpriseId: string;
  code: string;
  name: string;
  timezone: string;
  address?: string;
}

export interface CreateAreaRequest {
  plantId: string;
  code: string;
  name: string;
  description?: string;
}

export interface CreateLineRequest {
  areaId: string;
  code: string;
  name: string;
  description?: string;
}

export interface CreateWorkCellRequest {
  lineId: string;
  code: string;
  name: string;
  description?: string;
}

export interface AssignPlantMembershipRequest {
  userId: string;
  plantId: string;
  roleId?: string;
  isDefault?: boolean;
}

// Enterprise Fleet Analytics & Benchmarking (v2 Epic 5 Story 2)
export interface FleetSummaryDto {
  totalPlants: number;
  activeLines: number;
  totalMachines: number;
  runningMachines: number;
  idleMachines: number;
  downMachines: number;

  fleetAvgOee: number;
  fleetAvgAvailability: number;
  fleetAvgPerformance: number;
  fleetAvgQuality: number;

  totalGoodQuantity: number;
  totalScrapQuantity: number;
  fleetScrapRate: number;
  totalDowntimeMinutes: number;
}

export interface LineOeeBenchmarkDto {
  lineId: string;
  lineCode: string;
  lineName: string;
  areaId?: string;
  areaCode?: string;
  areaName?: string;

  oee: number;
  availability: number;
  performance: number;
  quality: number;

  totalGoodQuantity: number;
  totalScrapQuantity: number;
  scrapRate: number;

  totalMachines: number;
  runningMachines: number;
  idleMachines: number;
  downMachines: number;

  isBottleneck: boolean;
  status: string;
}

export interface PlantOeeBenchmarkDto {
  plantId: string;
  plantCode: string;
  plantName: string;
  timezone: string;
  address?: string;
  status: string;

  rank: number;
  oee: number;
  availability: number;
  performance: number;
  quality: number;
  oeeDeltaVsFleetAvg: number;
  benchmarkTier: 'WORLD_CLASS' | 'TARGET' | 'UNDERPERFORMING';

  totalGoodQuantity: number;
  totalScrapQuantity: number;
  scrapRate: number;

  totalMachines: number;
  runningMachines: number;
  idleMachines: number;
  downMachines: number;

  totalDowntimeMinutes: number;
  downtimeEventsCount: number;
  activeOrdersCount: number;
  completedOrdersCount: number;

  lineMetrics: LineOeeBenchmarkDto[];
}

export interface EnterpriseOeeMatrixDto {
  enterpriseId: string;
  enterpriseCode: string;
  enterpriseName: string;
  interval: string;
  calculatedAt: string;

  fleetSummary: FleetSummaryDto;
  plantMetrics: PlantOeeBenchmarkDto[];
}

export interface ScheduledReportResponseDto {
  reportId: string;
  reportType: string;
  format: 'CSV' | 'PDF';
  status: string;
  plantCount: number;
  fleetOee: number;
  fileSizeBytes: number;
  generatedAt: string;
  message: string;
}

// ==========================================
// Edge Resilience & Store-and-Forward Types (Sprint 9 Epic 5 Story 3)
// ==========================================

export type EdgeGatewayStatus = 'ONLINE' | 'OFFLINE' | 'SYNCING' | 'DEGRADED';
export type EdgeSyncStatus = 'PENDING' | 'PROCESSING' | 'RECONCILED' | 'FAILED' | 'PARTIAL';
export type EdgeTransactionType = 'PRODUCTION_OUTPUT' | 'BARCODE_SCAN' | 'DOWNTIME_EVENT' | 'MACHINE_STATE_TRANSITION' | 'OPERATOR_ACTION';
export type EdgeExecutionStatus = 'PROCESSED' | 'CONFLICT_RESOLVED' | 'FAILED' | 'DUPLICATE_IGNORED';

export interface EdgeGatewayDto {
  id: string;
  gatewayCode: string;
  name: string;
  plantId: string;
  plantName?: string;
  ipAddress?: string;
  macAddress?: string;
  status: EdgeGatewayStatus;
  lastHeartbeatAt?: string;
  lastSyncAt?: string;
  lastSyncSequenceId: number;
  bufferCapacityRecords: number;
  firmwareVersion: string;
  createdAt: string;
  updatedAt: string;
}

export interface EdgeGatewayCreateRequest {
  gatewayCode: string;
  name: string;
  plantId: string;
  ipAddress?: string;
  macAddress?: string;
  bufferCapacityRecords?: number;
  firmwareVersion?: string;
}

export interface EdgeGatewayHeartbeatRequest {
  status?: string;
  bufferedRecordCount?: number;
  firmwareVersion?: string;
  ipAddress?: string;
}

export interface EdgeTransactionRecordDto {
  sequenceId: number;
  idempotencyKey: string;
  transactionType: EdgeTransactionType;
  entityType: string;
  entityId?: string;
  payloadJson: string;
  vectorClockVersion?: number;
  recordedAt: string;
}

export interface EdgeSyncBatchRequestDto {
  gatewayCode: string;
  batchId: string;
  plantId: string;
  sequenceStart?: number;
  sequenceEnd?: number;
  disconnectedAt: string;
  reconnectedAt: string;
  transactions: EdgeTransactionRecordDto[];
}

export interface EdgeSyncBatchResultDto {
  id?: string;
  batchId: string;
  gatewayCode?: string;
  syncStatus: EdgeSyncStatus;
  totalRecords: number;
  processedRecords: number;
  failedRecords: number;
  duplicateIgnoredRecords: number;
  conflictResolvedRecords: number;
  reconciliationNotes?: string;
  reconciledAt?: string;
}

export interface EdgeTransactionLogDto {
  id: string;
  batchId?: string;
  batchCode?: string;
  gatewayCode?: string;
  sequenceId: number;
  idempotencyKey: string;
  transactionType: EdgeTransactionType;
  entityType: string;
  entityId?: string;
  payloadJson: string;
  vectorClockVersion: number;
  recordedAt: string;
  syncedAt: string;
  executionStatus: EdgeExecutionStatus;
  conflictResolutionNote?: string;
}

export interface EdgeCachedOrderDto {
  id: string;
  orderNumber: string;
  productCode: string;
  productName: string;
  targetQuantity: number;
  goodQuantity: number;
  scrapQuantity: number;
  status: string;
  machineId?: string;
  machineCode?: string;
}

export interface EdgeCachedMachineDto {
  id: string;
  machineCode: string;
  name: string;
  status: string;
  lineId?: string;
  lineName?: string;
}

export interface EdgeCachedBomItemDto {
  id: string;
  productCode: string;
  componentMaterialCode: string;
  componentDescription: string;
  quantityRequired: number;
  uom: string;
}

export interface EdgeCachedMaterialLotDto {
  id: string;
  lotNumber: string;
  materialCode: string;
  materialName: string;
  quantityRemaining: number;
  uom: string;
  status: string;
}

export interface EdgeOfflineCacheManifestDto {
  gatewayCode: string;
  plantId: string;
  plantName: string;
  manifestGeneratedAt: string;
  activeOrders: EdgeCachedOrderDto[];
  machines: EdgeCachedMachineDto[];
  bomItems: EdgeCachedBomItemDto[];
  materialLots: EdgeCachedMaterialLotDto[];
}

export interface SimulateDisconnectRequestDto {
  gatewayCode: string;
  disconnectDurationHours?: number;
  orderId?: string;
  producedPartsGood?: number;
  producedPartsScrap?: number;
  barcodeScans?: number;
  downtimeDurationMinutes?: number;
  downtimeReason?: string;
  machineId?: string;
}

// ---------------------------------------------------------------------------
// Digital Standard Operating Procedures (SOP) & Quality Sign-Off Gates
// ---------------------------------------------------------------------------

export type SopCategory = 
  | 'MACHINING' 
  | 'ASSEMBLY' 
  | 'QUALITY_CONTROL' 
  | 'MAINTENANCE' 
  | 'PACKAGING' 
  | 'SAFETY_PRE_CHECK';

export type SopStatus = 'DRAFT' | 'REVIEW' | 'PUBLISHED' | 'ARCHIVED';

export type SopStepType = 
  | 'INSTRUCTION' 
  | 'CHECKLIST_ITEM' 
  | 'NUMERIC_MEASUREMENT' 
  | 'PHOTO_CAPTURE' 
  | 'BARCODE_SCAN' 
  | 'SIGN_OFF';

export type SopSessionStatus = 'IN_PROGRESS' | 'PASSED' | 'FAILED' | 'ABORTED';

export type StepRecordStatus = 'PENDING' | 'PASSED' | 'FAILED' | 'SKIPPED' | 'FLAGGED';

export type GateStatus = 'PENDING' | 'PASSED' | 'FAILED' | 'BYPASSED';

export interface SopStepDto {
  id?: string;
  sopId?: string;
  stepNumber: number;
  title: string;
  instructionText: string;
  stepType: SopStepType;
  imageUrl?: string;
  cadViewNode?: string;
  mandatory: boolean;
  isMandatory?: boolean;
  nominalValue?: number;
  minTolerance?: number;
  maxTolerance?: number;
  unitOfMeasure?: string;
  safetyAlert?: string;
}

export interface SopDto {
  id: string;
  sopCode: string;
  title: string;
  productCode: string;
  plantId?: string;
  plantName?: string;
  version: string;
  category: SopCategory;
  status: SopStatus;
  description?: string;
  cadDrawingUrl?: string;
  safetyPrecautions?: string;
  estimatedDurationMinutes: number;
  requiresQualitySignOff: boolean;
  steps: SopStepDto[];
  createdAt?: string;
  updatedAt?: string;
}

export interface SopStepExecutionRecordDto {
  id: string;
  sessionId: string;
  stepId: string;
  stepNumber: number;
  stepTitle: string;
  instructionText: string;
  stepType: SopStepType;
  imageUrl?: string;
  cadViewNode?: string;
  mandatory: boolean;
  isMandatory?: boolean;
  nominalValue?: number;
  minTolerance?: number;
  maxTolerance?: number;
  unitOfMeasure?: string;
  safetyAlert?: string;
  status: StepRecordStatus;
  numericValue?: number;
  isWithinTolerance?: boolean;
  textFeedback?: string;
  photoEvidenceUrl?: string;
  barcodeScanned?: string;
  verifiedByUserId?: string;
  verifiedByName?: string;
  verifiedAt?: string;
  notes?: string;
  createdAt?: string;
}

export interface SopExecutionSessionDto {
  id: string;
  sopId: string;
  sopCode: string;
  sopTitle: string;
  cadDrawingUrl?: string;
  safetyPrecautions?: string;
  productionOrderId: string;
  orderNumber: string;
  productCode: string;
  machineId?: string;
  machineCode?: string;
  plantId?: string;
  plantName?: string;
  sessionStatus: SopSessionStatus;
  startedAt: string;
  completedAt?: string;
  operatorUserId?: string;
  operatorName?: string;
  qualitySignOffBy?: string;
  qualitySignOffName?: string;
  qualitySignOffAt?: string;
  qualitySignOffNotes?: string;
  totalSteps: number;
  completedSteps: number;
  passedSteps: number;
  failedSteps: number;
  stepRecords: SopStepExecutionRecordDto[];
}

export interface QualityGateStatusDto {
  gateId?: string;
  productionOrderId: string;
  orderNumber: string;
  productCode: string;
  sopId?: string;
  sopCode?: string;
  sopTitle?: string;
  sessionId?: string;
  gateStatus: GateStatus;
  compliant: boolean;
  isCompliant?: boolean;
  requiresQualityRole: boolean;
  totalMandatorySteps: number;
  completedMandatorySteps: number;
  failedMandatorySteps: number;
  signedOffByUserId?: string;
  signedOffByName?: string;
  signedOffAt?: string;
  signOffComments?: string;
  blockingReason?: string;
}

export interface StartSopSessionRequestDto {
  productionOrderId: string;
  sopId?: string;
  machineId?: string;
}

export interface RecordStepExecutionRequestDto {
  stepId: string;
  status?: StepRecordStatus;
  numericValue?: number;
  textFeedback?: string;
  photoEvidenceUrl?: string;
  barcodeScanned?: string;
  notes?: string;
}

export interface QualitySignOffRequestDto {
  gateStatus: GateStatus;
  signOffComments?: string;
}
// ---------------------------------------------------------------------------
// Spindle Vibration Spectral Analysis (FFT) & Machine Health (v2 Epic 6 Story 1)
// ---------------------------------------------------------------------------

export type IsoSeverityZone = 'ZONE_A' | 'ZONE_B' | 'ZONE_C' | 'ZONE_D';

export type MachineHealthStatus = 'EXCELLENT' | 'GOOD' | 'FAIR_DEGRADED' | 'WARNING' | 'CRITICAL';

export type MachineVibrationClass = 
  | 'CLASS_I_SMALL' 
  | 'CLASS_II_MEDIUM' 
  | 'CLASS_III_LARGE_RIGID' 
  | 'CLASS_IV_LARGE_FLEXIBLE';

export type FaultHarmonicType = 
  | 'NORMAL' 
  | 'UNBALANCE_1X' 
  | 'MISALIGNMENT_2X' 
  | 'LOOSENESS_3X' 
  | 'BPFO_BEARING_OUTER' 
  | 'BPFI_BEARING_INNER' 
  | 'BSF_BALL_SPIN' 
  | 'HIGH_FREQUENCY_NOISE';

export interface SpectralPeakDto {
  frequencyHz: number;
  amplitudeMmS: number;
  orderMultiple?: number;
  faultHarmonicType: FaultHarmonicType;
  confidence: number;
}

export interface FftSpectrumDto {
  machineId: string;
  machineName: string;
  axis: string;
  sampleRateHz: number;
  sampleCount: number;
  runningSpeedRpm?: number;
  fundamentalFrequencyHz?: number;
  frequencies: number[];
  amplitudes: number[];
  peaks: SpectralPeakDto[];
  rmsVelocityMmS: number;
  peakAccelerationG: number;
  crestFactor: number;
  kurtosis: number;
  bearingTemperatureC?: number;
  capturedAt?: string;
}

export interface IsoSeverityResultDto {
  zone: IsoSeverityZone;
  zoneTitle: string;
  zoneDescription: string;
  vibrationClass: MachineVibrationClass;
  rmsVelocityMmS: number;
  zoneABoundary: number;
  zoneBBoundary: number;
  zoneCBoundary: number;
  isAlert: boolean;
  isCritical: boolean;
}

export interface MachineHealthAssessmentDto {
  id: string;
  machineId: string;
  machineName: string;
  serialNumber: string;
  location: string;
  plantId?: string;
  plantName?: string;
  healthScore: number;
  healthStatus: MachineHealthStatus;
  healthStatusDescription: string;
  isoSeverityZone: IsoSeverityZone;
  isoZoneTitle: string;
  vibrationClass: MachineVibrationClass;
  rmsVelocityMmS: number;
  spindleTemperatureC?: number;
  dominantFaultType?: string;
  diagnosisSummary: string;
  recommendedAction?: string;
  dominantPeaks: SpectralPeakDto[];
  activeWorkOrder?: WorkOrderDto;
  assessedAt: string;
}

export interface FleetHealthSummaryDto {
  totalMachinesAssessed: number;
  averageFleetHealthScore: number;
  zoneACount: number;
  zoneBCount: number;
  zoneCCount: number;
  zoneDCount: number;
  criticalCount: number;
  warningCount: number;
  fairCount: number;
  healthyCount: number;
  assessments: MachineHealthAssessmentDto[];
}

export interface SimulateBurstRequestDto {
  machineId: string;
  faultType?: FaultHarmonicType;
  runningSpeedRpm?: number;
  sampleRateHz?: number;
  sampleCount?: number;
  noiseLevel?: number;
  bearingTemperatureC?: number;
}

export interface VibrationBurstIngestDto {
  machineId: string;
  axis?: string;
  sampleRateHz?: number;
  runningSpeedRpm?: number;
  bearingTemperatureC?: number;
  samples: number[];
}

export type ErpType = 'SAP_S4HANA' | 'ORACLE_NETSUITE' | 'GENERIC_ODATA_V4' | 'MOCK_ERP';
export type ErpSyncStatus = 'LOCAL_ONLY' | 'SYNCED' | 'PENDING_CONFIRMATION' | 'CONFIRMED' | 'SYNC_ERROR';
export type ErpSyncDirection = 'INBOUND_RELEASE' | 'OUTBOUND_CONFIRMATION' | 'POLL_ORDERS' | 'HEALTH_CHECK';

export interface ErpConnectorDto {
  id: string;
  plantId?: string;
  plantCode?: string;
  plantName?: string;
  name: string;
  erpType: ErpType;
  baseUrl: string;
  authType: string;
  apiKeyOrUser?: string;
  clientId?: string;
  companyIdOrClient?: string;
  syncIntervalSeconds: number;
  isActive: boolean;
  isAutoSyncEnabled: boolean;
  healthStatus: string;
  lastHealthCheckAt?: string;
  lastSyncAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateErpConnectorRequest {
  plantId?: string;
  name: string;
  erpType: ErpType;
  baseUrl: string;
  authType?: string;
  apiKeyOrUser?: string;
  secretOrToken?: string;
  clientId?: string;
  companyIdOrClient?: string;
  syncIntervalSeconds?: number;
  isAutoSyncEnabled?: boolean;
}

export interface ErpOrderConfirmationDto {
  id: string;
  productionOrderId: string;
  orderNumber?: string;
  erpOrderId: string;
  confirmationNumber: string;
  confirmedGoodQty: number;
  confirmedScrapQty: number;
  scrapReason?: string;
  laborHours: number;
  machineHours: number;
  erpPostingStatus: string;
  erpDocumentNumber?: string;
  errorMessage?: string;
  retryCount: number;
  postedAt?: string;
  createdAt: string;
}

export interface ErpSyncLogDto {
  id: string;
  connectorId?: string;
  connectorName?: string;
  plantId?: string;
  plantCode?: string;
  syncDirection: ErpSyncDirection;
  entityType: string;
  entityId?: string;
  erpReferenceId?: string;
  status: string;
  payloadJson?: string;
  responseJson?: string;
  errorMessage?: string;
  retryCount: number;
  durationMs: number;
  syncedAt: string;
}

// Material Backflushing & BOM Explosion Types (Sprint 11 Epic 7 Story 2)
export interface MaterialDto {
  id: string;
  plantId?: string;
  plantName?: string;
  materialCode: string;
  materialName: string;
  category: string;
  uom: string;
  currentStock: number;
  minimumStock: number;
  standardCost: number;
  scrapCostCenter: string;
  isLowStock: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface BomComponentExplosionItemDto {
  materialCode: string;
  materialName: string;
  requiredPerUnit: number;
  totalRequiredQuantity: number;
  currentStock: number;
  stockSufficient: boolean;
  uom: string;
  scrapCostCenter: string;
  unitCost: number;
  totalCost: number;
}

export interface BomExplosionDto {
  productCode: string;
  plannedQuantity: number;
  allMaterialsInStock: boolean;
  totalEstimatedMaterialCost: number;
  components: BomComponentExplosionItemDto[];
}

export interface MaterialConsumptionRecordDto {
  id: string;
  productionOrderId: string;
  orderNumber?: string;
  plantId?: string;
  plantCode?: string;
  materialId?: string;
  materialCode: string;
  materialName: string;
  lotNumber?: string;
  goodPiecesProduced: number;
  scrapPiecesProduced: number;
  scrapReasonCode?: string;
  scrapCostCenter: string;
  theoreticalQuantity: number;
  actualQuantity: number;
  variancePercentage: number;
  varianceAlertTriggered: boolean;
  uom: string;
  recordedAt: string;
}

export interface RecordProductionOutputRequest {
  incrementalGoodQuantity: number;
  incrementalScrapQuantity: number;
  scrapReasonCode?: string;
  scrapCostCenter?: string;
  lotNumber?: string;
  actualQuantities?: Record<string, number>;
}
