package mvrpl.br.controller;

import mvrpl.br.service.SshTunnelService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import com.jcraft.jsch.JSchException;

import java.io.IOException;
import java.util.Collections;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

@RestController
@RequestMapping("/tunnel")
public class TunnelController {

    private final SshTunnelService sshTunnelService;

    public TunnelController(SshTunnelService sshTunnelService) {
        this.sshTunnelService = sshTunnelService;
    }

    public static boolean isPortOpen(String ipAddress, int port, int timeoutMillis) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ipAddress, port), timeoutMillis);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @RequestMapping(value = "/airflow/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<String> handleTunnelRequestAirflow(
            @RequestBody(required = false) String body,
            HttpMethod method,
            HttpServletRequest request) {
        
        try {
            String path = request.getRequestURI().substring("/tunnel/airflow".length());
            
            HttpHeaders headers = new HttpHeaders();
            Collections.list(request.getHeaderNames()).forEach(headerName -> 
                headers.put(headerName, Collections.list(request.getHeaders(headerName)))
            );

            String targetIp = "56.124.51.250"; // IP do servidor remoto
            int targetPort = 8080; // Porta do serviço no servidor remoto

            if (!isPortOpen(targetIp, 22, 2000)) {
                return new ResponseEntity<>("Servidor SSH indisponível.", HttpStatus.SERVICE_UNAVAILABLE);
            }

            if (!sshTunnelService.checkRemotePortOpen(InetAddress.getByName(targetIp), targetPort, 2000)) {
                return new ResponseEntity<>("Porta do serviço remoto fechada.", HttpStatus.SERVICE_UNAVAILABLE);
            }

            return sshTunnelService.forwardRequest(path, method, body, headers, InetAddress.getByName(targetIp), targetPort);
        } catch (JSchException | IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>("Falha ao estabelecer o túnel SSH: " + e.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Erro interno ao processar a requisição: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/vscode/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<String> handleTunnelRequestVSCode(
            @RequestBody(required = false) String body,
            HttpMethod method,
            HttpServletRequest request) {
        
        try {
            String path = request.getRequestURI().substring("/tunnel/vscode".length());
            
            HttpHeaders headers = new HttpHeaders();
            Collections.list(request.getHeaderNames()).forEach(headerName -> 
                headers.put(headerName, Collections.list(request.getHeaders(headerName)))
            );

            String targetIp = "56.124.51.250"; // IP do servidor remoto
            int targetPort = 8888; // Porta do serviço no servidor remoto

            if (!isPortOpen(targetIp, 22, 2000)) {
                return new ResponseEntity<>("Servidor SSH indisponível.", HttpStatus.SERVICE_UNAVAILABLE);
            }

            if (!sshTunnelService.checkRemotePortOpen(InetAddress.getByName(targetIp), targetPort, 2000)) {
                return new ResponseEntity<>("Porta do serviço remoto fechada.", HttpStatus.SERVICE_UNAVAILABLE);
            }

            return sshTunnelService.forwardRequest(path, method, body, headers, InetAddress.getByName(targetIp), targetPort);
        } catch (JSchException | IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>("Falha ao estabelecer o túnel SSH: " + e.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Erro interno ao processar a requisição: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}