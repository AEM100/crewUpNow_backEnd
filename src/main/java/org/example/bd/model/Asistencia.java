package org.example.bd.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuario_evento")
public class Asistencia {
    @EmbeddedId
    private AsistenciaId id;

    @ManyToOne
    @MapsId("eventoId")
    @JoinColumn(name = "evento_id")
    private Evento evento;

    @ManyToOne
    @MapsId("usuarioId")
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    private LocalDateTime fechaUnion;
    @Column(name = "estado", columnDefinition = "ENUM('pendiente', 'aceptado', 'rechazado')")
    private String estado = "aceptado";

    public AsistenciaId getId() {
        return id;
    }

    public Evento getEvento() {
        return evento;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public LocalDateTime getFechaUnion() {
        return fechaUnion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEvento(Evento evento) {
        this.evento = evento;
    }

    public void setId(AsistenciaId id) {
        this.id = id;
    }

    public void setFechaUnion(LocalDateTime fechaUnion) {
        this.fechaUnion = fechaUnion;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
    // Getters y Setters...
}
