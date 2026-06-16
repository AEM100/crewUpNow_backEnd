package org.example.bd.repository;


import org.example.bd.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    // Hereda automáticamente todos los métodos: guardar, buscar por ID, borrar...
    Optional<Usuario> findByEmail(String email);
    // En UsuarioRepository.java
    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.asistencias WHERE u.id = :id")
    Optional<Usuario> findByIdWithAsistencias(@Param("id") Integer id);
}