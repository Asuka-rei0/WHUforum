import { defineNuxtConfig } from 'nuxt/config'

export default defineNuxtConfig({
  devServer: {
    host: '0.0.0.0',
    port: 3000,
  },
  ssr: true,
  modules: ['@nuxt/image'],
  runtimeConfig: {
    public: {
      apiBaseUrl: process.server
        ? process.env.NUXT_PUBLIC_API_BASE_URL_SSR ||
          process.env.NUXT_PUBLIC_API_BASE_URL ||
          'http://localhost:8080'
        : process.env.NUXT_PUBLIC_API_BASE_URL || 'http://localhost:8080',
      websocketUrl: process.env.NUXT_PUBLIC_WEBSOCKET_URL || '',
      websiteBaseUrl: process.env.NUXT_PUBLIC_WEBSITE_BASE_URL || 'http://localhost:3000',
    },
  },
  css: [
    'vditor/dist/index.css',
    '~/assets/fonts.css',
    '~/assets/global.css',
    '@icon-park/vue-next/styles/index.css',
  ],
  app: {
    pageTransition: { name: 'page', mode: 'out-in' },
    head: {
      title: '珞珈论坛',
      script: [
        {
          tagPriority: 'high',
          innerHTML: `
            (function () {
              try {
                const mode = localStorage.getItem('theme-mode');
                const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
                const theme = mode === 'dark' || mode === 'light' ? mode : (prefersDark ? 'dark' : 'light');
                document.documentElement.dataset.theme = theme;

                
                let themeColor = '#fff';
                let themeStatus = 'default';
                if (theme === 'dark') {
                  themeColor = '#333';
                  themeStatus = 'black-translucent';
                } else {
                  themeColor = '#ffffff';
                  themeStatus = 'default';
                }
                
                const androidMeta = document.createElement('meta');
                androidMeta.name = 'theme-color';
                androidMeta.content = themeColor;
                
                const iosMeta = document.createElement('meta');
                iosMeta.name = 'apple-mobile-web-app-status-bar-style';
                iosMeta.content = themeStatus;

                document.head.appendChild(androidMeta);
                document.head.appendChild(iosMeta);
              } catch (e) {
                console.warn('Theme initialization failed:', e);
              }
            })();
          `,
        },
      ],
      link: [
        {
          rel: 'icon',
          type: 'image/x-icon',
          href: '/favicon.ico',
        },
        {
          rel: 'apple-touch-icon',
          href: '/icon-180.png',
        },
        {
          rel: 'manifest',
          href: '/manifest.webmanifest',
        },
        // {
        //   rel: 'stylesheet',
        //   href: 'https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.7.2/css/all.min.css',
        //   referrerpolicy: 'no-referrer',
        // },
      ],
    },
    baseURL: '/',
    buildAssetsDir: '/_nuxt/',
  },
  vue: {
    compilerOptions: {
      isCustomElement: (tag) => ['l-hatch', 'l-hatch-spinner'].includes(tag),
    },
  },
  vite: {
    optimizeDeps: {},
    build: {},
  },
})
