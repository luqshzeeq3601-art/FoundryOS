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


