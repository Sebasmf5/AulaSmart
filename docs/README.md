# Documentación Técnica — AulaSmart

> **Versión:** 1.0.0  
> **Última actualización:** 2026-05-27  
> **Ambiente:** Microservicios Spring Boot + PostgreSQL + Flutter  

---

## Índice

1. [Arquitectura General](./MICROSERVICES.md)
2. [Matriz de Roles y Permisos](./ROLES.md)
3. [Referencia de Errores HTTP](./ERROR-CODES.md)
4. [API: usuarios-service](./API-USUARIOS.md)
5. [API: aula-service](./API-AULA.md)
6. [API: reserva-service](./API-RESERVA.md)
7. [API: chat-service](./API-CHAT.md)
8. [API: incidencia-service](./API-INCIDENCIA.md)
9. [API: Seguridad (Crypto/E2E)](./API-SECURITY.md)
10. [Guía de Despliegue](./DEPLOYMENT.md)

---

## Visión General

AulaSmart es un sistema de gestión de espacios académicos desarrollado para la **Unidad Central del Valle del Cauca (UCEVA)**. La plataforma permite a docentes, estudiantes y personal administrativo:

- Consultar y reservar aulas universitarias.
- Interactuar con un asistente virtual impulsado por inteligencia artificial.
- Reportar incidencias de infraestructura o equipamiento y recibir respuesta formal de la administración.
- Administrar usuarios del sistema con control de acceso basado en roles.

La arquitectura se compone de cinco microservicios independientes, un núcleo de seguridad compartido (`security-core`) y una aplicación móvil desarrollada en Flutter.

---

## Stack Tecnológico

| Capa | Tecnología |
|------|------------|
| Backend | Spring Boot 3.x, Java 17 |
| Seguridad | Spring Security, JWT, RSA+AES (E2E) |
| IA | Spring AI (OpenAI compatible) — modelo `kimi-k2.6` vía OpenCode Go |
| Base de datos | PostgreSQL 15 |
| Comunicación | REST, Feign Client |
| Contenerización | Docker, Docker Compose |
| Frontend | Flutter (Dart), Dio |

---

## Contacto y Mantenimiento

- **Repositorio:** `https://github.com/uceva/aulasmart`
- **Responsable técnico:** Equipo de desarrollo PIAulaSmart
- **Entidad:** Unidad Central del Valle del Cauca (UCEVA) — Tuluá, Colombia
