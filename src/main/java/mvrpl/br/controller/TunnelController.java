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

    @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<String> handleTunnelRequest(
            @RequestBody(required = false) String body,
            HttpMethod method,
            HttpServletRequest request) {
        
        try {
            String path = request.getRequestURI().substring("/tunnel".length());
            
            HttpHeaders headers = new HttpHeaders();
            Collections.list(request.getHeaderNames()).forEach(headerName -> 
                headers.put(headerName, Collections.list(request.getHeaders(headerName)))
            );

            String targetIp = "192.168.1.171"; // IP do servidor remoto
            int targetPort = 8087; // Porta do serviço no servidor remoto

            if (!isPortOpen(targetIp, targetPort, 2000)) {
                return new ResponseEntity<>("Serviço de destino indisponível no IP/Porta especificados.", HttpStatus.SERVICE_UNAVAILABLE);
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