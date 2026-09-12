import React, { useState, useEffect } from 'react';
import {
  Server,
  RefreshCw,
  ArrowDownLeft,
  ArrowUpRight,
  Database,
  Activity,
  Clock,
  Send,
  Radio,
  Layers,
  Box,
  AlertTriangle,
  FileSpreadsheet,
  CheckCircle2,
  XCircle,
  Sliders,
  DollarSign
} from 'lucide-react';
import { erpApi, materialsApi, api } from '../../services/api-client';
import {
  ErpConnectorDto,
  ErpOrderConfirmationDto,
  ErpSyncLogDto,
  PagedResponse,
  ProductionOrderDto,
  MaterialDto,
  BomExplosionDto,
  MaterialConsumptionRecordDto
} from '../../types';

export const ErpIntegrationHubView: React.FC = () => {
  // Navigation sub-tab state
  const [activeHubTab, setActiveHubTab] = useState<'erp-connectors' | 'materials-backflushing'>('materials-backflushing');

  // ERP SCM Connectors & Orders State (Epic 7 Story 1)
  const [connectors, setConnectors] = useState<ErpConnectorDto[]>([]);
  const [confirmations, setConfirmations] = useState<ErpOrderConfirmationDto[]>([]);
  const [syncLogs, setSyncLogs] = useState<ErpSyncLogDto[]>([]);
  const [orders, setOrders] = useState<ProductionOrderDto[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [testingConnectorId, setTestingConnectorId] = useState<string | null>(null);
  const [syncingConnectorId, setSyncingConnectorId] = useState<string | null>(null);
  const [submittingConfirmation, setSubmittingConfirmation] = useState<boolean>(false);
  const [selectedLogPayload, setSelectedLogPayload] = useState<string | null>(null);

  // Manual Confirmation Form State
  const [selectedOrderId, setSelectedOrderId] = useState<string>('');
  const [goodQty, setGoodQty] = useState<number>(100);
  const [scrapQty, setScrapQty] = useState<number>(0);
  const [scrapReason, setScrapReason] = useState<string>('');
  const [laborHours, setLaborHours] = useState<number>(4.0);
  const [machineHours, setMachineHours] = useState<number>(3.5);
  const [actionMessage, setActionMessage] = useState<string | null>(null);

  // Material Backflushing & BOM State (Epic 7 Story 2)
  const [materials, setMaterials] = useState<MaterialDto[]>([]);
  const [consumptionLogs, setConsumptionLogs] = useState<MaterialConsumptionRecordDto[]>([]);
  const [varianceAlerts, setVarianceAlerts] = useState<MaterialConsumptionRecordDto[]>([]);
  const [bomExplosion, setBomExplosion] = useState<BomExplosionDto | null>(null);
  const [selectedBomProduct, setSelectedBomProduct] = useState<string>('SHAFT-HD-001');
  const [bomQuantity, setBomQuantity] = useState<number>(100);
  const [bomLoading, setBomLoading] = useState<boolean>(false);

  // Backflushing Form State
  const [bfOrderId, setBfOrderId] = useState<string>('');
  const [bfGoodQty, setBfGoodQty] = useState<number>(50);
  const [bfScrapQty, setBfScrapQty] = useState<number>(2);
  const [bfScrapReason, setBfScrapReason] = useState<string>('SCRAP_DIMENSION_VARIANCE');
  const [bfCostCenter, setBfCostCenter] = useState<string>('CC-MACHINING-01');
  const [bfLotNumber, setBfLotNumber] = useState<string>('LOT-9012');
  const [bfSimulateVariance, setBfSimulateVariance] = useState<boolean>(false);
  const [submittingBackflush, setSubmittingBackflush] = useState<boolean>(false);

  const loadData = async () => {
    try {
      setLoading(true);
      const [
        connectorsRes,
        confsRes,
        logsRes,
        ordersRes,
        materialsRes,
        consumptionRes,
        varianceRes
      ] = await Promise.all([
        erpApi.getConnectors().catch(() => []),
        erpApi.getConfirmations().catch(() => []),
        erpApi.getSyncLogs().catch(() => []),
        api.get<PagedResponse<ProductionOrderDto>>('/production-orders', { size: 50 }).catch(() => ({ content: [] } as any)),
        materialsApi.getAllMaterials().catch(() => []),
        materialsApi.getConsumption().catch(() => []),
        materialsApi.getVarianceAlerts().catch(() => [])
      ]);

      setConnectors(connectorsRes);
      setConfirmations(confsRes);
      setSyncLogs(logsRes);
      const orderList = ordersRes.content || [];
      setOrders(orderList);
      if (orderList.length > 0 && !selectedOrderId) {
        setSelectedOrderId(orderList[0].id);
      }
      if (orderList.length > 0 && !bfOrderId) {
        setBfOrderId(orderList[0].id);
      }
      setMaterials(materialsRes);
      setConsumptionLogs(consumptionRes);
      setVarianceAlerts(varianceRes);
    } catch (err: any) {
      console.error('Failed to load ERP hub data', err);
    } finally {
      setLoading(false);
    }
  };

  const loadBomExplosion = async (productCode: string, qty: number) => {
    if (!productCode) return;
    try {
      setBomLoading(true);
      const explosion = await materialsApi.getBomExplosion(productCode, qty);
      setBomExplosion(explosion);
    } catch (err: any) {
      console.error('Failed to load BOM explosion', err);
    } finally {
      setBomLoading(false);
    }
  };

  useEffect(() => {
    loadData();
    const interval = setInterval(loadData, 15000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    if (selectedBomProduct) {
      loadBomExplosion(selectedBomProduct, bomQuantity);
    }
  }, [selectedBomProduct, bomQuantity]);

  const handleTestConnection = async (id: string) => {
    try {
      setTestingConnectorId(id);
      setActionMessage(null);
      const res = await erpApi.testConnector(id);
      setActionMessage(`Connector test: ${res.status} (${res.success ? 'Success' : 'Failed'})`);
      loadData();
    } catch (err: any) {
      setActionMessage(`Test failed: ${err.message || 'Error'}`);
    } finally {
      setTestingConnectorId(null);
    }
  };

  const handleSyncInbound = async (id: string) => {
    try {
      setSyncingConnectorId(id);
      setActionMessage(null);
      const res = await erpApi.syncInbound(id);
      setActionMessage(`Inbound sync complete: ${res.syncedCount} order(s) processed.`);
      loadData();
    } catch (err: any) {
      setActionMessage(`Sync failed: ${err.message || 'Error'}`);
    } finally {
      setSyncingConnectorId(null);
    }
  };

  const handleSubmitConfirmation = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedOrderId) return;
    try {
      setSubmittingConfirmation(true);
      setActionMessage(null);
      const res = await erpApi.submitConfirmation(selectedOrderId, {
        confirmedGoodQty: Number(goodQty),
        confirmedScrapQty: Number(scrapQty),
        scrapReason: scrapReason || undefined,
        laborHours: Number(laborHours),
        machineHours: Number(machineHours)
      });
      setActionMessage(`Confirmation posted: Document ${res.erpDocumentNumber || res.confirmationNumber} [${res.erpPostingStatus}]`);
      loadData();
    } catch (err: any) {
      setActionMessage(`Confirmation failed: ${err.message || 'Error'}`);
    } finally {
      setSubmittingConfirmation(false);
    }
  };

  const handleRecordBackflushing = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!bfOrderId) return;
    try {
      setSubmittingBackflush(true);
      setActionMessage(null);

      // If simulate variance is checked, inject a +12% quantity override for the first component
      const actualQuantities: Record<string, number> = {};
      if (bfSimulateVariance && bomExplosion && bomExplosion.components.length > 0) {
        const targetMat = bomExplosion.components[0];
        const nominal = targetMat.requiredPerUnit * (bfGoodQty + bfScrapQty);
        actualQuantities[targetMat.materialCode] = Number((nominal * 1.12).toFixed(2));
      }

      const records = await materialsApi.recordOutput(bfOrderId, {
        incrementalGoodQuantity: Number(bfGoodQty),
        incrementalScrapQuantity: Number(bfScrapQty),
        scrapReasonCode: bfScrapQty > 0 ? bfScrapReason : undefined,
        scrapCostCenter: bfCostCenter,
        lotNumber: bfLotNumber || undefined,
        actualQuantities: Object.keys(actualQuantities).length > 0 ? actualQuantities : undefined
      });

      const triggeredAlerts = records.filter(r => r.varianceAlertTriggered);
      if (triggeredAlerts.length > 0) {
        setActionMessage(`OUTPUT RECORDED: Backflushed ${records.length} BOM component(s). WARNING: ${triggeredAlerts.length} item exceeded 5% variance threshold!`);
      } else {
        setActionMessage(`OUTPUT RECORDED: Successfully backflushed ${records.length} BOM component(s). Inventory decremented.`);
      }

      loadData();
      if (selectedBomProduct) {
        loadBomExplosion(selectedBomProduct, bomQuantity);
      }
    } catch (err: any) {
      setActionMessage(`Backflushing failed: ${err.message || 'Error'}`);
    } finally {
      setSubmittingBackflush(false);
    }
  };

  return (
    <div className="p-6 space-y-6 max-w-7xl mx-auto font-mono text-zinc-200">
      {/* Header Banner */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center bg-zinc-900 border border-zinc-800 p-6 rounded-none shadow-2xl relative overflow-hidden">
        <div className="absolute top-0 right-0 w-32 h-32 bg-cyan-500/5 blur-3xl pointer-events-none" />
        <div>
          <div className="flex items-center space-x-3">
            <Server className="w-7 h-7 text-cyan-400" />
            <h1 className="text-2xl font-bold tracking-wider text-zinc-100 uppercase">
              ERP // SCM & MATERIAL HUB
            </h1>
            <span className="px-2.5 py-0.5 text-xs bg-cyan-950/80 text-cyan-400 border border-cyan-800 tracking-widest uppercase">
              EPIC 7 // S1 & S2
            </span>
          </div>
          <p className="text-xs text-zinc-400 mt-1 uppercase tracking-wider">
            SAP S/4HANA & NETSUITE ADAPTERS // AUTOMATED BOM BACKFLUSHING // SCRAP COST ALLOCATION
          </p>
        </div>
        <div className="mt-4 md:mt-0 flex items-center space-x-3">
          <button
            onClick={loadData}
            disabled={loading}
            className="flex items-center space-x-2 px-4 py-2 bg-zinc-800 hover:bg-zinc-700 text-zinc-100 text-xs uppercase tracking-wider border border-zinc-700 transition"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin text-cyan-400' : ''}`} />
            <span>Refresh Hub</span>
          </button>
        </div>
      </div>

      {/* Variance Alert Banner (if >5% threshold triggered) */}
      {varianceAlerts.length > 0 && (
        <div className="p-4 bg-rose-950/60 border-l-4 border-rose-500 border border-rose-800/80 text-rose-200 text-xs flex flex-col md:flex-row md:items-center justify-between gap-2 shadow-lg">
          <div className="flex items-center space-x-3">
            <AlertTriangle className="w-5 h-5 text-rose-400 animate-pulse flex-shrink-0" />
            <div>
              <div className="font-bold uppercase tracking-wider text-rose-300">
                CRITICAL VARIANCE ALERT: {varianceAlerts.length} BOM CONSUMPTION EXCEEDING 5% THRESHOLD
              </div>
              <div className="text-[11px] text-rose-400 font-mono mt-0.5">
                Latest: {varianceAlerts[0].materialCode} ({varianceAlerts[0].variancePercentage > 0 ? '+' : ''}{varianceAlerts[0].variancePercentage.toFixed(1)}%) on Order {varianceAlerts[0].orderNumber || varianceAlerts[0].productionOrderId} // Cost Center: {varianceAlerts[0].scrapCostCenter}
              </div>
            </div>
          </div>
          <button
            onClick={() => setActiveHubTab('materials-backflushing')}
            className="px-3 py-1 bg-rose-900/80 hover:bg-rose-800 text-rose-100 text-[10px] uppercase font-bold tracking-wider border border-rose-700 whitespace-nowrap self-start md:self-auto"
          >
            Review Material Ledger
          </button>
        </div>
      )}

      {/* Action Notification Message */}
      {actionMessage && (
        <div className="p-3 bg-zinc-900 border-l-4 border-cyan-500 text-cyan-300 text-xs flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <Radio className="w-4 h-4 animate-pulse text-cyan-400" />
            <span>{actionMessage}</span>
          </div>
          <button onClick={() => setActionMessage(null)} className="text-zinc-500 hover:text-zinc-300">
            ×
          </button>
        </div>
      )}

      {/* Primary Sub-Tab Selector */}
      <div className="flex border-b border-zinc-800 space-x-2">
        <button
          onClick={() => setActiveHubTab('materials-backflushing')}
          className={`flex items-center space-x-2 px-5 py-2.5 text-xs font-bold uppercase tracking-wider border-b-2 transition ${
            activeHubTab === 'materials-backflushing'
              ? 'border-cyan-400 text-cyan-300 bg-zinc-900/80'
              : 'border-transparent text-zinc-400 hover:text-zinc-200 hover:bg-zinc-900/40'
          }`}
        >
          <Layers className="w-4 h-4" />
          <span>[01] Material Backflushing & BOM Explosion</span>
          {varianceAlerts.length > 0 && (
            <span className="px-1.5 py-0.2 text-[9px] bg-rose-900 text-rose-200 rounded-full">
              {varianceAlerts.length}
            </span>
          )}
        </button>

        <button
          onClick={() => setActiveHubTab('erp-connectors')}
          className={`flex items-center space-x-2 px-5 py-2.5 text-xs font-bold uppercase tracking-wider border-b-2 transition ${
            activeHubTab === 'erp-connectors'
              ? 'border-cyan-400 text-cyan-300 bg-zinc-900/80'
              : 'border-transparent text-zinc-400 hover:text-zinc-200 hover:bg-zinc-900/40'
          }`}
        >
          <Server className="w-4 h-4" />
          <span>[02] ERP / SCM Connectors & Order Sync</span>
          <span className="px-1.5 py-0.2 text-[9px] bg-zinc-800 text-zinc-400">
            {connectors.length}
          </span>
        </button>
      </div>

      {/* TAB 1: MATERIAL BACKFLUSHING & BOM EXPLOSION */}
      {activeHubTab === 'materials-backflushing' && (
        <div className="space-y-6">
          {/* Top Row: BOM Explosion Simulator & Backflush Recording Action */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Left 2 Cols: Interactive BOM Explosion Card */}
            <div className="lg:col-span-2 space-y-4">
              <div className="bg-zinc-900 border border-zinc-800 p-5 space-y-4">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-zinc-800 pb-3">
                  <div className="flex items-center space-x-2">
                    <FileSpreadsheet className="w-5 h-5 text-cyan-400" />
                    <div>
                      <h2 className="text-sm font-bold text-zinc-100 uppercase tracking-wider">
                        Interactive BOM Explosion Engine
                      </h2>
                      <span className="text-[10px] text-zinc-400 uppercase">
                        Dynamic Multi-Level Bill of Materials & Stock Sufficiency Check
                      </span>
                    </div>
                  </div>

                  {/* Product & Qty Controls */}
                  <div className="flex items-center space-x-2">
                    <select
                      value={selectedBomProduct}
                      onChange={(e) => setSelectedBomProduct(e.target.value)}
                      className="bg-zinc-950 border border-zinc-700 text-zinc-200 p-1.5 text-xs font-mono focus:border-cyan-500 outline-none"
                    >
                      <option value="SHAFT-HD-001">SHAFT-HD-001 (Heavy Duty Shaft)</option>
                      <option value="BRACKET-M8">BRACKET-M8 (Mounting Bracket)</option>
                      <option value="VALVE-FL-50">VALVE-FL-50 (Flanged Valve)</option>
                      {orders.map(o => (
                        <option key={o.id} value={o.productCode}>
                          {o.productCode} ({o.orderNumber})
                        </option>
                      ))}
                    </select>

                    <div className="flex items-center space-x-1">
                      <span className="text-[11px] text-zinc-500">QTY:</span>
                      <input
                        type="number"
                        min="1"
                        value={bomQuantity}
                        onChange={(e) => setBomQuantity(Math.max(1, Number(e.target.value)))}
                        className="w-16 bg-zinc-950 border border-zinc-700 text-zinc-100 p-1.5 text-xs font-mono text-center focus:border-cyan-500 outline-none"
                      />
                    </div>
                  </div>
                </div>

                {/* BOM Explosion Summary Badges */}
                {bomExplosion ? (
                  <div className="space-y-4">
                    <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                      <div className="p-3 bg-zinc-950 border border-zinc-800">
                        <span className="text-[10px] text-zinc-500 uppercase block">Product / Planned Qty</span>
                        <div className="text-sm font-bold text-zinc-200 mt-0.5 font-mono">
                          {bomExplosion.productCode} × {bomExplosion.plannedQuantity} pcs
                        </div>
                      </div>

                      <div className="p-3 bg-zinc-950 border border-zinc-800">
                        <span className="text-[10px] text-zinc-500 uppercase block">Stock Sufficiency</span>
                        <div className="flex items-center space-x-2 mt-0.5">
                          {bomExplosion.allMaterialsInStock ? (
                            <span className="inline-flex items-center space-x-1 text-xs font-bold text-emerald-400">
                              <CheckCircle2 className="w-4 h-4" />
                              <span>ALL IN STOCK</span>
                            </span>
                          ) : (
                            <span className="inline-flex items-center space-x-1 text-xs font-bold text-rose-400">
                              <XCircle className="w-4 h-4" />
                              <span>STOCK DEFICIT</span>
                            </span>
                          )}
                        </div>
                      </div>

                      <div className="p-3 bg-zinc-950 border border-zinc-800">
                        <span className="text-[10px] text-zinc-500 uppercase block">Total Estimated Cost</span>
                        <div className="text-sm font-bold text-cyan-300 mt-0.5 font-mono flex items-center">
                          <DollarSign className="w-4 h-4 text-cyan-400" />
                          <span>{bomExplosion.totalEstimatedMaterialCost.toFixed(2)} USD</span>
                        </div>
                      </div>
                    </div>

                    {/* Components Data Table */}
                    <div className="border border-zinc-800 overflow-x-auto">
                      <table className="w-full text-left text-xs border-collapse">
                        <thead>
                          <tr className="bg-zinc-950 text-zinc-400 border-b border-zinc-800 uppercase text-[10px] tracking-wider">
                            <th className="p-2.5">Material Code</th>
                            <th className="p-2.5">Material Name</th>
                            <th className="p-2.5 text-right">Req / Unit</th>
                            <th className="p-2.5 text-right">Total Req</th>
                            <th className="p-2.5 text-right">Stock</th>
                            <th className="p-2.5 text-center">Sufficiency</th>
                            <th className="p-2.5 text-right">Unit Cost</th>
                            <th className="p-2.5 text-right">Total Cost</th>
                            <th className="p-2.5">Cost Center</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-zinc-800/60 font-mono">
                          {bomExplosion.components.map((comp) => (
                            <tr key={comp.materialCode} className="hover:bg-zinc-800/40 transition">
                              <td className="p-2.5 font-bold text-cyan-300 text-[11px]">
                                {comp.materialCode}
                              </td>
                              <td className="p-2.5 text-zinc-300">
                                {comp.materialName}
                              </td>
                              <td className="p-2.5 text-right text-zinc-400">
                                {comp.requiredPerUnit} {comp.uom}
                              </td>
                              <td className="p-2.5 text-right text-zinc-200 font-bold">
                                {comp.totalRequiredQuantity} {comp.uom}
                              </td>
                              <td className="p-2.5 text-right text-zinc-300">
                                {comp.currentStock} {comp.uom}
                              </td>
                              <td className="p-2.5 text-center">
                                <span
                                  className={`px-2 py-0.5 text-[10px] uppercase font-bold border ${
                                    comp.stockSufficient
                                      ? 'bg-emerald-950/60 text-emerald-400 border-emerald-800'
                                      : 'bg-rose-950/60 text-rose-400 border-rose-800'
                                  }`}
                                >
                                  {comp.stockSufficient ? 'SUFFICIENT' : 'DEFICIT'}
                                </span>
                              </td>
                              <td className="p-2.5 text-right text-zinc-400">
                                ${comp.unitCost.toFixed(2)}
                              </td>
                              <td className="p-2.5 text-right text-zinc-200 font-bold">
                                ${comp.totalCost.toFixed(2)}
                              </td>
                              <td className="p-2.5 text-zinc-400 text-[10px]">
                                {comp.scrapCostCenter}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                ) : (
                  <div className="p-6 text-center text-zinc-500 text-xs uppercase">
                    {bomLoading ? 'Calculating BOM Explosion...' : 'Select product to simulate BOM explosion.'}
                  </div>
                )}
              </div>
            </div>

            {/* Right Col: Piece Production Output Recording & Backflushing */}
            <div className="space-y-6">
              <div className="bg-zinc-900 border border-zinc-800 p-5 space-y-4">
                <div className="flex items-center space-x-2 text-sm font-semibold tracking-wider text-zinc-100 uppercase border-b border-zinc-800 pb-2">
                  <Box className="w-4 h-4 text-cyan-400" />
                  <span>Backflush Output Dispatcher</span>
                </div>

                <form onSubmit={handleRecordBackflushing} className="space-y-3 text-xs">
                  <div>
                    <label className="block text-zinc-400 uppercase text-[10px] mb-1">Target Production Order</label>
                    <select
                      value={bfOrderId}
                      onChange={(e) => setBfOrderId(e.target.value)}
                      className="w-full bg-zinc-950 border border-zinc-700 text-zinc-100 p-2 text-xs font-mono focus:border-cyan-500 outline-none"
                    >
                      {orders.map((o) => (
                        <option key={o.id} value={o.id}>
                          {o.orderNumber} ({o.productCode}) - {o.status}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="block text-zinc-400 uppercase text-[10px] mb-1">Good Output Qty</label>
                      <input
                        type="number"
                        min="0"
                        value={bfGoodQty}
                        onChange={(e) => setBfGoodQty(Number(e.target.value))}
                        className="w-full bg-zinc-950 border border-zinc-700 text-zinc-100 p-2 text-xs font-mono focus:border-cyan-500 outline-none"
                      />
                    </div>
                    <div>
                      <label className="block text-zinc-400 uppercase text-[10px] mb-1">Scrap Pieces</label>
                      <input
                        type="number"
                        min="0"
                        value={bfScrapQty}
                        onChange={(e) => setBfScrapQty(Number(e.target.value))}
                        className="w-full bg-zinc-950 border border-zinc-700 text-zinc-100 p-2 text-xs font-mono focus:border-cyan-500 outline-none"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-zinc-400 uppercase text-[10px] mb-1">Scrap Reason Classification</label>
                    <select
                      value={bfScrapReason}
                      onChange={(e) => setBfScrapReason(e.target.value)}
                      className="w-full bg-zinc-950 border border-zinc-700 text-zinc-100 p-2 text-xs font-mono focus:border-cyan-500 outline-none"
                    >
                      <option value="SCRAP_DIMENSION_VARIANCE">DIMENSION OUT OF TOLERANCE</option>
                      <option value="SCRAP_THERMAL_WARPAGE">THERMAL WARPAGE / OVERHEAT</option>
                      <option value="SCRAP_SURFACE_BLEMISH">SURFACE DEFECT / ROUGHNESS</option>
                      <option value="SCRAP_MATERIAL_CRACK">MATERIAL CRACKING / FRACTURE</option>
                      <option value="SCRAP_TOOL_BREAKAGE">TOOL CHATTER / BREAKAGE</option>
                      <option value="SCRAP_OPERATOR_SETUP">SETUP / CALIBRATION REJECT</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-zinc-400 uppercase text-[10px] mb-1">Scrap Cost Center Routing</label>
                    <select
                      value={bfCostCenter}
                      onChange={(e) => setBfCostCenter(e.target.value)}
                      className="w-full bg-zinc-950 border border-zinc-700 text-zinc-100 p-2 text-xs font-mono focus:border-cyan-500 outline-none"
                    >
                      <option value="CC-MACHINING-01">CC-MACHINING-01 (Precision CNC Machining)</option>
                      <option value="CC-FOUNDRY-02">CC-FOUNDRY-02 (Casting & Heat Treatment)</option>
                      <option value="CC-ASSEMBLY-03">CC-ASSEMBLY-03 (Mechanical Assembly)</option>
                      <option value="CC-QUALITY-04">CC-QUALITY-04 (Metrology & Quality Lab)</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-zinc-400 uppercase text-[10px] mb-1">Batch / Material Lot #</label>
                    <input
                      type="text"
                      value={bfLotNumber}
                      onChange={(e) => setBfLotNumber(e.target.value)}
                      className="w-full bg-zinc-950 border border-zinc-700 text-zinc-100 p-2 text-xs font-mono focus:border-cyan-500 outline-none"
                    />
                  </div>

                  {/* Variance Simulation Toggle */}
                  <div className="pt-1">
                    <label className="flex items-center space-x-2 text-[11px] text-amber-300 cursor-pointer bg-amber-950/30 p-2 border border-amber-900/60">
                      <input
                        type="checkbox"
                        checked={bfSimulateVariance}
                        onChange={(e) => setBfSimulateVariance(e.target.checked)}
                        className="rounded-none bg-zinc-900 border-zinc-700 text-amber-500 focus:ring-0"
                      />
                      <span>Simulate &gt;5% Consumption Variance (+12%)</span>
                    </label>
                  </div>

                  <button
                    type="submit"
                    disabled={submittingBackflush || !bfOrderId}
                    className="w-full mt-2 py-2.5 bg-cyan-600 hover:bg-cyan-500 text-zinc-950 font-bold uppercase tracking-wider text-xs flex items-center justify-center space-x-2 transition shadow-lg shadow-cyan-950/30"
                  >
                    <Sliders className={`w-3.5 h-3.5 ${submittingBackflush ? 'animate-spin' : ''}`} />
                    <span>{submittingBackflush ? 'Backflushing BOM Stock...' : 'Execute Material Backflush'}</span>
                  </button>
                </form>
              </div>
            </div>
          </div>

          {/* Bottom Row: Tracked Materials Inventory & Consumption Audit Ledger */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Left: Materials Inventory */}
            <div className="space-y-3">
              <div className="flex items-center justify-between border-b border-zinc-800 pb-2">
                <div className="flex items-center space-x-2 text-sm font-semibold tracking-wider text-zinc-200 uppercase">
                  <Database className="w-4 h-4 text-cyan-400" />
                  <span>Tracked Raw Materials Catalog ({materials.length})</span>
                </div>
                <span className="text-xs text-zinc-500">LIVE WAREHOUSE ERP SYNC</span>
              </div>

              <div className="bg-zinc-900 border border-zinc-800 overflow-x-auto max-h-80 overflow-y-auto">
                <table className="w-full text-left text-xs border-collapse">
                  <thead className="sticky top-0 bg-zinc-950 z-10">
                    <tr className="text-zinc-400 border-b border-zinc-800 uppercase text-[10px] tracking-wider">
                      <th className="p-2.5">Material Code</th>
                      <th className="p-2.5">Description</th>
                      <th className="p-2.5 text-right">Current Stock</th>
                      <th className="p-2.5 text-right">Min Stock</th>
                      <th className="p-2.5 text-right">Unit Cost</th>
                      <th className="p-2.5">Cost Center</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-zinc-800/60 font-mono">
                    {materials.map((mat) => (
                      <tr key={mat.id} className="hover:bg-zinc-800/40 transition">
                        <td className="p-2.5 font-bold text-cyan-300 text-[11px]">
                          {mat.materialCode}
                        </td>
                        <td className="p-2.5 text-zinc-300">
                          {mat.materialName}
                        </td>
                        <td className="p-2.5 text-right font-bold">
                          <span className={mat.isLowStock ? 'text-rose-400 font-bold' : 'text-zinc-200'}>
                            {mat.currentStock} {mat.uom}
                          </span>
                        </td>
                        <td className="p-2.5 text-right text-zinc-500">
                          {mat.minimumStock} {mat.uom}
                        </td>
                        <td className="p-2.5 text-right text-zinc-300">
                          ${mat.standardCost.toFixed(2)}
                        </td>
                        <td className="p-2.5 text-zinc-400 text-[10px]">
                          {mat.scrapCostCenter}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Right: Material Consumption Ledger */}
            <div className="space-y-3">
              <div className="flex items-center justify-between border-b border-zinc-800 pb-2">
                <div className="flex items-center space-x-2 text-sm font-semibold tracking-wider text-zinc-200 uppercase">
                  <Clock className="w-4 h-4 text-cyan-400" />
                  <span>Consumption & Scrap Ledger ({consumptionLogs.length})</span>
                </div>
                <span className="text-xs text-zinc-500">THEORETICAL VS ACTUAL</span>
              </div>

              <div className="bg-zinc-900 border border-zinc-800 overflow-x-auto max-h-80 overflow-y-auto">
                <table className="w-full text-left text-xs border-collapse">
                  <thead className="sticky top-0 bg-zinc-950 z-10">
                    <tr className="text-zinc-400 border-b border-zinc-800 uppercase text-[10px] tracking-wider">
                      <th className="p-2.5">Time</th>
                      <th className="p-2.5">Material</th>
                      <th className="p-2.5 text-right">Theo / Act</th>
                      <th className="p-2.5 text-right">Variance</th>
                      <th className="p-2.5">Cost Center</th>
                      <th className="p-2.5 text-center">Alert</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-zinc-800/60 font-mono">
                    {consumptionLogs.slice(0, 15).map((log) => (
                      <tr key={log.id} className="hover:bg-zinc-800/40 transition">
                        <td className="p-2.5 text-zinc-400 text-[10px]">
                          {new Date(log.recordedAt).toLocaleTimeString()}
                        </td>
                        <td className="p-2.5 font-bold text-zinc-200 text-[11px]">
                          {log.materialCode}
                          <span className="block text-[9px] text-zinc-500">{log.lotNumber || 'NO LOT'}</span>
                        </td>
                        <td className="p-2.5 text-right text-zinc-300 text-[11px]">
                          {log.theoreticalQuantity} / <span className="font-bold">{log.actualQuantity}</span> {log.uom}
                        </td>
                        <td className="p-2.5 text-right font-bold text-[11px]">
                          <span className={log.varianceAlertTriggered ? 'text-rose-400' : 'text-zinc-300'}>
                            {log.variancePercentage > 0 ? '+' : ''}{log.variancePercentage.toFixed(1)}%
                          </span>
                        </td>
                        <td className="p-2.5 text-zinc-400 text-[10px]">
                          {log.scrapCostCenter}
                        </td>
                        <td className="p-2.5 text-center">
                          {log.varianceAlertTriggered ? (
                            <span className="px-1.5 py-0.5 text-[9px] bg-rose-950 text-rose-400 border border-rose-800 uppercase font-bold">
                              &gt;5% ALERT
                            </span>
                          ) : (
                            <span className="text-[10px] text-zinc-500">OK</span>
                          )}
                        </td>
                      </tr>
                    ))}
                    {consumptionLogs.length === 0 && (
                      <tr>
                        <td colSpan={6} className="p-6 text-center text-zinc-500 text-xs uppercase">
                          No consumption transactions recorded yet.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* TAB 2: ERP SCM CONNECTORS & ORDER SYNC */}
      {activeHubTab === 'erp-connectors' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Left 2 Cols: Enterprise Connectors */}
          <div className="lg:col-span-2 space-y-4">
            <div className="flex items-center justify-between border-b border-zinc-800 pb-2">
              <div className="flex items-center space-x-2 text-sm font-semibold tracking-wider text-zinc-200 uppercase">
                <Database className="w-4 h-4 text-cyan-400" />
                <span>Configured Enterprise Connectors ({connectors.length})</span>
              </div>
              <span className="text-xs text-zinc-500">POLL INTERVAL: 300s</span>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {connectors.map((c) => (
                <div
                  key={c.id}
                  className="bg-zinc-900/90 border border-zinc-800 p-4 space-y-3 relative hover:border-zinc-700 transition"
                >
                  <div className="flex justify-between items-start">
                    <div>
                      <div className="text-xs font-bold text-zinc-100 uppercase flex items-center space-x-2">
                        <span>{c.name}</span>
                      </div>
                      <span className="text-[10px] text-zinc-500 uppercase tracking-wider block mt-0.5">
                        {c.erpType} • {c.plantCode || 'GLOBAL'}
                      </span>
                    </div>
                    <span
                      className={`px-2 py-0.5 text-[10px] font-bold tracking-wider uppercase border ${
                        c.healthStatus === 'HEALTHY'
                          ? 'bg-emerald-950/60 text-emerald-400 border-emerald-800'
                          : 'bg-rose-950/60 text-rose-400 border-rose-800'
                      }`}
                    >
                      {c.healthStatus}
                    </span>
                  </div>

                  <div className="text-xs space-y-1 bg-zinc-950 p-2.5 border border-zinc-800/80">
                    <div className="flex justify-between text-zinc-400">
                      <span>Base URL:</span>
                      <span className="text-zinc-300 truncate max-w-[180px] font-mono text-[11px]">
                        {c.baseUrl}
                      </span>
                    </div>
                    <div className="flex justify-between text-zinc-400">
                      <span>Auth Scheme:</span>
                      <span className="text-zinc-300">{c.authType}</span>
                    </div>
                    <div className="flex justify-between text-zinc-400">
                      <span>Auto-Sync:</span>
                      <span className={c.isAutoSyncEnabled ? 'text-emerald-400' : 'text-zinc-500'}>
                        {c.isAutoSyncEnabled ? 'ACTIVE' : 'DISABLED'}
                      </span>
                    </div>
                    <div className="flex justify-between text-zinc-400">
                      <span>Last Sync:</span>
                      <span className="text-zinc-400 text-[10px]">
                        {c.lastSyncAt ? new Date(c.lastSyncAt).toLocaleTimeString() : 'NEVER'}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center space-x-2 pt-1">
                    <button
                      onClick={() => handleTestConnection(c.id)}
                      disabled={testingConnectorId === c.id}
                      className="flex-1 py-1.5 bg-zinc-800 hover:bg-zinc-700 text-[11px] text-zinc-200 border border-zinc-700 uppercase tracking-wider flex items-center justify-center space-x-1 transition"
                    >
                      <Activity className={`w-3.5 h-3.5 ${testingConnectorId === c.id ? 'animate-spin text-cyan-400' : ''}`} />
                      <span>Ping ERP</span>
                    </button>
                    <button
                      onClick={() => handleSyncInbound(c.id)}
                      disabled={syncingConnectorId === c.id}
                      className="flex-1 py-1.5 bg-cyan-950/70 hover:bg-cyan-900/80 text-[11px] text-cyan-300 border border-cyan-800 uppercase tracking-wider flex items-center justify-center space-x-1 transition"
                    >
                      <ArrowDownLeft className={`w-3.5 h-3.5 ${syncingConnectorId === c.id ? 'animate-spin' : ''}`} />
                      <span>Pull Orders</span>
                    </button>
                  </div>
                </div>
              ))}
            </div>

            {/* Sync History & Execution Logs */}
            <div className="space-y-3 pt-4">
              <div className="flex items-center justify-between border-b border-zinc-800 pb-2">
                <div className="flex items-center space-x-2 text-sm font-semibold tracking-wider text-zinc-200 uppercase">
                  <Clock className="w-4 h-4 text-cyan-400" />
                  <span>Recent ERP Sync Transactions</span>
                </div>
                <span className="text-xs text-zinc-500">{syncLogs.length} LOGGED EVENTS</span>
              </div>

              <div className="bg-zinc-900 border border-zinc-800 overflow-x-auto">
                <table className="w-full text-left text-xs border-collapse">
                  <thead>
                    <tr className="bg-zinc-950 text-zinc-400 border-b border-zinc-800 uppercase text-[10px] tracking-wider">
                      <th className="p-2.5">Timestamp</th>
                      <th className="p-2.5">Direction</th>
                      <th className="p-2.5">Connector / Plant</th>
                      <th className="p-2.5">Status</th>
                      <th className="p-2.5">Details</th>
                      <th className="p-2.5 text-right">Payload</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-zinc-800/60 font-mono">
                    {syncLogs.slice(0, 8).map((log) => (
                      <tr key={log.id} className="hover:bg-zinc-800/40 transition">
                        <td className="p-2.5 text-zinc-400 text-[11px]">
                          {new Date(log.syncedAt).toLocaleTimeString()}
                        </td>
                        <td className="p-2.5">
                          <span
                            className={`inline-flex items-center space-x-1 px-1.5 py-0.5 text-[10px] font-bold uppercase border ${
                              log.syncDirection === 'INBOUND_RELEASE'
                                ? 'bg-blue-950/60 text-blue-400 border-blue-800'
                                : log.syncDirection === 'OUTBOUND_CONFIRMATION'
                                ? 'bg-amber-950/60 text-amber-400 border-amber-800'
                                : 'bg-zinc-950 text-zinc-400 border-zinc-800'
                            }`}
                          >
                            {log.syncDirection === 'INBOUND_RELEASE' ? (
                              <ArrowDownLeft className="w-3 h-3" />
                            ) : (
                              <ArrowUpRight className="w-3 h-3" />
                            )}
                            <span>{log.syncDirection.replace('_', ' ')}</span>
                          </span>
                        </td>
                        <td className="p-2.5 text-zinc-300">
                          {log.connectorName || 'ERP Gateway'} ({log.plantCode || 'GLOBAL'})
                        </td>
                        <td className="p-2.5">
                          <span
                            className={`px-2 py-0.5 text-[10px] uppercase font-bold border ${
                              log.status === 'SUCCESS'
                                ? 'bg-emerald-950/50 text-emerald-400 border-emerald-800'
                                : 'bg-rose-950/50 text-rose-400 border-rose-800'
                            }`}
                          >
                            {log.status}
                          </span>
                        </td>
                        <td className="p-2.5 text-zinc-400 text-[11px] truncate max-w-[200px]">
                          {log.errorMessage || log.erpReferenceId || 'Payload synchronized successfully'}
                        </td>
                        <td className="p-2.5 text-right">
                          {(log.payloadJson || log.responseJson) && (
                            <button
                              onClick={() => setSelectedLogPayload(log.responseJson || log.payloadJson || '{}')}
                              className="px-2 py-1 bg-zinc-800 hover:bg-zinc-700 text-[10px] text-zinc-300 border border-zinc-700 uppercase"
                            >
                              Inspect
                            </button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          {/* Right Col: Outbound Confirmation Dispatcher & Feed */}
          <div className="space-y-6">
            {/* Dispatcher Box */}
            <div className="bg-zinc-900 border border-zinc-800 p-5 space-y-4">
              <div className="flex items-center space-x-2 text-sm font-semibold tracking-wider text-zinc-100 uppercase border-b border-zinc-800 pb-2">
                <Send className="w-4 h-4 text-cyan-400" />
                <span>Outbound ERP Confirmation</span>
              </div>

              <form onSubmit={handleSubmitConfirmation} className="space-y-3 text-xs">
                <div>
                  <label className="block text-zinc-400 uppercase text-[10px] mb-1">Select Production Order</label>
                  <select
                    value={selectedOrderId}
                    onChange={(e) => setSelectedOrderId(e.target.value)}
                    className="w-full bg-zinc-950 border border-zinc-700 text-zinc-100 p-2 text-xs font-mono focus:border-cyan-500 outline-none"
                  >
                    {orders.map((o) => (
                      <option key={o.id} value={o.id}>
                        {o.orderNumber} ({o.productCode}) - {o.status}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-zinc-400 uppercase text-[10px] mb-1">Yield Qty (Good)</label>
                    <input
                      type="number"
                      value={goodQty}
                      onChange={(e) => setGoodQty(Number(e.target.value))}
                      className="w-full bg-zinc-950 border border-zinc-700 text-zinc-100 p-2 text-xs font-mono focus:border-cyan-500 outline-none"
                    />
                  </div>
                  <div>
                    <label className="block text-zinc-400 uppercase text-[10px] mb-1">Scrap Qty</label>
                    <input
                      type="number"
                      value={scrapQty}
                      onChange={(e) => setScrapQty(Number(e.target.value))}
                      className="w-full bg-zinc-950 border border-zinc-700 text-zinc-100 p-2 text-xs font-mono focus:border-cyan-500 outline-none"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-zinc-400 uppercase text-[10px] mb-1">Scrap Reason Code (Optional)</label>
                  <select
                    value={scrapReason}
                    onChange={(e) => setScrapReason(e.target.value)}
                    className="w-full bg-zinc-950 border border-zinc-700 text-zinc-100 p-2 text-xs font-mono focus:border-cyan-500 outline-none"
                  >
                    <option value="">NONE / NORMAL</option>
                    <option value="SCRAP_DIMENSION_VARIANCE">DIMENSION OUT OF TOLERANCE</option>
                    <option value="SCRAP_THERMAL_WARPAGE">THERMAL WARPAGE</option>
                    <option value="SCRAP_SURFACE_BLEMISH">SURFACE DEFECT / ROUGHNESS</option>
                    <option value="SCRAP_MATERIAL_CRACK">MATERIAL CRACKING</option>
                  </select>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-zinc-400 uppercase text-[10px] mb-1">Labor Hours</label>
                    <input
                      type="number"
                      step="0.1"
                      value={laborHours}
                      onChange={(e) => setLaborHours(Number(e.target.value))}
                      className="w-full bg-zinc-950 border border-zinc-700 text-zinc-100 p-2 text-xs font-mono focus:border-cyan-500 outline-none"
                    />
                  </div>
                  <div>
                    <label className="block text-zinc-400 uppercase text-[10px] mb-1">Machine Hours</label>
                    <input
                      type="number"
                      step="0.1"
                      value={machineHours}
                      onChange={(e) => setMachineHours(Number(e.target.value))}
                      className="w-full bg-zinc-950 border border-zinc-700 text-zinc-100 p-2 text-xs font-mono focus:border-cyan-500 outline-none"
                    />
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={submittingConfirmation || !selectedOrderId}
                  className="w-full mt-2 py-2.5 bg-cyan-600 hover:bg-cyan-500 text-zinc-950 font-bold uppercase tracking-wider text-xs flex items-center justify-center space-x-2 transition shadow-lg shadow-cyan-950/30"
                >
                  <Send className={`w-3.5 h-3.5 ${submittingConfirmation ? 'animate-spin' : ''}`} />
                  <span>{submittingConfirmation ? 'Transmitting to ERP...' : 'Post Confirmation to ERP'}</span>
                </button>
              </form>
            </div>

            {/* Confirmations Feed */}
            <div className="bg-zinc-900 border border-zinc-800 p-4 space-y-3">
              <div className="flex items-center justify-between border-b border-zinc-800 pb-2">
                <span className="text-xs font-bold text-zinc-200 uppercase tracking-wider">
                  Confirmed ERP Documents ({confirmations.length})
                </span>
              </div>

              <div className="space-y-2 max-h-72 overflow-y-auto pr-1">
                {confirmations.map((conf) => (
                  <div key={conf.id} className="p-3 bg-zinc-950 border border-zinc-800/80 space-y-1.5 text-xs">
                    <div className="flex justify-between items-start">
                      <span className="font-bold text-cyan-300 font-mono">
                        {conf.erpDocumentNumber || conf.confirmationNumber}
                      </span>
                      <span
                        className={`px-1.5 py-0.5 text-[10px] font-bold uppercase border ${
                          conf.erpPostingStatus === 'POSTED'
                            ? 'bg-emerald-950 text-emerald-400 border-emerald-800'
                            : 'bg-amber-950 text-amber-400 border-amber-800'
                        }`}
                      >
                        {conf.erpPostingStatus}
                      </span>
                    </div>
                    <div className="text-zinc-400 text-[11px] flex justify-between">
                      <span>Order: {conf.orderNumber || conf.erpOrderId}</span>
                      <span className="text-zinc-300">Good: {conf.confirmedGoodQty} | Scrap: {conf.confirmedScrapQty}</span>
                    </div>
                    <div className="text-[10px] text-zinc-500 flex justify-between">
                      <span>Labor: {conf.laborHours}h | Machine: {conf.machineHours}h</span>
                      <span>{conf.postedAt ? new Date(conf.postedAt).toLocaleTimeString() : 'Pending'}</span>
                    </div>
                  </div>
                ))}
                {confirmations.length === 0 && (
                  <div className="text-center py-6 text-zinc-600 text-xs uppercase">
                    No ERP confirmations recorded yet.
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Payload Modal */}
      {selectedLogPayload && (
        <div className="fixed inset-0 bg-black/80 flex items-center justify-center p-4 z-50">
          <div className="bg-zinc-900 border border-zinc-700 max-w-2xl w-full p-5 space-y-3 font-mono shadow-2xl">
            <div className="flex justify-between items-center border-b border-zinc-800 pb-2">
              <span className="text-sm font-bold text-cyan-400 uppercase">ERP Payload / Response Inspector</span>
              <button onClick={() => setSelectedLogPayload(null)} className="text-zinc-400 hover:text-zinc-100">
                ✕
              </button>
            </div>
            <pre className="p-3 bg-zinc-950 border border-zinc-800 text-[11px] text-emerald-400 overflow-x-auto max-h-96">
              {JSON.stringify(JSON.parse(selectedLogPayload), null, 2)}
            </pre>
            <div className="text-right">
              <button
                onClick={() => setSelectedLogPayload(null)}
                className="px-4 py-1.5 bg-zinc-800 hover:bg-zinc-700 text-xs text-zinc-200 border border-zinc-700 uppercase"
              >
                Close Inspector
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
