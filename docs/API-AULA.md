# API Reference: `aula-service`

> **Puerto:** `8083`  
> **URL Base:** `http://<host>:8083/api/v1/aula-service`  
> **E2E:** No implementado. Peticiones en plano.  

---

## Aulas

### `GET /aulas`
Lista todas las aulas registradas.

**Acceso:** Todos los roles autenticados.

**Response 200:**
```json
{
  "aulas": [
    {
      "id": 1,
      "codigo": 101,
      "nombre": "Aula 101",
      "capacidad": 40,
      "tipoAula": "78",
      "codigoAula": 1001,
      "bloqueId": 1,
      "facultadId": 2
    }
  ]
}
```

---

### `GET /aulas/{id}`
Obtiene un aula por su ID interno.

**Acceso:** Todos los roles autenticados.

---

### `GET /aulas/codigo/{codigo}`
Obtiene un aula por su código de negocio.

**Acceso:** Todos los roles autenticados.

---

### `GET /aulas/tipo/{codigo}`
Obtiene el nombre del tipo de aula dado su código.

**Acceso:** Todos los roles autenticados.

**Response 200:** Texto plano con el nombre del tipo.

---

### `GET /aulas/siga/{codigo}`
Obtiene el código SIGA asociado a un aula.

**Acceso:** Todos los roles autenticados.

**Response 200:** Número (`Long`).

---

### `GET /aulas/requiere-autorizacion/{codigo}`
Indica si el tipo de aula requiere autorización administrativa para ser reservado.

**Acceso:** Todos los roles autenticados.

**Response 200:** `true` o `false`.

---

### `GET /aulas/bloque/{bloqueId}`
Lista aulas filtradas por bloque.

**Acceso:** Todos los roles autenticados.

---

### `GET /aulas/facultad/{facultadId}`
Lista aulas filtradas por facultad.

**Acceso:** Todos los roles autenticados.

---

### `GET /aulas/buscar/{nombreAula}`
Busca aulas por nombre (búsqueda parcial).

**Acceso:** Todos los roles autenticados.

---

### `GET /aulas/tipo-aula/{tipoAula}`
Lista aulas filtradas por tipo (código numérico).

**Acceso:** Todos los roles autenticados.

---

### `GET /aulas/codigos`
Lista todos los códigos de aula registrados.

**Acceso:** Todos los roles autenticados.

**Response 200:** `List<Long>`

---

### `GET /aulas/codigos-siga`
Lista los códigos SIGA sincronizados.

**Acceso:** Todos los roles autenticados.

---

### `GET /aulas/sincronizadas-siga`
Lista aulas sincronizadas con SIGA como pares `{id, codigoAula}`.

**Acceso:** Todos los roles autenticados.

---

### `POST /aulas`
Crea una nueva aula.

**Acceso:** `ADMINISTRADOR`, `ADMINISTRATIVO`

**Request Body:** Objeto `Aula`.

---

### `PUT /aulas`
Actualiza un aula existente.

**Acceso:** `ADMINISTRADOR`, `ADMINISTRATIVO`

---

### `DELETE /aulas/{id}`
Elimina un aula.

**Acceso:** `ADMINISTRADOR`, `ADMINISTRATIVO`

---

## Tipos de Aula

### `GET /tipos-aula`
Lista todos los tipos de aula disponibles.

**Acceso:** Todos los roles autenticados.

---

## Facultades

### `GET /facultades`
Lista todas las facultades.

**Acceso:** Todos los roles autenticados.

---

## Bloques

### `GET /bloques`
Lista todos los bloques físicos del campus.

**Acceso:** Todos los roles autenticados.

---

### `GET /bloques/facultad/{facultadId}`
Lista bloques pertenecientes a una facultad.

**Acceso:** Todos los roles autenticados.

---

### `GET /bloques/buscar/{nombre}`
Busca bloques por nombre.

**Acceso:** Todos los roles autenticados.

---

## Integración SIGA

El `aula-service` sincroniza periódicamente (o bajo demanda) los espacios físicos con el sistema universitario **SIGA** mediante el endpoint externo:

```
GET https://uceva.datasae.co/siga_new/web/app.php/publicomanejoespacios
```

Los endpoints `/aulas/siga/*` y `/aulas/codigos-siga` exponen esta información para que otros microservicios (especialmente `reserva-service`) puedan validar disponibilidad contra el sistema oficial de la universidad.

