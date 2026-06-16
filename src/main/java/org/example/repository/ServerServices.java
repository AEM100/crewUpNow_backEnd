package org.example.repository;

import java.sql.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.transaction.Transactional;
import org.example.bd.model.Asistencia;
import org.example.bd.model.AsistenciaId;
import org.example.bd.model.Chat;
import org.example.bd.model.Evento;
import org.example.bd.model.Mensaje;
import org.example.bd.model.Usuario;
import org.example.bd.model.UsuarioChat;
import org.example.bd.repository.AsistenciaRepository;
import org.example.bd.repository.ChatRepository;
import org.example.bd.repository.EventoRepository;
import org.example.bd.repository.MensajeRepository;
import org.example.bd.repository.UsuarioChatRepository;
import org.example.bd.repository.UsuarioRepository;
import org.mindrot.jbcrypt.BCrypt;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;


public class ServerServices {
    private final ObjectMapper mapper = new ObjectMapper();
    private final UsuarioRepository usuarioRepository;
    private final EventoRepository eventoRepository;
    private final ChatRepository chatRepository;
    private final MensajeRepository mensajeRepository;
    private final UsuarioChatRepository usuarioChatRepository;
    private final AsistenciaRepository asistenciaRepository;

    public ServerServices(
            UsuarioRepository usuarioRepository,
            EventoRepository eventoRepository,
            ChatRepository chatRepository,
            MensajeRepository mensajeRepository,
            UsuarioChatRepository usuarioChatRepository,
            AsistenciaRepository asistenciaRepository
    ) {
        this.usuarioRepository = usuarioRepository;
        this.eventoRepository = eventoRepository;
        this.chatRepository = chatRepository;
        this.mensajeRepository = mensajeRepository;
        this.usuarioChatRepository = usuarioChatRepository;
        this.asistenciaRepository = asistenciaRepository;
    }
    public JsonNode register(JsonNode request) {
        ObjectNode response = mapper.createObjectNode();
        try {
            String nombre = request.get("name").asText();
            String email = request.get("email").asText();
            String passwordPlano = request.get("password").asText();

            Usuario nuevoUsuario = new Usuario();
            nuevoUsuario.setNombre(nombre);
            nuevoUsuario.setEmail(email);

            String passwordHash = BCrypt.hashpw(passwordPlano, BCrypt.gensalt());
            nuevoUsuario.setContraseña(passwordHash);

            nuevoUsuario.setBio("");
            nuevoUsuario.setFotoBase64("");
            nuevoUsuario.setTipoCuenta(Usuario.TipoCuenta.publica);
            nuevoUsuario.setRecibeNotificacion(true);
            nuevoUsuario.setUltimaConexion(LocalDateTime.now());
            nuevoUsuario.setIsAdmin(false);
            nuevoUsuario.setIsBanned(false);

            Usuario usuarioGuardado = usuarioRepository.save(nuevoUsuario);

            response.put("status", "SUCCESS");
            response.put("id", usuarioGuardado.getId());
            response.put("name", usuarioGuardado.getNombre());
            response.put("email", usuarioGuardado.getEmail());
            response.put("bio", "");
            response.put("foto_base64", "");
            response.put("isAdmin", false);
            response.put("isBanned", false);
            response.put("token", "TOKEN_" + usuarioGuardado.getId());

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Error al registrar el usuario: " + e.getMessage());
        }
        return response;
    }

    public JsonNode login(JsonNode request) {

        ObjectNode response = mapper.createObjectNode();
        try {
            String email = request.get("email").asText();
            String passwordIntentado = request.get("password").asText();

            Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);

            if (usuarioOpt.isEmpty()) {
                response.put("status", "ERROR");
                response.put("message", "El correo electrónico no está registrado.");
                return response;
            }

            Usuario usuario = usuarioOpt.get();

            if (usuario.getIsBanned()) {
                response.put("status", "ERROR");
                response.put("message", "Tu cuenta ha sido suspendida por un administrador.");
                return response;
            }

            if (!BCrypt.checkpw(passwordIntentado, usuario.getContraseña())) {
                response.put("status", "ERROR");
                response.put("message", "Contraseña incorrecta.");
                return response;
            }

            usuario.setUltimaConexion(LocalDateTime.now());
            usuarioRepository.save(usuario);

            response.put("status", "SUCCESS");
            response.put("id", usuario.getId());
            response.put("name", usuario.getNombre());
            response.put("email", usuario.getEmail());
            response.put("bio", usuario.getBio() != null ? usuario.getBio() : "");
            response.put("foto_base64", usuario.getFotoBase64() != null ? usuario.getFotoBase64() : "");
            response.put("isAdmin", usuario.getIsAdmin());
            response.put("isBanned", usuario.getIsBanned());
            response.put("token", "TOKEN_" + usuario.getId());

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Error crítico: " + e.getMessage());
        }
        return response;
    }
    @Transactional
    public JsonNode crearEvento(JsonNode request) {
        ObjectNode response = mapper.createObjectNode();
        try {
            int userId = request.get("userId").asInt();
            Usuario creador = usuarioRepository.findById(userId)
                    .orElseThrow(() -> new Exception("El usuario creador no existe."));

            Evento evento = new Evento();
            evento.setTitulo(request.get("title").asText());
            evento.setDescripcion(request.get("description").asText());
            evento.setUbicacion(request.get("ubicacion").asText());
            evento.setCreador(creador);
            evento.setFechaEvento(LocalDateTime.parse(request.get("fecha").asText()));


            evento = eventoRepository.save(evento);

            Asistencia asistenciaCreador = new Asistencia();
            asistenciaCreador.setId(new AsistenciaId(evento.getId(), creador.getId()));
            asistenciaCreador.setEvento(evento);
            asistenciaCreador.setUsuario(creador);
            asistenciaCreador.setEstado("aceptado");
            asistenciaRepository.save(asistenciaCreador);

            Chat chat = new Chat();
            chat.setTipo(Chat.TipoChat.grupal);
            chat.setFechaCreacion(LocalDateTime.now());
            chat = chatRepository.save(chat);

            evento.setChat(chat);
            eventoRepository.save(evento);

            UsuarioChat membresia = new UsuarioChat();
            membresia.setChat(chat);
            membresia.setUsuario(creador);
            membresia.setFecha(LocalDateTime.now());
            usuarioChatRepository.save(membresia);

            response.put("status", "SUCCESS");
            response.put("id", evento.getId());
            response.put("chatId", chat.getId());

        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "ERROR");
            response.put("message", "Error al crear evento: " + e.getMessage());
        }
        return response;
    }
    @Transactional
    public JsonNode banearUsuario(JsonNode request) {
        ObjectNode response = mapper.createObjectNode();
        try {
            int userIdToBan = request.get("userIdToBan").asInt();
            int adminId = request.get("adminId").asInt();
            Usuario admin = usuarioRepository.findById(adminId).orElse(null);

            if (admin == null || !admin.getIsAdmin()) {
                response.put("status", "ERROR");
                response.put("message", "No tienes permisos de administrador.");
                return response;
            }
            Optional<Usuario> usuarioOpt = usuarioRepository.findById(userIdToBan);

            if (usuarioOpt.isEmpty()) {
                response.put("status", "ERROR");
                response.put("message", "El usuario a banear no existe.");
                return response;
            }

            Usuario usuario = usuarioOpt.get();

            usuario.setIsBanned(true);
            usuarioRepository.save(usuario);

            response.put("status", "SUCCESS");
            response.put("message", "Usuario " + usuario.getNombre() + " baneado correctamente.");

        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "ERROR");
            response.put("message", "Error al procesar el baneo: " + e.getMessage());
        }
        return response;
    }
    @Transactional
    public JsonNode listarMisProximosEventos(JsonNode request) {
        ObjectNode response = mapper.createObjectNode();
        try {
            int userId = request.get("userId").asInt();
            LocalDateTime ahora = LocalDateTime.now();

            List<Evento> todos = eventoRepository.findAllWithAsistencias();

            List<Evento> misEventos = todos.stream()
                    .filter(e -> e.getAsistencias().stream()
                            .anyMatch(a -> a.getUsuario().getId() == userId))
                    .filter(e -> e.getFechaEvento().isAfter(ahora))
                    .sorted(Comparator.comparing(Evento::getFechaEvento))
                    .toList();

            ArrayNode arrayEventos = mapper.createArrayNode();

            for (Evento e : misEventos) {
                ObjectNode item = mapper.createObjectNode();
                item.put("id", e.getId());
                item.put("title", e.getTitulo());
                item.put("description", e.getDescripcion());
                item.put("ubicacion", e.getUbicacion());
                item.put("fecha", e.getFechaEvento().toString());

                item.put("organizer", e.getCreador() != null ? e.getCreador().getNombre() : "Desconocido");

                item.put("participantsCount", e.getAsistencias().size());

                arrayEventos.add(item);
            }

            response.put("status", "SUCCESS");
            response.set("events", arrayEventos);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "ERROR");
            response.put("message", "Error al listar mis eventos: " + e.getMessage());
        }
        return response;
    }

    @Transactional
    public JsonNode eliminarEvento(JsonNode request) {
        ObjectNode response = mapper.createObjectNode();
        try {
            int userId = request.get("userId").asInt();
            int eventId = request.get("eventId").asInt();

            Evento evento = eventoRepository.findById(eventId)
                    .orElseThrow(() -> new RuntimeException("Evento no encontrado"));

            Usuario usuario = usuarioRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            if (usuario.getIsAdmin() || evento.getCreador().getId().equals(userId)) {


                eventoRepository.delete(evento);
                eventoRepository.flush();

                response.put("status", "SUCCESS");
                response.put("message", "Evento, chat y mensajes eliminados correctamente.");
            } else {
                response.put("status", "ERROR");
                response.put("message", "No tienes permiso para eliminar este evento.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "ERROR");
            response.put("message", "Error al eliminar: " + e.getMessage());
        }
        return response;
    }
    @Transactional
    public JsonNode unirseEvento(JsonNode request) {
        ObjectNode response = mapper.createObjectNode();
        try {
            int userId = request.get("userId").asInt();
            int eventoId = request.get("eventId").asInt();
            boolean unirse = request.get("join").asBoolean();

            Usuario usuario = usuarioRepository.findById(userId)
                    .orElseThrow(() -> new Exception("Usuario no encontrado"));
            Evento evento = eventoRepository.findById(eventoId)
                    .orElseThrow(() -> new Exception("Evento no encontrado"));

            if (unirse) {
                AsistenciaId id = new AsistenciaId(eventoId, userId);
                if (!asistenciaRepository.existsById(id)) {
                    Asistencia nuevaAsistencia = new Asistencia();
                    nuevaAsistencia.setId(id);
                    nuevaAsistencia.setEvento(evento);
                    nuevaAsistencia.setUsuario(usuario);
                    asistenciaRepository.save(nuevaAsistencia);

                    if (evento.getChat() != null) {
                        boolean yaEsMiembro = usuarioChatRepository.existsByUsuarioAndChat(usuario, evento.getChat());
                        if (!yaEsMiembro) {
                            UsuarioChat membresia = new UsuarioChat();
                            membresia.setChat(evento.getChat());
                            membresia.setUsuario(usuario);
                            membresia.setFecha(LocalDateTime.now());
                            usuarioChatRepository.save(membresia);
                        }
                    }
                }
                response.put("status", "SUCCESS");
                response.put("message", "Te has unido al evento.");

            } else {
                if (evento.getCreador().getId() == userId) {
                    response.put("status", "ERROR");
                    response.put("message", "El creador no puede desapuntarse de su propio evento.");
                    return response;
                }

                asistenciaRepository.deleteById(new AsistenciaId(eventoId, userId));

                if (evento.getChat() != null) {
                    usuarioChatRepository.deleteByUsuarioAndChat(usuario, evento.getChat());
                }

                response.put("status", "SUCCESS");
                response.put("message", "Te has desapuntado.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "ERROR");
            response.put("message", "Error al procesar la solicitud: " + e.getMessage());
        }
        return response;
    }

    @Transactional
    public JsonNode listarEventos(JsonNode request) {
        ObjectNode response = mapper.createObjectNode();
        try {
            int currentUserId = request.has("userId") ? request.get("userId").asInt() : -1;

            List<Evento> eventos = eventoRepository.findAllWithAsistencias();
            ArrayNode arrayEventos = mapper.createArrayNode();

            for (Evento e : eventos) {
                ObjectNode item = mapper.createObjectNode();
                item.put("id", e.getId());
                item.put("title", e.getTitulo());
                item.put("description", e.getDescripcion());
                item.put("ubicacion", e.getUbicacion());
                item.put("fecha", e.getFechaEvento() != null ? e.getFechaEvento().toString() : "");

                if (e.getCreador() != null) {
                    item.put("organizer", e.getCreador().getNombre());
                    item.put("creatorId", e.getCreador().getId());
                } else {
                    item.put("organizer", "Desconocido");
                    item.put("creatorId", 0);
                }

                item.put("participantsCount", e.getAsistencias().size());

                boolean estaApuntado = e.getAsistencias().stream()
                        .anyMatch(a -> a.getUsuario().getId() == currentUserId);

                item.put("isUserJoined", estaApuntado);

                arrayEventos.add(item);
            }

            response.put("status", "SUCCESS");
            response.set("events", arrayEventos);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "ERROR");
            response.put("message", "Error al listar eventos: " + e.getMessage());
        }
        return response;
    }

    @Transactional
    public JsonNode eliminarCuenta(int userId) {
        ObjectNode resultado = mapper.createObjectNode();
        try {
            if (!usuarioRepository.existsById(userId)) {
                resultado.put("status", "ERROR");
                resultado.put("message", "No se pudo eliminar: El usuario no existía.");
                return resultado;
            }

            usuarioRepository.deleteById(userId);
            resultado.put("status", "SUCCESS");
            resultado.put("message", "Usuario eliminado correctamente de la base de datos");

        } catch (Exception e) {
            e.printStackTrace();
            resultado.put("status", "ERROR");
            resultado.put("message", "Error de Spring Data JPA en el servidor: " + e.getMessage());
        }
        return resultado;
    }
    public JsonNode listarMisChats(JsonNode request) {
        ObjectNode response = mapper.createObjectNode();
        try {
            System.out.println("Procesando listarMisChats con JSON: " + request.toString());

            JsonNode userIdNode = request.path("userId");
            if (userIdNode.isMissingNode() || userIdNode.isNull()) {
                System.out.println("❌ ERROR: El campo 'userId' no llegó en la petición.");
                response.put("status", "ERROR");
                return response;
            }

            int userId = userIdNode.asInt();

            List<UsuarioChat> membresias = usuarioChatRepository.findByUsuario_Id(userId);
            System.out.println("Membresías encontradas para el usuario " + userId + ": " + membresias.size());

            ArrayNode arrayChats = mapper.createArrayNode();

            for (UsuarioChat uc : membresias) {
                Chat chat = uc.getChat();
                if (chat == null) continue;

                ObjectNode chatJson = mapper.createObjectNode();
                chatJson.put("chatId", chat.getId());

                if (chat.getTipo() == Chat.TipoChat.grupal) {

                    try {
                        Evento evento = eventoRepository.findByChatId(chat.getId());
                        chatJson.put("title", (evento != null && evento.getTitulo() != null) ? evento.getTitulo() : "Chat Grupal");
                    } catch (Exception exRepo) {
                        System.out.println("⚠️ Error buscando evento para el chat " + chat.getId() + ": " + exRepo.getMessage());
                        chatJson.put("title", "Chat Grupal");
                    }
                } else {
                    chatJson.put("title", "Chat Individual");
                }

                arrayChats.add(chatJson);
            }

            response.put("status", "SUCCESS");
            response.set("chats", arrayChats);

        } catch (Exception e) {
            System.out.println(" CRASH en listarMisChats:");
            e.printStackTrace();
            response.put("status", "ERROR");
        }
        return response;
    }

    public JsonNode cargarMensajes(JsonNode request) {
        ObjectNode response = mapper.createObjectNode();
        try {
            int chatId = request.get("chatId").asInt();
            List<Mensaje> mensajes = mensajeRepository.findByChatIdOrderByFechaEnvioAsc(chatId);

            ArrayNode arrayMensajes = mapper.createArrayNode();
            for (Mensaje m : mensajes) {
                ObjectNode msgJson = mapper.createObjectNode();
                msgJson.put("id", m.getId());
                msgJson.put("senderId", m.getSender().getId());
                msgJson.put("senderName", m.getSender().getNombre());
                msgJson.put("content", m.getContenidoTexto());
                msgJson.put("type", m.getTipo().toString());
                msgJson.put("timestamp", m.getFechaEnvio().toString());
                arrayMensajes.add(msgJson);
            }

            response.put("status", "SUCCESS");
            response.set("messages", arrayMensajes);
        } catch (Exception e) {
            response.put("status", "ERROR");
        }
        return response;
    }

    public JsonNode enviarMensaje(JsonNode request) {
        ObjectNode response = mapper.createObjectNode();
        try {
            int chatId = request.get("chatId").asInt();
            int userId = request.get("userId").asInt();
            String text = request.get("content").asText();

            Chat chat = chatRepository.findById(chatId).orElseThrow();
            Usuario usuario = usuarioRepository.findById(userId).orElseThrow();

            Mensaje nuevoMensaje = new Mensaje();
            nuevoMensaje.setChat(chat);
            nuevoMensaje.setSender(usuario);
            nuevoMensaje.setContenidoTexto(text);
            nuevoMensaje.setTipo(Mensaje.TipoMedia.texto);
            nuevoMensaje.setFechaEnvio(LocalDateTime.now());

            mensajeRepository.save(nuevoMensaje);
            response.put("status", "SUCCESS");
        } catch (Exception e) {
            response.put("status", "ERROR");
        }
        return response;
    }
    @Transactional
    public JsonNode actualizarPerfil(JsonNode request) {
        ObjectNode response = mapper.createObjectNode();
        try {
            int userId = request.get("userId").asInt();
            String nuevoNombre = request.get("name").asText();
            String nuevoEmail = request.get("email").asText();
            String nuevaBio = request.get("bio").asText();
            String nuevaPassword = request.has("password") ? request.get("password").asText() : null;
            String nuevaFotoBase64 = request.has("foto_base64") ? request.get("foto_base64").asText() : null;

            Optional<Usuario> usuarioOpt = usuarioRepository.findById(userId);

            if (usuarioOpt.isEmpty()) {
                response.put("status", "ERROR");
                response.put("message", "El usuario no existe.");
                return response;
            }

            Usuario usuario = usuarioOpt.get();

            usuario.setNombre(nuevoNombre);
            usuario.setEmail(nuevoEmail);
            usuario.setBio(nuevaBio);

            if (nuevaPassword != null && !nuevaPassword.isEmpty()) {
                String nuevoHash = BCrypt.hashpw(nuevaPassword, BCrypt.gensalt());
                usuario.setContraseña(nuevoHash);
            }

            if (nuevaFotoBase64 != null && !nuevaFotoBase64.isEmpty()) {
                usuario.setFotoBase64(nuevaFotoBase64);
            }

            usuarioRepository.save(usuario);

            response.put("status", "SUCCESS");
            response.put("message", "Perfil actualizado correctamente");

        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "ERROR");
            response.put("message", "Error al actualizar perfil: " + e.getMessage());
        }
        return response;
    }
}