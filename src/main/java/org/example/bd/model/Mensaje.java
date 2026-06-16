package org.example.bd.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "mensaje")
public class Mensaje {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "chat_id")
    private Chat chat;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private Usuario sender;

    @Column(name = "contenido_texto", columnDefinition = "TEXT")
    private String contenidoTexto;

    @Enumerated(EnumType.STRING)
    private TipoMedia tipo;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    @Column(name = "url_media", length = 255)
    private String urlMedia;

    public enum TipoMedia { texto, foto, video }


    public Integer getId() {
        return id;
    }

    public Chat getChat() {
        return chat;
    }

    public Usuario getSender() {
        return sender;
    }

    public String getContenidoTexto() {
        return contenidoTexto;
    }

    public TipoMedia getTipo() {
        return tipo;
    }

    public LocalDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public String getUrlMedia() {
        return urlMedia;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setChat(Chat chat) {
        this.chat = chat;
    }

    public void setSender(Usuario sender) {
        this.sender = sender;
    }

    public void setTipo(TipoMedia tipo) {
        this.tipo = tipo;
    }

    public void setFechaEnvio(LocalDateTime fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public void setContenidoTexto(String contenidoTexto) {
        this.contenidoTexto = contenidoTexto;
    }

    public void setUrlMedia(String urlMedia) {
        this.urlMedia = urlMedia;
    }
}