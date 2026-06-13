package org.example.bd.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;


@Entity
@Table(name = "user_like")
@IdClass(UserLikeId.class)
public class UserLike {
    @Id
    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Id
    @ManyToOne
    @JoinColumn(name = "publicacion_id")
    private Publicacion publicacion;

    private LocalDateTime fecha;

    // Generar Getters, Setters y Constructores
}