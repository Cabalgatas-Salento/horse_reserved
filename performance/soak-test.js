import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const SOAK_DURATION = __ENV.SOAK_DURATION || '30m';

const rutasTrend = new Trend('rutas_duration');
const chatbotTrend = new Trend('chatbot_duration');
const failRate = new Rate('failed_requests');

export const options = {
  scenarios: {
    soak: {
      executor: 'constant-vus',
      vus: 10,
      duration: SOAK_DURATION,
      gracefulStop: '30s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    failed_requests: ['rate<0.01'],
    rutas_duration: ['p(95)<800', 'p(99)<1500'],
    chatbot_duration: ['p(95)<600', 'p(99)<1200'],
  },
};

const HEADERS = { 'Content-Type': 'application/json' };

export default function () {
  // GET rutas públicas
  const rutasRes = http.get(`${BASE_URL}/api/rutas/public`, { tags: { name: 'rutas' } });
  rutasTrend.add(rutasRes.timings.duration);
  const rutasOk = check(rutasRes, { 'rutas 200': (r) => r.status === 200 });

  sleep(1);

  // POST chatbot
  const chatRes = http.post(
    `${BASE_URL}/api/chatbot/faq/ask`,
    JSON.stringify({ question: '¿Cuáles son los horarios?' }),
    { headers: HEADERS, tags: { name: 'chatbot' } }
  );
  chatbotTrend.add(chatRes.timings.duration);
  const chatOk = check(chatRes, { 'chatbot 200': (r) => r.status === 200 });

  sleep(1);

  // Actuator health (detecta memory leaks a través de degradación de respuesta)
  const healthRes = http.get(`${BASE_URL}/actuator/health`, { tags: { name: 'health' } });
  check(healthRes, { 'app UP': (r) => r.status === 200 });

  failRate.add(!rutasOk || !chatOk);
  sleep(3);
}
