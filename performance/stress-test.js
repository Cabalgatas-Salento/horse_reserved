import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const failRate = new Rate('failed_requests');

export const options = {
  scenarios: {
    stress: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '30s', target: 20 },
        { duration: '30s', target: 60 },
        { duration: '30s', target: 100 },
        { duration: '30s', target: 0 },
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    http_req_duration: ['p(99)<3000'],
    failed_requests: ['rate<0.10'],
  },
};

const HEADERS = { 'Content-Type': 'application/json' };

export default function () {
  const responses = http.batch([
    ['GET', `${BASE_URL}/api/rutas/public`, null, { tags: { name: 'rutas' } }],
    [
      'POST',
      `${BASE_URL}/api/chatbot/faq/ask`,
      JSON.stringify({ question: '¿Cómo reservo?' }),
      { headers: HEADERS, tags: { name: 'chatbot' } },
    ],
    ['GET', `${BASE_URL}/actuator/health`, null, { tags: { name: 'health' } }],
  ]);

  const ok = check(responses[0], { 'rutas < 3s': (r) => r.timings.duration < 3000 }) &&
             check(responses[1], { 'chatbot < 3s': (r) => r.timings.duration < 3000 }) &&
             check(responses[2], { 'health UP': (r) => r.status === 200 });

  failRate.add(!ok);
  sleep(0.5);
}
