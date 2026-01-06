import http from 'k6/http';
import { check } from 'k6';

const PORT = __ENV.PORT || 8081;
const HOSTNAME = __ENV.HOSTNAME || 'host.docker.internal';
const BASE_URL = `http://${HOSTNAME}:${PORT}/api/v1`;

export const options = {
    scenarios: {
        io_heavy: {
            executor: 'constant-vus',
            vus: __ENV.IO_VUS || 100,
            duration: __ENV.DURATION || '30s',
            exec: 'ioTest',
        },
        cpu_heavy: {
            executor: 'constant-vus',
            vus: __ENV.CPU_VUS || 10,
            duration: __ENV.DURATION || '30s',
            exec: 'cpuTest',
        },
    },
    thresholds: {
        'http_req_failed': ['rate<0.01'], 
    },
};

export function ioTest() {
    const res = http.get(`${BASE_URL}/simulate-delay`);
    check(res, { 'status is 200': (r) => r.status === 200 });
}

export function cpuTest() {
    const res = http.get(`${BASE_URL}/simulate-cpu`);
    check(res, { 'status is 200': (r) => r.status === 200 });
}
