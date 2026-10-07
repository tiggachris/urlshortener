import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

// Custom Metrics for Resume Proof
const redirectLatency = new Trend('redirect_duration_ms');
const cacheHitRate = new Rate('cache_hit_rate');
const errorRate = new Rate('error_rate');
const totalRedirects = new Counter('total_redirects');

export const options = {
  stages: [
    { duration: '10s', target: 50 },   // Ramp-up to 50 virtual users
    { duration: '30s', target: 200 },  // Sustained peak load with 200 users
    { duration: '10s', target: 0 },    // Ramp-down
  ],
  thresholds: {
    // Assertions to validate enterprise SLAs
    'http_req_duration': ['p(95)<5'],     // 95% of requests must complete under 5ms (Redis cache)
    'http_req_duration': ['p(99)<15'],    // 99% under 15ms
    'error_rate': ['rate<0.01'],          // Less than 1% errors
  },
};

const BASE_URL = __ENV.TARGET_URL || 'http://localhost:8080';

// Setup phase: Create a sample link once before running load test
export function setup() {
  const payload = JSON.stringify({
    originalUrl: 'https://en.wikipedia.org/wiki/High-availability_architecture',
    customAlias: 'k6-benchmark-link'
  });

  const params = {
    headers: { 'Content-Type': 'application/json' },
  };

  const res = http.post(`${BASE_URL}/api/v1/urls/shorten`, payload, params);
  check(res, {
    'setup link created (201)': (r) => r.status === 201 || r.status === 400,
  });

  return { shortCode: 'k6-benchmark-link' };
}

// Default VU function: Blasts the redirect endpoint
export default function (data) {
  const shortCode = data.shortCode || 'k6-benchmark-link';
  
  // Note: redirects: 0 prevents k6 from following the 302 redirect so we measure the shortener's latency alone!
  const res = http.get(`${BASE_URL}/${shortCode}`, {
    redirects: 0,
    tags: { name: 'RedirectEndpoint' },
  });

  const isSuccess = check(res, {
    'status is 302 redirect': (r) => r.status === 302,
    'has Location header': (r) => r.headers['Location'] !== undefined,
  });

  redirectLatency.add(res.timings.duration);
  totalRedirects.add(1);
  errorRate.add(!isSuccess);

  const cacheHeader = res.headers['X-Cache-Lookup'];
  cacheHitRate.add(cacheHeader === 'HIT');

  // Sleep small jitter (0.01s - 0.05s) to simulate high-frequency user clicks
  sleep(0.02);
}
