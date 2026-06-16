package org.example.bd.model;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class AsistenciaId implements Serializable {
    private Integer eventoId;
    private Integer usuarioId;

    public AsistenciaId() {}

    public AsistenciaId(Integer eventoId, Integer usuarioId) {
        this.eventoId = eventoId;
        this.usuarioId = usuarioId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AsistenciaId that = (AsistenciaId) o;
        return Objects.equals(eventoId, that.eventoId) && Objects.equals(usuarioId, that.usuarioId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventoId, usuarioId);
    }
}