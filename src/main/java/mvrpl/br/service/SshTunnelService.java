package mvrpl.br.service;

import com.jcraft.jsch.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.io.InputStream;

@Service
public class SshTunnelService {

    private static final String LOCALHOST = "127.0.0.1";

    @Value("${ssh.port}")
    private int sshPort;

    @Value("${ssh.user}")
    private String sshUser;

    @Value("${ssh.privatekey.path}")
    private String privateKeyPath;

    public boolean checkRemotePortOpen(InetAddress remoteHost, int remotePort, int timeoutMillis) throws JSchException, IOException, InterruptedException {
        Session session = this.connectSSH(remoteHost, this.sshPort);
        session.connect(timeoutMillis);

        String command = String.format("nc -z 127.0.0.1 %d", remotePort);

        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);
        channel.setInputStream(null);

        channel.connect();

        Thread.sleep(500); // Aguarda um pouco para o comando ser executado

        channel.disconnect();
        session.disconnect();

        return channel.getExitStatus() == 0;
    }

    private Session connectSSH(InetAddress ipAddress, int port) throws JSchException {
        JSch jsch = new JSch();
        jsch.addIdentity(this.privateKeyPath);
        Session session = jsch.getSession(sshUser, ipAddress.getHostAddress(), port);
        session.setConfig("StrictHostKeyChecking", "no");
        return session;
    }

    /**
     * Encaminha uma requisição HTTP através de um túnel SSH.
     * @param path O caminho do endpoint a ser chamado no serviço de destino (ex: /api/users).
     * @param method O método HTTP (GET, POST, etc.).
     * @param requestBody O corpo da requisição (para POST, PUT, etc.).
     * @param headers Os cabeçalhos da requisição original.
     * @param remoteHost O endereço IP do servidor remoto.
     * @param remotePort A porta do serviço no servidor remoto.
     * @return A resposta do serviço de destino.
     */
    public ResponseEntity<String> forwardRequest(String path, HttpMethod method, String requestBody, HttpHeaders headers, InetAddress remoteHost, int remotePort) throws JSchException, IOException {
        Session session = this.connectSSH(remoteHost, this.sshPort);
        session.connect(2000);
        
        try {
            int localPort = session.setPortForwardingL(0, LOCALHOST, remotePort);

            RestTemplate restTemplate = new RestTemplate();
            String targetUrl = "http://" + LOCALHOST + ":" + localPort + path;

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            
            System.out.println("Forwarding request to: " + targetUrl);

            return restTemplate.exchange(targetUrl, method, entity, String.class);
        } finally {
            session.disconnect();
        }
    }
}