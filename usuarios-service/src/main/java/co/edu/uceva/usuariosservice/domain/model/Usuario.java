package co.edu.uceva.usuariosservice.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    @NotEmpty(message = "El nombre no puede estar vacío")
    @Size(min = 2, max = 30, message = "El nombre debe tener entre 2 y 30 caracteres")
    @Column(nullable = false)
    private String nombre;

    @NotEmpty(message = "El apellido no puede estar vacío")
    @Size(min = 2, max = 30, message = "El apellido debe tener entre 2 y 30 caracteres")
    @Column(nullable = false)
    private String apellido;

    @NotEmpty(message = "El correo no puede estar vacío")
    @Email(message = "Debe ser un correo válido (ejemplo@uceva.edu.co)")
    @Column(nullable = false, unique = true) // Agregué unique para evitar correos duplicados
    private String email;

    @NotEmpty(message = "La contraseña no puede estar vacía")
    @Size(min = 8, max = 255)
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[#$@!%&*-_?])[A-Za-z\\d#$@!%&*-_?]{8,}$",
            message = "La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial (#$@!%&*-_?)"
    )
    @Column(nullable = false)
    private String password;

    @NotEmpty(message = "El rol no puede estar vacío")
    @Pattern(
            regexp = "^(Estudiante|Coordinador|Docente|Administrativo|Decano|Rector|Administrador|Monitor|Directivo)$",
            message = "El rol debe ser uno de los siguientes: Estudiante, Docente, Coordinador, Administrativo, Decano, Rector, Monitor, Directivo o Administrador"
    )
    @Column(nullable = false)
    private String rol;

    // --- Getters y Setters ---

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long id) { this.codigo = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
}