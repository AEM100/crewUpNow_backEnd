package org.example.bd.repository;

import org.example.bd.model.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MensajeRepository extends JpaRepository<Mensaje, Integer> {

    List<Mensaje> findByChatIdOrderByFechaEnvioAsc(Integer chatId);
}