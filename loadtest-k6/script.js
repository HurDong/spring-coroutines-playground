import http from 'k6/http';
import { check, sleep } from 'k6';

const PORT = __ENV.PORT || '8081';
const VUS = __ENV.VUS || 50;
const DURATION = __ENV.DURATION || '10s';
const TARGET_VUS = __ENV.TARGET_VUS || 1000;
const FANOUT = __ENV.FANOUT || '3';

const BASE_URL = `http://localhost:${PORT}/api/v1/aggregate?fanout=${FANOUT}&delayMs=200`;

export const options = {
  scenarios: {
    load: {
      executor: 'constant-vus',
      vus: parseInt(VUS),
      duration: DURATION,
      startTime: '0s',
    },
    stress: {
        executor: 'ramping-vus',
        startVUs: parseInt(VUS),
        stages: [
            { duration: DURATION, target: parseInt(TARGET_VUS) },
        ],
        startTime: '10s',
    }
  },
  thresholds: {
    http_req_duration: ['p(95)<1000', 'p(99)<2000'], // 95% of requests should be below 1s
    http_req_failed: ['rate<0.01'], // http errors should be less than 1%
  },
};

export default function () {
  const res = http.get(BASE_URL);
  
  check(res, {
    'status is 200': (r) => r.status === 200,
    'response has thread info': (r) => r.json('thread') !== undefined,
  });
  
  sleep(1);
}
