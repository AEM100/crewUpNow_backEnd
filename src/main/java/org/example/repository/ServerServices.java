package org.example.repository;

import java.sql.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.transaction.Transactional;
import org.example.bd.model.Chat;
import org.example.bd.model.Evento;
import org.example.bd.model.Mensaje;
import org.example.bd.model.Usuario;
import org.example.bd.model.UsuarioChat;
import org.example.bd.repository.ChatRepository;
import org.example.bd.repository.EventoRepository;
import org.example.bd.repository.MensajeRepository;
import org.example.bd.repository.UsuarioChatRepository;
import org.example.bd.repository.UsuarioRepository; // Tu repositorio real
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public class ServerServices {
    private final ObjectMapper mapper = new ObjectMapper();
    private final UsuarioRepository usuarioRepository;
    private final EventoRepository eventoRepository;
    private final ChatRepository chatRepository; // 🔥 CORREGIDO: Tipo correcto
    private final MensajeRepository mensajeRepository;
    private final UsuarioChatRepository usuarioChatRepository;

    // 🔥 CORREGIDO: Constructor recibe los 5 repositorios obligatorios
    public ServerServices(
            UsuarioRepository usuarioRepository,
            EventoRepository eventoRepository,
            ChatRepository chatRepository,
            MensajeRepository mensajeRepository,
            UsuarioChatRepository usuarioChatRepository
    ) {
        this.usuarioRepository = usuarioRepository;
        this.eventoRepository = eventoRepository;
        this.chatRepository = chatRepository;
        this.mensajeRepository = mensajeRepository;
        this.usuarioChatRepository = usuarioChatRepository;
    } // 🔥 CORREGIDO: Se quitó la llave extra que cerraba la clase aquí

    public JsonNode register(JsonNode request) {
        ObjectNode response = mapper.createObjectNode();
        try {
            String nombre = request.get("name").asText();
            String email = request.get("email").asText();
            String password = request.get("password").asText();

            Usuario nuevoUsuario = new Usuario();
            nuevoUsuario.setNombre(nombre);
            nuevoUsuario.setEmail(email);
            nuevoUsuario.setContraseña(password);
            nuevoUsuario.setBio(""); // 🔥 MODIFICADO: Inicializamos la bio como texto vacío en la base de datos
            nuevoUsuario.setTipoCuenta(Usuario.TipoCuenta.publica);
            nuevoUsuario.setRecibeNotificacion(true);
            nuevoUsuario.setUltimaConexion(LocalDateTime.now());

            Usuario usuarioGuardado = usuarioRepository.save(nuevoUsuario);

            response.put("status", "SUCCESS");
            response.put("id", usuarioGuardado.getId());
            response.put("name", usuarioGuardado.getNombre());
            response.put("email", usuarioGuardado.getEmail());
            response.put("bio", usuarioGuardado.getBio() != null ? usuarioGuardado.getBio() : ""); // 🔥 MODIFICADO: Enviamos la bio vacía inicial a Android
            response.put("isAdmin", false);
            response.put("token", "TOKEN_GENERADO_" + usuarioGuardado.getId());

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Error al registrar el usuario: " + e.getMessage());
        }
        return response;
    }

    public JsonNode login(JsonNode request) {
        ObjectNode response = mapper.createObjectNode();
        try {
            // Log para ver qué llega desde Android
            System.out.println("DEBUG SERVER: Login request recibido -> " + request.toString());

            String email = request.get("email").asText();
            String password = request.get("password").asText();

            Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);

            if (usuarioOpt.isEmpty()) {
                System.out.println("DEBUG SERVER: El email " + email + " no existe en BD.");
                response.put("status", "ERROR");
                response.put("message", "El correo electrónico no está registrado.");
                return response;
            }

            Usuario usuario = usuarioOpt.get();

            // Log crítico: Inspección directa del objeto cargado por Hibernate
            System.out.println("DEBUG SERVER: Usuario encontrado. ID: " + usuario.getId());
            System.out.println("DEBUG SERVER: Valor de isAdmin recuperado: " + usuario.getIsAdmin());
            System.out.println("DEBUG SERVER: Valor de isBanned recuperado: " + usuario.getIsBanned());

            if (!usuario.getContraseña().equals(password)) {
                System.out.println("DEBUG SERVER: Contraseña incorrecta para el usuario: " + email);
                response.put("status", "ERROR");
                response.put("message", "Contraseña incorrecta.");
                return response;
            }

            usuario.setUltimaConexion(LocalDateTime.now());
            usuarioRepository.save(usuario);

            // Verificamos antes de enviar
            System.out.println("DEBUG SERVER: Preparando respuesta exitosa con isAdmin = " + usuario.getIsAdmin());

            response.put("status", "SUCCESS");
            response.put("id", usuario.getId());
            response.put("name", usuario.getNombre());
            response.put("email", usuario.getEmail());
            response.put("bio", usuario.getBio() != null ? usuario.getBio() : "");
            response.put("isAdmin", usuario.getIsAdmin());
            response.put("isBanned", usuario.getIsBanned());
            response.put("token", "TOKEN_LOGUEADO_" + usuario.getId());

            System.out.println("DEBUG SERVER: Respuesta JSON generada -> " + response.toString());

        } catch (Exception e) {
            System.err.println("DEBUG SERVER: Excepción crítica en login: " + e.getMessage());
            e.printStackTrace(); // Esto te mostrará en qué línea exacta falla
            response.put("status", "ERROR");
            response.put("message", "Error crítico en el servidor durante el login: " + e.getMessage());
        }
        return response;
    }

    @Transactional
    public JsonNode crearEvento(JsonNode request) {
        ObjectNode response = mapper.createObjectNode();
        try {
            int userId = request.get("userId").asInt();
            Optional<Usuario> creadorOpt = usuarioRepository.findById(userId);

            if (creadorOpt.isEmpty()) {
                response.put("status", "ERROR");
                response.put("message", "El usuario creador no existe.");
                return response;
            }

            Usuario creador = creadorOpt.get();

            // 1. Crear el Evento
            Evento evento = new Evento();
            evento.setTitulo(request.get("title").asText());
            evento.setDescripcion(request.get("description").asText());
            evento.setUbicacion(request.get("ubicacion").asText());
            evento.setCreador(creador);
            evento.setFechaEvento(LocalDateTime.parse(request.get("fecha").asText()));
            evento.setAsistentes(new java.util.HashSet<>());
            evento.getAsistentes().add(creador);

            // 2. Crear el Chat Grupal asociado
            Chat chat = new Chat();
            chat.setTipo(Chat.TipoChat.grupal);
            chat.setFechaCreacion(LocalDateTime.now());
            chat = chatRepository.save(chat); // Guardamos para obtener el ID

            // 3. Vincular Chat al Evento (asumiendo que en Evento tienes un campo chat)
            evento.setChat(chat);

            // 4. Crear la membresía del creador en el Chat (UsuarioChat)
            UsuarioChat membresia = new UsuarioChat();
            membresia.setChat(chat);
            membresia.setUsuario(creador);
            membresia.setFecha(LocalDateTime.now());
            usuarioChatRepository.save(membresia);

            // 5. Guardar evento final
            Evento guardado = eventoRepository.save(evento);

            response.put("status", "SUCCESS");
            response.put("id", guardado.getId());
            response.put("chatId", chat.getId()); // Útil para que Android sepa a dónde navegar

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

            List<Evento> todos = eventoRepository.findAll();

            List<Evento> misEventos = todos.stream()
                    .filter(e -> e.getAsistentes().stream().anyMatch(u -> u.getId() == userId))
                    .filter(e -> e.getFechaEvento().isAfter(ahora))
                    .sorted((e1, e2) -> e1.getFechaEvento().compareTo(e2.getFechaEvento()))
                    .toList();

            ArrayNode arrayEventos = mapper.createArrayNode();

            for (Evento e : misEventos) {
                ObjectNode item = mapper.createObjectNode();
                item.put("id", e.getId());
                item.put("title", e.getTitulo());
                item.put("description", e.getDescripcion());
                item.put("ubicacion", e.getUbicacion());
                item.put("fecha", e.getFechaEvento().toString());
                item.put("organizer", e.getCreador().getNombre());
                item.put("participantsCount", e.getAsistentes().size());
                arrayEventos.add(item);
            }

            response.put("status", "SUCCESS");
            response.set("events", arrayEventos);
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Error al listar mis eventos: " + e.getMessage());
        }
        return response;
    }

    @Transactional
    public JsonNode eliminarEvento(JsonNode request) {
        ObjectNode response = mapper.createObjectNode();
        try {
            int idEvento = request.get("eventId").asInt();
            eventoRepository.deleteById(idEvento);
            response.put("status", "SUCCESS");
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Error al eliminar: " + e.getMessage());
        }
        return response;
    }

    @Transactional
    public JsonNode unirseEvento(JsonNode request) {
        ObjectNode response = mapper.createObjectNode();

        // 1. Extraemos los datos comunes fuera de los bloques if/else
        int userId = request.get("userId").asInt();
        int eventoId = request.get("eventId").asInt();
        boolean unirse = request.get("join").asBoolean();

        try {
            // Buscamos usuario y evento (común para ambos casos)
            Usuario usuario = usuarioRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            Evento evento = eventoRepository.findById(eventoId)
                    .orElseThrow(() -> new RuntimeException("Evento no encontrado"));

            if (unirse) {
                // --- LÓGICA DE UNIRSE ---
                evento.getAsistentes().add(usuario);
                eventoRepository.save(evento);

                Chat chatDelEvento = evento.getChat();
                if (chatDelEvento != null && !usuarioChatRepository.existsByUsuarioAndChat(usuario, chatDelEvento)) {
                    UsuarioChat membresia = new UsuarioChat();
                    membresia.setChat(chatDelEvento);
                    membresia.setUsuario(usuario);
                    membresia.setFecha(LocalDateTime.now());
                    usuarioChatRepository.save(membresia);
                }
                response.put("status", "SUCCESS");
                response.put("message", "Te has unido al evento.");

            } else {
                // --- LÓGICA DE DESAPUNTARSE ---
                evento.getAsistentes().remove(usuario);
                eventoRepository.save(evento);

                if (evento.getChat() != null) {
                    // 🔥 Asegúrate de que este método exista en tu interfaz UsuarioChatRepository
                    usuarioChatRepository.deleteByUsuarioAndChat(usuario, evento.getChat());
                }
                response.put("status", "SUCCESS");
                response.put("message", "Te has desapuntado del evento.");
            }
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Error al procesar asistencia: " + e.getMessage());
        }
        return response;
    }

    @Transactional
    public JsonNode listarEventos(JsonNode request) {
        ObjectNode response = mapper.createObjectNode();
        try {
            // Obtenemos el userId, si es -1 significa invitado no registrado
            int currentUserId = request.has("userId") ? request.get("userId").asInt() : -1;

            // Recomendación: usa findAll() solo si tienes pocos eventos.
            // Si la app crece, filtra en la BD (ej. por fecha).
            List<Evento> eventos = eventoRepository.findAll();
            ArrayNode arrayEventos = mapper.createArrayNode();

            for (Evento e : eventos) {
                ObjectNode item = mapper.createObjectNode();
                item.put("id", e.getId());
                item.put("title", e.getTitulo());
                item.put("description", e.getDescripcion());
                item.put("ubicacion", e.getUbicacion());
                item.put("fecha", e.getFechaEvento() != null ? e.getFechaEvento().toString() : "");

                // Verificación de nulidad para evitar NullPointerException si un evento no tuviera creador
                if (e.getCreador() != null) {
                    item.put("organizer", e.getCreador().getNombre());
                    item.put("creatorId", e.getCreador().getId());
                } else {
                    item.put("organizer", "Desconocido");
                    item.put("creatorId", 0);
                }

                // hibernate carga la colección 'asistentes' aquí gracias al @Transactional
                item.put("participantsCount", e.getAsistentes().size());

                boolean estaApuntado = e.getAsistentes().stream()
                        .anyMatch(u -> u.getId() == currentUserId);
                item.put("isUserJoined", estaApuntado);

                arrayEventos.add(item);
            }

            response.put("status", "SUCCESS");
            response.set("events", arrayEventos);
        } catch (Exception e) {
            // Es buena práctica imprimir el error en el servidor para debuggear
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
            // 1. Extraemos los parámetros que viajan en el JSON desde Android
            int userId = request.get("userId").asInt();
            String nuevoNombre = request.get("name").asText();
            String nuevaBio = request.get("bio").asText();

            // 2. Buscamos el registro en la base de datos
            Optional<Usuario> usuarioOpt = usuarioRepository.findById(userId);

            if (usuarioOpt.isEmpty()) {
                response.put("status", "ERROR");
                response.put("message", "El usuario no existe.");
                return response;
            }

            Usuario usuario = usuarioOpt.get();

            // 3. Modificamos los valores (ajusta los nombres de tus setters si se llaman distinto)
            usuario.setNombre(nuevoNombre);
            // Nota: Asegúrate de tener el campo 'bio' o 'descripcion' en tu entidad Usuario
            usuario.setBio(nuevaBio);

            // 4. Guardamos los cambios
            usuarioRepository.save(usuario);

            // 5. Respondemos con éxito
            response.put("status", "SUCCESS");
            response.put("message", "Perfil actualizado correctamente en MySQL");

        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "ERROR");
            response.put("message", "Error en el servidor al actualizar perfil: " + e.getMessage());
        }
        return response;
    }
}