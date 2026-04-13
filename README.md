# VitalTrail — Backend Spring Boot

Backend REST de VitalTrail, responsable de la gestión de usuarios, autenticación JWT, suscripciones y pagos mediante Stripe, y notificaciones transaccionales por correo electrónico.

---

## Índice

1. [Stack tecnológico](#1-stack-tecnológico)
2. [Arquitectura del proyecto](#2-arquitectura-del-proyecto)
3. [Módulo de pagos con Stripe](#3-módulo-de-pagos-con-stripe)
4. [Seguridad y autenticación](#4-seguridad-y-autenticación)
5. [Requisitos previos](#5-requisitos-previos)
6. [Configuración del entorno](#6-configuración-del-entorno)
7. [Ejecución](#7-ejecución)
8. [Comandos útiles](#8-comandos-útiles)
9. [API Reference](#9-api-reference)
10. [Tests](#10-tests)
11. [Estructura de paquetes](#11-estructura-de-paquetes)

---

## 1. Stack tecnológico

| Tecnología | Versión | Rol |
|---|---|---|
| Java | 21 | Lenguaje de implementación |
| Spring Boot | 3.4.3 | Framework principal |
| Spring Security | (Boot 3.4.3) | Autenticación y autorización |
| Spring Data JPA / Hibernate | (Boot 3.4.3) | Persistencia |
| Spring WebFlux / WebClient | (Boot 3.4.3) | Cliente HTTP para notificaciones |
| Spring Retry + AOP | (Boot 3.4.3) | Reintentos con backoff exponencial |
| PostgreSQL | — | Base de datos relacional |
| Stripe Java SDK | 28.0.0 | Integración de pagos y webhooks |
| jjwt | 0.11.5 | Generación y validación de tokens JWT |
| Lombok | 1.18.30 | Reducción de boilerplate |
| java-dotenv | 5.2.2 | Variables de entorno desde `.env` |
| Maven Wrapper | 3.9.x | Gestión de dependencias y build |

---

## 2. Arquitectura del proyecto

El proyecto sigue una arquitectura por capas con separación clara entre la capa HTTP, la lógica de dominio y la infraestructura:

```
api/          → Controllers, DTOs, Assemblers, Security, filtros HTTP
domain/       → Servicios de negocio, Entidades JPA, Repositorios
infra/config/ → Beans de configuración (JWT, CORS, Stripe, WebClient, Retry)
```

Cada dominio funcional (usuario, pago, suscripción, factura, notificación) tiene su propio subpaquete tanto en `api/` como en `domain/`, lo que permite una separación de responsabilidades explícita.

---

## 3. Módulo de pagos con Stripe

Este módulo gestiona el ciclo de vida completo de las suscripciones premium: alta, renovación automática, cancelación, reactivación, historial de facturas y notificaciones transaccionales por correo.

### 3.1 Flujo de alta de suscripción

```
Frontend                Backend                     Stripe
   │                       │                           │
   │  POST /create-checkout │                           │
   │──────────────────────▶│                           │
   │                       │── Session.create() ──────▶│
   │                       │◀── { sessionId } ─────────│
   │◀── { sessionId } ─────│                           │
   │                       │                           │
   │── redirectToCheckout ─────────────────────────────▶│
   │      (usuario paga en Stripe Checkout)             │
   │                       │                           │
   │                       │◀── webhook: checkout.      │
   │                       │    session.completed ──────│
   │                       │── 200 OK ────────────────▶│
   │                       │                           │
   │                       │ [async] crea Subscription  │
   │                       │ activa isPremium           │
   │                       │ guarda customerId          │
   │                       │ envía email bienvenida     │
```

### 3.2 Eventos de Stripe procesados

| Evento | Acción |
|---|---|
| `checkout.session.completed` | Crea `SubscriptionEntity`, activa `isPremium`, guarda `customerId` y `paymentMethodId`, envía email de bienvenida |
| `customer.subscription.updated` | Sincroniza estado, período y `cancelAtPeriodEnd`; notifica si cambia la renovación automática |
| `customer.subscription.deleted` | Marca suscripción como `canceled`, desactiva `isPremium`, envía email de cancelación |
| `invoice.payment_succeeded` | Envía email de confirmación de renovación (solo en `billing_reason = subscription_cycle`) |
| `invoice.payment_failed` | Marca suscripción como `past_due`, desactiva `isPremium`, envía email de aviso |

### 3.3 Procesamiento asíncrono de webhooks

El endpoint de webhook devuelve `200 OK` inmediatamente tras verificar la firma. El procesamiento real se realiza en un pool de hilos dedicado (`webhookTaskExecutor`) para no bloquear la respuesta a Stripe.

```
POST /payments/stripe/webhook
        │
        ├── Verifica firma HMAC-SHA256 (Stripe-Signature)
        │       └── Inválida → 400 STRIPE_INVALID_SIGNATURE
        │
        ├── 200 OK → Stripe
        │
        └── [async, hilo webhook-N]
                ├── Deserializa StripeObject
                ├── Switch por event.type
                ├── Actualiza BD (SubscriptionEntity, UserEntity)
                ├── Envía notificación por email
                └── @Retryable: 3 intentos, backoff 1s → 2s
                        └── @Recover: log ERROR del fallo definitivo
```

**Configuración del pool:**

| Parámetro | Valor |
|---|---|
| `corePoolSize` | 2 |
| `maxPoolSize` | 5 |
| `queueCapacity` | 100 |
| Prefijo de hilo | `webhook-` |

### 3.4 Estados de suscripción

```
[checkout.session.completed]
           │
           ▼
        active ──────────────────────────────────────────▶ canceled
           │         customer.subscription.deleted              ▲
           │         (cancelación inmediata)                    │
           │                                                     │
           ├──[invoice.payment_failed]──▶ past_due              │
           │                                 │                   │
           │   [invoice.payment_succeeded]◀──┘                   │
           │                                                     │
           └──[cancelAtPeriodEnd=true]─▶ active ──fin período──▶ canceled
              (cancelación diferida)     (sigue activo)
```

---

## 4. Seguridad y autenticación

- Todos los endpoints requieren autenticación mediante **JWT Bearer token**, excepto los declarados como públicos en `SecurityConfig`.
- La sesión es **stateless** (sin cookies ni sesión HTTP).
- Los permisos a nivel de método se controlan con anotaciones propias de `@CheckSecurity`, que encapsulan `@PreAuthorize`:

| Anotación | Condición |
|---|---|
| `@CheckSecurity.Protected.canManage` | Cualquier usuario autenticado |
| `@CheckSecurity.Protected.isAdminOrOwner` | `ROLE_ADMIN` o propietario del `customerId` (path variable) |
| `@CheckSecurity.Protected.isAdminOrOwnerByAction` | `ROLE_ADMIN` o propietario del `customerId` (request body) |
| `@CheckSecurity.Public.canRead` | Sin restricción |

- El endpoint `POST /payments/stripe/webhook` es público por diseño; la autenticidad se garantiza mediante verificación de firma `Stripe-Signature` (`HMAC-SHA256`).

---

## 5. Requisitos previos

- **Java 21** o superior
- **Maven 3.9+** (o usar el wrapper `./mvnw` incluido)
- **PostgreSQL** accesible en la URL configurada
- **Cuenta de Stripe** con clave secreta y webhook configurado
- **Servicio de notificaciones** desplegado y accesible en `MAILGUN_BACKEND_URL`

> Para desarrollo local, el webhook de Stripe requiere que el endpoint sea accesible públicamente.
> Se recomienda usar el [Stripe CLI](https://stripe.com/docs/stripe-cli) con `stripe listen --forward-to localhost:8080/payments/stripe/webhook`.

---

## 6. Configuración del entorno

Crea un fichero `.env` en la raíz del proyecto. **No lo incluyas en el repositorio.**

```env
# Servidor
SERVER_PORT=8080

# PostgreSQL
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/vitaltrail_db
SPRING_DATASOURCE_USERNAME=tu_usuario
SPRING_DATASOURCE_PASSWORD=tu_password

# JWT
JWT_SECRET=tu_secreto_jwt_minimo_256_bits
JWT_EXPIRATION=86400000

# Stripe
STRIPE_API_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...

# Servicio de notificaciones (Mailgun backend)
MAILGUN_BACKEND_URL=http://localhost:8081
```

> En producción, sustituye `sk_test_` por `sk_live_` y `whsec_` por el secreto del webhook de producción de Stripe.

---

## 7. Ejecución

### Desarrollo local

```bash
./mvnw spring-boot:run
```

### Con Docker

```bash
docker build -f Dockerfile.dev -t vitaltrail-backend .
docker run --env-file .env -p 8080:8080 vitaltrail-backend
```

### Compilar sin ejecutar

```bash
./mvnw clean package -DskipTests
```

---

## 8. Comandos útiles

```bash
# Compilar
./mvnw compile

# Ejecutar todos los tests
./mvnw test

# Ejecutar una clase de test concreta
./mvnw test -Dtest=InvoiceServiceImplTest

# Ejecutar un método de test concreto
./mvnw test -Dtest=InvoiceServiceImplTest#getInvoices_devuelveLista

# Empaquetar (genera JAR en target/)
./mvnw clean package -DskipTests

# Verificar dependencias desactualizadas
./mvnw versions:display-dependency-updates
```

---

## 9. API Reference

> Todos los endpoints requieren cabecera `Authorization: Bearer <token>` salvo los indicados como públicos.

### Pagos y suscripción (Stripe)

#### `POST /payments/stripe/create-checkout-session`

Crea una sesión de checkout en Stripe para iniciar una suscripción.

**Body:**
```json
{
  "priceId": "price_xxx",
  "successUrl": "https://app.vitaltrail.com/success",
  "cancelUrl": "https://app.vitaltrail.com/cancel",
  "customerData": {
    "customerId": "cus_xxx",
    "email": "usuario@example.com"
  }
}
```

**Respuesta `200 OK`:**
```json
{ "sessionId": "cs_test_xxx" }
```

> `customerData` es opcional. Si se proporciona `customerId`, la sesión se asocia al cliente existente en Stripe. Si solo se proporciona `email`, se pre-rellena en el formulario. Si no se proporciona ninguno, Stripe crea un nuevo cliente.

---

#### `GET /payments/stripe/sessions/{sessionId}`

Recupera el estado y los datos de una sesión de checkout.

**Respuesta `200 OK`:** objeto con datos de la sesión y, si existe, de la suscripción asociada.

---

#### `POST /payments/stripe/cancel-subscription`

Cancela la suscripción activa del cliente con efecto inmediato.

**Requiere:** `ROLE_ADMIN` o ser el propietario del `customerId`.

**Body:**
```json
{ "customerId": "cus_xxx" }
```

**Respuesta `200 OK`:** `SubscriptionDto` con `status: "canceled"`.

---

#### `POST /payments/stripe/cancel-at-period-end`

Programa la cancelación de la suscripción para el final del período de facturación actual. El acceso premium se mantiene hasta esa fecha.

**Requiere:** `ROLE_ADMIN` o ser el propietario del `customerId`.

**Body:**
```json
{ "customerId": "cus_xxx" }
```

**Respuesta `200 OK`:** `SubscriptionDto` con `cancelAtPeriodEnd: true`.

---

#### `POST /payments/stripe/reactivate-subscription`

Revierte una cancelación diferida, restaurando la renovación automática.

**Requiere:** `ROLE_ADMIN` o ser el propietario del `customerId`.

**Body:**
```json
{ "customerId": "cus_xxx" }
```

**Respuesta `200 OK`:** `SubscriptionDto` con `cancelAtPeriodEnd: false`.

---

#### `POST /payments/stripe/webhook` *(público)*

Recibe eventos de Stripe. La autenticidad se verifica mediante la cabecera `Stripe-Signature`.

**Cabeceras requeridas:**
```
Stripe-Signature: t=xxx,v1=yyy,...
```

**Respuesta `200 OK`:** `"Webhook received"`

**Errores:**
- `400` — firma inválida (`STRIPE_INVALID_SIGNATURE`)
- `500` — error al procesar el evento (`STRIPE_WEBHOOK_ERROR`)

---

### Suscripción

#### `GET /subscription/{customerId}`

Devuelve los datos de la suscripción activa del cliente indicado.

**Requiere:** `ROLE_ADMIN` o ser el propietario del `customerId`.

**Respuesta `200 OK`:**
```json
{
  "subscriptionId": "sub_xxx",
  "subscriptionType": "premium",
  "billingInterval": "month",
  "status": "active",
  "currentPeriodStart": 1700000000,
  "currentPeriodEnd": 1702678400,
  "cancelAtPeriodEnd": false,
  "productName": "VitalTrail Premium",
  "priceId": "price_xxx",
  "cardBrand": "visa",
  "cardLast4": "4242",
  "cardExpMonth": 12,
  "cardExpYear": 2027,
  "customerId": "cus_xxx",
  "createdAt": "2024-11-15T10:30:00",
  "updatedAt": "2024-11-15T10:30:00"
}
```

---

### Facturas

#### `GET /payments/stripe/invoices/{customerId}`

Devuelve el historial de facturas del cliente (últimas 25).

**Requiere:** `ROLE_ADMIN` o ser el propietario del `customerId`.

**Respuesta `200 OK`:**
```json
[
  {
    "id": "in_xxx",
    "number": "INV-0001",
    "description": "Suscripción mensual",
    "status": "paid",
    "amountTotal": 999,
    "currency": "eur",
    "created": 1700000000,
    "periodStart": 1700000000,
    "periodEnd": 1702678400,
    "invoiceUrl": "https://invoice.stripe.com/...",
    "invoicePdf": "https://pay.stripe.com/invoice/..."
  }
]
```

> `amountTotal` está expresado en céntimos (999 = 9,99 €).

---

### Códigos de error

| Código | Error | HTTP |
|---|---|---|
| `UNAUTHORIZED` | Token ausente o inválido | 401 |
| `FORBIDDEN` | Sin permisos para el recurso | 403 |
| `USER_NOT_FOUND` | Usuario no encontrado | 404 |
| `SUBSCRIPTION_NOT_FOUND` | Suscripción no encontrada o no activa | 404 |
| `STRIPE_ERROR` | Error de comunicación con Stripe | 502 |
| `STRIPE_INVALID_SIGNATURE` | Firma del webhook inválida | 400 |
| `STRIPE_WEBHOOK_ERROR` | Error al procesar el evento de Stripe | 500 |
| `INTERNAL_SERVER_ERROR` | Error interno del servidor | 500 |

---

## 10. Tests

```bash
# Ejecutar todos los tests
./mvnw test
```

Los tests unitarios usan `@ExtendWith(MockitoExtension.class)` con Mockito y AssertJ. Los tests de integración requieren PostgreSQL activo y las variables de entorno configuradas.

**Cobertura actual:**

| Clase | Tests | Estado |
|---|---|---|
| `InvoiceServiceImpl` | Caso de éxito + error de Stripe | ✅ Implementado |
| `StripeServiceImpl` | — | 🔲 Pendiente |
| `WebhookServiceImpl` | — | 🔲 Pendiente |
| `StripeDataService` | — | 🔲 Pendiente |

---

## 11. Estructura de paquetes

```
src/main/java/com/springboot/vitaltrail/
├── api/
│   ├── exception/          # @ControllerAdvice, handlers de error personalizados
│   ├── invoice/            # InvoiceController, InvoiceDto
│   ├── notification/       # NotificationDto, NotificationAssembler, WebhookNotificationFactory
│   ├── payment/            # StripeController, CheckoutSessionDto, CustomerDataDto, SubscriptionActionDto
│   ├── security/           # SecurityConfig, AuthUtils, UserDetailsImpl, JWTAuthFilter
│   │   ├── authorization/  # CheckSecurity, AuthorizationConfig
│   │   └── jwt/            # JWTUtils
│   ├── subscription/       # SubscriptionController, SubscriptionDto, SubscriptionAssembler
│   └── user/               # UserController, UserDto, UserAssembler
│       ├── admin/
│       └── client/         # ClientDto, ClientAssembler
├── domain/
│   ├── exception/          # AppException, Error (enum), NotificationException
│   ├── invoice/            # InvoiceService, InvoiceServiceImpl
│   ├── notification/       # NotificationService, NotificationServiceImpl
│   ├── payment/            # StripeService, StripeServiceImpl, WebhookService, WebhookServiceImpl
│   │                       # StripeDataService, WebhookTestData
│   ├── subscription/       # SubscriptionEntity, SubscriptionRepository
│   │                       # SubscriptionService, SubscriptionServiceImpl
│   └── user/               # UserEntity (Role), UserRepository, UserService, UserServiceImpl
│       ├── admin/          # AdminEntity
│       └── client/         # ClientEntity
└── infra/
    └── config/             # AsyncConfig, JWTConfig, RetryConfig, SecurityConfig (CORS)
                            # StripeConfig, WebClientConfig
```
