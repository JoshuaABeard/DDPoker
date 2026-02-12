import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Static export configuration for Docker deployment
  output: 'export',

  // Add trailing slashes to URLs for static hosting compatibility
  trailingSlash: true,

  // Disable image optimization (not available in static export)
  images: {
    unoptimized: true,
  },

  // Base path (empty for root deployment)
  basePath: '',

  // Asset prefix (empty for same-origin serving)
  assetPrefix: '',
};

export default nextConfig;
