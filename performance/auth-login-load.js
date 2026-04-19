// Requiere backend con perfil test: ./gradlew bootRun --args='--spring.profiles.active=test'
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const failRate = new Rate('failed_requests');

export const options = {
  scenarios: {
    auth_login: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '20s', target: 10 },
        { duration: '40s', target: 20 },
        { duration: '20s', target: 0 },
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<500', 'avg<250'],
    failed_requests: ['rate<0.01'],
  },
};

const PAYLOAD = JSON.stringify({
  email: 'cliente@test.com',
  password: 'Clave$ecreta123!',
  recaptchaToken: 'test-token',
});

const HEADERS = { 'Content-Type': 'application/json' };

export default function () {
  const res = http.post(`${BASE_URL}/api/auth/login`, PAYLOAD, {
    headers: HEADERS,
    tags: { name: 'login' },
  });

  const ok = check(res, {
    'login status 200 o 401': (r) => r.status === 200 || r.status === 401,
    'responde en menos de 1s': (r) => r.timings.duration < 1000,
  });

  failRate.add(!ok);
  sleep(1);
}
