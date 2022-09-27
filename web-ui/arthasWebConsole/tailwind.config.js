/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      keyframes: {
        // "animate-spin-rev": {
        //   '0%': {
        //     transform: 'rotate(0deg)'
        //   },
        //   '100%': {
        //     transform: 'rotate(-360deg)'
        //   }
        // }
      },
      animation: {
        'spin-rev-pause':'0.3s linear 0s infinite reverse both pause spin',
        'spin-rev-running':'0.3s linear 0s infinite reverse both running spin'
      },
      
    }
  },
  plugins: [],
}
