package org.example.bd.repository;

import org.example.bd.model.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
// 🔥 CORREGIDO: Cambiado Long por Integer para que coincida con tu entidad Mensaje
public interface MensajeRepository extends JpaRepository<Mensaje, Integer> {

    // Spring mapeará esto automáticamente buscando por el objeto 'chat' e 'id' interno
    List<Mensaje> findByChatIdOrderByFechaEnvioAsc(Integer chatId);
}