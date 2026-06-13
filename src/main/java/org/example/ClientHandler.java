package org.example;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.repository.ServerServices;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ServerServices serverServices;
    private final ObjectMapper mapper = new ObjectMapper(); // Herramienta de Jackson para JSON

    // El constructor recibe el socket del cliente y el servicio con la base de datos
    public ClientHandler(Socket socket, ServerServices serverServices) {
        this.socket = socket;
        this.serverServices = serverServices;
    }

    @Override
    public void run() {
        // Inicializamos la "oreja" (BufferedReader) y la "boca" (PrintWriter) del socket
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            System.out.println("Handler asignado al cliente: " + socket.getInetAddress());

            String jsonRecibido;
            // El bucle se queda escuchando de forma permanente hasta que el cliente se desconecte
            while ((jsonRecibido = in.readLine()) != null) {
                System.out.println("JSON crudo recibido de Android: " + jsonRecibido);

                // 1. Convertir el texto a un nodo JSON para leerlo
                JsonNode request = mapper.readTree(jsonRecibido);

                // Validamos que el JSON tenga el campo "action"
                if (!request.has("action")) {
                    out.println("{\"status\":\"ERROR\",\"message\":\"Falta el campo action\"}");
                    continue;
                }

                String action = request.get("action").asText();
                JsonNode response = null;

                // 2. Nuestro "despachador" (El switch que centraliza tus 8 acciones futuras)
                switch (action) {
                    case "REGISTER":
                        // Le pasamos el JSON entero a tu servicio
                        response = serverServices.register(request);
                        break;

                    case "LOGIN": // 🔥 NUEVO CASO
                        response = serverServices.login(request);
                        break;
                    case "CREATE_EVENT":
                        response = serverServices.crearEvento(request);
                        break;

                    case "DELETE_EVENT":
                        response = serverServices.eliminarEvento(request);
                        break;

                    case "TOGGLE_JOIN":
                        response = serverServices.unirseEvento(request);
                        break;

                    case "FETCH_EVENTS": // El que añadimos para cargar el mapa al iniciar
                        response = serverServices.listarEventos(request);
                        break;
                    case "FETCH_MY_EVENTS":
                        response = serverServices.listarMisProximosEventos(request);
                        break;
                    // Dentro del switch (action) de tu servidor Java:
                    case "DELETE_ACCOUNT":
                        try {
                            // 1. Extraemos el userId usando Jackson (que es lo que usa tu clase)
                            int userId = request.get("userId").asInt();

                            // 2. Le pedimos a tu capa de servicios que lo borre de MySQL
                            // (Crearemos este método en tus ServerServices justo abajo)
                            response = serverServices.eliminarCuenta(userId);

                        } catch (Exception e) {
                            e.printStackTrace();
                            // Si algo falla al procesar el JSON, creamos un nodo de error de Jackson
                            ObjectNode errorNode = mapper.createObjectNode();
                            errorNode.put("status", "ERROR");
                            errorNode.put("message", "Error al procesar el borrado: " + e.getMessage());
                            response = errorNode;
                        }
                        break;
                    case "BAN_USER":
                        try {
                            // Delegamos al nuevo método que acabamos de crear
                            response = serverServices.banearUsuario(request);
                        } catch (Exception e) {
                            e.printStackTrace();
                            ObjectNode errorNode = mapper.createObjectNode();
                            errorNode.put("status", "ERROR");
                            errorNode.put("message", "Error en la acción BAN_USER: " + e.getMessage());
                            response = errorNode;
                        }
                        break;
                    case "FETCH_CHAT_LIST":
                        // Llama al método de servicios para listar los chats grupales/individuales del usuario
                        response = serverServices.listarMisChats(request);
                        break;
                    case "UPDATE_PROFILE":
                        try {
                            // Delegamos el JSON recibido directamente a tu clase de servicios
                            response = serverServices.actualizarPerfil(request);
                        } catch (Exception e) {
                            e.printStackTrace();
                            ObjectNode errorNode = mapper.createObjectNode();
                            errorNode.put("status", "ERROR");
                            errorNode.put("message", "Error al procesar la actualización: " + e.getMessage());
                            response = errorNode;
                        }
                        break;
                    case "FETCH_CHAT_MESSAGES":
                        // Llama al método para cargar todo el histórico de mensajes de un chat concreto
                        response = serverServices.cargarMensajes(request);
                        break;

                    case "SEND_CHAT_MESSAGE":
                        // Llama al método para insertar un nuevo mensaje en la base de datos
                        response = serverServices.enviarMensaje(request);
                        break;
                    // Aquí irás añadiendo tus otros casos más adelante:
                    // case "LOGIN": ...
                    // case "SEND_MESSAGE": ...

                    default:
                        // Si mandan una acción que no existe todavía
                        ObjectNode errorNode = mapper.createObjectNode();
                        errorNode.put("status", "ERROR");
                        errorNode.put("message", "Acción '" + action + "' no implementada en el servidor.");
                        response = errorNode;
                        break;
                }

                // 3. Convertir la respuesta del servicio a String JSON y mandarla a Android
                String jsonRespuesta = mapper.writeValueAsString(response);
                System.out.println("Enviando respuesta a Android: " + jsonRespuesta);

                out.println(jsonRespuesta); // Envía y añade el '\n' clave
            }

        } catch (Exception e) {
            System.out.println("El cliente " + socket.getInetAddress() + " ha cerrado la conexión o hubo un error.");
        } finally {
            // Nos aseguramos de cerrar el socket si sale del bucle
            try {
                if (!socket.isClosed()) {
                    socket.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}