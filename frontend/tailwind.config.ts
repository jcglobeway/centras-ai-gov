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
        'DEFAULT': '6px',
        'sm':      '4px',
        'md':      '6px',
        'lg':      '8px',
        'xl':      '12px',
        '2xl':     '22px',
        'full':    '9999px',
      },
      colors: {
        bg: {
          base:           'var(--bg-base)',
          surface:        'var(--bg-surface)',
          elevated:       'var(--bg-elevated)',
          prominent:      'var(--bg-prominent)',
          border:         'var(--bg-border)',
          'border-subtle':'var(--bg-border-subtle)',
        },
        accent: {
          DEFAULT:     'var(--accent)',
          hover:       'var(--accent-hover)',
          interactive: 'var(--accent-interactive)',
          muted:       'var(--accent-muted)',
        },
        success: 'var(--success)',
        warning: 'var(--warning)',
        error:   'var(--error)',
        text: {
          primary:   'var(--text-primary)',
          secondary: 'var(--text-secondary)',
          muted:     'var(--text-muted)',
          subtle:    'var(--text-subtle)',
        },
      },
      fontFamily: {
        sans:        ["Noto Sans KR", "sans-serif"],
        mono:        ["IBM Plex Mono", "monospace"],
        inter:       ["Inter", "Noto Sans KR", "sans-serif"],
        'plex-mono': ["IBM Plex Mono", "monospace"],
      },
    },
  },
  plugins: [],
};

export default config;
