package mvrpl.br.controller;

import mvrpl.br.service.SshTunnelService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;

import java.io.IOException;
import java.util.Collections;
import java.net.InetAddress;

@RestController
@RequestMapping("/tunnel")
public class TunnelController {

    private final SshTunnelService sshTunnelService;

    public TunnelController(SshTunnelService sshTunnelService) {
        this.sshTunnelService = sshTunnelService;
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

            return sshTunnelService.forwardRequest(path, method, body, headers, InetAddress.getByName("192.168.1.171"), 8087);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>("Falha ao estabelecer o túnel SSH: " + e.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Erro interno ao processar a requisição: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}