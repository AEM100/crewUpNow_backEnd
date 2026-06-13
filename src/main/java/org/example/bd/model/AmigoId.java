package org.example.bd.model;


import java.io.Serializable;
import java.util.Objects;

public class AmigoId implements Serializable {
    private Integer usuario;
    private Integer amigo;

    public AmigoId() {}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AmigoId amigoId = (AmigoId) o;
        return Objects.equals(usuario, amigoId.usuario) && Objects.equals(amigo, amigoId.amigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usuario, amigo);
    }
}