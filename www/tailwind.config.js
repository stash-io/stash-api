/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    './src/**/*.html',
  ],
  theme: {
    fontFamily: {
        sans: ['Poppins', 'sans-serif'],
    },
    fontSize: {
        base: ['16px', '24px'],
    },
    extend: {
        colors: {
            primary: {
                light: '#131417',
                DEFAULT: '#131417',
                dark: '#F2F2F2',
            },
            secondary: {
                light: '#F2F2F2',
                DEFAULT: '#F2F2F2',
                dark: '#131417',
            },
        },
    },
  },
  plugins: [],
}

