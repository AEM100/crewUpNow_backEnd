package org.example.bd.model;


import java.io.Serializable;
import java.util.Objects;

public class UsuarioChatId implements Serializable {
    private Integer chat;
    private Integer usuario;

    public UsuarioChatId() {}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UsuarioChatId that = (UsuarioChatId) o;
        return Objects.equals(chat, that.chat) && Objects.equals(usuario, that.usuario);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chat, usuario);
    }
}