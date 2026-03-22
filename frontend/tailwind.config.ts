import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./src/pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        // v3 RAG Admin dark theme
        bg: {
          base: "#0c0f16",
          surface: "#13171f",
          elevated: "#1a1f2a",
          border: "#222836",
        },
        accent: {
          DEFAULT: "#4b9eff",
          hover: "#3a8ef0",
          muted: "#1d4ed8",
        },
        success: "#00c47a",
        warning: "#f5a623",
        error: "#ff4560",
        text: {
          primary: "#dde2ec",
          secondary: "#8b93a8",
          muted: "#505868",
        },
      },
      fontFamily: {
        sans: ["Noto Sans KR", "sans-serif"],
        mono: ["JetBrains Mono", "monospace"],
      },
    },
  },
  plugins: [],
};

export default config;
