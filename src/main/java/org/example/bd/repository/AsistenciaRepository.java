package org.example.bd.repository;

import org.example.bd.model.Asistencia;
import org.example.bd.model.AsistenciaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AsistenciaRepository extends JpaRepository<Asistencia, AsistenciaId> {

}