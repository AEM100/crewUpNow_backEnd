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
import org.example.bd.repository.UsuarioRepository; // Tu repositorio real
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
    private final AsistenciaRepository asistenciaRepository; // Asegúrate de añadirlo aquí

    // Constructor que inyecta todos los repositorios necesarios
    public ServerServices(
            UsuarioRepository usuarioRepository,
            EventoRepository eventoRepository,
            ChatRepository chatRepository,
            MensajeRepository mensajeRepository,
            UsuarioChatRepository usuarioChatRepository,
            AsistenciaRepository asistenciaRepository // <-- Inclúyelo aquí
    ) {
        this.usuarioRepository = usuarioRepository;
        this.eventoRepository = eventoRepository;
        this.chatRepository = chatRepository;
        this.mensajeRepository = mensajeRepository;
        this.usuarioChatRepository = usuarioChatRepository;
        this.asistenciaRepository = asistenciaRepository; // <-- Y aquí
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

            // Generamos el hash
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

            // 🔥 VALIDACIÓN DE BANEO: Bloqueamos acceso si el usuario está suspendido
            if (usuario.getIsBanned()) {
                response.put("status", "ERROR");
                response.put("message", "Tu cuenta ha sido suspendida por un administrador.");
                return response;
            }

            // Comprobación de seguridad con BCrypt
            if (!BCrypt.checkpw(passwordIntentado, usuario.getContraseña())) {
                response.put("status", "ERROR");
                response.put("message", "Contraseña incorrecta.");
                return response;
            }

            usuario.setUltimaConexion(LocalDateTime.now());
            usuarioRepository.save(usuario);

            // Respuesta completa
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

            // 1. Crear el Evento
            Evento evento = new Evento();
            evento.setTitulo(request.get("title").asText());
            evento.setDescripcion(request.get("description").asText());
            evento.setUbicacion(request.get("ubicacion").asText());
            evento.setCreador(creador);
            evento.setFechaEvento(LocalDateTime.parse(request.get("fecha").asText()));

            // No necesitamos inicializar el Set manualmente, JPA lo maneja.
            // Guardamos primero el evento para obtener su ID necesario para la Asistencia
            evento = eventoRepository.save(evento);

            // 2. Registrar al creador como la primera Asistencia
            Asistencia asistenciaCreador = new Asistencia();
            asistenciaCreador.setId(new AsistenciaId(evento.getId(), creador.getId()));
            asistenciaCreador.setEvento(evento);
            asistenciaCreador.setUsuario(creador);
            asistenciaCreador.setEstado("aceptado");
            asistenciaRepository.save(asistenciaCreador);

            // 3. Crear el Chat Grupal asociado
            Chat chat = new Chat();
            chat.setTipo(Chat.TipoChat.grupal);
            chat.setFechaCreacion(LocalDateTime.now());
            chat = chatRepository.save(chat);

            // 4. Vincular Chat al Evento y guardar de nuevo
            evento.setChat(chat);
            eventoRepository.save(evento);

            // 5. Crear la membresía del creador en el Chat (UsuarioChat)
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
            // 1. Extraemos el ID del usuario que será baneado
            int userIdToBan = request.get("userIdToBan").asInt();
            int adminId = request.get("adminId").asInt();
            Usuario admin = usuarioRepository.findById(adminId).orElse(null);

            if (admin == null || !admin.getIsAdmin()) {
                response.put("status", "ERROR");
                response.put("message", "No tienes permisos de administrador.");
                return response;
            }
            // 2. Buscamos al usuario en la BD
            Optional<Usuario> usuarioOpt = usuarioRepository.findById(userIdToBan);

            if (usuarioOpt.isEmpty()) {
                response.put("status", "ERROR");
                response.put("message", "El usuario a banear no existe.");
                return response;
            }

            Usuario usuario = usuarioOpt.get();

            // 3. Aplicamos el baneo
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

            // Obtenemos todos los eventos (o idealmente filtra en BD)
            List<Evento> todos = eventoRepository.findAllWithAsistencias();

            // Filtramos usando la nueva relación Asistencia
            List<Evento> misEventos = todos.stream()
                    // 🔥 CAMBIO: Filtramos accediendo al usuario dentro de la Asistencia
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

                // Verificación de seguridad para el organizador
                item.put("organizer", e.getCreador() != null ? e.getCreador().getNombre() : "Desconocido");

                // 🔥 CAMBIO: Usamos la colección de asistencias
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

            // 1. Buscamos el evento y usuario
            Evento evento = eventoRepository.findById(eventId)
                    .orElseThrow(() -> new RuntimeException("Evento no encontrado"));

            Usuario usuario = usuarioRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // 2. Validación de permisos
            if (usuario.getIsAdmin() || evento.getCreador().getId().equals(userId)) {

                // 🔥 CAMBIO CLAVE: No toques la colección 'asistencias' (no hagas .clear())
                // Deja que la base de datos maneje el borrado en cascada.
                // Si intentas hacer .clear(), Hibernate intenta cargar la colección
                // de la BD y ahí es donde peta por falta de sesión.

                // 3. Ejecución directa del borrado
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

            // Buscamos las entidades básicas
            Usuario usuario = usuarioRepository.findById(userId)
                    .orElseThrow(() -> new Exception("Usuario no encontrado"));
            Evento evento = eventoRepository.findById(eventoId)
                    .orElseThrow(() -> new Exception("Evento no encontrado"));

            if (unirse) {
                // Verificar si ya existe la asistencia para no duplicar
                AsistenciaId id = new AsistenciaId(eventoId, userId);
                if (!asistenciaRepository.existsById(id)) {
                    Asistencia nuevaAsistencia = new Asistencia();
                    nuevaAsistencia.setId(id);
                    nuevaAsistencia.setEvento(evento);
                    nuevaAsistencia.setUsuario(usuario);
                    // JPA se encarga de guardar los valores por defecto (fecha, estado)
                    asistenciaRepository.save(nuevaAsistencia);

                    // Lógica de Chat: Unir al usuario al chat del evento
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
                // Desapuntarse
                if (evento.getCreador().getId() == userId) {
                    response.put("status", "ERROR");
                    response.put("message", "El creador no puede desapuntarse de su propio evento.");
                    return response;
                }

                // Borrado directo por clave compuesta: ¡Adiós a los errores de sesión!
                asistenciaRepository.deleteById(new AsistenciaId(eventoId, userId));

                // Eliminar membresía de chat
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

            // Recuperamos los eventos. Como la relación es @OneToMany,
            // no hay riesgo de LazyInitializationException si accedemos desde dentro de @Transactional
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

                // 🔥 CAMBIO: Ahora accedemos a la colección de Asistencia
                item.put("participantsCount", e.getAsistencias().size());

                // 🔥 CAMBIO: Buscamos al usuario dentro de la colección de asistencias
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
            // Imprime en consola para comprobar si el JSON llega bien a esta función
            System.out.println("Procesando listarMisChats con JSON: " + request.toString());

            // .path() evita que la app cruja si el campo no viene
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
                if (chat == null) continue; // Protección por si hay basura en la BD

                ObjectNode chatJson = mapper.createObjectNode();
                chatJson.put("chatId", chat.getId());

                if (chat.getTipo() == Chat.TipoChat.grupal) {
                    // Buscamos el evento de forma segura.
                    // Si este repositorio te da error, asegúrate de que en EventoRepository exista:
                    // Evento findByChat_Id(Long chatId); o como tengas tu relación.
                    try {
                        Evento evento = eventoRepository.findByChatId(chat.getId());
                        chatJson.put("title", (evento != null && evento.getTitulo() != null) ? evento.getTitulo() : "Chat Grupal");
                    } catch (Exception exRepo) {
                        System.out.println("⚠️ Error buscando evento para el chat " + chat.getId() + ": " + exRepo.getMessage());
                        chatJson.put("title", "Chat Grupal"); // Salvavidas si el repositorio falla
                    }
                } else {
                    chatJson.put("title", "Chat Individual");
                }

                arrayChats.add(chatJson);
            }

            response.put("status", "SUCCESS");
            response.set("chats", arrayChats);

        } catch (Exception e) {
            // 🔥 CRUCIAL: Esto pintará en la consola de tu Java la línea exacta que está fallando
            System.out.println("❌ CRASH en listarMisChats:");
            e.printStackTrace();
            response.put("status", "ERROR");
        }
        return response;
    }

    public JsonNode cargarMensajes(JsonNode request) {
        ObjectNode response = mapper.createObjectNode();
        try {
            int chatId = request.get("chatId").asInt();
            // 1. Buscamos los mensajes ordenados por fecha
            List<Mensaje> mensajes = mensajeRepository.findByChatIdOrderByFechaEnvioAsc(chatId);

            ArrayNode arrayMensajes = mapper.createArrayNode();
            for (Mensaje m : mensajes) {
                ObjectNode msgJson = mapper.createObjectNode();
                msgJson.put("id", m.getId());
                msgJson.put("senderId", m.getSender().getId());
                msgJson.put("senderName", m.getSender().getNombre()); // O el campo nombre que uses
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

            // 🔥 HASHING AL ACTUALIZAR: Protegemos la nueva contraseña
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