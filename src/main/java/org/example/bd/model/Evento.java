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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "evento")
public class Evento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @OneToOne(cascade ={CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "chat_id", referencedColumnName = "id")
    private Chat chat; // 🔥 El chat grupal de este evento

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

    // 2. Modifica la relación con los Asistentes
    @ManyToMany(fetch = FetchType.EAGER) // 🔥 Cambiado a EAGER
    @JoinTable(
            name = "usuario_evento",
            joinColumns = @JoinColumn(name = "evento_id"),
            inverseJoinColumns = @JoinColumn(name = "usuario_id")
    )
    private Set<Usuario> asistentes;

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

    public Set<Usuario> getAsistentes() {
        return asistentes;
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

    public void setAsistentes(Set<Usuario> asistentes) {
        this.asistentes = asistentes;
    }
}