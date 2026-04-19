import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  scenarios: {
    rutas_publicas: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '20s', target: 5 },
        { duration: '40s', target: 15 },
        { duration: '20s', target: 0 },
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800', 'avg<400'],
    checks: ['rate>0.99'],
  },
};

export default function () {
  const responses = http.batch([
    ['GET', `${BASE_URL}/actuator/health`, null, { tags: { name: 'health' } }],
    ['GET', `${BASE_URL}/api/rutas/public`, null, { tags: { name: 'rutas_public' } }],
  ]);

  check(responses[0], {
    'health responde 200': (response) => response.status === 200,
  });

  check(responses[1], {
    'rutas publicas responde 200': (response) => response.status === 200,
    'rutas publicas devuelve json': (response) =>
      (response.headers['Content-Type'] || '').includes('application/json'),
  });

  sleep(1);
}
