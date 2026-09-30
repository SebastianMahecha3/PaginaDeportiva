package Fifan.t.Egoisu.entidades;

import Fifan.t.Egoisu.entidades.enums.Rol;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Usuario registrado. La contraseña se guarda siempre como hash BCrypt. El rol lo decide el servidor, nunca el formulario. */
@Entity
@Table(name = "usuarios")
@Getter @Setter @NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String username;

    @Column(nullable = false)
    private String password;

    /** Segunda contraseña válida (hash BCrypt). Solo la usa el administrador, como respaldo. Puede ser null. */
    @Column(length = 100)
    private String passwordRespaldo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol = Rol.ROLE_USER;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();
}
