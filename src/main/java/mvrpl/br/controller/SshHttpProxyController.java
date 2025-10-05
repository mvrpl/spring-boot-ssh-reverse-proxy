package mvrpl.br.controller;

import com.jcraft.jsch.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.*;

@RestController
@RequestMapping("/proxy")
public class SshHttpProxyController {

    String privateKeyPath = "C:\\Users\\mvrpl\\.ssh\\id_rsa";

    @RequestMapping("/**")
    public void proxy(HttpServletRequest request, HttpServletResponse response) throws Exception {
        JSch jsch = new JSch();
        jsch.addIdentity(this.privateKeyPath /*, "optional-passphrase" */);
        Session session = jsch.getSession("root", "192.168.1.171", 22);
        //session.setPassword("ssh-password");
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        ChannelDirectTCPIP channel = (ChannelDirectTCPIP) session.openChannel("direct-tcpip");
        channel.setHost("127.0.0.1");
        channel.setPort(8087);

        InputStream in = channel.getInputStream();
        OutputStream out = channel.getOutputStream();
        channel.connect();

        String method = request.getMethod();
        String path = request.getRequestURI().replaceFirst("/proxy", "");
        StringBuilder httpRequest = new StringBuilder();
        httpRequest.append(method).append(" ").append(path).append(" HTTP/1.1\n");
        httpRequest.append("Host: 127.0.0.1\n");

        Collections.list(request.getHeaderNames()).forEach(h -> {
            if (!h.equalsIgnoreCase("host")) {
                httpRequest.append(h).append(": ").append(request.getHeader(h)).append("\n");
            }
        });
        httpRequest.append("\n");

        out.write(httpRequest.toString().getBytes());
        out.flush();

        try (InputStream reqBody = request.getInputStream()) {
            reqBody.transferTo(out);
        }
        out.flush();

        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String statusLine = reader.readLine();
        if (statusLine == null) {
            response.setStatus(502);
            return;
        }

        String[] statusParts = statusLine.split(" ", 3);
        try {
            response.setStatus(Integer.parseInt(statusParts[1]));
        } catch (Exception e) {
            response.setStatus(502);
            return;
        }

        String line;
        while (!(line = reader.readLine()).isEmpty()) {
            int idx = line.indexOf(":");
            if (idx > 0) {
                String name = line.substring(0, idx).trim();
                String value = line.substring(idx + 1).trim();
                response.addHeader(name, value);
            }
        }

        OutputStream clientOut = response.getOutputStream();
        char[] buf = new char[8192];
        int len;
        Writer writer = new OutputStreamWriter(clientOut);
        while ((len = reader.read(buf)) != -1) {
            writer.write(buf, 0, len);
        }
        writer.flush();

        channel.disconnect();
        session.disconnect();
    }
}
