import React, { useState, useRef, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { Building2, ChevronDown, Check, RefreshCw } from 'lucide-react';

export const PlantSwitcher: React.FC = () => {
  const { activePlant, authorizedPlants, switchPlant } = useAuth();
  const [isOpen, setIsOpen] = useState(false);
  const [isSwitching, setIsSwitching] = useState(false);
  const [switchError, setSwitchError] = useState<string | null>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };
    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setIsOpen(false);
    };
    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleEscape);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleEscape);
    };
  }, []);

  const handleSelectPlant = async (plantId: string) => {
    if (activePlant && activePlant.id === plantId) {
      setIsOpen(false);
      return;
    }

    try {
      setIsSwitching(true);
      setSwitchError(null);
      await switchPlant(plantId);
      setIsOpen(false);
    } catch {
      setSwitchError('Could not switch plant. Try again.');
    } finally {
      setIsSwitching(false);
    }
  };

  if (!activePlant && authorizedPlants.length === 0) {
    return null;
  }

  return (
    <div className="relative inline-block text-left" ref={dropdownRef}>
      <button
        type="button"
        onClick={() => setIsOpen(prev => !prev)}
        disabled={isSwitching || authorizedPlants.length <= 1}
        className={`flex items-center gap-2 px-2.5 py-1 text-xs font-mono border transition-colors ${
          isOpen
            ? 'bg-industrial-800 border-hazard-red text-white'
            : 'bg-industrial-900/80 border-substrate-border text-industrial-200 hover:border-industrial-500 hover:text-white'
        } ${authorizedPlants.length <= 1 ? 'cursor-default' : 'cursor-pointer'}`}
        aria-haspopup={authorizedPlants.length > 1 ? 'listbox' : undefined}
        aria-expanded={authorizedPlants.length > 1 ? isOpen : undefined}
        aria-label={`Active plant: ${activePlant ? `${activePlant.code}, ${activePlant.name}` : 'none selected'}${authorizedPlants.length > 1 ? '. Change plant' : ''}`}
      >
        <Building2 size={13} className="text-hazard-red shrink-0" />
        <div className="flex flex-col items-start leading-tight">
          <div className="flex items-center gap-1.5">
            <span className="font-bold tracking-wider text-white">
              {activePlant ? activePlant.code : 'SELECT PLANT'}
            </span>
            {activePlant?.status === 'ACTIVE' && (
              <span className="w-1.5 h-1.5 bg-terminal-green" />
            )}
          </div>
          {activePlant && (
            <span className="text-xs text-industrial-400 truncate max-w-[120px] sm:max-w-[160px]">
              {activePlant.name}
            </span>
          )}
        </div>

        {isSwitching ? (
          <RefreshCw size={12} className="animate-spin text-industrial-400 ml-1 shrink-0" />
        ) : authorizedPlants.length > 1 ? (
          <ChevronDown size={12} className={`text-industrial-400 ml-1 shrink-0 transition-transform ${isOpen ? 'rotate-180' : ''}`} />
        ) : null}
      </button>

      {isOpen && authorizedPlants.length > 1 && (
        <div className="absolute left-0 mt-1 w-72 bg-substrate-dark border border-substrate-border shadow-2xl z-50 py-1">
          <div className="px-3 py-1.5 border-b border-substrate-border text-xs font-mono text-industrial-400 uppercase tracking-wider flex justify-between items-center">
            <span>Plants ({authorizedPlants.length})</span>

          </div>

          {switchError && (
            <p role="alert" className="px-3 py-2 text-xs text-hazard-red border-b border-substrate-border">
              {switchError}
            </p>
          )}
          <div role="listbox" aria-label="Authorized plants" className="max-h-60 overflow-y-auto divide-y divide-substrate-border/40">
            {authorizedPlants.map((plant) => {
              const isSelected = activePlant?.id === plant.id;
              return (
                <button
                  key={plant.id}
                  type="button"
                  role="option"
                  aria-selected={isSelected}
                  onClick={() => handleSelectPlant(plant.id)}
                  disabled={isSwitching}
                  className={`w-full text-left px-3 py-2 flex items-center justify-between transition-colors ${
                    isSelected
                      ? 'bg-industrial-800 text-white'
                      : 'text-industrial-300 hover:bg-industrial-850 hover:text-white'
                  }`}
                >
                  <div className="flex flex-col">
                    <div className="flex items-center gap-2">
                      <span className="font-mono text-xs font-bold">{plant.code}</span>
                      <span className={`text-xs px-1 py-0.5 border ${
                        plant.status === 'ACTIVE'
                          ? 'border-terminal-green/40 text-terminal-green'
                          : 'border-hazard-amber/40 text-hazard-amber'
                      }`}>
                        {plant.status}
                      </span>
                    </div>
                    <span className="text-xs text-industrial-400 font-sans mt-0.5">{plant.name}</span>
                    {plant.timezone && (
                      <span className="text-xs font-mono text-industrial-500">{plant.timezone}</span>
                    )}
                  </div>

                  {isSelected && (
                    <Check size={14} className="text-terminal-green shrink-0 ml-2" />
                  )}
                </button>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
};
