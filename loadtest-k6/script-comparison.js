import http from 'k6/http';
import { check, sleep } from 'k6';

const TARGET_ENV = __ENV.TARGET_ENV || 'blocking'; // 'blocking' or 'non-blocking'
const HOSTNAME = __ENV.HOSTNAME || 'localhost';

const CONFIG = {
    blocking: {
        url: `http://${HOSTNAME}:8081/api/v1/blocking-db`,
        vus: 300, 
    },
    'non-blocking': {
        url: `http://${HOSTNAME}:8082/api/v1/non-blocking-db`,
        vus: 1000, 
    }
};

const currentConfig = CONFIG[TARGET_ENV];

export const options = {
    scenarios: {
        comparison_test: {
            executor: 'constant-vus',
            vus: currentConfig.vus,
            duration: '15s', // Increased duration to see stabilization
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<2000'], // Latency threshold
        http_req_failed: ['rate<0.01'], 
    },
};

export default function () {
    const res = http.get(currentConfig.url);

    check(res, {
        'status is 200': (r) => r.status === 200,
    });
}
