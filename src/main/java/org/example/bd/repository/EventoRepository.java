package org.example.bd.repository;


import org.example.bd.model.Evento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventoRepository extends JpaRepository<Evento, Integer> {

    Evento findByChatId(Integer chatId);

    @Query("SELECT e FROM Evento e LEFT JOIN FETCH e.asistencias")
    List<Evento> findAllWithAsistencias();

}