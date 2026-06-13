package org.example.bd.model;


import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuario_chat")
@IdClass(UsuarioChatId.class)
public class UsuarioChat {
    @Id
    @ManyToOne
    @JoinColumn(name = "chat_id")
    private Chat chat;

    @Id
    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    private LocalDateTime fecha;

    // Generar Getters y Setters

    public Chat getChat() {
        return chat;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setChat(Chat chat) {
        this.chat = chat;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
}