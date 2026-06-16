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
    private final ObjectMapper mapper = new ObjectMapper();

    public ClientHandler(Socket socket, ServerServices serverServices) {
        this.socket = socket;
        this.serverServices = serverServices;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            System.out.println("Handler asignado al cliente: " + socket.getInetAddress());

            String jsonRecibido;
            while ((jsonRecibido = in.readLine()) != null) {
                System.out.println("JSON crudo recibido de Android: " + jsonRecibido);

                JsonNode request = mapper.readTree(jsonRecibido);

                if (!request.has("action")) {
                    out.println("{\"status\":\"ERROR\",\"message\":\"Falta el campo action\"}");
                    continue;
                }

                String action = request.get("action").asText();
                JsonNode response = null;

                switch (action) {
                    case "REGISTER":
                        response = serverServices.register(request);
                        break;

                    case "LOGIN":
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

                    case "FETCH_EVENTS":
                        response = serverServices.listarEventos(request);
                        break;
                    case "FETCH_MY_EVENTS":
                        response = serverServices.listarMisProximosEventos(request);
                        break;
                    case "DELETE_ACCOUNT":
                        try {
                            int userId = request.get("userId").asInt();


                            response = serverServices.eliminarCuenta(userId);

                        } catch (Exception e) {
                            e.printStackTrace();
                            ObjectNode errorNode = mapper.createObjectNode();
                            errorNode.put("status", "ERROR");
                            errorNode.put("message", "Error al procesar el borrado: " + e.getMessage());
                            response = errorNode;
                        }
                        break;
                    case "BAN_USER":
                        try {
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
                        response = serverServices.listarMisChats(request);
                        break;
                    case "UPDATE_PROFILE":
                        try {
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
                        response = serverServices.cargarMensajes(request);
                        break;

                    case "SEND_CHAT_MESSAGE":
                        response = serverServices.enviarMensaje(request);
                        break;


                    default:
                        ObjectNode errorNode = mapper.createObjectNode();
                        errorNode.put("status", "ERROR");
                        errorNode.put("message", "Acción '" + action + "' no implementada en el servidor.");
                        response = errorNode;
                        break;
                }

                String jsonRespuesta = mapper.writeValueAsString(response);
                System.out.println("Enviando respuesta a Android: " + jsonRespuesta);

                out.println(jsonRespuesta);
            }

        } catch (Exception e) {
            System.out.println("El cliente " + socket.getInetAddress() + " ha cerrado la conexión o hubo un error.");
        } finally {
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