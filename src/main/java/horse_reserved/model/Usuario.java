package horse_reserved.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "usuarios")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * Clase de usuario que permite el login e identificar a las personas que usan el sistema
 */
public class Usuario implements UserDetails {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "primer_nombre", nullable = false, length = 100)
    private String primerNombre;

    @NotBlank
    @Column(name = "primer_apellido", nullable = false, length = 100)
    private String primerApellido;

    @Column(name = "tipo_documento", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private TipoDocumento tipoDocumento;

    @NotBlank
    //@Positive
    @Column(name = "documento", nullable = false, length = 50)
    private String documento;

    @Email
    @NotBlank
    @Column(name = "email", nullable = false, unique = true, length = 200)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    //@NotBlank
    @Column(name = "telefono", length = 20)
    private String telefono;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private Rol role;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "password_changed_at", nullable = false)
    private Instant passwordChangedAt = Instant.EPOCH;

    @Builder.Default
    @Column(name = "habeas_data_consented", nullable = false)
    private Boolean habeasDataConsented = false;

    @Column(name = "habeas_data_consented_at")
    private Instant habeasDataConsentedAt;

    /**
     * Define una relacion de uno a muchos entre Cliente y reservas
     */
    @Builder.Default
    @OneToMany(mappedBy = "cliente")
    private List<Reserva> reservacionesComoCliente = new ArrayList<>();

    /**
     * Define una relacion de uno a muchos entre Operador y reservas
     */
    @Builder.Default
    @OneToMany(mappedBy = "operador")
    private List<Reserva> reservacionesComoOperador = new ArrayList<>();

    // Implementación de UserDetails

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }
}