/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    // "all/ui/ui/index.html",
    // "all/ui/index.html",
    "all/**/*.{vue,js,ts,jsx,tsx,html}",
  ],
  theme: {
    extend: {
      keyframes: {
      },
      animation: {
        'spin-rev-pause':'0.3s linear 0s infinite reverse both pause spin',
        'spin-rev-running':'0.3s linear 0s infinite reverse both running spin'
      },
      
    },
    daisyui: {
      themes: ["corporate"],
    },
  },
  plugins: [
    require("daisyui")
  ],
}
