package org.example.bd.repository;

import jakarta.transaction.Transactional;
import org.example.bd.model.Chat;
import org.example.bd.model.Usuario;
import org.example.bd.model.UsuarioChat;
import org.example.bd.model.UsuarioChatId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsuarioChatRepository extends JpaRepository<UsuarioChat, UsuarioChatId> {

    List<UsuarioChat> findByUsuario_Id(Integer usuarioId);
    boolean existsByUsuarioAndChat(Usuario usuario, Chat chat);
    @Modifying
    @Transactional
    void deleteByUsuarioAndChat(Usuario usuario, Chat chat);
}