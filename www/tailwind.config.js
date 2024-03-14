/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./src/**/*.html"],
  theme: {
    fontFamily: {
      sans: ["Poppins", "sans-serif"],
    },
    extend: {
      colors: {
        primary: "#131417",
        secondary: "#F2F2F2",
        muted: "#E4E4E7",
      },
    },
  },
  plugins: [],
};
