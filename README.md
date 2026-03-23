# AulaSmart - Gestión de Aulas UCEVA

## Descripción del Proyecto
AulaSmart es una aplicación móvil desarrollada para la Unidad Central del Valle del Cauca (UCEVA), diseñada para optimizar la gestión y consulta de la disponibilidad de espacios físicos dentro del campus. El sistema permite a estudiantes y docentes visualizar en tiempo real qué aulas están ocupadas, qué asignatura se está impartiendo y quién es el docente responsable, facilitando la movilidad académica y el aprovechamiento de la infraestructura universitaria.

## Reglas de Colaboración
Para mantener la integridad del código y asegurar entregas de alta calidad, el equipo adopta el flujo de trabajo **GitFlow**. Se establecen las siguientes reglas de obligatorio cumplimiento para todos los desarrolladores:

### 1. Estructura de Ramas
* **main**: Contiene exclusivamente el código estable y versiones listas para despliegue. No se permiten cambios directos en esta rama.
* **develop**: Rama principal de integración donde se consolidan las funcionalidades terminadas antes de ser promovidas a producción.
* **feature/**: Ramas temporales para el desarrollo de nuevas características (ejemplo: `feature/parser-utf8`). Deben integrarse a develop mediante un Pull Request.

### 2. Política de Integración y Calidad
Toda contribución al repositorio debe pasar por un proceso de revisión formal bajo los siguientes criterios:
* **Pull Request Obligatorio**: Ningún cambio se fusiona sin un PR previo.
* **Checklist de Calidad**: Se debe verificar el cumplimiento de los principios Clean Code (SRP y Naming) y la correcta gestión de métricas de eficiencia.
* **Normalización de Datos**: Es mandatorio asegurar que la entrada de datos maneje correctamente caracteres especiales y valores nulos para evitar fallos en la interfaz.

## Configuración de Seguridad
La rama **main** cuenta con las siguientes reglas de protección configuradas en GitHub:
* **Restricción de cambios directos**: Se bloquea la capacidad de realizar `push` directamente a la rama principal.
* **Revisión de Pares**: Se requiere al menos una (1) aprobación de un compañero de equipo antes de realizar cualquier fusión.
* **Verificación de Plantilla**: Todo PR debe completar el formato de `PULL_REQUEST_TEMPLATE.md` definido en el repositorio.

## Autores
* Sebastián Morales Flórez - 230231002 Ingeniería de Sistemas,
* José Sebastian Arenas Moncada - Ingeniería de Sistemas,
* Angela Patricia Aponte Escudero - 230231005 - Ingeniería de Sistemas
* Jhon Edward Steven Loaiza - Ingeniería de Sistemas, 
