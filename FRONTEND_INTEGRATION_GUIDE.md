# Guía de integración frontend — Cuando Llega Pro

Este documento describe el contrato **real** del backend actual para que un cliente web o móvil pueda implementarse sin inferencias. La API está escrita en Spring Boot y el servidor local usa por defecto `http://localhost:8400`.

> Regla de lectura: los términos y campos que provienen de MGP permanecen en español (`bandera`, `codigoLinea`, `arribo`, etc.). Los contratos nuevos del mapa persistido usan nombres en inglés, salvo donde se indica una compatibilidad heredada.

## 1. Arquitectura funcional

El backend tiene tres grupos de datos con responsabilidades diferentes:

| Área | Fuente | Persistencia | Uso recomendado en frontend |
|---|---|---:|---|
| Catálogo de mapa | PostgreSQL, cargado desde `paradas_mgp.json` y sincronizado | Sí | Pintar líneas, paradas físicas y sentidos en el mapa |
| Catálogo heredado | Proxy de MGP, con caché | Parcialmente cacheado | Flujo tradicional línea → calle → intersección → parada y recorridos |
| Telemetría de arribos | Proxy de MGP | No como fuente de verdad; memoria/caché temporal | Próximos colectivos, estado de GPS y tiempo de llegada |

La separación es importante: **no intentar obtener arribos desde los endpoints de mapa**, ni usar la telemetría para descubrir paradas. El mapa usa catálogo estático; los arribos usan telemetría dinámica.

## 2. Base URL, formato y autenticación

### Base URL

```text
Desarrollo local en la misma máquina: http://localhost:8400
Emulador Android:                   http://10.0.2.2:8400
Dispositivo físico en LAN:          http://<IP-LAN-DE-LA-PC>:8400
```

Todas las fechas son ISO-8601 UTC, por ejemplo `2026-09-21T22:00:00Z`.

### Autenticación JWT

Salvo estas rutas públicas, toda la API requiere JWT:

- `GET /healthz`
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`

Header para rutas protegidas:

```http
Authorization: Bearer <token>
Accept: application/json
```

El token se recibe en `token` al registrar o iniciar sesión. `tokenType` normalmente es `Bearer`; el cliente debe anteponerlo en el header anterior. El frontend debe conservar el token de manera segura y tratar un `401` como sesión expirada/no válida.

### CORS

En desarrollo acepta orígenes locales comunes (`localhost`, `127.0.0.1`, `10.0.2.2` y `192.168.*.*`). Para producción se configura `app.cors.allowed-origins` en backend. Métodos permitidos: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`.

### Errores HTTP

Los errores son `application/problem+json`, siguiendo RFC 7807. Estructura base:

```json
{
  "type": "about:blank",
  "title": "Resource Not Found",
  "status": 404,
  "detail": "Stop not found: P9999",
  "timestamp": "2026-09-21T22:00:00Z"
}
```

| Estado | Significado |
|---:|---|
| `400` | Parámetro inválido o validación de body fallida. Puede incluir `errors`, una lista de `{ field, rejectedValue, message }`. |
| `401` | Falta JWT o es inválido. |
| `403` | JWT válido sin permiso suficiente; la administración exige rol `ADMIN`. |
| `404` | Recurso inexistente o no perteneciente al usuario autenticado. |
| `502` | El proxy/MGP no respondió y no existe fallback utilizable. |
| `500` | Error inesperado. |

## 3. Autenticación y usuario

### Registrar usuario

```http
POST /api/v1/auth/register
Content-Type: application/json
```

```json
{
  "email": "ana@example.com",
  "password": "minimo-seis-caracteres",
  "fullName": "Ana Pérez"
}
```

Validaciones: `email` requerido y válido, `password` requerida y mínimo seis caracteres, `fullName` requerido.

Respuesta `201`:

```json
{
  "token": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "user": {
    "id": 1,
    "email": "ana@example.com",
    "fullName": "Ana Pérez",
    "role": "USER",
    "createdAt": "2026-09-21T22:00:00Z",
    "presetsCount": 0
  }
}
```

`expiresIn` se expresa en milisegundos.

### Iniciar sesión

```http
POST /api/v1/auth/login
Content-Type: application/json
```

```json
{
  "email": "ana@example.com",
  "password": "minimo-seis-caracteres"
}
```

Respuesta `200`: misma forma que el registro.

## 4. Presets del usuario

Todos requieren JWT. Un preset guarda una combinación línea, parada, sentido y configuración visual/notificaciones. Los nombres de campos heredados están en español y se deben enviar exactamente así.

### Modelo `PresetConfig`

```json
{
  "alias": "Ir a casa",
  "icon": "bus",
  "color": "#2E7D32",
  "activeSchedule": {
    "startTime": "07:00",
    "endTime": "22:00",
    "activeDays": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"]
  },
  "notificationSettings": {
    "notifyArrival": true,
    "alertMinutesBefore": 5,
    "soundEnabled": true
  }
}
```

`activeSchedule` y `notificationSettings` pueden ser `null` si el producto lo permite; `config` completo es obligatorio al crear/editar.

### Rutas

| Método | Ruta | Body | Respuesta |
|---|---|---|---|
| `GET` | `/api/v1/presets` | — | `PresetListDTO[]` |
| `GET` | `/api/v1/presets/{id}` | — | `PresetDetailDTO` |
| `POST` | `/api/v1/presets` | `PresetRequestDTO` | `201 PresetDetailDTO` |
| `PUT` | `/api/v1/presets/{id}` | `PresetRequestDTO` | `PresetDetailDTO` |
| `DELETE` | `/api/v1/presets/{id}` | — | `204` sin body |

Body de creación/edición:

```json
{
  "codigoLinea": "521",
  "identificadorParada": "P3613",
  "bandera": "AL BOSQUE",
  "config": {
    "alias": "Ir a casa",
    "icon": "bus",
    "color": "#2E7D32",
    "activeSchedule": null,
    "notificationSettings": null
  }
}
```

`codigoLinea` e `identificadorParada` no pueden ser vacíos. `bandera` es opcional: `null` significa que los arribos del preset no se filtran por sentido.

Resumen de preset:

```json
{
  "id": 12,
  "codigoLinea": "521",
  "identificadorParada": "P3613",
  "bandera": "AL BOSQUE",
  "alias": "Ir a casa",
  "icon": "bus",
  "color": "#2E7D32"
}
```

Detalle de preset agrega `userId`, `config` completo y `createdAt`.

## 5. Dashboard autenticado

```http
GET /api/v1/me/dashboard
Authorization: Bearer <token>
```

Obtiene todos los presets del usuario y su telemetría en un snapshot. Es útil para una pantalla principal; no reemplaza el stream del mapa.

```json
{
  "userEmail": "ana@example.com",
  "generatedAt": "2026-09-21T22:00:00Z",
  "totalPresets": 1,
  "presets": [
    {
      "presetId": 12,
      "codigoLinea": "521",
      "identificadorParada": "P3613",
      "bandera": "AL BOSQUE",
      "config": { "alias": "Ir a casa" },
      "telemetry": { "...": "ArrivalResponseDTO; ver sección 7" }
    }
  ]
}
```

## 6. Mapa persistido: API recomendada para el nuevo mapa

Prefijo: `/api/v1/transit/map`. Todas estas rutas requieren JWT.

Este catálogo está en PostgreSQL y no depende de que MGP esté disponible en el momento de pintar el mapa. Sus consultas están cacheadas en el backend.

### Advertencia crítica: código comercial vs ID interno

La base guarda un ID municipal interno, mientras que usuarios y frontend trabajan con el número comercial visible.

Ejemplo:

```text
Comercial que usa el frontend: 521
ID interno persistido/MGP:     100
```

Para `directions` y `stops`, el backend acepta el código comercial y lo resuelve automáticamente. También tolera el interno por compatibilidad, pero **el frontend debe usar siempre el comercial**.

Actualmente algunas respuestas exponen ambos por motivos de diagnóstico:

| Respuesta | Campo interno, no usar como input UI | Campo comercial, usar en UI y requests |
|---|---|---|
| `GET /map/lines` | `code` | `name` |
| `GET /map/stops/{identifier}` | `directions[].codeTransitLine` | `directions[].nameTransitLine` |
| arribos consolidados/SSE | — | `lines[].lineCode` |

No usar `code` ni `codeTransitLine` para construir la interacción de usuario. En particular, el selector debe mostrar y enviar `name`.

### 6.1 Listar líneas para selector

```http
GET /api/v1/transit/map/lines
```

Respuesta:

```json
[
  {
    "code": "100",
    "name": "521"
  }
]
```

En frontend, modelar por ejemplo:

```ts
type MapLine = { internalCode: string; commercialCode: string };
// internalCode = response.code; commercialCode = response.name
```

Para seleccionar una línea o llamar la próxima ruta, usar `commercialCode`.

### 6.2 Listar sentidos de una línea

```http
GET /api/v1/transit/map/lines/521/directions
```

Respuesta:

```json
[
  {
    "direction": "AL BOSQUE",
    "expandedDirection": "AL BOSQUE"
  },
  {
    "direction": "AL PUERTO",
    "expandedDirection": "AL PUERTO X LOMAS"
  }
]
```

Para la UI, mostrar `expandedDirection` cuando exista; enviar siempre `direction` exactamente como se recibió. No normalizar mayúsculas, espacios ni tildes.

### 6.3 Pines de paradas por línea y sentido

```http
GET /api/v1/transit/map/lines/521/stops?direction=AL%20BOSQUE
```

Parámetro requerido:

| Nombre | Tipo | Descripción |
|---|---|---|
| `direction` | string | Valor exacto de `DirectionDto.direction` |

Respuesta:

```json
[
  {
    "identifier": "P3613",
    "code": "17211",
    "description": "AL PUERTO",
    "latitude": -38.018582,
    "longitude": -57.55541
  }
]
```

- `identifier`: ID canónico de la parada física. Usarlo en navegación, clicks y telemetría.
- `code`: código adicional de MGP. No usarlo como clave de mapa.
- `description`: etiqueta textual de la parada; no necesariamente coincide con el sentido solicitado.
- `latitude`/`longitude`: coordenadas WGS84 para MapLibre, Leaflet, Google Maps, etc.

Una línea válida con un sentido sin paradas puede responder `200 []`; una línea inexistente responde `404`.

### 6.4 Detalle de una parada física

```http
GET /api/v1/transit/map/stops/P3613
```

Respuesta:

```json
{
  "identifier": "P3613",
  "latitude": -38.018582,
  "longitude": -57.55541,
  "directions": [
    {
      "codeTransitLine": "100",
      "nameTransitLine": "521",
      "direction": "AL BOSQUE",
      "expandedDirection": "AL BOSQUE"
    },
    {
      "codeTransitLine": "101",
      "nameTransitLine": "522",
      "direction": "AL FARO",
      "expandedDirection": "AL FARO"
    }
  ]
}
```

Esta ruta representa el poste/parada física y devuelve todas las combinaciones línea–sentido que convergen allí. Para telemetría individual, usar `identifier` como `stopId` y `nameTransitLine` como `lineCode` comercial.

### 6.5 Arribos consolidados (JSON de una sola vez)

```http
GET /api/v1/transit/map/stops/P3613/arrivals
```

Es una respuesta JSON que espera a completar las consultas de todas las líneas. Mantenerla para depuración, clientes sin SSE o cargas puntuales; para la pantalla interactiva se recomienda el stream SSE de la sección siguiente.

```json
{
  "identifier": "P3613",
  "latitude": -38.018582,
  "longitude": -57.55541,
  "lines": [
    {
      "lineCode": "521",
      "status": "LIVE",
      "timestamp": "2026-09-21T22:00:00Z",
      "deltaMinutes": 0,
      "directions": [
        {
          "direction": "AL BOSQUE",
          "expandedDirection": "AL BOSQUE",
          "arrivals": []
        }
      ],
      "error": null
    }
  ]
}
```

La consulta es una vez por **línea comercial única**, no una vez por sentido. El backend filtra los arribos de esa línea para cada `direction` localmente.

Si una línea no tiene fallback y MGP/proxy falla, el HTTP general sigue respondiendo y esa línea particular lleva:

```json
{
  "lineCode": "521",
  "status": "UNAVAILABLE",
  "timestamp": null,
  "deltaMinutes": null,
  "directions": [{ "direction": "AL BOSQUE", "arrivals": [] }],
  "error": "..."
}
```

No ocultar automáticamente otras líneas por la falla de una sola.

### 6.6 Arribos progresivos con Server-Sent Events (SSE): ruta principal del panel de parada

```http
GET /api/v1/transit/map/stops/P3613/arrivals/stream
Accept: text/event-stream
Authorization: Bearer <token>
```

La conexión queda abierta mientras el cliente la mantenga. No es una respuesta JSON única.

Eventos:

| Evento | Data | Cuándo llega |
|---|---|---|
| `cycle-start` | `{ "identifier": "P3613" }` | Inicio de cada ronda |
| `line-arrivals` | Un `StopLineArrivalsDto` | Una vez por línea, ni bien termina esa consulta |
| `cycle-complete` | `{ "identifier": "P3613" }` | Ya se emitieron todas las líneas de la ronda |
| `stream-error` | `{ "message": "Unable to refresh stop arrivals" }` | Falla no recuperable del ciclo/stream |

Ejemplo de wire format SSE:

```text
event: cycle-start
data: {"identifier":"P3613"}

event: line-arrivals
data: {"lineCode":"521","status":"LIVE","timestamp":"2026-09-21T22:00:00Z","deltaMinutes":0,"directions":[...] ,"error":null}

event: cycle-complete
data: {"identifier":"P3613"}
```

El backend inicia una ronda inmediatamente y repite una nueva aproximadamente cada 20 segundos. Dentro de cada ronda inicia consultas de líneas en tareas virtuales, pero protege MGP globalmente: máximo dos inicios por segundo y máximo dos requests upstream simultáneos. Por ello no asumir orden de `line-arrivals`.

#### Reglas frontend para SSE

1. Abrir **un stream por panel de parada visible**, no uno por línea ni por pin.
2. Reemplazar/actualizar sólo la línea recibida en cada `line-arrivals`; conservar las demás hasta su próxima actualización.
3. Cerrar el stream al cerrar el panel, cambiar de parada, desmontar pantalla o pasar la aplicación a background.
4. Reconectar con backoff si la red se corta. Al reconectar, el servidor entrega una ronda nueva.
5. No abrir streams para pines que no están seleccionados.
6. Si se usa navegador con `EventSource` nativo, recordar que normalmente no permite enviar header `Authorization`. Usar un cliente SSE que soporte headers de JWT, una implementación `fetch` streaming, o el mecanismo equivalente del framework móvil. No agregar token en query string.
7. Postman puede inspeccionar el stream configurando `Accept: text/event-stream` y el header `Authorization`.

### 6.7 Flujo recomendado del mapa

```text
Selector de línea
  GET /map/lines
  usar name como código comercial

Selector de sentido
  GET /map/lines/{commercialCode}/directions

Pines
  GET /map/lines/{commercialCode}/stops?direction={direction}

Click en pin
  opcional: GET /map/stops/{identifier} para metadatos estáticos
  principal: GET /map/stops/{identifier}/arrivals/stream

Cerrar panel/cambiar pin
  cerrar SSE anterior
```

## 7. Telemetría individual

Ruta protegida, útil para un preset puntual o una pantalla no basada en el stream:

```http
GET /api/v1/telemetry/arrivals?lineCode=521&stopId=P3613&bandera=AL%20BOSQUE
```

Parámetros:

| Parámetro | Requerido | Descripción |
|---|---:|---|
| `lineCode` | Sí | Código comercial, por ejemplo `521`. El backend lo resuelve a ID interno. |
| `stopId` | Sí | `identifier` de parada física, por ejemplo `P3613`. |
| `bandera` | No | Sentido; si se manda, filtra arribos por coincidencia de rama. |

Respuesta `ArrivalResponseDTO`:

```json
{
  "lineCode": "521",
  "stopId": "P3613",
  "branch": "AL BOSQUE",
  "status": "LIVE",
  "timestamp": "2026-09-21T22:00:00Z",
  "deltaMinutes": 0,
  "arrivals": [
    {
      "lineCode": "521",
      "branch": "AL BOSQUE",
      "remainingMinutes": 4,
      "distanceMeters": 820,
      "estimatedArrivalTime": "19:34",
      "vehicleUnit": "123",
      "accessible": true,
      "status": "LIVE",
      "timestamp": "2026-09-21T22:00:00Z",
      "latitude": -38.01,
      "longitude": -57.55,
      "scheduleDeviation": "+01:16",
      "driverId": "PE,509",
      "stopLatitude": -38.018582,
      "stopLongitude": -57.55541,
      "bearing": 90.0,
      "speedKmH": 24.5
    }
  ],
  "stopLatitude": -38.018582,
  "stopLongitude": -57.55541
}
```

Campos de `BusArrivalItemDTO` pueden ser `null` porque MGP no siempre informa GPS, distancia, conductor, accesibilidad o desviación.

### Estados de telemetría

| Estado | Significado | UI requerida |
|---|---|---|
| `LIVE` | Información llegada desde MGP en la ronda actual | Mostrar normalmente |
| `ESTIMATED_FALLBACK` | Derivada de una última lectura válida por caída/omisión temporal | Mostrar contador, pero indicar `Estimado`/estado tenue |
| `EXPIRED` | La última lectura válida superó el límite de extrapolación | No presentar ETA como confiable |
| `UNAVAILABLE` | Sólo en el DTO consolidado de mapa; no hubo dato ni fallback para esa línea | Mostrar error local de línea, no cancelar el resto |

### Cómo actualizar el contador en UI

1. Al recibir `remainingMinutes` y `timestamp`, iniciar/actualizar el contador local una vez por segundo.
2. Cada nuevo `line-arrivals` o respuesta de telemetría es la corrección de realidad; reemplaza el valor local de ese coche.
3. Usar `vehicleUnit` como identidad estable del colectivo. No usar posición del array.
4. Un coche `<= 25 min` que desaparece de MGP puede seguir llegando como `ESTIMATED_FALLBACK`; conservarlo en UI mientras el backend lo emita.
5. Un coche con ETA inicial `> 25 min` se muestra si llega `LIVE`, pero no se conserva si desaparece: MGP es poco confiable para esas estimaciones largas.
6. Si un coche reaparece con el mismo `vehicleUnit`, sustituir su estado estimado por su dato `LIVE`.

No calcular una ETA nueva desde `distanceMeters`, `speedKmH` o GPS en frontend. Usarlos sólo para visualización; el backend ya normaliza la telemetría y gestiona fallback.

### Caché y comportamiento esperado

- Arribos `LIVE`: cacheados aproximadamente 15 segundos por `stopId + internalLineCode`.
- Fallback temporal: cache de corta duración y última lectura válida en memoria.
- Peticiones simultáneas de la misma parada/línea se coalescen: no generan múltiples requests MGP.
- Las claves usan ID interno, aunque el cliente envíe el comercial. Por ejemplo `511` y `98` comparten cache.

No hacer polling individual cada segundo: para mapa usar SSE; para una pantalla individual, no consultar más rápido que aproximadamente 15–20 segundos.

## 8. Catálogo heredado de MGP

Prefijo: `/api/v1/transit`. Requiere JWT. Estas rutas conservan los nombres y la semántica de MGP; no confundirlas con `/api/v1/transit/map`.

| Método | Ruta | Resultado |
|---|---|---|
| `GET` | `/api/v1/transit/lines` | `TransitLineDTO[]` |
| `GET` | `/api/v1/transit/lines/{lineCode}/streets` | `TransitStreetDTO[]` |
| `GET` | `/api/v1/transit/lines/{lineCode}/streets/{streetCode}/intersections` | `TransitIntersectionDTO[]` |
| `GET` | `/api/v1/transit/lines/{lineCode}/streets/{streetCode}/intersections/{intersectionCode}/stops` | `TransitStopWithFlagDTO[]` |
| `GET` | `/api/v1/transit/lines/{lineCode}/route` | `TransitRouteResponseDTO` |
| `GET` | `/api/v1/transit/lines/{lineCode}/stops?streetCode={streetCode}&intersectionCode={intersectionCode}` | `ConsolidatedStopDTO` |
| `GET` | `/api/v1/transit/lines/{lineCode}/stops` | `TransitStopDTO[]` |

Para el flujo tradicional usar exactamente la secuencia:

```text
líneas → calles → intersecciones → paradas con bandera → telemetría
```

### DTOs heredados

`TransitLineDTO`:

```json
{
  "id": "...",
  "codigo": "511",
  "descripcion": "Línea 511",
  "codigoEntidad": "98",
  "codigoEmpresa": 1
}
```

`TransitStreetDTO` y `TransitIntersectionDTO`:

```json
{ "codigo": "5449", "descripcion": "ALMAFUERTE - MAR DEL PLATA" }
```

`TransitStopWithFlagDTO`:

```json
{
  "codigo": "17453",
  "identificador": "P4031",
  "descripcion": "...",
  "abreviaturaBandera": "A ACANTILADOS",
  "abreviaturaAmpliadaBandera": "A ACANTILADOS",
  "latitudParada": -38.0,
  "longitudParada": -57.0
}
```

`ConsolidatedStopDTO`:

```json
{
  "stopId": "P4031",
  "calle": "...",
  "interseccion": "...",
  "banderas": ["A ACANTILADOS"]
}
```

`TransitStopDTO`:

```json
{
  "id": "P3692",
  "nombre": "...",
  "calle": "...",
  "latitude": -38.0,
  "longitude": -57.0
}
```

`TransitRouteResponseDTO` permite dibujar polilíneas:

```json
{
  "lineCode": "521",
  "branches": [
    {
      "bandera": "AL BOSQUE",
      "descripcion": "AL BOSQUE",
      "points": [
        {
          "latitude": -38.0,
          "longitude": -57.0,
          "descripcion": "...",
          "isPuntoPaso": true
        }
      ],
      "coordinates": [[-38.0, -57.0]]
    }
  ],
  "allPoints": [],
  "coordinates": [[-38.0, -57.0]]
}
```

El orden de cada coordenada es `[latitude, longitude]`.

## 9. Administración y salud

### Health check público

```http
GET /healthz
```

```json
{ "status": "UP" }
```

### Invalidación administrativa de caché

Requiere JWT con rol `ADMIN`:

```http
DELETE /api/v1/admin/cache
DELETE /api/v1/admin/cache?name=arrivals
```

Respuesta:

```json
{
  "status": "SUCCESS",
  "purgedCaches": ["arrivals"]
}
```

No exponer esta operación en una UI de usuario final. Es una herramienta interna/operativa.

## 10. Tipos TypeScript sugeridos para el nuevo mapa

```ts
export type TelemetryStatus =
  | "LIVE"
  | "ESTIMATED_FALLBACK"
  | "EXPIRED";

export type BusArrival = {
  lineCode: string | null;
  branch: string | null;
  remainingMinutes: number | null;
  distanceMeters: number | null;
  estimatedArrivalTime: string | null;
  vehicleUnit: string | null;
  accessible: boolean | null;
  status: TelemetryStatus;
  timestamp: string | null;
  latitude: number | null;
  longitude: number | null;
  scheduleDeviation: string | null;
  driverId: string | null;
  stopLatitude: number | null;
  stopLongitude: number | null;
  bearing: number | null;
  speedKmH: number | null;
};

export type StopDirectionArrivals = {
  direction: string;
  expandedDirection: string | null;
  arrivals: BusArrival[];
};

export type StopLineArrivals = {
  lineCode: string; // comercial, por ejemplo "521"
  status: TelemetryStatus | "UNAVAILABLE";
  timestamp: string | null;
  deltaMinutes: number | null;
  directions: StopDirectionArrivals[];
  error: string | null;
};

export type MapStop = {
  identifier: string;
  code: string | null; // auxiliar de MGP; no usar como clave
  description: string | null;
  latitude: number | null;
  longitude: number | null;
};
```

## 11. Checklist de implementación para otra IA/frontend

- [ ] Configurar `API_BASE_URL` por entorno y agregar el JWT a toda ruta protegida.
- [ ] Implementar login/registro y persistencia segura del token.
- [ ] Implementar manejo central de `401`, `403`, `404`, `502` y `problem+json`.
- [ ] Para mapa, consumir `/api/v1/transit/map/**`, no el catálogo heredado salvo que la pantalla use explícitamente el selector por calles.
- [ ] Usar el código comercial (`name` / `nameTransitLine`) en los requests de mapa y telemetría.
- [ ] Usar `identifier` como clave estable de parada y `vehicleUnit` como clave estable de colectivo.
- [ ] Para el panel de parada, abrir una única conexión SSE autenticada; no hacer seis requests desde frontend.
- [ ] Aplicar cada evento `line-arrivals` de manera incremental y tolerar orden no determinista.
- [ ] Cerrar el SSE al navegar, desmontar, cambiar de pin o ir a background.
- [ ] Actualizar visualmente el contador cada segundo, pero corregirlo con cada evento del backend.
- [ ] Diferenciar `LIVE`, `ESTIMATED_FALLBACK`, `EXPIRED` y `UNAVAILABLE` en UX.
- [ ] No mostrar como real una ETA estimada, ni preservar en frontend por su cuenta coches que el backend dejó de emitir.
- [ ] Nunca enviar el JWT como query parameter del SSE.

