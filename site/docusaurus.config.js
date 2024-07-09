// @ts-check

import {themes as prismThemes} from 'prism-react-renderer';

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'RefTree',
  tagline: 'Visualize your Scala data structures',
  url: 'https://stanch.github.io',
  baseUrl: '/reftree',
  organizationName: 'stanch',
  projectName: 'reftree',
  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'throw',
  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          sidebarPath: './sidebars.js',
          path: '../site-gen/target/mdoc',
          editUrl: 'https://github.com/stanch/reftree/tree/main/docs/',
        },
        theme: {
          customCss: './src/css/custom.css',
        },
      }),
    ],
  ],

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      navbar: {
        title: 'RefTree',
        items: [
          {
            href: 'https://github.com/stanch/reftree',
            label: 'GitHub',
            position: 'right',
          },
        ],
      },
      footer: {
        links: [],
        copyright: `Copyright © 2014–${new Date().getFullYear()} Nick Stanchenko`
      },
      prism: {
        theme: prismThemes.github,
        darkTheme: prismThemes.dracula,
        // https://github.com/PrismJS/prism/issues/3458
        additionalLanguages: ['java', 'scala'],
      },
    }),
};

export default config;
