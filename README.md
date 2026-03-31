# OptiPlant - Sistema de Inventario Multi-Sucursal

**Backend REST API** desarrollado con Spring Boot 3.x, Java 21, PostgreSQL y Docker.

## 📋 Descripción

Sistema web para gestión de inventario de múltiples sucursales de una organización. Cada sucursal opera con autonomía pero comparte visibilidad en tiempo real con la red.

### Módulos Implementados

- ✅ **Gestión de Inventario** - CRUD completo con trazabilidad de movimientos
- ✅ **Módulo de Compras** - Órdenes, recepción, costo promedio ponderado
- ✅ **Módulo de Ventas** - Registro, validación de stock, comprobantes
- ✅ **Transferencia entre Sucursales** - Flujo completo con estados
- ✅ **Autenticación JWT** - Spring Security con roles (ADMIN, GERENTE, OPERADOR)
- ✅ **API REST Documentada** - OpenAPI 3.0 / Swagger UI

## 🏗️ Arquitectura

### Stack Tecnológico

- **Backend:** Spring Boot 3.3.4
- **Java:** 21 (records, sealed classes)
- **Base de Datos:** PostgreSQL 16
- **Seguridad:** JWT + Spring Security
- **ORM:** Spring Data JPA + Hibernate
- **Migraciones:** Flyway
- **Build:** Gradle
- **Containerización:** Docker & Docker Compose
- **Documentación API:** Springdoc OpenAPI / Swagger UI

### Estructura de Proyecto

```
backend/sistema-inventarios/
├── src/main/java/com/inventario/
│   ├── config/          # Configuraciones (Spring Security, JWT, OpenAPI)
│   ├── controller/      # @RestController endpoints
│   ├── service/         # Lógica de negocio
│   ├── repository/      # Spring Data JPA
│   ├── model/          # @Entity JPA
│   ├── dto/            # Data Transfer Objects (request/response)
│   ├── exception/      # Excepciones personalizadas + GlobalExceptionHandler
│   └── mapper/         # MapStruct mappers
├── src/main/resources/
│   ├── application.properties
│   ├── application-docker.properties
│   └── db/migration/   # Scripts Flyway
└── build.gradle
```

## 🚀 Instalación y Ejecución

### Requisitos Previos

- Docker y Docker Compose instalados
- Git
- (Opcional) Java 21 y Gradle para desarrollo local

### Opción 1: Con Docker Compose (Recomendado)

```bash
# 1. Clonar el repositorio
git clone <repo_url>
cd trabajo

# 2. Crear archivo .env (ya viene con valores por defecto)
cp .env.example .env

# 3. Levantar los servicios
docker compose up --build

# 4. Verificar que todo esté corriendo
# Backend: http://localhost:8080/api/v1/actuator/health
# API Docs: http://localhost:8080/api/v1/swagger-ui.html
```

### Opción 2: Desarrollo Local

```bash
# 1. Asegurarse que PostgreSQL está corriendo en localhost:5432

# 2. Compilar el proyecto
cd backend/sistema-inventarios
./gradlew build

# 3. Ejecutar con perfil local
./gradlew bootRun --args='--spring.profiles.active=default'
```

## 📚 Documentación de API

Una vez que la aplicación está corriendo:

- **Swagger UI:** http://localhost:8080/api/v1/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/api/v1/v3/api-docs

## 🔐 Autenticación

Todos los endpoints requieren autenticación JWT excepto:
- `POST /api/v1/auth/login` - Login
- `POST /api/v1/auth/refresh` - Refresh token
- Swagger UI docs

### Flujo de Autenticación

1. **Login:**
   ```bash
   curl -X POST http://localhost:8080/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{
       "email": "admin@optiplan.com",
       "password": "password"
     }'
   ```

2. **Respuesta:**
   ```json
   {
     "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     "refreshToken": "...",
     "userId": 1,
     "email": "admin@optiplan.com",
     "nombre": "Administrador",
     "rol": "ADMIN",
     "branchId": null
   }
   ```

3. **Usar token en siguientes peticiones:**
   ```bash
   curl -X GET http://localhost:8080/api/v1/sucursales \
     -H "Authorization: Bearer <accessToken>"
   ```

## 👥 Roles y Permisos

| Rol | Permisos |
|-----|----------|
| **ADMIN** | Ver todo, crear/editar/eliminar sucursales, productos, usuarios, aprobar transferencias |
| **GERENTE_SUCURSAL** | Ver su sucursal, crear transferencias, aprobar recepciones, ver reportes |
| **OPERADOR_INVENTARIO** | Registrar movimientos, crear ventas, recibir transferencias |

## 📊 Endpoints Principales

### Autenticación
- `POST /auth/login` - Login con email y contraseña
- `POST /auth/refresh` - Refrescar token

### Sucursales
- `GET /sucursales` - Listar todas las sucursales
- `GET /sucursales/{id}` - Obtener una sucursal
- `POST /sucursales` - Crear sucursal (ADMIN)
- `PUT /sucursales/{id}` - Actualizar sucursal (ADMIN)
- `DELETE /sucursales/{id}` - Desactivar sucursal (ADMIN)

### Productos
- `GET /productos` - Listar todos
- `GET /productos/{id}` - Obtener detalle
- `GET /productos/buscar?keyword=x` - Buscar
- `POST /productos` - Crear (ADMIN)
- `PUT /productos/{id}` - Actualizar (ADMIN)
- `DELETE /productos/{id}` - Desactivar (ADMIN)

### Inventario
- `GET /inventario?sucursalId=x` - Listar inventario por sucursal
- `GET /inventario/{productId}?sucursalId=x` - Obtener item específico
- `POST /inventario/movimiento` - Registrar movimiento (OPERADOR)

### Compras
- `GET /compras` - Listar (ADMIN)
- `POST /compras` - Crear (ADMIN)
- `PATCH /compras/{id}/cambiar-estado` - Cambiar estado (ADMIN)
- `POST /compras/{id}/recibir` - Recibir compra (OPERADOR)

### Ventas
- `GET /ventas` - Listar
- `POST /ventas` - Crear (OPERADOR)
- `DELETE /ventas/{id}` - Cancelar (GERENTE)

### Transferencias
- `GET /transferencias?branchId=x` - Listar
- `POST /transferencias` - Crear (GERENTE)
- `PATCH /transferencias/{id}/aprobar` - Aprobar (GERENTE)
- `PATCH /transferencias/{id}/enviar` - Enviar (GERENTE)
- `PATCH /transferencias/{id}/recibir` - Recibir (OPERADOR)

## 🗄️ Base de Datos

### Migraciones Flyway

Se ejecutan automáticamente al iniciar la aplicación:

1. **V1__Initial_Schema.sql** - Crea todas las tablas
2. **V2__Insert_Initial_Data.sql** - Inserta datos de prueba

### Usuarios de Prueba

| Email | Contraseña | Rol | Sucursal |
|-------|-----------|-----|----------|
| admin@optiplan.com | password | ADMIN | - |
| gerente@sucursal.com | password | GERENTE_SUCURSAL | CENTRAL |
| operador@sucursal.com | password | OPERADOR_INVENTARIO | CENTRAL |

## 🧪 Testing

Para ejecutar los tests:

```bash
cd backend/sistema-inventarios
./gradlew test
```

## 📋 Características Implementadas

### Seguridad
- ✅ Autenticación JWT con tokens de acceso y refresh
- ✅ Spring Security con roles basados en autorización
- ✅ GlobalExceptionHandler para manejo centralizado de errores
- ✅ Validación de entrada con Jakarta Bean Validation
- ✅ Contraseñas hasheadas con BCrypt

### Servicios
- ✅ **SucursalService** - CRUD de sucursales
- ✅ **ProductoService** - CRUD de productos con búsqueda
- ✅ **InventarioService** - Movimientos con cálculo de costo ponderado
- ✅ **CompraService** - Flujo completo de compras
- ✅ **VentaService** - Creación y cancelación de ventas
- ✅ **TransferenciaService** - Flujo multi-estado de transferencias

### API
- ✅ Endpoints REST completos con códigos HTTP apropiados
- ✅ Documentación OpenAPI 3.0 con Swagger UI
- ✅ Validaciones automáticas en DTOs
- ✅ Paginación en listados
- ✅ Control de acceso por roles (@PreAuthorize)

### Base de Datos
- ✅ Migraciones automáticas con Flyway
- ✅ Triggers para updated_at automático
- ✅ Índices en campos de búsqueda y FKs
- ✅ Restricciones de integridad referencial
- ✅ Comentarios en tablas relevantes

## 🔍 Validaciones Implementadas

### Stock
- Validación de stock disponible antes de ventas/transferencias
- InsufficientStockException cuando no hay stock
- Cálculo automático de costo promedio ponderado

### Entidades
- Email único en usuarios
- SKU único en productos
- Código único en sucursales
- Rango válido para porcentajes de descuento

## 📝 Logging

La aplicación incluye logging detallado:

```properties
logging.level.com.inventario=DEBUG  # Nivel debug para la app
logging.level.org.springframework.security=DEBUG
```

Todos los servicios registran (log):
- Operaciones CRUD
- Cambios de estado
- Validaciones fallidas
- Errores de transacciones

## 🐛 Troubleshooting

### "Connection refused" en PostgreSQL
```bash
# Verificar que el contenedor de BD está corriendo
docker compose ps

# Reiniciar los servicios
docker compose down
docker compose up --build
```

### Errores de compilación
```bash
# Limpiar build anterior
./gradlew clean

# Compilar de nuevo
./gradlew build
```

### Token expirado
```bash
# Usar el refresh token para obtener nuevo access token
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Authorization: Bearer <refreshToken>"
```

## 📦 Dependencias Principales

```gradle
Spring Boot 3.3.4
Spring Security 6
Spring Data JPA
PostgreSQL Driver
JWT (jjwt) 0.12.3
Lombok
MapStruct
Flyway
Springdoc OpenAPI 2.0.2
Apache POI (reportes Excel)
OpenPDF (reportes PDF)
```

## 🤝 Contribuciones

Este proyecto fue desarrollado como prueba técnica para OptiPlant Consultores.

## 📄 Licencia

Proyecto propietario de OptiPlant Consultores.

---

**Desarrollado con:** Spring Boot · Java 21 · PostgreSQL · Docker · JWT
**Última actualización:** Marzo 2026
