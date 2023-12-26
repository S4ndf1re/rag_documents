const config = {
  content: ['./src/**/*.{html,js,svelte,ts}', './node_modules/flowbite-svelte/**/*.{html,js,svelte,ts}'],

  plugins: [require('flowbite/plugin')],

  darkMode: 'class',

  theme: {
    extend: {
      colors: {
        primary: {
          '50': '#e7fffa',
          '100': '#c2fff3',
          '200': '#8cffe9',
          '300': '#3dffda',
          '400': '#00ffd0',
          '500': '#00ffee',
          '600': '#00dae3',
          '700': '#00abb5',
          '800': '#008790',
          '900': '#006d77',
          '950': '#004a55',
        },
        secondary: {
          '50': '#f5f8f7',
          '100': '#dee9e5',
          '200': '#bcd3cb',
          '300': '#93b5aa',
          '400': '#6d9489',
          '500': '#52796f',
          '600': '#406159',
          '700': '#364f49',
          '800': '#2e413d',
          '900': '#293835',
          '950': '#141f1c',
        },
      }
    }
  }
};

module.exports = config;
