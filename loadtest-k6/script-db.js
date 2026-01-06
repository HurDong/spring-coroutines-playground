import http from 'k6/http';
import { check } from 'k6';

const PORT = __ENV.PORT || 8081;
const HOSTNAME = __ENV.HOSTNAME || 'host.docker.internal';
const BASE_URL = `http://${HOSTNAME}:${PORT}/api/v1`;

export const options = {
    scenarios: {
        db_stress: {
            executor: 'constant-vus',
            vus: __ENV.VUS || 100,
            duration: __ENV.DURATION || '30s',
            exec: 'dbTest',
        },
    },
    thresholds: {
        'http_req_failed': ['rate<0.01'], 
    },
};

export function dbTest() {
    const res = http.get(`${BASE_URL}/blocking-db`);
    check(res, { 'status is 200': (r) => r.status === 200 });
}
