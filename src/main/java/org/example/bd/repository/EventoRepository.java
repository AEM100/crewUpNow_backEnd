package org.example.bd.repository;


import org.example.bd.model.Evento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventoRepository extends JpaRepository<Evento, Integer> {

    // 🔥 Este método custom es la clave para asociar tus chats con tus pantallas
    Evento findByChatId(Integer chatId);
}