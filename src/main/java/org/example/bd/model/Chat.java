package org.example.bd.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat")
public class Chat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 30, nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoChat tipo;

    public enum TipoChat {
        individual, grupal
    }

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;


    // Generar Getters, Setters y Constructores

    public Integer getId() {
        return id;
    }

    public TipoChat getTipo() {
        return tipo;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public void setTipo(TipoChat tipo) {
        this.tipo = tipo;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}