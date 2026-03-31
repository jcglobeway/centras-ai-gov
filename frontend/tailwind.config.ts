import type { Config } from "tailwindcss";

const config: Config = {
  darkMode: 'class',
  content: [
    "./src/pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      borderRadius: {
        'DEFAULT': '0.125rem',
        'sm':      '0.125rem',
        'md':      '0.25rem',
        'lg':      '0.25rem',
        'xl':      '0.5rem',
        '2xl':     '0.75rem',
        'full':    '9999px',
      },
      colors: {
        bg: {
          base:      'var(--bg-base)',
          surface:   'var(--bg-surface)',
          elevated:  'var(--bg-elevated)',
          prominent: 'var(--bg-prominent)',
          border:    'var(--bg-border)',
        },
        accent: {
          DEFAULT: 'var(--accent)',
          hover:   '#3a8ef0',
          muted:   '#1d4ed8',
        },
        success: 'var(--success)',
        warning: 'var(--warning)',
        error:   'var(--error)',
        text: {
          primary:   'var(--text-primary)',
          secondary: 'var(--text-secondary)',
          muted:     'var(--text-muted)',
        },
      },
      fontFamily: {
        sans:        ["Noto Sans KR", "sans-serif"],
        mono:        ["IBM Plex Mono", "monospace"],
        inter:       ["Inter", "sans-serif"],
        'plex-mono': ["IBM Plex Mono", "monospace"],
      },
    },
  },
  plugins: [],
};

export default config;
