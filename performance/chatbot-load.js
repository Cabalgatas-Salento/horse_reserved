import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const failRate = new Rate('failed_requests');

export const options = {
  scenarios: {
    chatbot_load: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '30s', target: 20 },
        { duration: '90s', target: 30 },
        { duration: '30s', target: 20 },
        { duration: '30s', target: 0 },
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<600'],
    failed_requests: ['rate<0.01'],
  },
};

const PREGUNTAS = [
  '¿Cómo hago una reserva?',
  '¿Cuáles son los horarios disponibles?',
  '¿Qué incluye el precio?',
  '¿Puedo cancelar mi reserva?',
  '¿Qué debo llevar a la cabalgata?',
  '¿Hay restricciones de edad o peso?',
  '¿Cómo llego al punto de encuentro?',
];

const HEADERS = { 'Content-Type': 'application/json' };

export default function () {
  const pregunta = PREGUNTAS[Math.floor(Math.random() * PREGUNTAS.length)];

  const res = http.post(
    `${BASE_URL}/api/chatbot/faq/ask`,
    JSON.stringify({ question: pregunta }),
    { headers: HEADERS, tags: { name: 'chatbot_ask' } }
  );

  const ok = check(res, {
    'chatbot responde 200': (r) => r.status === 200,
    'respuesta tiene answer': (r) => r.json('answer') !== undefined,
    'responde en menos de 1s': (r) => r.timings.duration < 1000,
  });

  failRate.add(!ok);
  sleep(1);
}
