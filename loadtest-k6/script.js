import http from 'k6/http';
import { check, sleep } from 'k6';

const PORT = __ENV.PORT || '8081';
const BASE_URL = `http://localhost:${PORT}/api/v1/aggregate?fanout=3&delayMs=200`;

export const options = {
  scenarios: {
    load: {
      executor: 'constant-vus',
      vus: 50,
      duration: '10s',
      startTime: '0s',
    },
    stress: {
        executor: 'ramping-vus',
        startVUs: 50,
        stages: [
            { duration: '1m', target: 1000 },
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
