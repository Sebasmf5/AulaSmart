# Guía de Despliegue

Este documento describe el procedimiento para desplegar AulaSmart en **AWS EC2** con **RDS PostgreSQL**.

---

## Arquitectura de Despliegue

```
┌─────────────────────────────────────────────┐
│                 Cliente                      │
│            (Flutter/Android/iOS)             │
└─────────────────────┬───────────────────────┘
                      │ HTTP
                      │
┌─────────────────────▼───────────────────────┐
│          EC2 Instance (t3.medium)            │
│  ┌─────────────┐  ┌─────────────┐           │
│  │ Docker      │  │ Docker      │           │
│  │ Compose     │  │ Compose     │           │
│  │             │  │             │           │
│  │ usuarios    │  │ reserva     │           │
│  │ :8081       │  │ :8082       │           │
│  │             │  │             │           │
│  │ aula        │  │ incidencia  │           │
│  │ :8083       │  │ :8085       │           │
│  │             │  │             │           │
│  │ chat        │  │             │           │
│  │ :8086       │  │             │           │
│  └─────────────┘  └─────────────┘           │
│                                              │
│  Volumen: incidencia-uploads                 │
│  (imágenes de evidencia)                     │
└─────────────────────┬───────────────────────┘
                      │ PostgreSQL (5432)
                      │
┌─────────────────────▼───────────────────────┐
│      RDS PostgreSQL (db.t3.micro)            │
│                                              │
│   usuariosdb | reservadb | auladb |          │
│   incidenciadb                               │
└─────────────────────────────────────────────┘
```

---

## Paso 1: Base de Datos (RDS)

### Crear instancia
1. AWS Console → RDS → Create database
2. **Engine:** PostgreSQL 15
3. **Template:** Free tier
4. **DB instance identifier:** `aulasmart-db`
5. **Master username:** `postgres`
6. **Master password:** `[generar segura]`
7. **Instance class:** `db.t3.micro`
8. **Public access:** Yes
9. **VPC:** Default

### Crear bases de datos
```bash
psql -h <RDS-ENDPOINT> -U postgres -c "CREATE DATABASE usuariosdb;"
psql -h <RDS-ENDPOINT> -U postgres -c "CREATE DATABASE reservadb;"
psql -h <RDS-ENDPOINT> -U postgres -c "CREATE DATABASE auladb;"
psql -h <RDS-ENDPOINT> -U postgres -c "CREATE DATABASE incidenciadb;"
```

---

## Paso 2: Instancia EC2

### Launch Instance
- **AMI:** Ubuntu Server 24.04 LTS
- **Type:** `t3.medium` (mínimo recomendado)
- **Key pair:** Crear nuevo, descargar `.pem`
- **Security Group:**
  - SSH (22) → Tu IP
  - TCP 8081-8087 → `0.0.0.0/0`

### Instalar Docker
```bash
ssh -i "aulasmart.pem" ubuntu@<IP-PÚBLICA>

sudo apt update && sudo apt upgrade -y
sudo snap install docker
sudo usermod -aG docker $USER
newgrp docker

sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

---

## Paso 3: Subir proyecto

```bash
# Desde tu PC
scp -i "aulasmart.pem" -r ./AulaSmart ubuntu@<IP-PÚBLICA>:/home/ubuntu/
```

---

## Paso 4: Configurar variables de entorno

Crear `/home/ubuntu/AulaSmart/.env`:

```env
POSTGRES_USER=postgres
POSTGRES_PASSWORD=<tu-password-rds>
DB_USERNAME=postgres
DB_PASSWORD=<tu-password-rds>
JWT_SECRET_KEY=<clave-secreta-jwt-256-bits>
OPEN_CODE_API=<api-key-opencode>
API_KEY_GROQ=<api-key-groq>
```

---

## Paso 5: Configurar docker-compose.yml

Reemplazar `<RDS-ENDPOINT>` con el endpoint real:

```yaml
version: '3.8'

services:
  usuarios-service:
    build:
      context: .
      dockerfile: usuarios-service/Dockerfile
    ports:
      - "8081:8084"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://<RDS-ENDPOINT>:5432/usuariosdb
      - SPRING_DATASOURCE_USERNAME=${DB_USERNAME}
      - SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
      - APPLICATION_SECURITY_JWT_SECRET_KEY=${JWT_SECRET_KEY}
      - TZ=America/Bogota

  reserva-service:
    build:
      context: .
      dockerfile: reserva-service/Dockerfile
    ports:
      - "8082:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://<RDS-ENDPOINT>:5432/reservadb
      - SPRING_DATASOURCE_USERNAME=${DB_USERNAME}
      - SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
      - APPLICATION_SECURITY_JWT_SECRET_KEY=${JWT_SECRET_KEY}
      - URL_SERVICIO_ASISTENCIA=http://aula-service:8082
      - TZ=America/Bogota

  aula-service:
    build:
      context: .
      dockerfile: aula-service/Dockerfile
    ports:
      - "8083:8082"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://<RDS-ENDPOINT>:5432/auladb
      - SPRING_DATASOURCE_USERNAME=${DB_USERNAME}
      - SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
      - APPLICATION_SECURITY_JWT_SECRET_KEY=${JWT_SECRET_KEY}
      - TZ=America/Bogota

  incidencia-service:
    build:
      context: .
      dockerfile: incidencia-service/Dockerfile
    ports:
      - "8085:8087"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://<RDS-ENDPOINT>:5432/incidenciadb
      - SPRING_DATASOURCE_USERNAME=${DB_USERNAME}
      - SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
      - APPLICATION_SECURITY_JWT_SECRET_KEY=${JWT_SECRET_KEY}
      - API_KEY_GROQ=${API_KEY_GROQ}
      - TZ=America/Bogota
    volumes:
      - incidencia-uploads:/app/uploads/incidencias

  chat-service:
    build:
      context: .
      dockerfile: chat-service/Dockerfile
    ports:
      - "8086:8086"
    environment:
      - APPLICATION_SECURITY_JWT_SECRET_KEY=${JWT_SECRET_KEY}
      - URL_SERVICIO_ASISTENCIA=http://aula-service:8082
      - URL_SERVICIO_RESERVA=http://reserva-service:8080
      - SPRING_AI_OPENAI_API_KEY=${OPEN_CODE_API}
      - SPRING_AI_OPENAI_BASE_URL=https://opencode.ai/zen/go/v1
      - TZ=America/Bogota

volumes:
  incidencia-uploads:
```

---

## Paso 6: Construir y levantar

```bash
cd /home/ubuntu/AulaSmart

docker-compose up --build -d

# Verificar estado
docker-compose ps
docker-compose logs -f
```

---

## Paso 7: Configurar Flutter

Actualizar las URLs base en el frontend:

```dart
const String baseUrlUsuarios = 'http://<IP-EC2>:8081/api/v1';
const String baseUrlReserva = 'http://<IP-EC2>:8082/api/v1';
const String baseUrlAula = 'http://<IP-EC2>:8083/api/v1';
const String baseUrlIncidencia = 'http://<IP-EC2>:8085/api/v1';
const String baseUrlChat = 'http://<IP-EC2>:8086/api/v1';
```

---

## Consideraciones de Producción

### HTTPS
Para producción real, se recomienda:
- Usar un **Application Load Balancer (ALB)** con certificado SSL/TLS (ACM)
- O configurar **Nginx** como reverse proxy con Let's Encrypt en la instancia EC2

### Backups
- **RDS:** Activar backups automáticos (7 días recomendado)
- **Imágenes de incidencias:** Crear snapshots del volumen EBS periódicamente, o migrar a S3

### Escalado
- Para alta disponibilidad, migrar de EC2 única a **ECS Fargate** con ALB
- Usar **ElastiCache Redis** para sesiones E2E compartidas entre instancias

### Monitoreo
- Activar **CloudWatch Logs** para centralizar logs
- Configurar alarmas de CPU/RAM en EC2
- Revisar métricas de RDS (conexiones, latencia)

---

## Costos Estimados (Mensuales)

| Recurso | Tipo | Costo USD |
|---------|------|-----------|
| EC2 | t3.medium | ~$30 |
| RDS PostgreSQL | db.t3.micro (Free tier 12 meses) | ~$13 |
| EBS (disco) | 20 GB gp3 | ~$1.60 |
| Transferencia de datos | Saliente | ~$5-10 |
| **Total** | | **~$50/mes** |

