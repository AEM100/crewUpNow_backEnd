package org.example.bd.repository;

import org.example.bd.model.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Integer> {
    // Hereda los métodos estándar como findById, save, etc.
}