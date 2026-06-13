package org.example.bd.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "amigo")
@IdClass(AmigoId.class)
public class Amigo {
    @Id
    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Id
    @ManyToOne
    @JoinColumn(name = "amigo_id")
    private Usuario amigo;

    @Column(name = "fecha_amistad")
    private LocalDateTime fechaAmistad;

    // Generar Getters y Setters
}