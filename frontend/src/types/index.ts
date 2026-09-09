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

export type DowntimeReasonCode = 'BREAKDOWN' | 'SETUP' | 'MATERIAL_SHORTAGE' | 'OTHER';

export interface DowntimeEventDto {
  id: string;
  machineId: string;
  machineName: string;
  reasonCode: DowntimeReasonCode;
  description?: string;
  startTime: string;
  endTime?: string;
  resolutionNote?: string;
  resolvedBy?: string;
  resolverName?: string;
  createdAt: string;
  updatedAt: string;
  version: number;
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

