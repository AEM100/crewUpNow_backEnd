package org.example.bd.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 50)
    private String nombre;

    @Column(length = 100, unique = true)
    private String email;

    @Column(name = "url_foto", length = 255)
    private String urlFoto;

    @Column(length = 255)
    private String contraseña;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_cuenta")
    private TipoCuenta tipoCuenta;

    @Column(name = "recibe_notificacion")
    private Boolean recibeNotificacion;

    @Column(name = "ultima_conexion")
    private LocalDateTime ultimaConexion;

    public enum TipoCuenta { privada, publica }

    @Column(length = 255)
    private String bio;

    @Column(name = "is_admin")
    private Boolean isAdmin = false;

    @Column(name = "is_banned")
    private Boolean isBanned = false;

    // Sus Getters y Setters
    public Boolean getIsAdmin() { return isAdmin; }
    public void setIsAdmin(Boolean isAdmin) { this.isAdmin = isAdmin; }

    public Boolean getIsBanned() { return isBanned; }
    public void setIsBanned(Boolean isBanned) { this.isBanned = isBanned; }
    // 2. Añade sus correspondientes Getter y Setter al final de la clase:
    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }
    public Integer getId() {
        return id;
    }

    public String getContraseña() {
        return contraseña;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setUrlFoto(String urlFoto) {
        this.urlFoto = urlFoto;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setContraseña(String contraseña) {
        this.contraseña = contraseña;
    }

    public void setTipoCuenta(TipoCuenta tipoCuenta) {
        this.tipoCuenta = tipoCuenta;
    }

    public void setRecibeNotificacion(Boolean recibeNotificacion) {
        this.recibeNotificacion = recibeNotificacion;
    }

    public void setUltimaConexion(LocalDateTime ultimaConexion) {
        this.ultimaConexion = ultimaConexion;
    }
}