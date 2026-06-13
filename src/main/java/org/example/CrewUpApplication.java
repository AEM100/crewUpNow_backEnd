package org.example;

import org.example.bd.repository.ChatRepository;
import org.example.bd.repository.EventoRepository;
import org.example.bd.repository.MensajeRepository;
import org.example.bd.repository.UsuarioChatRepository;
import org.example.bd.repository.UsuarioRepository;
import org.example.network.SocketServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CrewUpApplication implements CommandLineRunner {

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private EventoRepository eventoRepository;
    @Autowired
    private ChatRepository chatRepository;
    @Autowired
    private MensajeRepository mensajeRepository;
    @Autowired
    private UsuarioChatRepository usuarioChatRepository;

    public static void main(String[] args) {
        SpringApplication.run(CrewUpApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Iniciando componentes del servidor...");

        //  ahora le pasamos los 5 repositorios inyectados al SocketServer
        SocketServer socketServer = new SocketServer(
                usuarioRepository,
                eventoRepository,
                chatRepository,
                mensajeRepository,
                usuarioChatRepository
        );

        // Arrancamos el socket
        socketServer.start();
    }
}