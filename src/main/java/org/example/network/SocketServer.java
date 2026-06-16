package org.example.network;

import org.example.ClientHandler;
import org.example.bd.repository.AsistenciaRepository;
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
    private final EventoRepository eventoRepository;
    private final ChatRepository chatRepository;
    private final MensajeRepository mensajeRepository;
    private final UsuarioChatRepository usuarioChatRepository;
    private final AsistenciaRepository asistenciaRepository;

    public SocketServer(
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

    public void start() {
        ServerServices serverServices = new ServerServices(
                usuarioRepository,
                eventoRepository,
                chatRepository,
                mensajeRepository,
                usuarioChatRepository,
                asistenciaRepository
        );
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(9000)) {
                System.out.println("=== Servidor de Sockets TCP escuchando en el puerto 9000 ===");

                while (true) {
                    Socket client = serverSocket.accept();
                    System.out.println("¡Conexión aceptada desde " + client.getInetAddress() + "!");

                    ClientHandler handler = new ClientHandler(client, serverServices);

                    new Thread(handler).start();
                }

            } catch (Exception e) {
                System.err.println("Error crítico en el ServerSocket: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }
}