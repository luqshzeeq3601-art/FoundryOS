/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        industrial: {
          950: '#080808',
          900: '#0f0f11',
          850: '#151518',
          800: '#1d1d22',
          700: '#2b2b33',
          600: '#3e3e48',
          500: '#5c5c6b',
          400: '#8c8c9e',
          300: '#b4b4c4',
          200: '#dcdce5',
          100: '#f0f0f5',
          50: '#fcfcfe',
        },
        hazard: {
          red: '#FF2A2A',
          redDark: '#8F0E0E',
          amber: '#F59E0B',
          amberDark: '#8B5000',
        },
        terminal: {
          green: '#22C55E',
          greenGlow: '#4AF626',
          cyan: '#06B6D4',
        },
        substrate: {
          dark: '#0C0D0E',
          card: '#121417',
          border: '#262930',
          borderLight: '#3A3F4B',
        }
      },
      fontFamily: {
        mono: ['"JetBrains Mono"', 'ui-monospace', 'SFMono-Regular', 'Menlo', 'Monaco', 'Consolas', 'monospace'],
        sans: ['"Inter"', 'system-ui', '-apple-system', 'sans-serif'],
      },
      letterSpacing: {
        tighter: '-0.04em',
        widest: '0.15em',
        mega: '0.25em',
      },
      borderRadius: {
        none: '0px',
      }
    },
  },
  plugins: [],
}
