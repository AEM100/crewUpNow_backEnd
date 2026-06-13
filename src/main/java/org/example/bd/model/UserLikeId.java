package org.example.bd.model;


import java.io.Serializable;
import java.util.Objects;

public class UserLikeId implements Serializable {
    private Integer usuario;
    private Integer publicacion;

    public UserLikeId() {}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserLikeId that = (UserLikeId) o;
        return Objects.equals(usuario, that.usuario) && Objects.equals(publicacion, that.publicacion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usuario, publicacion);
    }
}