import http from 'k6/http';
import { check, sleep, fail } from 'k6';
import { Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const EMAIL = __ENV.TEST_EMAIL || 'cliente@test.com';
const PASSWORD = __ENV.TEST_PASSWORD || 'Clave$ecreta123!';

const crearReservaTrend = new Trend('crear_reserva_duration');
const cancelarReservaTrend = new Trend('cancelar_reserva_duration');

export const options = {
  scenarios: {
    reserva_flow: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '20s', target: 5 },
        { duration: '60s', target: 10 },
        { duration: '30s', target: 5 },
        { duration: '10s', target: 0 },
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    crear_reserva_duration: ['p(95)<1000'],
    cancelar_reserva_duration: ['p(95)<800'],
  },
};

const HEADERS = { 'Content-Type': 'application/json' };

function login() {
  const res = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ email: EMAIL, password: PASSWORD, recaptchaToken: 'test-token' }),
    { headers: HEADERS, tags: { name: 'login' } }
  );
  if (res.status !== 200) return null;
  return res.json('token');
}

function authHeaders(token) {
  return { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` };
}

export default function () {
  const token = login();
  if (!token) {
    fail('Login falló — no se puede continuar el flujo');
  }

  // GET rutas públicas
  const rutasRes = http.get(`${BASE_URL}/api/rutas/public`, { tags: { name: 'listar_rutas' } });
  check(rutasRes, { 'rutas 200': (r) => r.status === 200 });

  const rutas = rutasRes.json();
  if (!rutas || rutas.length === 0) return;

  const rutaId = rutas[0].id;
  const fecha = new Date();
  fecha.setDate(fecha.getDate() + 3);
  const fechaStr = fecha.toISOString().split('T')[0];

  // POST crear reserva
  const crearRes = http.post(
    `${BASE_URL}/api/reservaciones`,
    JSON.stringify({
      rutaId,
      fecha: fechaStr,
      horaInicio: '09:00:00',
      cantPersonas: 1,
      participantes: [
        {
          primerNombre: 'Test',
          primerApellido: 'Usuario',
          tipoDocumento: 'cedula',
          documento: '999888777',
          edad: 30,
          cmAltura: 170,
          kgPeso: 70,
        },
      ],
    }),
    { headers: authHeaders(token), tags: { name: 'crear_reserva' } }
  );

  crearReservaTrend.add(crearRes.timings.duration);
  const reservaOk = check(crearRes, { 'reserva creada 201': (r) => r.status === 201 });
  if (!reservaOk) {
    console.error(`[crear_reserva] status=${crearRes.status} body=${crearRes.body}`);
  }

  // GET mis reservas
  const misRes = http.get(`${BASE_URL}/api/reservaciones/mias`, {
    headers: authHeaders(token),
    tags: { name: 'mis_reservas' },
  });
  check(misRes, { 'mis reservas 200': (r) => r.status === 200 });

  // PATCH cancelar (solo si se creó correctamente)
  if (reservaOk && crearRes.status === 201) {
    const reservaId = crearRes.json('id');
    const cancelarRes = http.patch(
      `${BASE_URL}/api/reservaciones/${reservaId}/cancelar`,
      null,
      { headers: authHeaders(token), tags: { name: 'cancelar_reserva' } }
    );
    cancelarReservaTrend.add(cancelarRes.timings.duration);
    check(cancelarRes, { 'reserva cancelada 200': (r) => r.status === 200 });
  }

  sleep(2);
}
