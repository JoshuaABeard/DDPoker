import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Enable server-side rendering for dynamic data fetching
  // (Static export disabled to support API integration with fresh data)

  // Disable image optimization for development
  images: {
    unoptimized: true,
  },
};

export default nextConfig;
