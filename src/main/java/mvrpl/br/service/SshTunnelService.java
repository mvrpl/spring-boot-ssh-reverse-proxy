package mvrpl.br.service;

import com.jcraft.jsch.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.InetAddress;

@Service
public class SshTunnelService {

    @Value("${ssh.port}")
    private int sshPort;

    @Value("${ssh.user}")
    private String sshUser;

    @Value("${ssh.privatekey.path}")
    private String privateKeyPath;

    /**
     * Encaminha uma requisição HTTP através de um túnel SSH.
     * @param path O caminho do endpoint a ser chamado no serviço de destino (ex: /api/users).
     * @param method O método HTTP (GET, POST, etc.).
     * @param requestBody O corpo da requisição (para POST, PUT, etc.).
     * @param headers Os cabeçalhos da requisição original.
     * @return A resposta do serviço de destino.
     */
    public ResponseEntity<String> forwardRequest(String path, HttpMethod method, String requestBody, HttpHeaders headers, InetAddress remoteHost, int remotePort) throws JSchException, IOException {
        JSch jsch = new JSch();
        jsch.addIdentity(this.privateKeyPath /*, "optional-passphrase" */);
        Session session = jsch.getSession(this.sshUser, remoteHost.getHostAddress(), this.sshPort);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        int localPort;
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            localPort = serverSocket.getLocalPort();
        }

        session.setPortForwardingL(localPort, "127.0.0.1", remotePort);
        
        try {
            RestTemplate restTemplate = new RestTemplate();
            String targetUrl = "http://127.0.0.1:" + localPort + path;
            
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            
            System.out.println("Forwarding request to: " + targetUrl);

            return restTemplate.exchange(targetUrl, method, entity, String.class);
        } finally {
            session.disconnect();
        }
    }
}