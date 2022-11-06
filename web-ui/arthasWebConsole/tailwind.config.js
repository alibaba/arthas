/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "all/**/*.{vue,ts,css}",
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
