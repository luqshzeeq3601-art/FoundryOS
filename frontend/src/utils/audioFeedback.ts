// Web Audio API & Haptic Vibration Utility for Industrial Barcode Scanning

import { BarcodeValidationStatus } from '../types';

let audioCtx: AudioContext | null = null;

const getAudioContext = (): AudioContext | null => {
  if (typeof window === 'undefined') return null;
  if (!audioCtx) {
    const AudioContextClass = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
    if (AudioContextClass) {
      audioCtx = new AudioContextClass();
    }
  }
  if (audioCtx && audioCtx.state === 'suspended') {
    audioCtx.resume();
  }
  return audioCtx;
};

export const playSound = (type: 'success' | 'error' | 'warning' | 'scan') => {
  try {
    const ctx = getAudioContext();
    if (!ctx) return;

    const now = ctx.currentTime;
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();

    osc.connect(gain);
    gain.connect(ctx.destination);

    if (type === 'success' || type === 'scan') {
      // Crisp high harmonic double-chime (1760 Hz to 2637 Hz)
      osc.type = 'sine';
      osc.frequency.setValueAtTime(1760, now);
      osc.frequency.exponentialRampToValueAtTime(2637, now + 0.08);

      gain.gain.setValueAtTime(0.3, now);
      gain.gain.exponentialRampToValueAtTime(0.01, now + 0.12);

      osc.start(now);
      osc.stop(now + 0.12);
    } else if (type === 'error') {
      // Low dual sawtooth buzz (220 Hz / 164 Hz)
      osc.type = 'sawtooth';
      osc.frequency.setValueAtTime(220, now);
      osc.frequency.linearRampToValueAtTime(164, now + 0.2);

      gain.gain.setValueAtTime(0.4, now);
      gain.gain.exponentialRampToValueAtTime(0.01, now + 0.25);

      osc.start(now);
      osc.stop(now + 0.25);
    } else if (type === 'warning') {
      // Mid-pitch pulse (440 Hz)
      osc.type = 'triangle';
      osc.frequency.setValueAtTime(440, now);
      osc.frequency.setValueAtTime(554, now + 0.08);

      gain.gain.setValueAtTime(0.35, now);
      gain.gain.exponentialRampToValueAtTime(0.01, now + 0.18);

      osc.start(now);
      osc.stop(now + 0.18);
    }
  } catch (err) {
    console.debug('Audio feedback error (browser policy):', err);
  }
};

export const triggerHaptic = (type: 'success' | 'error' | 'warning' | 'scan') => {
  if (typeof navigator !== 'undefined' && 'vibrate' in navigator) {
    try {
      if (type === 'success' || type === 'scan') {
        navigator.vibrate(50); // 50ms quick pulse
      } else if (type === 'error') {
        navigator.vibrate([100, 50, 100]); // double error buzz
      } else if (type === 'warning') {
        navigator.vibrate([80, 40, 80]); // warning pulse
      }
    } catch (err) {
      console.debug('Haptic feedback error:', err);
    }
  }
};

export const triggerScanFeedback = (status: BarcodeValidationStatus) => {
  if (status === 'VALID') {
    playSound('success');
    triggerHaptic('success');
  } else if (status === 'INVALID_BOM' || status === 'EXPIRED' || status === 'QUARANTINED') {
    playSound('warning');
    triggerHaptic('warning');
  } else {
    playSound('error');
    triggerHaptic('error');
  }
};
