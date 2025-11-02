/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        sock: {
          primary: '#6750A4',
          secondary: '#625B71'
        }
      }
    }
  },
  plugins: []
};
