import http from 'k6/http';
import { check } from 'k6';

const PORT = __ENV.PORT || 8081;
const HOSTNAME = __ENV.HOSTNAME || 'host.docker.internal';
const BASE_URL = `http://${HOSTNAME}:${PORT}/api/v1`;

export const options = {
    scenarios: {
        attack: {
            executor: 'constant-vus',
            vus: 200, // Flood the server with requests to the faulty service
            duration: '15s',
            exec: 'attackTest',
            gracefulStop: '0s', // Stop immediately
        },
        victim: {
            executor: 'constant-arrival-rate',
            rate: 1, // Try to call health check once per second
            timeUnit: '1s',
            duration: '15s',
            preAllocatedVUs: 5,
            exec: 'victimTest',
        },
    },
    thresholds: {
        'http_req_duration{scenario:victim}': ['p(95)<100'], // Health check should be fast (<100ms)
    },
};

export function attackTest() {
    // This request takes 5s
    http.get(`${BASE_URL}/faulty-service`);
}

export function victimTest() {
    // This request should be instant
    const res = http.get(`${BASE_URL}/health`);
    check(res, { 'Health Check OK': (r) => r.status === 200 });
}
