/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./ui/index.html",
    "./index.html",
    "./src/*.{vue,js,ts,jsx,tsx}",
    "./ui/src/**/*.{vue,js,ts,jsx,tsx}",
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
