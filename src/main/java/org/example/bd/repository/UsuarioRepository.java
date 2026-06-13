package org.example.bd.repository;


import org.example.bd.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    // Hereda automáticamente todos los métodos: guardar, buscar por ID, borrar...
    Optional<Usuario> findByEmail(String email);
}