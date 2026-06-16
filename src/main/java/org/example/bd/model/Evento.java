package org.example.bd.model;


import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "evento")
public class Evento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @OneToOne(cascade = {CascadeType.MERGE, CascadeType.REFRESH, CascadeType.REMOVE})
    @JoinColumn(name = "chat_id", referencedColumnName = "id")
    private Chat chat;

    // Su Getter y Setter
    public Chat getChat() { return chat; }
    public void setChat(Chat chat) { this.chat = chat; }
    @Column(length = 100, nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "fecha_evento", nullable = false)
    private LocalDateTime fechaEvento;

    @Column(length = 150)
    private String ubicacion;

    // 1. Modifica la relación con el Creador
    @ManyToOne(fetch = FetchType.EAGER) // 🔥 Cambiado a EAGER
    @JoinColumn(name = "creador_id")
    private Usuario creador;

    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Asistencia> asistencias = new HashSet<>();

    // Getter para contar participantes fácilmente
    public int getParticipantsCount() {
        return asistencias.size();
    }

    // Generar Getters, Setters y Constructores

    public Integer getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public LocalDateTime getFechaEvento() {
        return fechaEvento;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public Usuario getCreador() {
        return creador;
    }

    public Set<Asistencia> getAsistencias() {
        return asistencias;
    }

    public void setAsistencias(Set<Asistencia> asistencias) {
        this.asistencias = asistencias;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setFechaEvento(LocalDateTime fechaEvento) {
        this.fechaEvento = fechaEvento;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public void setCreador(Usuario creador) {
        this.creador = creador;
    }

}