package org.example.network;

import org.example.ClientHandler;
import org.example.bd.repository.ChatRepository;
import org.example.bd.repository.EventoRepository;
import org.example.bd.repository.MensajeRepository;
import org.example.bd.repository.UsuarioChatRepository;
import org.example.bd.repository.UsuarioRepository;
import org.example.repository.ServerServices;

import java.net.ServerSocket;
import java.net.Socket;

public class SocketServer {

    private final UsuarioRepository usuarioRepository;
    private final  EventoRepository eventoRepository;
    private final ChatRepository chatRepository;
    private final MensajeRepository mensajeRepository;
    private final UsuarioChatRepository usuarioChatRepository;
    // El constructor ahora recibe el repositorio desde fuera
    public SocketServer(
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
    }

    public void start() {
        // Creamos la instancia de tu servicio pasándole el repositorio que recibimos
        ServerServices serverServices = new ServerServices(
                usuarioRepository,
                eventoRepository,
                chatRepository,
                mensajeRepository,
                usuarioChatRepository
        );
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(9000)) {
                System.out.println("=== Servidor de Sockets TCP escuchando en el puerto 9000 ===");

                while (true) {
                    // Se detiene aquí hasta que Android intente conectarse
                    Socket client = serverSocket.accept();
                    System.out.println("¡Conexión aceptada desde " + client.getInetAddress() + "!");

                    // Creamos el handler para este cliente específico
                    ClientHandler handler = new ClientHandler(client, serverServices);

                    // Lo lanzamos en un hilo independiente
                    new Thread(handler).start();
                }

            } catch (Exception e) {
                System.err.println("Error crítico en el ServerSocket: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }
}